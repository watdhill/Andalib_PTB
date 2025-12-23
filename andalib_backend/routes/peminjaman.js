
const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const peminjamanController = require('../controllers/peminjamanController');


const krsUploadDir = path.join(__dirname, '..', 'uploads', 'krs');


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
        fileSize: 5 * 1024 * 1024 
    }
});




router.get('/', peminjamanController.getAllPeminjaman);


router.get('/search', peminjamanController.searchPeminjaman);


router.get('/active/:nim', peminjamanController.getActivePeminjamanByAnggota);


router.get('/:id', peminjamanController.getPeminjamanById);


router.post('/', upload.single('krsImage'), peminjamanController.createPeminjaman);


router.put('/:id', upload.single('krsImage'), peminjamanController.updatePeminjaman);


router.post('/:id/upload-krs', upload.single('krsImage'), peminjamanController.uploadKrsImage);


router.delete('/:id', peminjamanController.deletePeminjaman);

module.exports = router;
