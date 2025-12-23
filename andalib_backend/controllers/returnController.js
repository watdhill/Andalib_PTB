// controllers/returnController.js
const { PrismaClient } = require("@prisma/client");
const prisma = new PrismaClient();

const multer = require("multer");
const path = require("path");
const fs = require("fs");


const admin = require("firebase-admin");

const FCM_ADMIN_TOPIC = process.env.FCM_ADMIN_TOPIC || "andalib-admin";


function initFirebaseAdminIfNeeded() {
  if (admin.apps && admin.apps.length > 0) return;

  try {
    
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      admin.initializeApp();
      console.log("[FCM] Initialized using GOOGLE_APPLICATION_CREDENTIALS");
      return;
    }

    
    if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
      });
      console.log("[FCM] Initialized using FIREBASE_SERVICE_ACCOUNT_JSON");
      return;
    }

  
    const fallbackPath = path.join(process.cwd(), "serviceAccountKey.json");
    if (fs.existsSync(fallbackPath)) {
      // eslint-disable-next-line import/no-dynamic-require, global-require
      const serviceAccount = require(fallbackPath);
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
      });
      console.log("[FCM] Initialized using serviceAccountKey.json (fallback)");
      return;
    }

    console.warn(
      "[FCM] Firebase Admin belum bisa di-init. Set GOOGLE_APPLICATION_CREDENTIALS atau FIREBASE_SERVICE_ACCOUNT_JSON."
    );
  } catch (e) {
    console.error("[FCM] Init error:", e.message);
  }
}

async function sendDamageProofFcm({ peminjamanId, pengembalianId, buktiKerusakanUrl }) {
  initFirebaseAdminIfNeeded();
  if (!admin.apps || admin.apps.length === 0) return;

  const message = {
    topic: FCM_ADMIN_TOPIC,
    notification: {
      title: "Bukti Kerusakan Diunggah",
      body: `Peminjaman ID: ${peminjamanId} (Pengembalian ID: ${pengembalianId})`,
    },
    data: {
      type: "RETURN_DAMAGE_PROOF",
      peminjamanId: String(peminjamanId),
      pengembalianId: String(pengembalianId),
      buktiKerusakanUrl: buktiKerusakanUrl ? String(buktiKerusakanUrl) : "",
    },
  };

  try {
    const resp = await admin.messaging().send(message);
    console.log("[FCM] Sent:", resp);
  } catch (e) {
    
    console.error("[FCM] Send failed:", e.message);
  }
}


function parseTanggalIndonesia(dateStr) {
  // input: "dd/MM/yyyy"
  if (!dateStr || typeof dateStr !== "string") return new Date();

  const parts = dateStr.split("/");
  if (parts.length !== 3) return new Date(dateStr);

  const [dd, MM, yyyy] = parts;
  const day = Number(dd);
  const month = Number(MM) - 1; 
  const year = Number(yyyy);

  if (Number.isNaN(day) || Number.isNaN(month) || Number.isNaN(year)) {
    return new Date(dateStr);
  }

 
  return new Date(Date.UTC(year, month, day, 0, 0, 0));
}

function formatDateID(date) {
  
  return new Date(date).toLocaleDateString("id-ID", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

function toUtcDateOnly(d) {
  const date = new Date(d);
  return new Date(
    Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate(), 0, 0, 0)
  );
}

function assertReturnNotBeforeBorrow(returnDate, borrowDate) {
  const r = toUtcDateOnly(returnDate);
  const b = toUtcDateOnly(borrowDate);
  if (r < b) {
    const err = new Error("Tanggal pengembalian tidak boleh sebelum tanggal peminjaman.");
    err.statusCode = 400;
    throw err;
  }
}


exports.searchMembers = async (req, res) => {
  const q = (req.query.query || "").trim();
  if (!q) return res.json([]);

  try {
    const like = `%${q.toLowerCase()}%`;

    const rows = await prisma.$queryRaw`
      SELECT nim, name, email
      FROM anggota
      WHERE LOWER(name) LIKE ${like}
         OR LOWER(nim)  LIKE ${like}
      LIMIT 5
    `;

    return res.json(
      rows.map((m) => ({
        nim: m.nim,
        nama: m.name,
        email: m.email,
      }))
    );
  } catch (error) {
    console.error("Error searchMembers:", error);
    return res.status(500).json({ error: "Gagal mencari anggota" });
  }
};


exports.getActiveBorrowings = async (req, res) => {
  const { nim } = req.params;

  try {
    const activeLoans = await prisma.peminjaman.findMany({
      where: {
        anggotaNim: nim,
        status: "DIPINJAM",
      },
      select: {
        id: true,
        tanggalPinjam: true,
        jatuhTempo: true,
        buku: {
          select: {
            id: true,
            title: true,
            author: true,
          },
        },
      },
    });

    const formattedLoans = activeLoans.map((loan) => ({
      id: loan.id,
      judulBuku: loan.buku.title,
      tglPinjam: formatDateID(loan.tanggalPinjam),
      jatuhTempo: formatDateID(loan.jatuhTempo),
      author: loan.buku.author,
    }));

    res.json(formattedLoans);
  } catch (error) {
    console.error("Error getActiveBorrowings:", error);
    res.status(500).json({ error: "Gagal mengambil data peminjaman" });
  }
};


const damageProofUploadDir = path.join(__dirname, "..", "uploads", "kerusakan");


if (!fs.existsSync(damageProofUploadDir)) {
  fs.mkdirSync(damageProofUploadDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, damageProofUploadDir),
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + "-" + Math.round(Math.random() * 1e9);
    const ext = path.extname(file.originalname);
    cb(null, `kerusakan-${uniqueSuffix}${ext}`);
  },
});

const fileFilter = (req, file, cb) => {
  const allowedTypes = ["image/jpeg", "image/jpg", "image/png", "image/webp"];
  if (allowedTypes.includes(file.mimetype)) cb(null, true);
  else cb(new Error("Hanya file gambar (JPEG, PNG, WEBP) yang diperbolehkan"), false);
};

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: 5 * 1024 * 1024 },
});


exports.uploadDamageProof = upload.single("buktiKerusakan");


exports.uploadDamageProofOnly = async (req, res) => {
  if (!req.file) {
    return res.status(400).json({ success: false, message: "File bukti kerusakan tidak ditemukan" });
  }

  const buktiKerusakanUrl = `/uploads/kerusakan/${req.file.filename}`;
  return res.json({ success: true, buktiKerusakanUrl });
};


exports.updateDamageProof = async (req, res) => {
  const { returnId } = req.params;

  if (!req.file) {
    return res.status(400).json({ success: false, message: "File bukti kerusakan tidak ditemukan" });
  }

  const id = parseInt(returnId, 10);
  if (Number.isNaN(id)) {
    return res.status(400).json({ success: false, message: "returnId tidak valid" });
  }

  try {
    const existingReturn = await prisma.pengembalian.findUnique({ where: { id } });
    if (!existingReturn) {
      return res.status(404).json({ success: false, message: "Pengembalian tidak ditemukan" });
    }


    if (existingReturn.buktiKerusakanUrl) {
      const relative = existingReturn.buktiKerusakanUrl.replace(/^\/uploads\//, "");
      const oldPath = path.join(__dirname, "..", "uploads", relative);
      if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath);
    }

    const buktiKerusakanUrl = `/uploads/kerusakan/${req.file.filename}`;

    const updatedReturn = await prisma.pengembalian.update({
      where: { id },
      data: { buktiKerusakanUrl },
    });

   
    sendDamageProofFcm({
      peminjamanId: updatedReturn.peminjamanId,
      pengembalianId: updatedReturn.id,
      buktiKerusakanUrl,
    }).catch(() => {});

    return res.json({
      success: true,
      message: "Bukti kerusakan berhasil diperbarui",
      data: updatedReturn,
      buktiKerusakanUrl,
    });
  } catch (error) {
    console.error("Error updateDamageProof:", error);
    return res.status(500).json({
      success: false,
      message: "Gagal memperbarui bukti kerusakan",
      detail: error.message,
    });
  }
};




exports.createReturn = async (req, res) => {
  const { peminjamanId, tanggalPengembalian, denda, keterangan } = req.body;
  let { buktiKerusakanUrl } = req.body;

  if (req.file) {
    buktiKerusakanUrl = `/uploads/kerusakan/${req.file.filename}`;
  }

  if (!peminjamanId) {
    return res.status(400).json({
      success: false,
      message: "ID Peminjaman tidak boleh kosong.",
    });
  }

  let fcmPayloadToSend = null;

  try {
    const result = await prisma.$transaction(async (tx) => {
      const pId = parseInt(peminjamanId, 10);
      if (Number.isNaN(pId)) {
        const err = new Error("peminjamanId tidak valid.");
        err.statusCode = 400;
        throw err;
      }

      const loanDenda = parseInt(denda || 0, 10) || 0;
      const parsedTanggal = tanggalPengembalian
        ? parseTanggalIndonesia(tanggalPengembalian)
        : new Date();

      const peminjaman = await tx.peminjaman.findUnique({
        where: { id: pId },
        include: { pengembalian: true, buku: true },
      });

      if (!peminjaman) {
        const err = new Error("Data peminjaman tidak ditemukan.");
        err.statusCode = 404;
        throw err;
      }

      assertReturnNotBeforeBorrow(parsedTanggal, peminjaman.tanggalPinjam);

      if (peminjaman.pengembalian || peminjaman.status === "DIKEMBALIKAN") {
        return { __alreadyReturned: true };
      }

      const newReturn = await tx.pengembalian.create({
        data: {
          peminjamanId: pId,
          tanggalPengembalian: parsedTanggal,
          denda: loanDenda,
          buktiKerusakanUrl: buktiKerusakanUrl || null,
          keterangan: keterangan || null,
        },
      });

      await tx.buku.update({
        where: { id: peminjaman.bukuId },
        data: { stok: { increment: 1 } },
      });

      await tx.peminjaman.update({
        where: { id: pId },
        data: { status: "DIKEMBALIKAN" },
      });

   
      if (buktiKerusakanUrl) {
        fcmPayloadToSend = {
          peminjamanId: pId,
          pengembalianId: newReturn.id,
          buktiKerusakanUrl,
        };
      }

      return newReturn;
    });

    if (result?.__alreadyReturned) {
      return res.status(409).json({
        success: false,
        message: "Peminjaman ini sudah dikembalikan.",
      });
    }

    
    if (fcmPayloadToSend) {
      sendDamageProofFcm(fcmPayloadToSend).catch(() => {});
    }

    return res.status(201).json({ success: true, data: result });
  } catch (error) {
    console.error("Error createReturn:", error);

    if (error?.code === "P2002") {
      return res.status(409).json({
        success: false,
        message: "Pengembalian untuk peminjaman ini sudah tercatat (duplikat).",
        detail: error.message,
      });
    }

    const status = error.statusCode || 500;
    return res.status(status).json({
      success: false,
      message: status === 500 ? "Gagal memproses pengembalian" : error.message,
      detail: error.message,
    });
  }
};


exports.getReturnHistory = async (req, res) => {
  try {
    const records = await prisma.pengembalian.findMany({
      orderBy: { tanggalPengembalian: "desc" },
      include: {
        peminjaman: {
          include: { buku: true, anggota: true },
        },
      },
    });

    const result = records.map((r) => ({
      id: r.id,
      peminjamanId: r.peminjamanId,
      judulBuku: r.peminjaman.buku.title,
      namaAnggota: r.peminjaman.anggota.name,
      nimAnggota: r.peminjaman.anggota.nim,
      tanggalPinjam: formatDateID(r.peminjaman.tanggalPinjam),
      jatuhTempo: formatDateID(r.peminjaman.jatuhTempo),
      tanggalPengembalian: formatDateID(r.tanggalPengembalian),
      denda: r.denda,
      keterangan: r.keterangan,
      buktiKerusakanUrl: r.buktiKerusakanUrl,
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

  if (Number.isNaN(returnId)) {
    return res.status(400).json({
      success: false,
      message: "ID pengembalian tidak valid",
    });
  }

  try {
    await prisma.$transaction(async (tx) => {
      const pengembalian = await tx.pengembalian.findUnique({
        where: { id: returnId },
        include: { peminjaman: true },
      });

      if (!pengembalian) {
        const err = new Error(`Pengembalian dengan ID ${returnId} tidak ditemukan`);
        err.statusCode = 404;
        throw err;
      }

      const peminjaman = pengembalian.peminjaman;
      if (!peminjaman) {
        const err = new Error(
          `Relasi peminjaman untuk pengembalian ID ${returnId} tidak ditemukan`
        );
        err.statusCode = 404;
        throw err;
      }

      
      if (pengembalian.buktiKerusakanUrl) {
        try {
          const relative = pengembalian.buktiKerusakanUrl.replace(/^\/uploads\//, "");
          const filePath = path.join(__dirname, "..", "uploads", relative);
          if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
        } catch (fileErr) {
          console.error("Gagal menghapus file bukti kerusakan:", fileErr.message);
        }
      }

      
      if (peminjaman.status === "DIKEMBALIKAN") {
        const buku = await tx.buku.findUnique({
          where: { id: peminjaman.bukuId },
          select: { stok: true },
        });

        if (!buku) {
          const err = new Error("Data buku tidak ditemukan untuk rollback.");
          err.statusCode = 404;
          throw err;
        }

        if (buku.stok <= 0) {
          const err = new Error("Stok buku tidak valid untuk rollback.");
          err.statusCode = 400;
          throw err;
        }

        await tx.buku.update({
          where: { id: peminjaman.bukuId },
          data: { stok: { decrement: 1 } },
        });

        await tx.peminjaman.update({
          where: { id: peminjaman.id },
          data: { status: "DIPINJAM" },
        });
      }

      await tx.pengembalian.delete({ where: { id: returnId } });
    });

    res.json({
      success: true,
      message:
        "Data pengembalian berhasil dihapus. Status peminjaman dan stok buku sudah dikembalikan seperti semula.",
    });
  } catch (error) {
    console.error("Error deleteReturn:", error);
    const status = error.statusCode || 500;
    res.status(status).json({
      success: false,
      message: status === 500 ? "Gagal menghapus pengembalian" : error.message,
      detail: error.message,
    });
  }
};


exports.updateReturn = async (req, res) => {
  const { returnId } = req.params;
  const { peminjamanId, tanggalPengembalian, denda, buktiKerusakanUrl, keterangan } = req.body;

  const id = parseInt(returnId, 10);
  const pinjamId = parseInt(peminjamanId, 10);
  const dendaInt = parseInt(denda || 0, 10) || 0;

  if (Number.isNaN(id) || Number.isNaN(pinjamId)) {
    return res.status(400).json({ success: false, message: "ID tidak valid" });
  }

  let fcmPayloadToSend = null;

  try {
    const updated = await prisma.$transaction(async (tx) => {
      const existing = await tx.pengembalian.findUnique({
        where: { id },
        include: { peminjaman: true },
      });

      if (!existing) {
        const err = new Error("Data pengembalian tidak ditemukan");
        err.statusCode = 404;
        throw err;
      }

      if (pinjamId !== existing.peminjamanId) {
        const err = new Error("peminjamanId tidak sesuai dengan data pengembalian.");
        err.statusCode = 400;
        throw err;
      }

      const wasEmpty = !existing.buktiKerusakanUrl;
      const nowFilled = !!buktiKerusakanUrl;

      const tanggalJs = tanggalPengembalian
        ? parseTanggalIndonesia(tanggalPengembalian)
        : new Date();

      assertReturnNotBeforeBorrow(tanggalJs, existing.peminjaman.tanggalPinjam);

      const result = await tx.pengembalian.update({
        where: { id },
        data: {
          tanggalPengembalian: tanggalJs,
          denda: dendaInt,
          buktiKerusakanUrl: buktiKerusakanUrl || null,
          keterangan: keterangan || null,
        },
      });

      if (wasEmpty && nowFilled) {
        fcmPayloadToSend = {
          peminjamanId: existing.peminjamanId,
          pengembalianId: id,
          buktiKerusakanUrl,
        };
      }

      return result;
    });

    
    if (fcmPayloadToSend) {
      sendDamageProofFcm(fcmPayloadToSend).catch(() => {});
    }

    return res.json({ success: true, data: updated });
  } catch (error) {
    console.error("Error updateReturn:", error);
    const status = error.statusCode || 500;
    return res.status(status).json({
      success: false,
      message: status === 500 ? "Gagal mengupdate pengembalian" : error.message,
      detail: error.message,
    });
  }
};
