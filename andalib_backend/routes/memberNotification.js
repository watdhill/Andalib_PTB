// ============================================================
// MEMBER NOTIFICATION ROUTES
// ============================================================
// Routes untuk API notifikasi penghapusan anggota
// Base URL: /api/member-notifications
//
// SEMUA routes ini dilindungi dengan auth middleware
// Hanya admin yang sudah login yang bisa akses

const express = require('express');
const router = express.Router();
const memberNotificationController = require('../controllers/memberNotificationController');
const { authenticateToken } = require('../middlewares/authMiddleware');

// ============================================================
// APPLY AUTH MIDDLEWARE KE SEMUA ROUTES
// ============================================================
// Middleware ini memastikan:
// - Request punya JWT token yang valid
// - Token di-decode dan admin info disimpan di req.user
router.use(authenticateToken);

// ============================================================
// API ENDPOINTS
// ============================================================

// GET /api/member-notifications/unread
// Ambil notifikasi yang belum dibaca
// Digunakan oleh: Android WorkManager (background polling)
router.get('/unread', memberNotificationController.getUnreadNotifications);

// GET /api/member-notifications/count
// Hitung jumlah notifikasi yang belum dibaca
// Digunakan oleh: Badge counter di icon bell
router.get('/count', memberNotificationController.getUnreadCount);

// GET /api/member-notifications
// Ambil semua notifikasi (read + unread)
// Digunakan oleh: MemberNotificationsScreen
router.get('/', memberNotificationController.getAllNotifications);

// PUT /api/member-notifications/:id/read
// Tandai notifikasi sebagai sudah dibaca
// Digunakan oleh: Saat user tap notifikasi
router.put('/:id/read', memberNotificationController.markAsRead);

// POST /api/member-notifications
// Buat notifikasi baru (untuk book activities)
// Digunakan oleh: Saat buku ditambah/diupdate/dihapus
router.post('/', memberNotificationController.createBookNotification);

// ============================================================
// CATATAN PENTING:
// ============================================================
// 1. Urutan routes penting! '/unread' dan '/count' harus di atas '/'
//    karena Express match route dari atas ke bawah
//
// 2. Auth middleware (authenticateToken) harus sudah ter-setup
//    di file middlewares/authMiddleware.js
//
// 3. Middleware harus set req.user.id dengan admin ID dari JWT token

module.exports = router;
