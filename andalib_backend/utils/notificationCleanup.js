// ============================================================
// NOTIFICATION CLEANUP SCHEDULER
// ============================================================
// Auto-delete notifications yang sudah dibaca > 2 menit

const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

/**
 * Cleanup notifications yang sudah dibaca lebih dari 2 menit
 */
const cleanupReadNotifications = async () => {
    try {
        const twoMinutesAgo = new Date(Date.now() - 2 * 60 * 1000); // 2 menit yang lalu

        const result = await prisma.notification.deleteMany({
            where: {
                isRead: true,
                readAt: {
                    lte: twoMinutesAgo // Less than or equal (lebih lama dari 2 menit)
                }
            }
        });

        if (result.count > 0) {
            console.log(`🗑️  Deleted ${result.count} read notifications (older than 2 minutes)`);
        }
    } catch (error) {
        console.error('❌ Cleanup error:', error.message);
    }
};

/**
 * Start cleanup scheduler (runs every 30 seconds)
 */
const startCleanupScheduler = () => {
    console.log('🔄 Notification cleanup scheduler started (runs every 30 seconds)');

    // Run immediately on start
    cleanupReadNotifications();

    // Then run every 30 seconds
    setInterval(cleanupReadNotifications, 30 * 1000);
};

module.exports = {
    startCleanupScheduler,
    cleanupReadNotifications
};
