const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();
const path = require('path');
const fs = require('fs');

// Helper: Parse tanggal format dd/MM/yyyy ke Date
function parseTanggalIndonesia(dateStr) {
    if (!dateStr || typeof dateStr !== 'string') {
        return new Date();
    }

    const parts = dateStr.split('/');
    if (parts.length !== 3) {
        return new Date(dateStr);
    }

    const [dd, MM, yyyy] = parts;
    const day = Number(dd);
    const month = Number(MM) - 1;
    const year = Number(yyyy);

    if (isNaN(day) || isNaN(month) || isNaN(year)) {
        return new Date(dateStr);
    }

    return new Date(Date.UTC(year, month, day, 0, 0, 0));
}

// Helper: Format Date ke dd/MM/yyyy
function formatTanggal(date) {
    return new Date(date).toLocaleDateString('id-ID', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
}

// ============================================================
// [1] GET ALL PEMINJAMAN
// ============================================================
exports.getAllPeminjaman = async (req, res) => {
    try {
        const peminjaman = await prisma.peminjaman.findMany({
            orderBy: { tanggalPinjam: 'desc' },
            include: {
                buku: {
                    select: {
                        id: true,
                        title: true,
                        author: true,
                        stok: true
                    }
                },
                anggota: {
                    select: {
                        nim: true,
                        name: true,
                        major: true,
                        contact: true
                    }
                },
                admin: {
                    select: {
                        id: true,
                        name: true
                    }
                }
            }
        });

        // Format response sesuai dengan Borrowing.kt di Android
        const result = peminjaman.map(p => ({
            id: p.id,
            borrowerName: p.anggota.name,
            nim: p.anggotaNim,
            major: p.anggota.major || '',
            contact: p.anggota.contact || '',
            bookTitle: p.buku.title,
            author: p.buku.author || '',
            stok: p.buku.stok || 0,
            isbn: '', // Tidak ada di DB
            identityPath: p.krsImagePath || '',
            borrowDate: formatTanggal(p.tanggalPinjam),
            returnDate: formatTanggal(p.jatuhTempo),
            status: p.status,
            bukuId: p.bukuId,
            adminName: p.admin?.name || null
        }));

        res.json(result);
    } catch (error) {
        console.error('Error getAllPeminjaman:', error);
        res.status(500).json({ error: 'Gagal mengambil data peminjaman' });
    }
};

// ============================================================
// [2] SEARCH PEMINJAMAN
// ============================================================
exports.searchPeminjaman = async (req, res) => {
    const { q } = req.query;

    if (!q || q.length < 1) {
        return res.json([]);
    }

    try {
        const peminjaman = await prisma.peminjaman.findMany({
            where: {
                OR: [
                    { anggota: { name: { contains: q } } },
                    { anggota: { nim: { contains: q } } },
                    { buku: { title: { contains: q } } }
                ]
            },
            orderBy: { tanggalPinjam: 'desc' },
            include: {
                buku: {
                    select: {
                        id: true,
                        title: true,
                        author: true
                    }
                },
                anggota: {
                    select: {
                        nim: true,
                        name: true,
                        major: true,
                        contact: true
                    }
                }
            }
        });

        const result = peminjaman.map(p => ({
            id: p.id,
            borrowerName: p.anggota.name,
            nim: p.anggotaNim,
            major: p.anggota.major || '',
            contact: p.anggota.contact || '',
            bookTitle: p.buku.title,
            author: p.buku.author || '',
            isbn: '',
            identityPath: p.krsImagePath || '',
            borrowDate: formatTanggal(p.tanggalPinjam),
            returnDate: formatTanggal(p.jatuhTempo),
            status: p.status,
            bukuId: p.bukuId
        }));

        res.json(result);
    } catch (error) {
        console.error('Error searchPeminjaman:', error);
        res.status(500).json({ error: 'Gagal mencari data peminjaman' });
    }
};

// ============================================================
// [3] GET PEMINJAMAN BY ID
// ============================================================
exports.getPeminjamanById = async (req, res) => {
    const { id } = req.params;
    const peminjamanId = parseInt(id, 10);

    if (isNaN(peminjamanId)) {
        return res.status(400).json({ error: 'ID tidak valid' });
    }

    try {
        const p = await prisma.peminjaman.findUnique({
            where: { id: peminjamanId },
            include: {
                buku: true,
                anggota: true,
                admin: true
            }
        });

        if (!p) {
            return res.status(404).json({ error: 'Peminjaman tidak ditemukan' });
        }

        const result = {
            id: p.id,
            borrowerName: p.anggota.name,
            nim: p.anggotaNim,
            major: p.anggota.major || '',
            contact: p.anggota.contact || '',
            bookTitle: p.buku.title,
            author: p.buku.author || '',
            isbn: '',
            identityPath: p.krsImagePath || '',
            borrowDate: formatTanggal(p.tanggalPinjam),
            returnDate: formatTanggal(p.jatuhTempo),
            status: p.status,
            bukuId: p.bukuId,
            adminName: p.admin?.name || null
        };

        res.json(result);
    } catch (error) {
        console.error('Error getPeminjamanById:', error);
        res.status(500).json({ error: 'Gagal mengambil data peminjaman' });
    }
};

// ============================================================
// [4] CREATE PEMINJAMAN
// ============================================================
exports.createPeminjaman = async (req, res) => {
    const {
        nim,           // NIM anggota yang meminjam
        bukuId,        // ID buku yang dipinjam
        tanggalPinjam, // Format: dd/MM/yyyy (opsional, default: sekarang)
        jatuhTempo,    // Format: dd/MM/yyyy (wajib)
        adminId        // ID admin yang mencatat (opsional)
    } = req.body;

    // Validasi input wajib
    if (!nim) {
        return res.status(400).json({ success: false, message: 'NIM anggota wajib diisi' });
    }
    if (!bukuId) {
        return res.status(400).json({ success: false, message: 'ID buku wajib diisi' });
    }
    if (!jatuhTempo) {
        return res.status(400).json({ success: false, message: 'Tanggal jatuh tempo wajib diisi' });
    }

    try {
        const result = await prisma.$transaction(async (tx) => {
            // 1. Cek apakah anggota ada
            const anggota = await tx.anggota.findUnique({ where: { nim } });
            if (!anggota) {
                throw new Error(`Anggota dengan NIM ${nim} tidak ditemukan`);
            }

            // 2. Cek apakah buku ada dan stoknya tersedia
            const buku = await tx.buku.findUnique({ where: { id: parseInt(bukuId) } });
            if (!buku) {
                throw new Error(`Buku dengan ID ${bukuId} tidak ditemukan`);
            }
            if (buku.stok < 1) {
                throw new Error(`Stok buku "${buku.title}" habis`);
            }

            // 3. Parse tanggal
            const tglPinjam = tanggalPinjam ? parseTanggalIndonesia(tanggalPinjam) : new Date();
            const tglTempo = parseTanggalIndonesia(jatuhTempo);

            // 4. Handle KRS image jika ada upload
            let krsPath = null;
            if (req.file) {
                krsPath = `/uploads/krs/${req.file.filename}`;
            }

            // 5. Buat peminjaman baru
            const newPeminjaman = await tx.peminjaman.create({
                data: {
                    anggotaNim: nim,
                    bukuId: parseInt(bukuId),
                    tanggalPinjam: tglPinjam,
                    jatuhTempo: tglTempo,
                    adminId: adminId ? parseInt(adminId) : null,
                    krsImagePath: krsPath,
                    status: 'DIPINJAM'
                },
                include: {
                    buku: true,
                    anggota: true
                }
            });

            // 6. Kurangi stok buku
            await tx.buku.update({
                where: { id: parseInt(bukuId) },
                data: { stok: { decrement: 1 } }
            });

            return newPeminjaman;
        });

        // Format response
        const response = {
            id: result.id,
            borrowerName: result.anggota.name,
            nim: result.anggotaNim,
            major: result.anggota.major || '',
            contact: result.anggota.contact || '',
            bookTitle: result.buku.title,
            author: result.buku.author || '',
            isbn: '',
            identityPath: result.krsImagePath || '',
            borrowDate: formatTanggal(result.tanggalPinjam),
            returnDate: formatTanggal(result.jatuhTempo),
            status: result.status,
            bukuId: result.bukuId
        };

        res.status(201).json({ success: true, data: response });
    } catch (error) {
        console.error('Error createPeminjaman:', error);
        res.status(500).json({ success: false, message: error.message });
    }
};

// ============================================================
// [5] UPDATE PEMINJAMAN
// ============================================================
exports.updatePeminjaman = async (req, res) => {
    const { id } = req.params;
    const peminjamanId = parseInt(id, 10);

    if (isNaN(peminjamanId)) {
        return res.status(400).json({ success: false, message: 'ID tidak valid' });
    }

    const {
        jatuhTempo,    // Format: dd/MM/yyyy
        status         // DIPINJAM atau DIKEMBALIKAN
    } = req.body;

    try {
        // Cek apakah peminjaman ada
        const existing = await prisma.peminjaman.findUnique({ where: { id: peminjamanId } });
        if (!existing) {
            return res.status(404).json({ success: false, message: 'Peminjaman tidak ditemukan' });
        }

        // Prepare data update
        const updateData = {};

        if (jatuhTempo) {
            updateData.jatuhTempo = parseTanggalIndonesia(jatuhTempo);
        }

        if (status && ['DIPINJAM', 'DIKEMBALIKAN'].includes(status)) {
            updateData.status = status;
        }

        // Handle KRS image jika ada upload baru
        if (req.file) {
            updateData.krsImagePath = `/uploads/krs/${req.file.filename}`;

            // Hapus file lama jika ada
            if (existing.krsImagePath) {
                const oldPath = path.join(__dirname, '..', existing.krsImagePath);
                if (fs.existsSync(oldPath)) {
                    fs.unlinkSync(oldPath);
                }
            }
        }

        const updated = await prisma.peminjaman.update({
            where: { id: peminjamanId },
            data: updateData,
            include: {
                buku: true,
                anggota: true
            }
        });

        const response = {
            id: updated.id,
            borrowerName: updated.anggota.name,
            nim: updated.anggotaNim,
            major: updated.anggota.major || '',
            contact: updated.anggota.contact || '',
            bookTitle: updated.buku.title,
            author: updated.buku.author || '',
            isbn: '',
            identityPath: updated.krsImagePath || '',
            borrowDate: formatTanggal(updated.tanggalPinjam),
            returnDate: formatTanggal(updated.jatuhTempo),
            status: updated.status,
            bukuId: updated.bukuId
        };

        res.json({ success: true, data: response });
    } catch (error) {
        console.error('Error updatePeminjaman:', error);
        res.status(500).json({ success: false, message: 'Gagal mengupdate peminjaman' });
    }
};

// ============================================================
// [6] DELETE PEMINJAMAN
// ============================================================
exports.deletePeminjaman = async (req, res) => {
    const { id } = req.params;
    const peminjamanId = parseInt(id, 10);

    if (isNaN(peminjamanId)) {
        return res.status(400).json({ success: false, message: 'ID tidak valid' });
    }

    try {
        await prisma.$transaction(async (tx) => {
            // 1. Ambil data peminjaman
            const peminjaman = await tx.peminjaman.findUnique({
                where: { id: peminjamanId },
                include: { pengembalian: true }
            });

            if (!peminjaman) {
                throw new Error('Peminjaman tidak ditemukan');
            }

            // 2. Jika ada pengembalian terkait, hapus dulu
            if (peminjaman.pengembalian) {
                await tx.pengembalian.delete({
                    where: { id: peminjaman.pengembalian.id }
                });
            }

            // 3. Jika status masih DIPINJAM, kembalikan stok buku
            if (peminjaman.status === 'DIPINJAM') {
                await tx.buku.update({
                    where: { id: peminjaman.bukuId },
                    data: { stok: { increment: 1 } }
                });
            }

            // 4. Hapus file KRS jika ada
            if (peminjaman.krsImagePath) {
                const krsPath = path.join(__dirname, '..', peminjaman.krsImagePath);
                if (fs.existsSync(krsPath)) {
                    fs.unlinkSync(krsPath);
                }
            }

            // 5. Hapus peminjaman
            await tx.peminjaman.delete({
                where: { id: peminjamanId }
            });
        });

        res.json({ success: true, message: 'Peminjaman berhasil dihapus' });
    } catch (error) {
        console.error('Error deletePeminjaman:', error);
        res.status(500).json({ success: false, message: error.message });
    }
};

// ============================================================
// [7] GET PEMINJAMAN AKTIF BY ANGGOTA (untuk modul return)
// ============================================================
exports.getActivePeminjamanByAnggota = async (req, res) => {
    const { nim } = req.params;

    try {
        const activeLoans = await prisma.peminjaman.findMany({
            where: {
                anggotaNim: nim,
                status: 'DIPINJAM'
            },
            include: {
                buku: {
                    select: {
                        id: true,
                        title: true,
                        author: true
                    }
                }
            },
            orderBy: { tanggalPinjam: 'desc' }
        });

        const result = activeLoans.map(p => ({
            id: p.id,
            judulBuku: p.buku.title,
            tglPinjam: formatTanggal(p.tanggalPinjam),
            jatuhTempo: formatTanggal(p.jatuhTempo),
            author: p.buku.author
        }));

        res.json(result);
    } catch (error) {
        console.error('Error getActivePeminjamanByAnggota:', error);
        res.status(500).json({ error: 'Gagal mengambil data peminjaman aktif' });
    }
};

// ============================================================
// [8] UPLOAD KRS IMAGE (untuk update KRS saja)
// ============================================================
exports.uploadKrsImage = async (req, res) => {
    const { id } = req.params;
    const peminjamanId = parseInt(id, 10);

    if (isNaN(peminjamanId)) {
        return res.status(400).json({ success: false, message: 'ID tidak valid' });
    }

    if (!req.file) {
        return res.status(400).json({ success: false, message: 'File KRS tidak ditemukan' });
    }

    try {
        const existing = await prisma.peminjaman.findUnique({ where: { id: peminjamanId } });
        if (!existing) {
            return res.status(404).json({ success: false, message: 'Peminjaman tidak ditemukan' });
        }

        // Hapus file lama jika ada
        if (existing.krsImagePath) {
            const oldPath = path.join(__dirname, '..', existing.krsImagePath);
            if (fs.existsSync(oldPath)) {
                fs.unlinkSync(oldPath);
            }
        }

        // Update dengan path baru
        const krsPath = `/uploads/krs/${req.file.filename}`;
        await prisma.peminjaman.update({
            where: { id: peminjamanId },
            data: { krsImagePath: krsPath }
        });

        res.json({ success: true, krsImagePath: krsPath });
    } catch (error) {
        console.error('Error uploadKrsImage:', error);
        res.status(500).json({ success: false, message: 'Gagal mengupload KRS' });
    }
};
