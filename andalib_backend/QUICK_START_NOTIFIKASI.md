# 🚀 QUICK START: Notifikasi Peminjaman

## ✅ Status Implementasi

Sistem notifikasi peminjaman telah berhasil diintegrasikan ke aplikasi Andalib. Berikut adalah rangkuman perubahan yang dilakukan:

---

## 📦 File-File yang Ditambahkan / Dimodifikasi

### ✨ File Baru Dibuat:

1. **`utils/peminjamanNotificationHelper.js`**

   - Helper functions untuk membuat notifikasi peminjaman
   - Mendukung 4 jenis notifikasi: peminjaman baru, pengembalian, reminder, dan overdue
   - Includes: `createPeminjamanNotification()`, `createPengembalianNotification()`, `notifyUpcomingDueDate()`, `notifyOverduePeminjaman()`

2. **`utils/notificationScheduler.js`**

   - Scheduler untuk menjalankan reminder otomatis menggunakan node-cron
   - Support untuk reminder peminjaman akan jatuh tempo (08:00 setiap hari)
   - Support untuk overdue reminder (09:00 setiap hari)

3. **`routes/peminjamanNotification.js`**

   - Routes untuk manual trigger notifikasi (opsional)
   - Endpoints: `/api/notifications/peminjaman/upcoming-due-date` dan `/overdue`

4. **`NOTIFIKASI_PEMINJAMAN.md`**
   - Dokumentasi lengkap sistem notifikasi
   - Panduan implementasi, API endpoints, dan troubleshooting

---

### 🔧 File Dimodifikasi:

1. **`controllers/peminjamanController.js`**

   - Tambah import: `peminjamanNotificationHelper`
   - Trigger notifikasi di `createPeminjaman()` setelah membuat peminjaman baru

2. **`controllers/returnController.js`**
   - Tambah import: `peminjamanNotificationHelper`
   - Trigger notifikasi di `createReturn()` setelah pengembalian dicatat
   - Include `anggota` di relation untuk mendapatkan data anggota

---

## 🔄 Cara Kerja Sistem

### 1️⃣ **Notifikasi Otomatis Saat Peminjaman**

Ketika admin membuat peminjaman baru:

```
POST /api/peminjaman
{
  "nim": "123456",
  "bukuId": 1,
  "jatuhTempo": "31/12/2024",
  "adminId": 1
}
```

✅ Sistem akan otomatis membuat notifikasi bertipe `PEMINJAMAN_BARU` untuk admin tersebut

### 2️⃣ **Notifikasi Otomatis Saat Pengembalian**

Ketika admin mencatat pengembalian buku:

```
POST /api/returns
{
  "peminjamanId": 1,
  "tanggalPengembalian": "25/12/2024",
  "denda": 0
}
```

✅ Sistem akan otomatis membuat notifikasi bertipe `PENGEMBALIAN_BUKU` untuk admin

- Jika terlambat: Notifikasi khusus dengan menampilkan besar denda
- Jika ada kerusakan: Notifikasi ditandai ada kerusakan

### 3️⃣ **Reminder Otomatis (Cron Job)**

**Harian Jam 08:00**: Reminder peminjaman akan jatuh tempo (1-2 hari ke depan)
**Harian Jam 09:00**: Reminder peminjaman yang sudah overdue

---

## 📝 Implementasi di app.js

Tambahkan inisialasi scheduler di `app.js`:

```javascript
// Di bagian atas app.js, tambahkan import:
const { startAllSchedulers } = require("./utils/notificationScheduler");

// Di bagian route peminjaman notification, tambahkan:
const peminjamanNotificationRoutes = require("./routes/peminjamanNotification");
app.use("/api/notifications/peminjaman", peminjamanNotificationRoutes);

// Di bagian app.listen(), tambahkan:
app.listen(PORT, "0.0.0.0", () => {
  console.log(`Server is running on port ${PORT}`);

  // ✅ Start notification cleanup scheduler
  startCleanupScheduler();

  // ✅ Start peminjaman notification schedulers
  startAllSchedulers(); // <--- TAMBAHKAN INI
});
```

---

## 📦 Dependencies

Pastikan package yang diperlukan sudah terinstall:

```bash
# Untuk scheduler (optional, jika ingin cron job otomatis)
npm install node-cron

# Pastikan sudah punya ini:
npm install prisma @prisma/client
npm install express dotenv multer
```

Jika belum, install:

```bash
npm install node-cron
```

---

## 🧪 Testing Notifikasi

### Test 1: Notifikasi Peminjaman Baru

1. Buat peminjaman baru:

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

2. Cek notifikasi yang tercipta:

```bash
curl http://localhost:3000/api/member-notifications/unread \
  -H "Authorization: Bearer <token>"
```

Expected response:

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "notification_type": "PEMINJAMAN_BARU",
      "title": "Peminjaman Baru Dicatat",
      "message": "John Doe (123456) meminjam buku \"Clean Code\". Jatuh tempo: 31/12/2024",
      "member_name": "John Doe",
      "member_nim": "123456",
      "is_read": false
    }
  ]
}
```

### Test 2: Notifikasi Pengembalian

1. Catat pengembalian buku:

```bash
curl -X POST http://localhost:3000/api/returns \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "peminjamanId": 1,
    "tanggalPengembalian": "25/12/2024",
    "denda": 0
  }'
```

2. Cek notifikasi baru yang tercipta

### Test 3: Manual Trigger Reminder (Testing Cron)

```bash
# Trigger upcoming due date reminder
curl http://localhost:3000/api/notifications/peminjaman/upcoming-due-date \
  -H "Authorization: Bearer <token>"

# Trigger overdue reminder
curl http://localhost:3000/api/notifications/peminjaman/overdue \
  -H "Authorization: Bearer <token>"
```

---

## 🎯 Notifikasi Types

| Type                          | Trigger                | Deskripsi                                           |
| ----------------------------- | ---------------------- | --------------------------------------------------- |
| `PEMINJAMAN_BARU`             | Create peminjaman      | Saat admin membuat peminjaman baru                  |
| `PENGEMBALIAN_BUKU`           | Create return          | Saat buku dikembalikan (tepat waktu atau terlambat) |
| `PEMINJAMAN_AKAN_JATUH_TEMPO` | Cron job (08:00)       | Reminder 1-2 hari sebelum jatuh tempo               |
| `PEMINJAMAN_OVERDUE`          | Cron job (09:00)       | Reminder untuk peminjaman yang sudah overdue        |
| `RETURN_DAMAGE_PROOF`         | Upload bukti kerusakan | Saat upload bukti kerusakan saat pengembalian       |

---

## 🔐 Security

✅ Semua notifikasi dilindungi dengan auth middleware
✅ Admin hanya bisa melihat notifikasi miliknya
✅ Metadata disimpan aman dalam format JSON

---

## 📊 Database Schema

Notifikasi disimpan di table `Notification`:

- `id` - Primary key
- `adminId` - Admin yang menerima (FK)
- `type` - Jenis notifikasi
- `title` - Judul singkat
- `message` - Pesan lengkap
- `metadata` - JSON untuk data tambahan
- `isRead` - Status sudah dibaca
- `readAt` - Timestamp dibaca
- `createdAt` - Timestamp dibuat

---

## 🐛 Troubleshooting

**Q: Notifikasi tidak muncul saat create peminjaman?**

- ✅ Pastikan `adminId` ada di request body
- ✅ Cek admin dengan ID tersebut ada di database
- ✅ Cek logs console untuk error

**Q: Scheduler tidak jalan?**

- ✅ Pastikan `node-cron` sudah diinstall
- ✅ Restart server
- ✅ Cek timezone sistem

**Q: Database error?**

- ✅ Jalankan migration: `npx prisma migrate dev`
- ✅ Cek model `Notification` di schema.prisma sudah ada

---

## 📞 Support

Untuk bantuan lebih lanjut:

1. Baca dokumentasi lengkap: `NOTIFIKASI_PEMINJAMAN.md`
2. Cek logs console untuk error messages
3. Test manual trigger endpoints untuk debug

---

**Status:** ✅ Ready to Use
**Last Updated:** 22 December 2024
