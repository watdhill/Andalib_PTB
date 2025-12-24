// ============================================================
// DEBUG SCRIPT UNTUK NOTIFIKASI PEMINJAMAN
// ============================================================
// Jalankan: node DEBUG_NOTIFIKASI.js

const { PrismaClient } = require("@prisma/client");
const prisma = new PrismaClient();

async function debugNotification() {
  console.log("🔍 DEBUGGING NOTIFIKASI PEMINJAMAN\n");
  console.log("=".repeat(60) + "\n");

  try {
    // 1. Cek apakah Notification table ada
    console.log("1️⃣  Checking Notification table...");
    const notifications = await prisma.notification.findMany({
      take: 5,
      orderBy: { createdAt: "desc" },
    });
    console.log(
      `✅ Notification table found. Total records: ${notifications.length}`
    );
    console.log("   Recent notifications:");
    notifications.forEach((n, idx) => {
      console.log(`   [${idx + 1}] ${n.type} - ${n.title}`);
      console.log(`       Message: ${n.message.substring(0, 50)}...`);
      console.log(`       CreatedAt: ${n.createdAt}`);
      console.log("");
    });

    // 2. Cek apakah Admin ada
    console.log("\n2️⃣  Checking Admin table...");
    const admins = await prisma.admin.findMany();
    console.log(`✅ Found ${admins.length} admin(s):`);
    admins.forEach((a, idx) => {
      console.log(
        `   [${idx + 1}] ID: ${a.id}, Name: ${a.name}, Email: ${a.email}`
      );
    });

    // 3. Cek apakah Peminjaman ada
    console.log("\n3️⃣  Checking Peminjaman table...");
    const peminjaman = await prisma.peminjaman.findMany({
      take: 3,
      orderBy: { tanggalPinjam: "desc" },
      include: {
        anggota: { select: { name: true, nim: true } },
        buku: { select: { title: true } },
        admin: { select: { name: true } },
      },
    });
    console.log(`✅ Found ${peminjaman.length} peminjaman record(s):`);
    peminjaman.forEach((p, idx) => {
      console.log(
        `   [${idx + 1}] ${p.anggota.name} meminjam "${p.buku.title}"`
      );
      console.log(`       Admin: ${p.admin?.name || "NULL"}`);
      console.log(`       Status: ${p.status}`);
      console.log("");
    });

    // 4. Cek apakah ada notifikasi untuk peminjaman yang ada
    console.log("\n4️⃣  Checking notification-peminjaman relationship...");
    const notificationsForPeminjaman = await prisma.notification.findMany({
      where: { type: "PEMINJAMAN_BARU" },
      take: 5,
    });
    console.log(
      `✅ Found ${notificationsForPeminjaman.length} PEMINJAMAN_BARU notification(s)`
    );

    // 5. Test manual notification create
    console.log("\n5️⃣  Testing manual notification creation...");
    if (admins.length > 0) {
      const testNotif = await prisma.notification.create({
        data: {
          adminId: admins[0].id,
          type: "PEMINJAMAN_BARU",
          title: "TEST - Peminjaman Baru Dicatat",
          message: "Ini adalah notifikasi test",
          metadata: JSON.stringify({ test: true }),
          isRead: false,
        },
      });
      console.log(`✅ Test notification created successfully!`);
      console.log(`   ID: ${testNotif.id}`);
      console.log(`   Admin ID: ${testNotif.adminId}`);
      console.log(`   Type: ${testNotif.type}`);
      console.log(`   Created At: ${testNotif.createdAt}`);

      // Delete test notification
      await prisma.notification.delete({ where: { id: testNotif.id } });
      console.log("✅ Test notification deleted");
    } else {
      console.log("⚠️  No admins found - cannot test");
    }

    console.log("\n" + "=".repeat(60));
    console.log("✅ DEBUG COMPLETE\n");
  } catch (error) {
    console.error("❌ ERROR:", error.message);
    console.error("\nStack trace:");
    console.error(error);
  } finally {
    await prisma.$disconnect();
  }
}

// Run debug
debugNotification();
