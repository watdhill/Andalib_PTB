// routes/admin.js
const express = require('express');
const { authenticateToken, isAdmin } = require('../middlewares/authMiddleware');
// const { getAdminProfile } = require('../controllers/adminController'); // Jika Anda membuat controller ini

const router = express.Router();

// Contoh route Admin yang dilindungi (hanya bisa diakses setelah login dan jika user adalah Admin)
router.get('/dashboard', authenticateToken, isAdmin, (req, res) => {
    // Di sini Anda bisa memanggil fungsi controller untuk mengambil data dashboard
    res.status(200).json({ 
        message: `Selamat datang di Dashboard Admin, ${req.user.email}!`,
        userId: req.user.id
    });
});

module.exports = router;