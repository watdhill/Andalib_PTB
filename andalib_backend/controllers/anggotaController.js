const { PrismaClient } = require('@prisma/client');
const fs = require('fs');
const path = require('path');

const prisma = new PrismaClient();

// 1. Menambahkan Anggota Baru
const createAnggota = async (req, res) => {
    const { nim, name, gender, faculty, major, contact, email } = req.body;
    const photoPath = req.file ? req.file.filename : null;

    if (!nim || !name || !gender || !faculty || !major || !contact) {
        if (req.file) fs.unlinkSync(req.file.path);
        return res.status(400).json({
            success: false,
            message: 'NIM, Nama, Gender, Fakultas, Jurusan, dan Kontak wajib diisi.'
        });
    }

    try {
        const newAnggota = await prisma.anggota.create({
            data: {
                nim, name, gender, faculty, major, contact, email,
                photoPath
            },
        });

        res.status(201).json({
            success: true,
            message: 'Anggota berhasil ditambahkan',
            data: newAnggota
        });
    } catch (error) {
        console.error("Create Error:", error);
        if (req.file && fs.existsSync(req.file.path)) fs.unlinkSync(req.file.path);

        if (error.code === 'P2002') {
            return res.status(409).json({ success: false, message: 'NIM sudah terdaftar.' });
        }
        res.status(500).json({ success: false, message: 'Gagal menambahkan anggota. Cek format data.' });
    }
};

// 2. Mendapatkan Semua Anggota
const getAllAnggota = async (req, res) => {
    try {
        const anggotaList = await prisma.anggota.findMany({
            orderBy: { name: 'asc' }
        });

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

// 2a. Mencari Anggota berdasarkan Query
const searchAnggota = async (req, res) => {
    const { q } = req.query;

    if (!q || q.length < 1) {
        return res.json([]);
    }

    try {
        const anggotaList = await prisma.anggota.findMany({
            where: {
                OR: [
                    { name: { contains: q } },
                    { nim: { contains: q } }
                ]
            },
            select: {
                nim: true,
                name: true,
                major: true,
                contact: true,
                email: true
            },
            take: 10,
            orderBy: { name: 'asc' }
        });

        res.json(anggotaList);
    } catch (error) {
        console.error('Search Anggota Error:', error);
        res.status(500).json({ error: 'Gagal mencari anggota' });
    }
};

// 2b. Mendapatkan Detail Anggota Berdasarkan NIM
const getAnggotaByNim = async (req, res) => {
    const { targetNim } = req.params;

    try {
        const member = await prisma.anggota.findUnique({
            where: { nim: targetNim }
        });

        if (!member) {
            return res.status(404).json({
                success: false,
                message: 'Anggota tidak ditemukan.'
            });
        }

        const memberWithPhotoUrl = {
            ...member,
            photoUrl: member.photoPath ? `http://${req.headers.host}/uploads/${member.photoPath}` : null
        };

        res.status(200).json({
            success: true,
            data: memberWithPhotoUrl
        });
    } catch (error) {
        console.error("Get Anggota By NIM Error:", error);
        res.status(500).json({
            success: false,
            message: 'Gagal mengambil data anggota.'
        });
    }
};

// 3. Mengubah Data Anggota (Berdasarkan NIM)
const updateAnggota = async (req, res) => {
    const { targetNim } = req.params;
    const { name, gender, faculty, major, contact, email } = req.body;
    const newPhoto = req.file ? req.file.filename : undefined;

    try {
        const oldData = await prisma.anggota.findUnique({
            where: { nim: targetNim }
        });

        if (!oldData) return res.status(404).json({ success: false, message: 'Anggota tidak ditemukan.' });

        const updateData = {
            name, gender, faculty, major, contact,
            email: email || oldData.email
        };

        if (newPhoto) {
            updateData.photoPath = newPhoto;
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

        const activeBorrowings = await prisma.peminjaman.count({
            where: {
                anggotaNim: targetNim,
                status: 'DIPINJAM'
            },
        });

        if (activeBorrowings > 0) {
            return res.status(400).json({ success: false, message: 'Anggota ini masih meminjam buku.' });
        }

        await prisma.anggota.delete({ where: { nim: targetNim } });

        if (memberToDelete.photoPath) {
            const filePath = path.join(__dirname, '../uploads/', memberToDelete.photoPath);
            if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
        }

        // ========== NOTIFIKASI: CREATE NOTIFICATIONS ==========
        console.log('🔔 Creating notifications...');
        try {
            const allAdmins = await prisma.admin.findMany({
                select: { id: true, name: true }
            });

            const currentAdminId = req.user?.id || null;
            const currentAdmin = currentAdminId ? await prisma.admin.findUnique({
                where: { id: currentAdminId }
            }) : null;

            const adminName = currentAdmin?.name || 'Admin';
            const metadata = JSON.stringify({
                memberName: memberToDelete.name,
                memberNim: memberToDelete.nim,
                deletedBy: adminName,
                deletedById: currentAdminId
            });

            const notificationPromises = allAdmins.map(admin =>
                prisma.notification.create({
                    data: {
                        adminId: admin.id,
                        type: 'MEMBER_DELETED',
                        title: 'Admin menghapus anggota',
                        message: `${memberToDelete.name} (${memberToDelete.nim})`,
                        metadata: metadata
                    }
                })
            );

            await Promise.all(notificationPromises);
            console.log(`✅ Created ${allAdmins.length} notifications`);
        } catch (notifError) {
            console.error('❌ Notification error:', notifError.message);
        }
        // ========== END NOTIFIKASI ==========

        res.status(200).json({ success: true, message: 'Anggota berhasil dihapus.' });
    } catch (error) {
        console.error("Delete Error:", error);
        res.status(500).json({ success: false, message: 'Gagal menghapus anggota.' });
    }
};

// ============================================================
// 6. Upload Member Photo
// ============================================================
const uploadMemberPhoto = async (req, res) => {
    try {
        const memberId = parseInt(req.params.id);

        if (!req.file) {
            return res.status(400).json({
                success: false,
                message: 'No file uploaded'
            });
        }

        // Check if member exists
        const member = await prisma.anggota.findUnique({
            where: { id: memberId }
        });

        if (!member) {
            // Delete uploaded file if member not found
            fs.unlinkSync(req.file.path);
            return res.status(404).json({
                success: false,
                message: 'Anggota tidak ditemukan'
            });
        }

        // Delete old photo if exists
        if (member.photoPath) {
            const oldPhotoPath = path.join(__dirname, '..', 'uploads', member.photoPath);
            if (fs.existsSync(oldPhotoPath)) {
                fs.unlinkSync(oldPhotoPath);
            }
        }

        // Update member with new photo
        const photoPath = req.file.filename;
        const updatedMember = await prisma.anggota.update({
            where: { id: memberId },
            data: { photoPath }
        });

        res.status(200).json({
            success: true,
            message: 'Foto berhasil diupload',
            data: {
                photoPath: photoPath,
                photoUrl: `/uploads/${photoPath}`
            }
        });
    } catch (error) {
        console.error('Upload Photo Error:', error);

        // Delete uploaded file on error
        if (req.file) {
            fs.unlinkSync(req.file.path);
        }

        res.status(500).json({
            success: false,
            message: 'Gagal mengupload foto'
        });
    }
};

module.exports = {
    createAnggota,
    getAllAnggota,
    searchAnggota,
    getAnggotaByNim,
    updateAnggota,
    deleteAnggota,
    uploadMemberPhoto
};