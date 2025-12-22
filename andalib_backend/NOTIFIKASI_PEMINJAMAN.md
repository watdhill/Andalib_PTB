# 📋 Dokumentasi Sistem Notifikasi Peminjaman

## 📌 Overview

Sistem notifikasi peminjaman telah diintegrasikan ke dalam aplikasi Andalib. Sistem ini akan otomatis membuat notifikasi ketika:

1. **Peminjaman baru dicatat** - Notifikasi saat admin membuat peminjaman
2. **Buku dikembalikan** - Notifikasi saat peminjaman diselesaikan
3. **Peminjaman akan jatuh tempo** - Reminder otomatis 1-2 hari sebelum jatuh tempo
4. **Peminjaman overdue** - Notifikasi untuk peminjaman yang sudah melewati jatuh tempo

---

## 🔧 Arsitektur Sistem

### File-file yang Terlibat:

```
utils/
  └── peminjamanNotificationHelper.js    ← Helper functions untuk notifikasi
controllers/
  ├── peminjamanController.js            ← Modified: trigger notifikasi saat create
  └── returnController.js                ← Modified: trigger notifikasi saat return
routes/
  └── peminjamanNotification.js          ← Routes untuk manual trigger (opsional)
prisma/
  └── schema.prisma                      ← Model Notification (sudah ada)
```

---

## 📊 Database Model

Model `Notification` di Prisma sudah support untuk menyimpan notifikasi peminjaman:

```prisma
model Notification {
  id        Int      @id @default(autoincrement())
  adminId   Int      // Admin yang menerima notifikasi
  admin     Admin    @relation(fields: [adminId], references: [id], onDelete: Cascade)
  type      String   // PEMINJAMAN_BARU, PENGEMBALIAN_BUKU, PEMINJAMAN_AKAN_JATUH_TEMPO, PEMINJAMAN_OVERDUE
  title     String   // Judul notifikasi
  message   String   @db.Text // Pesan lengkap
  metadata  String?  @db.Text // Data tambahan (JSON format)
  isRead    Boolean  @default(false)
  readAt    DateTime?
  createdAt DateTime @default(now())

  @@index([adminId, isRead])
  @@index([type])
  @@index([createdAt])
}
```

---

## 🔄 Jenis-jenis Notifikasi

### 1️⃣ PEMINJAMAN_BARU

**Trigger:** Saat admin membuat peminjaman baru (`POST /api/peminjaman`)

**Metadata:**

```json
{
  "memberName": "John Doe",
  "memberNim": "123456",
  "bookTitle": "Clean Code",
  "bookId": 5,
  "peminjamanId": 10,
  "dueDate": "2024-12-31T00:00:00.000Z"
}
```

**Contoh Pesan:**

```
John Doe (123456) meminjam buku "Clean Code". Jatuh tempo: 31/12/2024
```

### 2️⃣ PENGEMBALIAN_BUKU

**Trigger:** Saat admin mencatat pengembalian (`POST /api/returns`)

**Metadata:**

```json
{
  "memberName": "John Doe",
  "memberNim": "123456",
  "bookTitle": "Clean Code",
  "bookId": 5,
  "peminjamanId": 10,
  "pengembalianId": 3,
  "terlambat": false,
  "denda": 0,
  "kondisiKerusakan": "Normal"
}
```

**Contoh Pesan (tepat waktu):**

```
John Doe (123456) mengembalikan buku "Clean Code" tepat waktu.
```

**Contoh Pesan (terlambat):**

```
John Doe (123456) mengembalikan buku "Clean Code" TERLAMBAT. Denda: Rp10.000 (Buku mengalami kerusakan)
```

### 3️⃣ PEMINJAMAN_AKAN_JATUH_TEMPO

**Trigger:** Jalankan reminder 1-2 hari sebelum jatuh tempo (via cron job)

**Metadata:**

```json
{
  "memberName": "John Doe",
  "memberNim": "123456",
  "bookTitle": "Clean Code",
  "bookId": 5,
  "peminjamanId": 10,
  "dueDate": "2024-12-31T00:00:00.000Z"
}
```

**Contoh Pesan:**

```
Peminjaman buku "Clean Code" oleh John Doe (123456) akan jatuh tempo pada 31/12/2024.
```

### 4️⃣ PEMINJAMAN_OVERDUE

**Trigger:** Jalankan reminder untuk peminjaman yang sudah melewati jatuh tempo (via cron job)

**Metadata:**

```json
{
  "memberName": "John Doe",
  "memberNim": "123456",
  "bookTitle": "Clean Code",
  "bookId": 5,
  "peminjamanId": 10,
  "daysOverdue": 3
}
```

**Contoh Pesan:**

```
Buku "Clean Code" dipinjam oleh John Doe (123456) sudah terlambat 3 hari. Jatuh tempo: 31/12/2024.
```

---

## 🚀 API Endpoints

### Notifikasi Peminjaman (Opsional - untuk Manual Trigger)

#### Reminder Peminjaman akan Jatuh Tempo

```http
GET /api/notifications/peminjaman/upcoming-due-date
Authorization: Bearer <token>
```

**Query Parameters:**

- `adminId` (optional): ID admin tertentu, jika kosong semua admin mendapat notifikasi

**Response Success:**

```json
{
  "success": true,
  "message": "Notifikasi reminder berhasil dibuat"
}
```

#### Reminder Peminjaman Overdue

```http
GET /api/notifications/peminjaman/overdue
Authorization: Bearer <token>
```

**Query Parameters:**

- `adminId` (optional): ID admin tertentu

**Response Success:**

```json
{
  "success": true,
  "message": "Notifikasi overdue berhasil dibuat"
}
```

### Notifikasi Umum (Sudah Ada)

Untuk mendapatkan dan manage notifikasi, gunakan endpoints yang sudah ada di `/api/member-notifications`:

```http
GET /api/member-notifications/unread          # Ambil notifikasi belum dibaca
GET /api/member-notifications                 # Ambil semua notifikasi
GET /api/member-notifications/count           # Hitung notifikasi belum dibaca
PUT /api/member-notifications/:id/read        # Tandai sebagai dibaca
```

---

## ⚙️ Integrasi dengan app.js

Tambahkan route peminjaman notifikasi di `app.js`:

```javascript
// Di app.js, tambahkan setelah route lainnya
const peminjamanNotificationRoutes = require("./routes/peminjamanNotification");
app.use("/api/notifications/peminjaman", peminjamanNotificationRoutes);
```

---

## 🔄 Mengatur Cron Job (Scheduler)

Untuk notifikasi otomatis reminder dan overdue, Anda bisa menggunakan:

### Option 1: Node-Cron (Recommended)

Install package:

```bash
npm install node-cron
```

Buat file scheduler di `utils/notificationScheduler.js`:

```javascript
const cron = require("node-cron");
const {
  notifyUpcomingDueDate,
  notifyOverduePeminjaman,
} = require("./peminjamanNotificationHelper");

/**
 * Jalankan reminder setiap hari jam 08:00 (jatuh tempo 1-2 hari ke depan)
 */
const scheduleUpcomingDueDateReminder = () => {
  cron.schedule("0 8 * * *", async () => {
    console.log("⏰ Running upcoming due date reminder...");
    try {
      await notifyUpcomingDueDate();
    } catch (error) {
      console.error("Error in upcoming due date reminder:", error);
    }
  });
  console.log("✅ Scheduled: Upcoming Due Date Reminder at 08:00 every day");
};

/**
 * Jalankan overdue check setiap hari jam 09:00
 */
const scheduleOverdueReminder = () => {
  cron.schedule("0 9 * * *", async () => {
    console.log("⏰ Running overdue reminder...");
    try {
      await notifyOverduePeminjaman();
    } catch (error) {
      console.error("Error in overdue reminder:", error);
    }
  });
  console.log("✅ Scheduled: Overdue Reminder at 09:00 every day");
};

/**
 * Start all schedulers
 */
const startAllSchedulers = () => {
  scheduleUpcomingDueDateReminder();
  scheduleOverdueReminder();
  console.log("🚀 All notification schedulers started!");
};

module.exports = {
  startAllSchedulers,
  scheduleUpcomingDueDateReminder,
  scheduleOverdueReminder,
};
```

Gunakan di `app.js`:

```javascript
const { startAllSchedulers } = require("./utils/notificationScheduler");

// Setelah server startup
app.listen(PORT, () => {
  console.log(`Server running at ${PORT}`);
  startAllSchedulers(); // ✅ Start notification schedulers
});
```

### Option 2: Heroku Scheduler atau Cloud Scheduler

Jika hosting di cloud, Anda bisa setup external scheduler yang memanggil endpoint:

```bash
curl https://your-app.com/api/notifications/peminjaman/upcoming-due-date \
  -H "Authorization: Bearer <admin-token>"
```

---

## 🧪 Testing

### 1. Test Notifikasi Peminjaman Baru

```bash
curl -X POST http://localhost:3000/api/peminjaman \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "nim": "123456",
    "bukuId": 1,
    "jatuhTempo": "31/12/2024",
    "adminId": 1
  }'
```

Kemudian cek notifikasi:

```bash
curl -X GET http://localhost:3000/api/member-notifications/unread \
  -H "Authorization: Bearer <token>"
```

### 2. Test Notifikasi Pengembalian

```bash
curl -X POST http://localhost:3000/api/returns \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "peminjamanId": 1,
    "tanggalPengembalian": "25/12/2024",
    "denda": 10000
  }'
```

### 3. Test Manual Reminder

```bash
curl -X GET "http://localhost:3000/api/notifications/peminjaman/upcoming-due-date" \
  -H "Authorization: Bearer <token>"
```

---

## 📝 Catatan Penting

1. **Admin ID Required**: Notifikasi hanya dibuat jika ada `adminId` saat membuat peminjaman
2. **Async Operation**: Notifikasi dibuat secara async dan tidak mengganggu flow peminjaman
3. **Error Handling**: Jika ada error saat membuat notifikasi, tidak akan throw error ke response
4. **Metadata JSON**: Metadata disimpan sebagai string JSON untuk fleksibilitas
5. **Timezone**: Gunakan UTC untuk consistency di environment yang berbeda

---

## 🐛 Troubleshooting

### Notifikasi tidak muncul setelah create peminjaman?

1. Pastikan `adminId` dikirim di request body
2. Cek apakah `Notification` table sudah di-migrate
3. Cek logs console untuk error message

### Scheduler cron job tidak jalan?

1. Pastikan `node-cron` sudah diinstall: `npm list node-cron`
2. Cek timezone server: `date`
3. Jika using PM2, restart: `pm2 restart app.js`

### Notifikasi lama tidak otomatis terhapus?

- Ada cleanup job di `utils/notificationCleanup.js` yang akan menghapus notifikasi yang sudah dibaca 2 menit kemudian
- Pastikan cleanup job sudah running

---

## 🔐 Security Notes

- Semua endpoint dilindungi dengan `authenticateToken` middleware
- Admin hanya bisa melihat notifikasi miliknya sendiri
- Pastikan JWT token aman dan tidak expired

---

## 📚 Referensi

- Notification Model: `prisma/schema.prisma`
- Helper Functions: `utils/peminjamanNotificationHelper.js`
- Controller Integration: `controllers/peminjamanController.js`, `controllers/returnController.js`
- Routes: `routes/peminjamanNotification.js`

---

**Last Updated:** 22 December 2024
**Status:** ✅ Ready for Production
