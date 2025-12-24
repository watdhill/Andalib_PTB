// controllers/bukuController.js
const { PrismaClient } = require('@prisma/client');

const prisma = new PrismaClient();

/**
 * Menambahkan Buku baru ke database
 */
const createBuku = async (req, res) => {
    const { isbn, title, author, publicationYear, stok, kategoriId, kategoriName } = req.body;

    if (!isbn || !title || !author || (!kategoriId && !kategoriName)) {
        return res.status(400).json({ message: 'Field isbn, title, author, dan kategori (id atau name) wajib diisi.' });
    }
    try {
        // Cek apakah ISBN sudah digunakan
        const existingBuku = await prisma.buku.findUnique({ where: { isbn } });
        if (existingBuku) {
            return res.status(400).json({ message: 'ISBN sudah digunakan oleh buku lain.' });
        }

        // Tentukan kategori: gunakan kategoriId jika ada, atau cari/buat berdasarkan kategoriName
        let kategori;
        if (kategoriId) {
            kategori = await prisma.kategori.findUnique({ where: { id: Number(kategoriId) } });
            if (!kategori) {
                return res.status(404).json({ message: 'Kategori tidak ditemukan.' });
            }
        } else {
            // cari berdasarkan nama (unik) - gunakan findUnique karena `name` bertanda @unique
            kategori = await prisma.kategori.findUnique({ where: { name: kategoriName } });
            if (!kategori) {
                kategori = await prisma.kategori.create({ data: { name: kategoriName } });
            }
        }

        const newBuku = await prisma.buku.create({
            data: {
                isbn,
                title,
                author,
                publicationYear: publicationYear ? Number(publicationYear) : null,
                stok: stok ? Number(stok) : 1,
                kategori: { connect: { id: kategori.id } },
            },
        });

        res.status(201).json({ message: 'Buku berhasil ditambahkan', buku: newBuku });
    } catch (error) {
        console.error('Error creating Buku:', error);
        // Handle unique constraint error dari database
        if (error.code === 'P2002') {
            return res.status(400).json({ message: 'ISBN sudah digunakan oleh buku lain.' });
        }
        res.status(500).json({ message: 'Gagal menambahkan buku.' });
    }
};

/**
 * Mendapatkan semua Buku
 */
const getAllBuku = async (req, res) => {
    try {
        const bukuList = await prisma.buku.findMany({
            include: { kategori: true },
            orderBy: { title: 'asc' },
        });
        res.status(200).json(bukuList);
    } catch (error) {
        console.error('Error fetching Buku:', error);
        res.status(500).json({ message: 'Gagal mengambil data buku.' });
    }
};

/**
 * Mencari Buku berdasarkan query
 */
const searchBuku = async (req, res) => {
    const { q } = req.query;

    if (!q || q.length < 1) {
        return res.json([]);
    }

    try {
        const bukuList = await prisma.buku.findMany({
            where: {
                OR: [
                    { isbn: { contains: q } },
                    { title: { contains: q } },
                    { author: { contains: q } }
                ]
            },
            include: { kategori: true },
            take: 10,
            orderBy: { title: 'asc' }
        });
        res.status(200).json(bukuList);
    } catch (error) {
        console.error('Search Buku Error:', error);
        res.status(500).json({ message: 'Gagal mencari buku.' });
    }
};

/**
 * Mendapatkan detail Buku berdasarkan id
 */
const getBukuById = async (req, res) => {
    const id = parseInt(req.params.id);
    try {
        const buku = await prisma.buku.findUnique({
            where: { id },
            include: { kategori: true },
        });
        if (!buku) return res.status(404).json({ message: 'Buku tidak ditemukan.' });
        res.status(200).json(buku);
    } catch (error) {
        console.error('Error fetching Buku by ID:', error);
        res.status(500).json({ message: 'Gagal mengambil detail buku.' });
    }
};

/**
 * Mengubah data Buku
 */
const updateBuku = async (req, res) => {
    const id = parseInt(req.params.id);
    const { isbn, title, author, publicationYear, stok, kategoriId, kategoriName } = req.body;

    if (!isbn && !title && !author && !publicationYear && !stok && !kategoriId && !kategoriName) {
        return res.status(400).json({ message: 'Setidaknya satu field harus diisi untuk update.' });
    }

    try {
        // Pastikan buku ada
        const existing = await prisma.buku.findUnique({ where: { id } });
        if (!existing) return res.status(404).json({ message: 'Buku tidak ditemukan.' });

        // Cek apakah ISBN baru sudah digunakan oleh buku lain
        if (isbn && isbn !== existing.isbn) {
            const existingIsbn = await prisma.buku.findUnique({ where: { isbn } });
            if (existingIsbn) {
                return res.status(400).json({ message: 'ISBN sudah digunakan oleh buku lain.' });
            }
        }

        // Jika kategoriId atau kategoriName diberikan, pastikan atau buat kategori
        let connectKategori = undefined;
        if (kategoriId) {
            const kategori = await prisma.kategori.findUnique({ where: { id: Number(kategoriId) } });
            if (!kategori) return res.status(404).json({ message: 'Kategori tidak ditemukan.' });
            connectKategori = { connect: { id: Number(kategoriId) } };
        } else if (kategoriName) {
            let kategori = await prisma.kategori.findUnique({ where: { name: kategoriName } });
            if (!kategori) {
                kategori = await prisma.kategori.create({ data: { name: kategoriName } });
            }
            connectKategori = { connect: { id: kategori.id } };
        }

        const updated = await prisma.buku.update({
            where: { id },
            data: {
                ...(isbn !== undefined ? { isbn } : {}),
                ...(title !== undefined ? { title } : {}),
                ...(author !== undefined ? { author } : {}),
                ...(publicationYear !== undefined ? { publicationYear: publicationYear ? Number(publicationYear) : null } : {}),
                ...(stok !== undefined ? { stok: Number(stok) } : {}),
                ...(connectKategori ? { kategori: connectKategori } : {}),
            },
            include: { kategori: true },
        });

        res.status(200).json({ message: 'Data buku berhasil diubah', buku: updated });
    } catch (error) {
        console.error('Error updating Buku:', error);
        if (error.code === 'P2025') return res.status(404).json({ message: 'Buku tidak ditemukan.' });
        // Handle unique constraint error dari database
        if (error.code === 'P2002') {
            return res.status(400).json({ message: 'ISBN sudah digunakan oleh buku lain.' });
        }
        res.status(500).json({ message: 'Gagal mengubah data buku.' });
    }
};

/**
 * Menghapus Buku (cek peminjaman aktif)
 */
const deleteBuku = async (req, res) => {
    const id = parseInt(req.params.id);

    try {
        // cek apakah buku ada
        const existing = await prisma.buku.findUnique({ where: { id } });
        if (!existing) return res.status(404).json({ message: 'Buku tidak ditemukan.' });

        // cek peminjaman aktif terkait buku
        const activeBorrowings = await prisma.peminjaman.count({ where: { bukuId: id, status: 'DIPINJAM' } });
        if (activeBorrowings > 0) {
            return res.status(400).json({ message: `Buku ini tidak bisa dihapus karena ada ${activeBorrowings} peminjaman aktif.` });
        }

        await prisma.buku.delete({ where: { id } });
        res.status(200).json({ message: 'Buku berhasil dihapus.' });
    } catch (error) {
        console.error('Error deleting Buku:', error);
        if (error.code === 'P2025') return res.status(404).json({ message: 'Buku tidak ditemukan.' });
        res.status(500).json({ message: 'Gagal menghapus buku.' });
    }
};

module.exports = {
    createBuku,
    getAllBuku,
    searchBuku,
    getBukuById,
    updateBuku,
    deleteBuku,
};
