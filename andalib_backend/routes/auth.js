const express = require('express');

// PASTIKAN NAMA FUNGSI INI COCOK DENGAN YANG DI EKSPOR DI CONTROLLER
const { 
    register, 
    login     
} = require('../controllers/authController');

const router = express.Router();

// Route untuk Pendaftaran
router.post('/register', register); 

// Route untuk Login
router.post('/login', login); 

module.exports = router;