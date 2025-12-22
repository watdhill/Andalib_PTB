// ============================================================
// NOTIFICATION SCHEDULER
// ============================================================
// Scheduler untuk menjalankan notifikasi otomatis
// Menggunakan node-cron untuk schedule tasks

// Sebelum menggunakan, install dulu:
// npm install node-cron

const cron = require("node-cron");
const {
  notifyUpcomingDueDate,
  notifyOverduePeminjaman,
} = require("./peminjamanNotificationHelper");

/**
 * ⏰ Jalankan reminder peminjaman yang akan jatuh tempo
 * Jadwal: Setiap hari jam 08:00
 *
 * Notifikasi akan dibuat untuk peminjaman yang:
 * - Status: DIPINJAM
 * - Jatuh tempo dalam 1-2 hari ke depan
 */
const scheduleUpcomingDueDateReminder = () => {
  const task = cron.schedule("0 8 * * *", async () => {
    console.log(
      "⏰ [" +
        new Date().toISOString() +
        "] Running upcoming due date reminder..."
    );
    try {
      await notifyUpcomingDueDate();
      console.log("✅ Upcoming due date reminder completed");
    } catch (error) {
      console.error("❌ Error in upcoming due date reminder:", error);
    }
  });

  console.log("✅ Scheduled: Upcoming Due Date Reminder at 08:00 every day");
  return task;
};

/**
 * ⏰ Jalankan reminder untuk peminjaman yang sudah overdue
 * Jadwal: Setiap hari jam 09:00
 *
 * Notifikasi akan dibuat untuk peminjaman yang:
 * - Status: DIPINJAM
 * - Jatuh tempo sudah lewat (kemarin atau lebih lama)
 */
const scheduleOverdueReminder = () => {
  const task = cron.schedule("0 9 * * *", async () => {
    console.log(
      "⏰ [" + new Date().toISOString() + "] Running overdue reminder..."
    );
    try {
      await notifyOverduePeminjaman();
      console.log("✅ Overdue reminder completed");
    } catch (error) {
      console.error("❌ Error in overdue reminder:", error);
    }
  });

  console.log("✅ Scheduled: Overdue Reminder at 09:00 every day");
  return task;
};

/**
 * 🚀 Start semua scheduler
 * Panggil function ini saat server startup
 */
const startAllSchedulers = () => {
  console.log("\n🔄 ============================================");
  console.log("   INITIALIZING NOTIFICATION SCHEDULERS");
  console.log("   ============================================");

  try {
    scheduleUpcomingDueDateReminder();
    scheduleOverdueReminder();

    console.log("🚀 All notification schedulers started successfully!");
    console.log("   ============================================\n");
  } catch (error) {
    console.error("❌ Error starting schedulers:", error);
    // Jangan throw error agar server tetap berjalan
  }
};

/**
 * 🧹 Stop semua scheduler
 * Berguna untuk graceful shutdown
 */
const stopAllSchedulers = () => {
  console.log("🛑 Stopping all schedulers...");
  cron.getTasks().forEach((task) => {
    task.stop();
  });
  console.log("✅ All schedulers stopped");
};

// ============================================================
// ALTERNATIVE: Manual Trigger (untuk testing)
// ============================================================

/**
 * Jalankan reminder secara manual (untuk testing)
 */
const triggerUpcomingDueDateReminderManual = async (adminId = null) => {
  console.log("🔔 Triggering upcoming due date reminder manually...");
  try {
    await notifyUpcomingDueDate(adminId);
    console.log("✅ Manual trigger completed");
    return { success: true };
  } catch (error) {
    console.error("❌ Error:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Jalankan overdue reminder secara manual (untuk testing)
 */
const triggerOverdueReminderManual = async (adminId = null) => {
  console.log("🔔 Triggering overdue reminder manually...");
  try {
    await notifyOverduePeminjaman(adminId);
    console.log("✅ Manual trigger completed");
    return { success: true };
  } catch (error) {
    console.error("❌ Error:", error);
    return { success: false, error: error.message };
  }
};

module.exports = {
  startAllSchedulers,
  stopAllSchedulers,
  scheduleUpcomingDueDateReminder,
  scheduleOverdueReminder,
  triggerUpcomingDueDateReminderManual,
  triggerOverdueReminderManual,
};
