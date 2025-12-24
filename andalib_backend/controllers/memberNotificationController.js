

const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

const getUnreadNotifications = async (req, res) => {
    try {
        const currentAdminId = req.user.id;

        const notifications = await prisma.notification.findMany({
            where: {
                adminId: currentAdminId,
                isRead: false
            },
            orderBy: {
                createdAt: 'desc'
            }
        });

    
        const formattedNotifications = notifications.map(notif => {
            let parsedMetadata = {};
            try {
                if (notif.metadata) {
                    parsedMetadata = JSON.parse(notif.metadata);
                }
            } catch (e) {
                console.error('Error parsing metadata:', e);
            }

            return {
                id: notif.id,
                admin_id: notif.adminId,
                notification_type: notif.type,
                title: notif.title,
                message: notif.message,
                member_name: parsedMetadata.memberName || null,
                member_nim: parsedMetadata.memberNim || null,
                deleted_by_admin_name: parsedMetadata.deletedBy || null,
                is_read: notif.isRead,
                created_at: notif.createdAt.toISOString()
            };
        });

        res.status(200).json({
            success: true,
            message: 'Success',
            data: formattedNotifications
        });
    } catch (error) {
        console.error('Get Unread Notifications Error:', error);
        res.status(500).json({
            success: false,
            message: 'Gagal mengambil notifikasi'
        });
    }
};

const getAllNotifications = async (req, res) => {
    try {
        const currentAdminId = req.user.id;

        const notifications = await prisma.notification.findMany({
            where: {
                adminId: currentAdminId
            },
            orderBy: {
                createdAt: 'desc'
            }
        });

        const formattedNotifications = notifications.map(notif => {
            let parsedMetadata = {};
            try {
                if (notif.metadata) {
                    parsedMetadata = JSON.parse(notif.metadata);
                }
            } catch (e) {
                console.error('Error parsing metadata:', e);
            }

            return {
                id: notif.id,
                admin_id: notif.adminId,
                notification_type: notif.type,
                title: notif.title,
                message: notif.message,
                member_name: parsedMetadata.memberName || null,
                member_nim: parsedMetadata.memberNim || null,
                deleted_by_admin_name: parsedMetadata.deletedBy || null,
                is_read: notif.isRead,
                created_at: notif.createdAt.toISOString()
            };
        });

        res.status(200).json({
            success: true,
            message: 'Success',
            data: formattedNotifications
        });
    } catch (error) {
        console.error('Get All Notifications Error:', error);
        res.status(500).json({
            success: false,
            message: 'Gagal mengambil notifikasi'
        });
    }
};

const markAsRead = async (req, res) => {
    try {
        const notificationId = parseInt(req.params.id);
        const currentAdminId = req.user.id;

        const notification = await prisma.notification.findFirst({
            where: {
                id: notificationId,
                adminId: currentAdminId
            }
        });

        if (!notification) {
            return res.status(404).json({
                success: false,
                message: 'Notifikasi tidak ditemukan'
            });
        }

        
        await prisma.notification.update({
            where: {
                id: notificationId
            },
            data: {
                isRead: true,
                readAt: new Date()
            }
        });

        res.status(200).json({
            success: true,
            message: 'Notifikasi ditandai sebagai sudah dibaca'
        });
    } catch (error) {
        console.error('Mark As Read Error:', error);
        res.status(500).json({
            success: false,
            message: 'Gagal update status notifikasi'
        });
    }
};


const getUnreadCount = async (req, res) => {
    try {
        const currentAdminId = req.user.id;

        const count = await prisma.notification.count({
            where: {
                adminId: currentAdminId,
                isRead: false
            }
        });

        res.status(200).json({
            success: true,
            message: 'Success',
            count: count
        });
    } catch (error) {
        console.error('Get Unread Count Error:', error);
        res.status(500).json({
            success: false,
            message: 'Gagal menghitung notifikasi'
        });
    }
};

module.exports = {
    getUnreadNotifications,
    getAllNotifications,
    markAsRead,
    getUnreadCount
};
