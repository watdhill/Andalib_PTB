// ============================================================
// EXAMPLE: APP.JS WITH PEMINJAMAN NOTIFICATION INTEGRATION
// ============================================================
// Ini adalah contoh lengkap bagaimana mengintegrasikan
// sistem notifikasi peminjaman ke dalam app.js

// Gunakan file ini sebagai referensi untuk memodifikasi app.js asli Anda

const express = require("express");
const dotenv = require("dotenv");
const path = require("path");

// ============================================================
// IMPORT ROUTES
// ============================================================
const authRoutes = require("./routes/auth");
const adminRoutes = require("./routes/admin");
const anggotaRoutes = require("./routes/anggota");
const returnsRoutes = require("./routes/return");
const peminjamanRoutes = require("./routes/peminjaman");
const bukuRoutes = require("./routes/buku");
const returnNotificationRoutes = require("./routes/returnNotification");
const memberNotificationRoutes = require("./routes/memberNotification");

// ============================================================
// ✅ IMPORT NOTIFIKASI PEMINJAMAN (NEW)
// ============================================================
const { startAllSchedulers } = require("./utils/notificationScheduler");
const peminjamanNotificationRoutes = require("./routes/peminjamanNotification");

// ============================================================
// IMPORT UTILITIES
// ============================================================
const { startCleanupScheduler } = require("./utils/notificationCleanup");
const {
  startNotificationCleanupJob,
} = require("./controllers/returnNotifController");

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// ============================================================
// MIDDLEWARE
// ============================================================
app.use(express.json());
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

// ============================================================
// API ROUTES
// ============================================================

// Authentication
app.use("/api/auth", authRoutes);

// Admin management
app.use("/api/admin", adminRoutes);

// Member/Anggota management
app.use("/api/anggota", anggotaRoutes);

// Peminjaman management
app.use("/api/peminjaman", peminjamanRoutes);

// Buku management
app.use("/api/buku", bukuRoutes);

// Returns/Pengembalian
app.use("/api/returns", returnsRoutes);

// Return Notification
app.use("/api/returnNotif", returnNotificationRoutes);

// Member Notifications (untuk notifikasi penghapusan anggota, dll)
app.use("/api/member-notifications", memberNotificationRoutes);

// ============================================================
// ✅ PEMINJAMAN NOTIFICATIONS ROUTES (NEW)
// ============================================================
// Routes untuk notifikasi peminjaman, return, dan reminder
// Endpoints:
// - GET /api/notifications/peminjaman/upcoming-due-date
// - GET /api/notifications/peminjaman/overdue
app.use("/api/notifications/peminjaman", peminjamanNotificationRoutes);

// ============================================================
// BASIC ROUTE
// ============================================================
app.get("/", (req, res) => {
  res.send("Perpustakaan Backend API Running");
});

// ============================================================
// SERVER STARTUP
// ============================================================
app.listen(PORT, "0.0.0.0", () => {
  console.log(`\n${"=".repeat(60)}`);
  console.log(`Server is running on port ${PORT}`);
  console.log(`${"=".repeat(60)}\n`);

  // ============================================================
  // ✅ START NOTIFICATION SCHEDULERS (NEW)
  // ============================================================
  // Jalankan semua scheduler untuk:
  // 1. Reminder peminjaman akan jatuh tempo (08:00 setiap hari)
  // 2. Reminder peminjaman overdue (09:00 setiap hari)
  console.log("📋 Initializing notification schedulers...\n");

  try {
    // Start cleanup scheduler (existing)
    startCleanupScheduler();

    // Start peminjaman notification schedulers (new)
    startAllSchedulers();

    // Start return notification cleanup job (existing)
    startNotificationCleanupJob();

    console.log("✅ All schedulers initialized successfully!\n");
  } catch (error) {
    console.error("❌ Error initializing schedulers:", error);
    // Jangan exit, biarkan server tetap berjalan
    console.warn("⚠️  Server running but schedulers failed to start\n");
  }

  console.log(`${"=".repeat(60)}`);
  console.log("Available APIs:");
  console.log("  ✅ /api/auth                    - Authentication");
  console.log("  ✅ /api/admin                   - Admin management");
  console.log("  ✅ /api/anggota                 - Member management");
  console.log("  ✅ /api/peminjaman              - Borrowing management");
  console.log("  ✅ /api/buku                    - Book management");
  console.log("  ✅ /api/returns                 - Returns management");
  console.log("  ✅ /api/member-notifications    - Member notifications");
  console.log(
    "  ✅ /api/notifications/peminjaman - Borrowing notifications ⭐ NEW"
  );
  console.log(`${"=".repeat(60)}\n`);
});

// ============================================================
// NOTES
// ============================================================
/*
 * IMPLEMENTASI NOTIFIKASI PEMINJAMAN:
 *
 * 1. FEATURES:
 *    - Notifikasi otomatis saat create peminjaman (PEMINJAMAN_BARU)
 *    - Notifikasi otomatis saat return (PENGEMBALIAN_BUKU)
 *    - Reminder harian untuk peminjaman akan jatuh tempo (08:00)
 *    - Reminder harian untuk peminjaman overdue (09:00)
 *
 * 2. DEPENDENCIES:
 *    npm install node-cron
 *
 * 3. ENDPOINT BARU:
 *    GET /api/notifications/peminjaman/upcoming-due-date
 *    GET /api/notifications/peminjaman/overdue
 *
 * 4. API YANG SUDAH ADA (untuk view notifikasi):
 *    GET /api/member-notifications/unread       - Lihat notifikasi belum dibaca
 *    GET /api/member-notifications              - Lihat semua notifikasi
 *    GET /api/member-notifications/count        - Hitung notifikasi belum dibaca
 *    PUT /api/member-notifications/:id/read     - Tandai notifikasi sebagai dibaca
 *
 * 5. FILES YANG MODIFIED:
 *    - controllers/peminjamanController.js       (trigger notifikasi)
 *    - controllers/returnController.js           (trigger notifikasi)
 *
 * 6. FILES YANG DITAMBAH:
 *    - utils/peminjamanNotificationHelper.js     (helper functions)
 *    - utils/notificationScheduler.js            (cron job scheduler)
 *    - routes/peminjamanNotification.js          (API routes)
 *
 * 7. DOKUMENTASI:
 *    - NOTIFIKASI_PEMINJAMAN.md                  (lengkap)
 *    - QUICK_START_NOTIFIKASI.md                 (quick guide)
 *    - CHECKLIST_IMPLEMENTASI.md                 (checklist)
 *    - IMPLEMENTASI_SUMMARY.md                   (summary)
 */

module.exports = app;
