// ============================================================
// PEMINJAMAN NOTIFICATION ROUTES
// ============================================================
// Routes untuk API notifikasi peminjaman
// Base URL: /api/notifications/peminjaman
//
// SEMUA routes ini dilindungi dengan auth middleware
// Hanya admin yang sudah login yang bisa akses

const express = require("express");
const router = express.Router();
const peminjamanNotificationHelper = require("../utils/peminjamanNotificationHelper");
const { authenticateToken } = require("../middlewares/authMiddleware");

// ============================================================
// APPLY AUTH MIDDLEWARE KE SEMUA ROUTES
// ============================================================
router.use(authenticateToken);

// ============================================================
// API ENDPOINTS
// ============================================================

// GET /api/notifications/peminjaman/upcoming-due-date
// Jalankan notifikasi reminder untuk peminjaman yang akan jatuh tempo
// Dapat dijalankan manual atau dari cron job
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

// GET /api/notifications/peminjaman/overdue
// Jalankan notifikasi untuk peminjaman yang overdue
// Dapat dijalankan manual atau dari cron job
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
