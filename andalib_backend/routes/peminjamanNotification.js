

const express = require("express");
const router = express.Router();
const peminjamanNotificationHelper = require("../utils/peminjamanNotificationHelper");
const { authenticateToken } = require("../middlewares/authMiddleware");


router.use(authenticateToken);



router.get("/upcoming-due-date", async (req, res) => {
  try {
    const adminId = req.query.adminId ? parseInt(req.query.adminId) : null;
    await peminjamanNotificationHelper.notifyUpcomingDueDate(adminId);

    res.status(200).json({
      success: true,
      message: "Notifikasi reminder berhasil dibuat",
    });
  } catch (error) {
    console.error("Error:", error);
    res.status(500).json({
      success: false,
      message: "Gagal membuat notifikasi reminder",
    });
  }
});


router.get("/overdue", async (req, res) => {
  try {
    const adminId = req.query.adminId ? parseInt(req.query.adminId) : null;
    await peminjamanNotificationHelper.notifyOverduePeminjaman(adminId);

    res.status(200).json({
      success: true,
      message: "Notifikasi overdue berhasil dibuat",
    });
  } catch (error) {
    console.error("Error:", error);
    res.status(500).json({
      success: false,
      message: "Gagal membuat notifikasi overdue",
    });
  }
});

module.exports = router;
