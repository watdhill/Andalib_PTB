// ============================================================
// DASHBOARD CONTROLLER
// ============================================================
// Controller untuk mengambil statistik dashboard perpustakaan

const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

// ============================================================
// GET DASHBOARD STATISTICS
// ============================================================
const getDashboardStats = async (req, res) => {
    try {
        // Total buku
        const totalBooks = await prisma.buku.count();

        // Total anggota
        const totalMembers = await prisma.anggota.count();

        // Total peminjaman aktif (belum dikembalikan)
        const activeBorrowings = await prisma.peminjaman.count({
            where: {
                status: 'DIPINJAM'
            }
        });

        // Total peminjaman terlambat (masih dipinjam dan melewati jatuh tempo)
        const today = new Date();
        const overdueBorrowings = await prisma.peminjaman.count({
            where: {
                status: 'DIPINJAM',
                jatuhTempo: {
                    lt: today
                }
            }
        });

        // Total stok buku tersedia
        const booksWithStock = await prisma.buku.findMany({
            select: {
                stok: true
            }
        });
        const totalStock = booksWithStock.reduce((sum, book) => sum + (book.stok || 0), 0);

        // Aktivitas terkini (5 peminjaman terakhir)
        const recentActivities = await prisma.peminjaman.findMany({
            take: 5,
            orderBy: {
                tanggalPinjam: 'desc'
            },
            include: {
                anggota: {
                    select: {
                        nim: true,
                        name: true
                    }
                },
                buku: {
                    select: {
                        title: true
                    }
                },
                pengembalian: {
                    select: {
                        tanggalPengembalian: true
                    }
                }
            }
        });

        // Format aktivitas
        const formattedActivities = recentActivities.map(activity => ({
            id: activity.id,
            memberName: activity.anggota.name,
            memberNim: activity.anggota.nim,
            bookTitle: activity.buku.title,
            borrowDate: activity.tanggalPinjam.toISOString(),
            dueDate: activity.jatuhTempo.toISOString(),
            returnDate: activity.pengembalian ? activity.pengembalian.tanggalPengembalian.toISOString() : null,
            status: activity.status === 'DIKEMBALIKAN' ? 'returned' : (new Date() > activity.jatuhTempo ? 'overdue' : 'active')
        }));

        res.status(200).json({
            success: true,
            message: 'Dashboard statistics retrieved successfully',
            data: {
                totalBooks,
                totalMembers,
                activeBorrowings,
                overdueBorrowings,
                totalStock,
                recentActivities: formattedActivities
            }
        });
    } catch (error) {
        console.error('Get Dashboard Stats Error:', error);
        res.status(500).json({
            success: false,
            message: 'Gagal mengambil statistik dashboard',
            error: error.message
        });
    }
};

module.exports = {
    getDashboardStats
};
