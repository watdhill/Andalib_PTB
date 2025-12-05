// routes/anggota.js
const express = require('express');
const { authenticateToken, isAdmin } = require('../middlewares/authMiddleware');
const { createAnggota, getAllAnggota, getAnggotaById, updateAnggota, deleteAnggota } = require('../controllers/anggotaController');

const router = express.Router();

// Semua rute ini memerlukan Admin yang terautentikasi
router.use(authenticateToken, isAdmin);

// CREATE (Menambahkan Anggota)
router.post('/', createAnggota);

// READ (Mendapatkan Semua Anggota)
router.get('/', getAllAnggota);

// READ (Mendapatkan Detail Anggota)
router.get('/:id', getAnggotaById);

// UPDATE (Mengubah Anggota)
router.put('/:id', updateAnggota);
// atau router.patch('/:id', updateAnggota);

// DELETE (Menghapus Anggota)
router.delete('/:id', deleteAnggota);

module.exports = router;