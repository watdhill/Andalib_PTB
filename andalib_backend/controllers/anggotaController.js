const { PrismaClient } = require('@prisma/client');
const fs = require('fs');
const path = require('path');

const prisma = new PrismaClient();

// 1. Menambahkan Anggota Baru
const createAnggota = async (req, res) => {
    // Ambil data dari body
    // Catatan: field 'email' dan 'photo' bersifat opsional sesuai permintaan
    const { nim, name, gender, faculty, major, contact, email } = req.body;
    
    // Ambil file foto jika ada
    const photoPath = req.file ? req.file.filename : null;

    // VALIDASI: Semuanya wajib diisi KECUALI foto dan email
    if (!nim || !name || !gender || !faculty || !major || !contact) {
        // Hapus foto jika validasi gagal agar tidak menumpuk sampah file
        if (req.file) fs.unlinkSync(req.file.path);
        return res.status(400).json({ 
            success: false, 
            message: 'NIM, Nama, Gender, Fakultas, Jurusan, dan Kontak wajib diisi.' 
        });
    }

    try {
        const newAnggota = await prisma.anggota.create({
            data: {
                nim: nim, // NIM adalah Primary Key (String)
                name: name,
                gender: gender,   // Harus sesuai ENUM: "LAKI_LAKI" atau "PEREMPUAN"
                faculty: faculty, // Harus sesuai ENUM Faculty
                major: major,     // Harus sesuai ENUM Major
                contact: contact,
                email: email, 
                photoPath: photoPath,
            },
        });

        res.status(201).json({ 
            success: true,
            message: 'Anggota berhasil ditambahkan', 
            data: newAnggota 
        });
    } catch (error) {
        console.error("Create Error:", error);
        // Hapus foto jika database gagal
        if (req.file && fs.existsSync(req.file.path)) fs.unlinkSync(req.file.path); 

        // Error P2002: Unique constraint failed (NIM sudah ada)
        if (error.code === 'P2002') {
            return res.status(409).json({ success: false, message: 'NIM sudah terdaftar.' });
        }
        // Error P2003: Foreign key constraint failed (jika Enum salah)
        res.status(500).json({ success: false, message: 'Gagal menambahkan anggota. Cek format data.' });
    }
};

// 2. Mendapatkan Semua Anggota
const getAllAnggota = async (req, res) => {
    try {
        const anggotaList = await prisma.anggota.findMany({
            orderBy: { name: 'asc' }
        });
        
        // Mapping URL foto agar bisa diakses Android
        const mappedList = anggotaList.map(member => ({
            ...member,
            photoUrl: member.photoPath ? `http://${req.headers.host}/uploads/${member.photoPath}` : null
        }));

        res.status(200).json({
            success: true,
            data: mappedList
        });
    } catch (error) {
        res.status(500).json({ success: false, message: 'Gagal mengambil data anggota.' });
    }
};

// 3. Mengubah Data Anggota (Berdasarkan NIM)
const updateAnggota = async (req, res) => {
    const { targetNim } = req.params; // NIM yang akan diedit (dikirim via URL)
    const { name, gender, faculty, major, contact, email } = req.body;
    const newPhoto = req.file ? req.file.filename : undefined;

    try {
        // Cari data lama untuk menghapus foto lama jika ada foto baru
        const oldData = await prisma.anggota.findUnique({ 
            where: { nim: targetNim } 
        });

        if (!oldData) return res.status(404).json({ success: false, message: 'Anggota tidak ditemukan.' });

        const updateData = {
            name,
            gender,
            faculty,
            major,
            contact,
            email: email || oldData.email // Keep old email if not provided
        };

        if (newPhoto) {
            updateData.photoPath = newPhoto;
            // Hapus file fisik lama
            if (oldData.photoPath) {
                const oldPath = path.join(__dirname, '../uploads/', oldData.photoPath);
                if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath);
            }
        }

        const updatedAnggota = await prisma.anggota.update({
            where: { nim: targetNim },
            data: updateData,
        });

        res.status(200).json({ 
            success: true,
            message: 'Data anggota berhasil diubah', 
            data: updatedAnggota 
        });
    } catch (error) {
        console.error("Update Error:", error);
        res.status(500).json({ success: false, message: 'Gagal mengubah data anggota.' });
    }
};

// 4. Menghapus Anggota (Berdasarkan NIM)
const deleteAnggota = async (req, res) => {
    const { targetNim } = req.params;

    try {
        const memberToDelete = await prisma.anggota.findUnique({ where: { nim: targetNim } });
        
        if (!memberToDelete) return res.status(404).json({ success: false, message: 'Anggota tidak ditemukan.' });

        // Cek Peminjaman aktif menggunakan relasi anggotaNim
        const activeBorrowings = await prisma.peminjaman.count({
            where: { 
                anggotaNim: targetNim, 
                isReturned: false 
            },
        });

        if (activeBorrowings > 0) {
            return res.status(400).json({ success: false, message: 'Anggota ini masih meminjam buku.' });
        }

        // Hapus dari DB
        await prisma.anggota.delete({ where: { nim: targetNim } });

        // Hapus File Foto
        if (memberToDelete.photoPath) {
            const filePath = path.join(__dirname, '../uploads/', memberToDelete.photoPath);
            if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
        }

        res.status(200).json({ success: true, message: 'Anggota berhasil dihapus.' });
    } catch (error) {
        console.error("Delete Error:", error);
        res.status(500).json({ success: false, message: 'Gagal menghapus anggota.' });
    }
};

module.exports = { createAnggota, getAllAnggota, updateAnggota, deleteAnggota };