const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

// [1] Cari Anggota berdasarkan Nama atau NIM
exports.searchMembers = async (req, res) => {
  const { query } = req.query; 

  if (!query || query.length < 1) {
    return res.json([]);
  }

  try {
    const members = await prisma.anggota.findMany({
      where: {
        OR: [
          {
            name: {
              contains: query   // mode dihapus
            }
          },
          {
            nim: {
              contains: query
            }
          }
        ]
      },
      select: { 
        nim: true,
        name: true,
        email: true 
      },
      take: 5
    });

    // Mapping ke bentuk yang diharapkan frontend
    res.json(
      members.map(m => ({
        nim: m.nim,
        nama: m.name,
        email: m.email
      }))
    );
  } catch (error) {
    console.error("Error searchMembers:", error);
    res.status(500).json({ error: "Gagal mencari anggota" });
  }
};


// [2] Ambil Peminjaman Aktif (Buku yang belum dikembalikan oleh Anggota)
exports.getActiveBorrowings = async (req, res) => {
  const { nim } = req.params;
  
  try {
    const activeLoans = await prisma.peminjaman.findMany({
      where: {
        anggotaNim: nim,
        status: 'DIPINJAM' 
      },
      select: {
          id: true,
          tanggalPinjam: true,
          jatuhTempo: true,
          buku: {
              select: {
                  id: true,
                  title: true, // Nama field yang diambil
                  author: true
              }
          }
      }
    });
    
    // SINKRONISASI: Format tanggal ke dd/MM/yyyy dan ubah nama field
    const formattedLoans = activeLoans.map(loan => {
        const dateFormatter = (date) => new Date(date).toLocaleDateString('id-ID', { day: '2-digit', month: '2-digit', year: 'numeric' });
        
        return {
            id: loan.id,
            // Perubahan nama field agar sinkron dengan Frontend (PeminjamanResponse)
            judulBuku: loan.buku.title, 
            tglPinjam: dateFormatter(loan.tanggalPinjam),
            jatuhTempo: dateFormatter(loan.jatuhTempo),
            author: loan.buku.author // Tambahkan author untuk tampilan detail
        };
    });
    
    res.json(formattedLoans);
  } catch (error) {
    console.error("Error getActiveBorrowings:", error);
    res.status(500).json({ error: "Gagal mengambil data peminjaman" });
  }
};


// [3] Proses Pengembalian (Create Return)
exports.createReturn = async (req, res) => {
  // Ambil semua data dari body request
  const { 
    peminjamanId, 
    tanggalPengembalian, 
    denda,
    // >>> FIELD BARU DARI FRONTEND <<<
    buktiKerusakanUrl, // Opsional
    keterangan         // Opsional
    // ----------------------------------
  } = req.body;

  // Cek ID Peminjaman harus ada
  if (!peminjamanId) {
     return res.status(400).json({ success: false, message: 'ID Peminjaman tidak boleh kosong.' });
  }

  try {
    // Gunakan Transaction untuk memastikan konsistensi (Buat Pengembalian & Update Stok & Status)
    const result = await prisma.$transaction(async (tx) => {
      
      const pId = parseInt(peminjamanId);
      const loanDenda = parseInt(denda || 0);

      // 1. Buat record di tabel Pengembalian
      const newReturn = await tx.pengembalian.create({
        data: {
          peminjamanId: pId,
          tanggalPengembalian: new Date(tanggalPengembalian),
          denda: loanDenda,
          buktiKerusakanUrl: buktiKerusakanUrl || null, // <<< Menerima data opsional
          keterangan: keterangan || null,               // <<< Menerima data opsional
        }
      });

      // 2. Ambil data peminjaman untuk tahu buku apa yang dikembalikan
      const peminjaman = await tx.peminjaman.findUnique({
        where: { id: pId }
      });
      
      // Safety check: Pastikan peminjaman ditemukan
      if (!peminjaman) {
          throw new Error(`Peminjaman dengan ID ${pId} tidak ditemukan.`);
      }

      // 3. Tambahkan stok buku (+1)
      await tx.buku.update({
        where: { id: peminjaman.bukuId },
        data: { stok: { increment: 1 } }
      });
      
      // 4. Update Status Peminjaman terkait menjadi DIKEMBALIKAN
      await tx.peminjaman.update({
          where: { id: pId },
          data: { status: 'DIKEMBALIKAN' }
      });

      return newReturn;
    });

    res.status(201).json({ success: true, data: result });
  } catch (error) {
    console.error("Error createReturn:", error);
    res.status(500).json({ success: false, message: "Gagal memproses pengembalian", detail: error.message });
  }
};
// [4] Ambil Riwayat Pengembalian (join Pengembalian + Peminjaman + Anggota + Buku)
exports.getReturnHistory = async (req, res) => {
  try {
    const records = await prisma.pengembalian.findMany({
      orderBy: { tanggalPengembalian: 'desc' },
      include: {
        peminjaman: {
          include: {
            buku: true,
            anggota: true
          }
        }
      }
    });

    const formatDate = (date) =>
      new Date(date).toLocaleDateString('id-ID', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      });

    const result = records.map(r => ({
      id: r.id,                                         // ID pengembalian
      peminjamanId: r.peminjamanId,                    // FK ke Peminjaman
      judulBuku: r.peminjaman.buku.title,
      namaAnggota: r.peminjaman.anggota.name,
      nimAnggota: r.peminjaman.anggota.nim,
      tanggalPinjam: formatDate(r.peminjaman.tanggalPinjam),
      jatuhTempo: formatDate(r.peminjaman.jatuhTempo),
      tanggalPengembalian: formatDate(r.tanggalPengembalian),
      denda: r.denda,
      keterangan: r.keterangan,
      buktiKerusakanUrl: r.buktiKerusakanUrl
    }));

    res.json(result);
  } catch (error) {
    console.error("Error getReturnHistory:", error);
    res.status(500).json({ error: "Gagal mengambil riwayat pengembalian" });
  }
};

exports.deleteReturn = async (req, res) => {
  const { id } = req.params;
  const returnId = parseInt(id, 10);

  if (isNaN(returnId)) {
    return res.status(400).json({ success: false, message: "ID tidak valid" });
  }

  try {
    const existing = await prisma.pengembalian.findUnique({
      where: { id: returnId }
    });

    if (!existing) {
      return res.status(404).json({ success: false, message: "Data pengembalian tidak ditemukan" });
    }

    await prisma.pengembalian.delete({
      where: { id: returnId }
    });

    return res.json({ success: true, message: "Pengembalian berhasil dihapus" });
  } catch (error) {
    console.error("Error deleteReturn:", error);
    return res.status(500).json({ success: false, message: "Gagal menghapus pengembalian" });
  }
};

// [6] Update data pengembalian
exports.updateReturn = async (req, res) => {
  const { returnId } = req.params;
  const {
    peminjamanId,
    tanggalPengembalian,
    denda,
    buktiKerusakanUrl,
    keterangan
  } = req.body;

  const id = parseInt(returnId, 10);
  const pinjamId = parseInt(peminjamanId, 10);
  const dendaInt = parseInt(denda || 0, 10);

  if (isNaN(id) || isNaN(pinjamId)) {
    return res.status(400).json({ success: false, message: "ID tidak valid" });
  }

  try {
    // Konversi "dd/MM/yyyy" ke Date
    let tanggalJs;
    if (tanggalPengembalian) {
      const [dd, mm, yyyy] = tanggalPengembalian.split("/");
      tanggalJs = new Date(`${yyyy}-${mm}-${dd}T00:00:00.000Z`);
    } else {
      tanggalJs = new Date();
    }

    const updated = await prisma.pengembalian.update({
      where: { id },
      data: {
        tanggalPengembalian: tanggalJs,
        denda: dendaInt,
        buktiKerusakanUrl: buktiKerusakanUrl || null,
        keterangan: keterangan || null
      }
    });

    return res.json({ success: true, data: updated });
  } catch (error) {
    console.error("Error updateReturn:", error);
    return res.status(500).json({
      success: false,
      message: "Gagal mengupdate pengembalian",
      detail: error.message
    });
  }
};
