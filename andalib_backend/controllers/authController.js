const { PrismaClient } = require('@prisma/client');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const prisma = new PrismaClient();

// Fungsi untuk membuat Token JWT
const generateToken = (id, role) => {
    // process.env.JWT_SECRET di sini bernilai undefined
    return jwt.sign({ id, role }, process.env.JWT_SECRET, { 
        expiresIn: '7d', 
    });
};

// @desc    Mendaftarkan Admin baru (Register)
// @route   POST /api/auth/register
exports.register = async (req, res) => {
    // Registrasi ini diasumsikan hanya untuk model Admin (Pengguna Aplikasi)
    const { name, email, password } = req.body;

    // 1. Validasi Input
    if (!name || !email || !password) {
        return res.status(400).json({ message: 'Nama, email, dan password wajib diisi.' });
    }

    try {
        // 2. Cek Email menggunakan Model ADMIN
        // Error 'findUnique' sebelumnya terjadi karena menggunakan 'prisma.user'
        const adminExists = await prisma.admin.findUnique({ where: { email } }); 
        
        if (adminExists) {
            return res.status(409).json({ message: 'Email sudah terdaftar sebagai Admin.' });
        }

        // 3. Hash Password
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(password, salt);

        // 4. Buat Admin baru
        const admin = await prisma.admin.create({
            data: {
                name,
                email,
                password: hashedPassword,
                // Model Admin tidak memiliki field 'role' di schema Anda.
            },
        });

        if (admin) {
            // 5. Generate Token (gunakan ID dan role 'admin' statis)
            const token = generateToken(admin.id, 'admin'); // Role statis 'admin'
            res.status(201).json({
                message: 'Pendaftaran Admin berhasil!',
                token: token,
                user: { id: admin.id, name: admin.name, email: admin.email, role: 'admin' },
            });
        } else {
            res.status(400).json({ message: 'Data Admin tidak valid.' });
        }
    } catch (error) {
        console.error('Error saat register:', error);
        res.status(500).json({ message: 'Server error: ' + error.message });
    }
};

// @desc    Login Admin
// @route   POST /api/auth/login
exports.login = async (req, res) => {
    const { email, password } = req.body;

    if (!email || !password) {
        return res.status(400).json({ message: 'Email dan password wajib diisi.' });
    }

    try {
        // 1. Cari Admin menggunakan Model ADMIN
        const admin = await prisma.admin.findUnique({ where: { email } });
        if (!admin) {
            return res.status(401).json({ message: 'Kredensial tidak valid.' });
        }

        // 2. Bandingkan Password
        const isMatch = await bcrypt.compare(password, admin.password);
        if (!isMatch) {
            return res.status(401).json({ message: 'Kredensial tidak valid.' });
        }

        // 3. Generate Token dan kirim respons sukses
        const token = generateToken(admin.id, 'admin');
        res.status(200).json({
            message: 'Login Admin berhasil!',
            token: token,
            user: { id: admin.id, name: admin.name, email: admin.email, role: 'admin' },
        });

    } catch (error) {
        console.error('Error saat login:', error);
        res.status(500).json({ message: 'Server error: ' + error.message });
    }
};