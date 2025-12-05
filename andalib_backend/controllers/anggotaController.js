// controllers/anggotaController.js
const { PrismaClient } = require('@prisma/client');

const prisma = new PrismaClient();

/**
 * 1. Menambahkan Anggota Baru
 */
const createAnggota = async (req, res) => {
    const { name, memberId, address, phone } = req.body;
    
    if (!name) {
        return res.status(400).json({ message: 'Nama anggota wajib diisi.' });
    }

    try {
        const newAnggota = await prisma.anggota.create({
            data: {
                name,
                memberId: memberId || null, // memberId bersifat opsional
                address,
                phone,
            },
        });

        res.status(201).json({ 
            message: 'Anggota berhasil ditambahkan', 
            anggota: newAnggota 
        });
    } catch (error) {
        console.error('Error creating Anggota:', error);
        // Tangani error unik (jika memberId sudah ada)
        if (error.code === 'P2002' && error.meta.target.includes('memberId')) {
            return res.status(409).json({ message: 'Nomor anggota sudah digunakan.' });
        }
        res.status(500).json({ message: 'Gagal menambahkan anggota.' });
    }
};

/**
 * 2. Mendapatkan Semua Anggota
 */
const getAllAnggota = async (req, res) => {
    try {
        const anggotaList = await prisma.anggota.findMany({
            // Bisa tambahkan filtering, sorting, pagination jika dibutuhkan
            orderBy: { name: 'asc' } 
        });
        res.status(200).json(anggotaList);
    } catch (error) {
        console.error('Error fetching Anggota:', error);
        res.status(500).json({ message: 'Gagal mengambil data anggota.' });
    }
};

/**
 * 3. Mendapatkan Detail Anggota
 */
const getAnggotaById = async (req, res) => {
    const id = parseInt(req.params.id);

    try {
        const anggota = await prisma.anggota.findUnique({
            where: { id },
        });

        if (!anggota) {
            return res.status(404).json({ message: 'Anggota tidak ditemukan.' });
        }
        res.status(200).json(anggota);
    } catch (error) {
        console.error('Error fetching Anggota by ID:', error);
        res.status(500).json({ message: 'Gagal mengambil detail anggota.' });
    }
};

/**
 * 4. Mengubah Data Anggota
 */
const updateAnggota = async (req, res) => {
    const id = parseInt(req.params.id);
    const { name, memberId, address, phone } = req.body;

    // Pastikan setidaknya ada satu field yang diubah
    if (!name && !memberId && !address && !phone) {
        return res.status(400).json({ message: 'Setidaknya satu field harus diisi untuk update.' });
    }

    try {
        const updatedAnggota = await prisma.anggota.update({
            where: { id },
            data: {
                name,
                memberId,
                address,
                phone,
            },
        });

        res.status(200).json({ 
            message: 'Data anggota berhasil diubah', 
            anggota: updatedAnggota 
        });
    } catch (error) {
        console.error('Error updating Anggota:', error);
        // Error P2025: Record to update not found
        if (error.code === 'P2025') {
            return res.status(404).json({ message: 'Anggota tidak ditemukan.' });
        }
        res.status(500).json({ message: 'Gagal mengubah data anggota.' });
    }
};

/**
 * 5. Menghapus Anggota
 */
const deleteAnggota = async (req, res) => {
    const id = parseInt(req.params.id);

    try {
        // Cek apakah anggota memiliki Peminjaman aktif
        const activeBorrowings = await prisma.peminjaman.count({
            where: {
                anggotaId: id,
                isReturned: false,
            },
        });

        if (activeBorrowings > 0) {
            return res.status(400).json({ message: `Anggota ini tidak bisa dihapus karena masih memiliki ${activeBorrowings} peminjaman aktif.` });
        }

        await prisma.anggota.delete({
            where: { id },
        });

        res.status(200).json({ message: 'Anggota berhasil dihapus.' });
    } catch (error) {
        console.error('Error deleting Anggota:', error);
        if (error.code === 'P2025') {
            return res.status(404).json({ message: 'Anggota tidak ditemukan.' });
        }
        res.status(500).json({ message: 'Gagal menghapus anggota.' });
    }
};


module.exports = {
    createAnggota,
    getAllAnggota,
    getAnggotaById,
    updateAnggota,
    deleteAnggota,
};