const express = require('express');
const router = express.Router();
const returnController = require('../controllers/returnController');

// Route untuk mencari anggota
router.get('/members/search', returnController.searchMembers);

// Route untuk mendapatkan buku yang sedang dipinjam oleh NIM tertentu
router.get('/borrowings/active/:nim', returnController.getActiveBorrowings);

// Route untuk submit pengembalian
router.post('/process', returnController.createReturn);

router.get('/history', returnController.getReturnHistory);

router.post('/update/:returnId', returnController.updateReturn);

router.delete('/history/:id', returnController.deleteReturn);

module.exports = router;