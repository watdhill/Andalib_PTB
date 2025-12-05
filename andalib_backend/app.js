// app.js
const express = require('express');
const dotenv = require('dotenv');
const path = require('path');
const authRoutes = require('./routes/auth');
const adminRoutes = require('./routes/admin');
const anggotaRoutes = require('./routes/anggota');

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(express.json()); // Body parser untuk JSON

// Serve static files untuk uploads
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Routes
app.use('/api/auth', authRoutes); // /api/auth/login, /api/auth/register
app.use('/api/admin', adminRoutes); // Route khusus untuk Admin (sudah dilindungi)
app.use('/api/anggota', anggotaRoutes); // Route untuk CRUD Anggota

// Basic route
app.get('/', (req, res) => {
    res.send('Perpustakaan Backend API Running');
});

// Server
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});