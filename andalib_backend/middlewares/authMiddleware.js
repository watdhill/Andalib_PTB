// middlewares/authMiddleware.js
const jwt = require('jsonwebtoken');

const jwtSecret = process.env.JWT_SECRET;

/**
 * Middleware untuk memverifikasi JWT dari header Authorization
 */
const authenticateToken = (req, res, next) => {
    // Ambil token dari header Authorization (Bearer <token>)
    const authHeader = req.headers['authorization'];
    // Split 'Bearer <token>' dan ambil elemen index 1 (token)
    const token = authHeader && authHeader.split(' ')[1]; 

    if (token == null) {
        // 401: Unauthorized (Tidak ada token)
        return res.status(401).json({ message: 'Akses ditolak. Token tidak tersedia.' });
    }

    jwt.verify(token, jwtSecret, (err, user) => {
        if (err) {
            // 403: Forbidden (Token tidak valid/expired)
            return res.status(403).json({ message: 'Token tidak valid atau sudah kadaluarsa.' });
        }
        
        // Token valid, simpan data user (payload JWT) di objek request
        req.user = user; 
        next(); // Lanjutkan ke controller
    });
};

/**
 * Middleware untuk memastikan pengguna adalah Admin
 */
const isAdmin = (req, res, next) => {
    // Memeriksa peran yang tersimpan saat token dibuat (di authController.js)
    if (req.user && req.user.role === 'admin') {
        next();
    } else {
        // 403: Forbidden (Bukan admin)
        res.status(403).json({ message: 'Akses ditolak. Hanya untuk Admin.' });
    }
};

module.exports = {
    authenticateToken,
    isAdmin
};