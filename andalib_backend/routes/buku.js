// routes/buku.js
const express = require('express');
const router = express.Router();
const { createBuku, getAllBuku, getBukuById, updateBuku, deleteBuku } = require('../controllers/bukuController');

// POST /api/buku  -> tambah buku
router.post('/', createBuku);

// GET /api/buku -> list semua buku
router.get('/', getAllBuku);

// GET /api/buku/:id -> detail buku
router.get('/:id', getBukuById);

// PUT /api/buku/:id -> update buku
router.put('/:id', updateBuku);

// DELETE /api/buku/:id -> delete buku
router.delete('/:id', deleteBuku);

module.exports = router;
