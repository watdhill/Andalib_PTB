// ============================================================
// PEMINJAMAN NOTIFICATION HELPER
// ============================================================
// Helper untuk membuat notifikasi peminjaman baru

const { PrismaClient } = require("@prisma/client");
const prisma = new PrismaClient();

/**
 * Membuat notifikasi ketika peminjaman baru dibuat
 * @param {Object} peminjaman - Data peminjaman yang baru dibuat
 * @param {number} adminId - ID admin yang membuat peminjaman
 */
const createPeminjamanNotification = async (peminjaman, adminId) => {
  try {
    console.log("📢 [createPeminjamanNotification] START");
    console.log("   adminId:", adminId, "type:", typeof adminId);
    console.log("   peminjaman.id:", peminjaman.id);
    console.log("   peminjaman.anggota:", peminjaman.anggota);
    console.log("   peminjaman.buku:", peminjaman.buku);

    // Validasi adminId
    if (!adminId) {
      console.warn(
        "⚠️  [createPeminjamanNotification] adminId tidak ada, skip"
      );
      return;
    }

    const adminIdInt = parseInt(adminId);
    if (isNaN(adminIdInt)) {
      console.error(
        "❌ [createPeminjamanNotification] adminId invalid:",
        adminId
      );
      return;
    }

    const metadata = {
      memberName: peminjaman.anggota.name,
      memberNim: peminjaman.anggotaNim,
      bookTitle: peminjaman.buku.title,
      bookId: peminjaman.bukuId,
      peminjamanId: peminjaman.id,
      dueDate: peminjaman.jatuhTempo.toISOString(),
    };

    console.log("💾 [createPeminjamanNotification] Creating notification...");
    console.log("   data:", {
      adminId: adminIdInt,
      type: "PEMINJAMAN_BARU",
      title: "Peminjaman Baru Dicatat",
      memberName: metadata.memberName,
      memberNim: metadata.memberNim,
    });

    const result = await prisma.notification.create({
      data: {
        adminId: adminIdInt,
        type: "PEMINJAMAN_BARU",
        title: "Peminjaman Baru Dicatat",
        message: `${peminjaman.anggota.name} (${
          peminjaman.anggotaNim
        }) meminjam buku "${
          peminjaman.buku.title
        }". Jatuh tempo: ${formatTanggalIndonesia(peminjaman.jatuhTempo)}`,
        metadata: JSON.stringify(metadata),
        isRead: false,
      },
    });

    console.log(
      "✅ [createPeminjamanNotification] SUCCESS - Notification ID:",
      result.id
    );
  } catch (error) {
    console.error("❌ [createPeminjamanNotification] ERROR:", error.message);
    console.error("   Stack:", error.stack);
  }
};

/**
 * Format tanggal ke format Indonesia (dd/MM/yyyy)
 */
function formatTanggalIndonesia(date) {
  if (!date) return "";
  const d = new Date(date);
  const day = String(d.getDate()).padStart(2, "0");
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

module.exports = {
  createPeminjamanNotification,
};
