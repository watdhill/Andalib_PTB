# ✅ CHECKLIST IMPLEMENTASI NOTIFIKASI PEMINJAMAN

Gunakan checklist ini untuk memastikan semua komponen telah terintegrasi dengan benar.

---

## 📋 File-File yang Sudah Dibuat

- [x] `utils/peminjamanNotificationHelper.js` - Helper functions notifikasi
- [x] `utils/notificationScheduler.js` - Scheduler untuk cron jobs
- [x] `routes/peminjamanNotification.js` - Routes untuk manual trigger
- [x] `NOTIFIKASI_PEMINJAMAN.md` - Dokumentasi lengkap
- [x] `QUICK_START_NOTIFIKASI.md` - Quick start guide
- [x] `CHECKLIST_IMPLEMENTASI.md` - File ini

---

## 🔧 File-File yang Sudah Dimodifikasi

- [x] `controllers/peminjamanController.js`

  - [x] Tambah import `peminjamanNotificationHelper`
  - [x] Trigger `createPeminjamanNotification()` di `createPeminjaman()`

- [x] `controllers/returnController.js`
  - [x] Tambah import `peminjamanNotificationHelper`
  - [x] Include `anggota` di relasi peminjaman
  - [x] Trigger `createPengembalianNotification()` di `createReturn()`

---

## 📝 Langkah-Langkah Implementasi di app.js

### Step 1: Tambah Import (Di bagian atas file)

```javascript
// ✅ Tambahkan di app.js bagian imports
const { startAllSchedulers } = require("./utils/notificationScheduler");
const peminjamanNotificationRoutes = require("./routes/peminjamanNotification");
```

### Step 2: Tambah Route (Setelah route lainnya)

```javascript
// ✅ Tambahkan setelah route peminjaman yang lain
app.use("/api/notifications/peminjaman", peminjamanNotificationRoutes);
```

### Step 3: Tambah Scheduler di app.listen()

```javascript
// ✅ Modifikasi bagian app.listen()
app.listen(PORT, "0.0.0.0", () => {
  console.log(`Server is running on port ${PORT}`);

  // ✅ Start notification cleanup scheduler (sudah ada)
  startCleanupScheduler();

  // ✅ TAMBAHKAN INI: Start peminjaman notification schedulers
  startAllSchedulers();
});
```

---

## 📦 Dependencies to Install

Jalankan command berikut jika belum:

```bash
# Install node-cron untuk scheduler
npm install node-cron

# Verify yang sudah installed
npm list prisma express dotenv multer node-cron
```

---

## ✨ Features yang Sudah Diaktifkan

### Notifikasi Otomatis:

- [x] **PEMINJAMAN_BARU** - Saat create peminjaman

  - Dipicu: `POST /api/peminjaman` dengan `adminId`
  - Pesan: "{Member} meminjam buku \"{Title}\". Jatuh tempo: {Date}"

- [x] **PENGEMBALIAN_BUKU** - Saat pengembalian dicatat

  - Dipicu: `POST /api/returns`
  - Pesan: Berbeda untuk tepat waktu vs terlambat + dengan/tanpa kerusakan

- [x] **PEMINJAMAN_AKAN_JATUH_TEMPO** - Reminder (08:00 setiap hari)

  - Cron: Harian jam 08:00
  - Target: Peminjaman jatuh tempo dalam 1-2 hari ke depan

- [x] **PEMINJAMAN_OVERDUE** - Reminder overdue (09:00 setiap hari)
  - Cron: Harian jam 09:00
  - Target: Peminjaman yang sudah melewati jatuh tempo

---

## 🧪 Testing Checklist

Jalankan testing ini untuk verify semuanya berfungsi:

### Test 1: Create Peminjaman dengan Notifikasi

- [ ] Create peminjaman dengan admin yang valid
- [ ] Cek notifikasi muncul di `/api/member-notifications/unread`
- [ ] Notifikasi bertipe `PEMINJAMAN_BARU`

### Test 2: Return Peminjaman dengan Notifikasi

- [ ] Create return untuk peminjaman yang ada
- [ ] Cek notifikasi muncul di `/api/member-notifications/unread`
- [ ] Notifikasi bertipe `PENGEMBALIAN_BUKU`
- [ ] Cek denda dan kerusakan di metadata

### Test 3: Manual Trigger Reminder

- [ ] Call `/api/notifications/peminjaman/upcoming-due-date`
- [ ] Verify notifikasi dibuat untuk peminjaman yang akan jatuh tempo
- [ ] Call `/api/notifications/peminjaman/overdue`
- [ ] Verify notifikasi dibuat untuk peminjaman overdue

### Test 4: Mark Notifikasi as Read

- [ ] GET `/api/member-notifications/unread` - catat ID notifikasi
- [ ] PUT `/api/member-notifications/{id}/read`
- [ ] Verify notifikasi tidak lagi di unread list

### Test 5: Count Unread Notifikasi

- [ ] GET `/api/member-notifications/count`
- [ ] Verify count sesuai dengan unread notifications

---

## 🔍 Verification Commands

Gunakan command berikut untuk verify implementasi:

```bash
# 1. Check apakah file helper sudah ada
ls -la utils/peminjamanNotificationHelper.js

# 2. Check apakah scheduler sudah ada
ls -la utils/notificationScheduler.js

# 3. Check apakah routes sudah ada
ls -la routes/peminjamanNotification.js

# 4. Check import di controllers
grep -n "peminjamanNotificationHelper" controllers/peminjamanController.js
grep -n "peminjamanNotificationHelper" controllers/returnController.js

# 5. Check apakah node-cron sudah installed
npm list node-cron

# 6. Test server bisa start tanpa error
npm start
```

---

## 📊 Database Verification

Pastikan Notification table sudah ada di database:

```sql
-- Check apakah table Notification ada
SHOW TABLES LIKE 'Notification';

-- Check struktur table
DESCRIBE Notification;

-- Verify ada indexes untuk fast query
SHOW INDEXES FROM Notification;
```

Atau gunakan Prisma:

```bash
# Check schema
npx prisma studio

# Run migration jika ada yang baru
npx prisma migrate dev
```

---

## 🎯 Workflow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                   PEMINJAMAN WORKFLOW                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. ADMIN CREATE PEMINJAMAN                               │
│     POST /api/peminjaman                                  │
│            ↓                                               │
│     ✅ Create Peminjaman Record                           │
│            ↓                                               │
│     🔔 createPeminjamanNotification()                     │
│            ↓                                               │
│     INSERT Notification (type: PEMINJAMAN_BARU)           │
│                                                             │
│  2. DAILY REMINDER (08:00)                                 │
│     CRON: notifyUpcomingDueDate()                         │
│            ↓                                               │
│     Find Peminjaman jatuh tempo 1-2 hari lagi             │
│            ↓                                               │
│     INSERT Notification (type: PEMINJAMAN_AKAN_JATUH_TEMPO)
│                                                             │
│  3. DAILY REMINDER (09:00)                                 │
│     CRON: notifyOverduePeminjaman()                       │
│            ↓                                               │
│     Find Peminjaman overdue (jatuh tempo sudah lewat)     │
│            ↓                                               │
│     INSERT Notification (type: PEMINJAMAN_OVERDUE)         │
│                                                             │
│  4. ADMIN RETURN PEMINJAMAN                                │
│     POST /api/returns                                      │
│            ↓                                               │
│     ✅ Create Pengembalian Record                         │
│            ↓                                               │
│     ✅ Update status Peminjaman → DIKEMBALIKAN            │
│            ↓                                               │
│     🔔 createPengembalianNotification()                   │
│            ↓                                               │
│     INSERT Notification (type: PENGEMBALIAN_BUKU)         │
│            ↓                                               │
│     (Tergantung denda & kerusakan, format berbeda)        │
│                                                             │
│  5. ADMIN VIEW NOTIFIKASI                                  │
│     GET /api/member-notifications/unread                   │
│            ↓                                               │
│     Return list notifikasi yang belum dibaca               │
│                                                             │
│  6. CLEANUP JOB (Every 2 minutes)                          │
│     DELETE Notification yang sudah dibaca > 2 menit        │
│            ↓                                               │
│     Automatic cleanup                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚨 Common Issues & Solutions

### Issue 1: "Cannot find module 'node-cron'"

**Solution:**

```bash
npm install node-cron
```

### Issue 2: Notifikasi tidak muncul saat create peminjaman

**Checklist:**

- [ ] `adminId` dikirim di request body
- [ ] Admin dengan ID tersebut ada di database
- [ ] `Notification` model sudah di-migrate
- [ ] Import `peminjamanNotificationHelper` sudah ada

### Issue 3: Scheduler tidak jalan

**Checklist:**

- [ ] `startAllSchedulers()` dipanggil di app.listen()
- [ ] `node-cron` sudah installed
- [ ] Server sudah restart
- [ ] Timezone server benar

### Issue 4: Database error saat membuat notifikasi

**Checklist:**

- [ ] Jalankan `npx prisma migrate dev`
- [ ] Cek struktur table Notification
- [ ] Pastikan adminId valid dan ada relasi ke Admin

---

## 📞 Next Steps

1. **Integrate di app.js** - Ikuti Step 1-3 di atas
2. **Install dependencies** - `npm install node-cron`
3. **Test notifikasi** - Jalankan testing checklist
4. **Setup cron jobs** - Jika ingin reminder otomatis
5. **Monitor logs** - Cek console output untuk errors

---

## 📚 Reference Files

- Main Helper: `utils/peminjamanNotificationHelper.js`
- Scheduler: `utils/notificationScheduler.js`
- Routes: `routes/peminjamanNotification.js`
- Full Docs: `NOTIFIKASI_PEMINJAMAN.md`
- Quick Start: `QUICK_START_NOTIFIKASI.md`

---

**Last Updated:** 22 December 2024
**Status:** Ready for Implementation ✅
