// app.js
const express = require('express');
const dotenv = require('dotenv');
require("dotenv").config();
const path = require('path');
const authRoutes = require('./routes/auth');
const adminRoutes = require('./routes/admin');
const anggotaRoutes = require('./routes/anggota');
const returnsRoutes = require('./routes/return');
const peminjamanRoutes = require('./routes/peminjaman');
const bukuRoutes = require('./routes/buku');
const dashboardRoutes = require('./routes/dashboard');
dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
    if (req.method === 'OPTIONS') {
        return res.sendStatus(200);
    }
    next();
});
app.use(express.json()); // Body parser untuk JSON
app.use(express.urlencoded({ extended: true }));

// Serve static files untuk uploads
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Routes
app.use('/api/auth', authRoutes); // /api/auth/login, /api/auth/register
app.use('/api/admin', adminRoutes); // Route khusus untuk Admin (sudah dilindungi)
app.use('/api/anggota', anggotaRoutes); // Route untuk CRUD Anggota
app.use('/api/returns', returnsRoutes);
app.use('/api/peminjaman', peminjamanRoutes); // Route untuk CRUD Peminjaman
app.use('/api/buku', bukuRoutes); // Route untuk CRUD Buku
app.use('/api/dashboard', dashboardRoutes); // Route untuk Dashboard Stats
// ========== NOTIFIKASI MEMBER: ROUTES ==========
// Route untuk notifikasi penghapusan anggota
const memberNotificationRoutes = require('./routes/memberNotification');
app.use('/api/member-notifications', memberNotificationRoutes);

// Cleanup scheduler untuk auto-delete notifikasi yang sudah dibaca > 2 menit
const { startCleanupScheduler } = require('./utils/notificationCleanup');
// ================================================

// Basic route
app.get('/', (req, res) => {
    res.send('Perpustakaan Backend API Running');
});

// Server
app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server is running on port ${PORT}`);

    // ✅ Start notification cleanup scheduler
    startCleanupScheduler();
});