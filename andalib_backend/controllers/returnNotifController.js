// controllers/returnNotifController.js
const { PrismaClient } = require("@prisma/client");
const prisma = new PrismaClient();

const listMine = async (req, res) => {
  try {
    const adminId = req.user.id;
    const take = parseInt(req.query.take || "50", 10);

    const data = await prisma.notification.findMany({
      where: { adminId },
      orderBy: { createdAt: "desc" },
      take,
    });

    return res.json({ success: true, data });
  } catch (e) {
    console.error("listMine error:", e);
    return res.status(500).json({ success: false, message: "Gagal mengambil notifikasi" });
  }
};

const markRead = async (req, res) => {
  try {
    const adminId = req.user.id;
    const id = parseInt(req.params.id, 10);

    if (isNaN(id)) {
      return res.status(400).json({ success: false, message: "ID notifikasi tidak valid" });
    }

    const updated = await prisma.notification.updateMany({
      where: { id, adminId },
      data: { isRead: true, readAt: new Date() },
    });

    return res.json({ success: true, updated: updated.count });
  } catch (e) {
    console.error("markRead error:", e);
    return res.status(500).json({ success: false, message: "Gagal update notifikasi" });
  }
};

const deleteMine = async (req, res) => {
  try {
    const adminId = req.user.id;
    const id = parseInt(req.params.id, 10);

    if (isNaN(id)) {
      return res.status(400).json({ success: false, message: "ID notifikasi tidak valid" });
    }

    await prisma.notification.deleteMany({
      where: { id, adminId },
    });

    return res.json({ success: true });
  } catch (e) {
    console.error("deleteMine error:", e);
    return res.status(500).json({ success: false, message: "Gagal menghapus notifikasi" });
  }
};

// OPTIONAL: cleanup job (kalau kamu memang mau taruh di controller ini)
const startNotificationCleanupJob = () => {
  setInterval(async () => {
    const twoMinutesAgo = new Date(Date.now() - 2 * 60 * 1000);
    try {
      await prisma.notification.deleteMany({
        where: {
          isRead: true,
          readAt: { lte: twoMinutesAgo },
        },
      });
    } catch (e) {
      console.error("Cleanup notif error:", e.message);
    }
  }, 60 * 1000); // tiap 1 menit
};

module.exports = {
  listMine,
  markRead,
  deleteMine,
  startNotificationCleanupJob,
};
