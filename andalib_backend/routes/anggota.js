const express = require('express');
const router = express.Router();
const anggotaController = require('../controllers/anggotaController');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

// --- KONFIGURASI MULTER (UPLOAD) ---
// Pastikan folder uploads tersedia
const uploadDir = path.join(__dirname, '../uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        // Nama file unik: nim-timestamp.ext (contoh: 12345-168000.jpg)
        // Jika NIM belum ada di body (saat update), pakai 'temp'
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, 'member-' + uniqueSuffix + path.extname(file.originalname));
    }
});

const fileFilter = (req, file, cb) => {
    // Terima hanya gambar
    if (file.mimetype.startsWith('image/')) {
        cb(null, true);
    } else {
        cb(new Error('Hanya file gambar yang diperbolehkan!'), false);
    }
};

const upload = multer({
    storage: storage,
    limits: { fileSize: 5 * 1024 * 1024 }, // Limit 5MB
    fileFilter: fileFilter
});

// --- ROUTES API ---

// GET: Ambil semua anggota
router.get('/', anggotaController.getAllAnggota);

// GET: Cari anggota berdasarkan query
router.get('/search', anggotaController.searchAnggota);

// GET: Ambil detail anggota by NIM
router.get('/:targetNim', anggotaController.getAnggotaByNim);

// POST: Tambah anggota baru (dengan upload foto 'photo')
// Sesuai Android: @Part photo: MultipartBody.Part
router.post('/', upload.single('photo'), anggotaController.createAnggota);

// PUT: Update anggota (dengan upload foto 'photo' opsional)
router.put('/:targetNim', upload.single('photo'), anggotaController.updateAnggota);

// DELETE: Hapus anggota
router.delete('/:targetNim', anggotaController.deleteAnggota);

// POST: Upload foto anggota (untuk update foto existing member)
router.post('/:id/photo', upload.single('photo'), anggotaController.uploadMemberPhoto);

module.exports = router;