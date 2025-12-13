// routes/peminjaman.js
const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const peminjamanController = require('../controllers/peminjamanController');

// ============================================================
// MULTER CONFIG untuk upload KRS
// ============================================================
const krsUploadDir = path.join(__dirname, '..', 'uploads', 'krs');

// Buat folder jika belum ada
if (!fs.existsSync(krsUploadDir)) {
    fs.mkdirSync(krsUploadDir, { recursive: true });
}

const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, krsUploadDir);
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        const ext = path.extname(file.originalname);
        cb(null, `krs-${uniqueSuffix}${ext}`);
    }
});

const fileFilter = (req, file, cb) => {
    // Hanya terima file gambar
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    if (allowedTypes.includes(file.mimetype)) {
        cb(null, true);
    } else {
        cb(new Error('Hanya file gambar (JPEG, PNG, WEBP) yang diperbolehkan'), false);
    }
};

const upload = multer({
    storage: storage,
    fileFilter: fileFilter,
    limits: {
        fileSize: 5 * 1024 * 1024 // Max 5MB
    }
});

// ============================================================
// ROUTES
// ============================================================

// GET /api/peminjaman - Ambil semua peminjaman
router.get('/', peminjamanController.getAllPeminjaman);

// GET /api/peminjaman/search?q=... - Cari peminjaman
router.get('/search', peminjamanController.searchPeminjaman);

// GET /api/peminjaman/active/:nim - Ambil peminjaman aktif berdasarkan NIM anggota
router.get('/active/:nim', peminjamanController.getActivePeminjamanByAnggota);

// GET /api/peminjaman/:id - Ambil peminjaman berdasarkan ID
router.get('/:id', peminjamanController.getPeminjamanById);

// POST /api/peminjaman - Buat peminjaman baru (dengan optional KRS upload)
router.post('/', upload.single('krsImage'), peminjamanController.createPeminjaman);

// PUT /api/peminjaman/:id - Update peminjaman (dengan optional KRS upload)
router.put('/:id', upload.single('krsImage'), peminjamanController.updatePeminjaman);

// POST /api/peminjaman/:id/upload-krs - Upload KRS saja
router.post('/:id/upload-krs', upload.single('krsImage'), peminjamanController.uploadKrsImage);

// DELETE /api/peminjaman/:id - Hapus peminjaman
router.delete('/:id', peminjamanController.deletePeminjaman);

module.exports = router;
