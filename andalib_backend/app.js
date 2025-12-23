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

app.use(express.json()); 
app.use(express.urlencoded({ extended: true }));


app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Routes
app.use('/api/auth', authRoutes); 
app.use('/api/admin', adminRoutes); 
app.use('/api/anggota', anggotaRoutes); 
app.use('/api/returns', returnsRoutes);
app.use('/api/peminjaman', peminjamanRoutes); 
app.use('/api/buku', bukuRoutes); 
app.use('/api/dashboard', dashboardRoutes); 


const memberNotificationRoutes = require('./routes/memberNotification');
app.use('/api/member-notifications', memberNotificationRoutes);


const { startCleanupScheduler } = require('./utils/notificationCleanup');



app.get('/', (req, res) => {
    res.send('Perpustakaan Backend API Running');
});

// Server
app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server is running on port ${PORT}`);

    
    startCleanupScheduler();
});