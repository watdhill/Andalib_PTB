# 🔔 NOTIFICATION SYSTEM ARCHITECTURE

## 📊 Notification Types & Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    NOTIFICATION SYSTEM FLOW                      │
└─────────────────────────────────────────────────────────────────┘

TRIGGER 1: CREATE PEMINJAMAN
────────────────────────────
  Admin membuat peminjaman
        ↓
  POST /api/peminjaman {nim, bukuId, jatuhTempo, adminId}
        ↓
  peminjamanController.createPeminjaman()
        ↓
  Buat Peminjaman record + kurangi stok buku
        ↓
  ✅ createPeminjamanNotification(peminjaman, adminId)
        ↓
  INSERT Notification table:
  {
    type: "PEMINJAMAN_BARU",
    title: "Peminjaman Baru Dicatat",
    message: "{Member} meminjam {Book}. Jatuh tempo: {Date}",
    metadata: {memberName, memberNim, bookTitle, dueDate, ...}
  }
        ↓
  Response: 201 Created ✅


TRIGGER 2: RETURN PEMINJAMAN
─────────────────────────────
  Admin mencatat pengembalian
        ↓
  POST /api/returns {peminjamanId, tanggalPengembalian, denda}
        ↓
  returnController.createReturn()
        ↓
  Buat Pengembalian record + kembalikan stok buku + update status
        ↓
  ✅ createPengembalianNotification(peminjaman, pengembalian, adminId)
        ↓
  INSERT Notification table:
  {
    type: "PENGEMBALIAN_BUKU",
    title: "Buku Dikembalikan" atau "Pengembalian Terlambat",
    message: "{Member} mengembalikan {Book}..." (format berbeda tergantung status),
    metadata: {memberName, memberNim, bookTitle, denda, terlambat, ...}
  }
        ↓
  Response: 201 Created ✅


TRIGGER 3: DAILY REMINDER - UPCOMING DUE DATE (08:00)
──────────────────────────────────────────────────
  CRON JOB: Setiap hari jam 08:00
        ↓
  notifyUpcomingDueDate()
        ↓
  QUERY: Temukan Peminjaman yang:
  - Status: DIPINJAM
  - jatuhTempo antara besok dan 2 hari ke depan
        ↓
  Untuk setiap peminjaman + setiap admin:
  INSERT Notification table:
  {
    type: "PEMINJAMAN_AKAN_JATUH_TEMPO",
    title: "Reminder: Peminjaman Akan Jatuh Tempo",
    message: "Peminjaman {Book} oleh {Member} akan jatuh tempo pada {Date}",
    metadata: {memberName, memberNim, bookTitle, dueDate, ...}
  }
        ↓
  Notifikasi dikirim ke semua admin


TRIGGER 4: DAILY REMINDER - OVERDUE (09:00)
────────────────────────────────────────
  CRON JOB: Setiap hari jam 09:00
        ↓
  notifyOverduePeminjaman()
        ↓
  QUERY: Temukan Peminjaman yang:
  - Status: DIPINJAM
  - jatuhTempo < hari ini
        ↓
  Untuk setiap peminjaman + setiap admin:
  INSERT Notification table:
  {
    type: "PEMINJAMAN_OVERDUE",
    title: "Peminjaman Overdue",
    message: "Buku {Book} oleh {Member} sudah terlambat {X} hari",
    metadata: {memberName, memberNim, bookTitle, daysOverdue, ...}
  }
        ↓
  Notifikasi dikirim ke semua admin


VIEWING NOTIFICATIONS
─────────────────────
  Admin membuka notifikasi screen
        ↓
  GET /api/member-notifications/unread
        ↓
  Query: SELECT * FROM Notification WHERE adminId = ? AND isRead = false
        ↓
  Format dan return notifikasi
        ↓
  Display di UI dengan parse metadata


MARK AS READ
─────────────
  Admin tap/baca notifikasi
        ↓
  PUT /api/member-notifications/:id/read
        ↓
  UPDATE Notification SET isRead = true, readAt = NOW()
        ↓
  Response: 200 OK


AUTO CLEANUP (Every 2 minutes)
─────────────────────────────
  CLEANUP JOB: Setiap 2 menit
        ↓
  Query notifikasi yang:
  - isRead = true
  - readAt sudah lebih dari 2 menit lalu
        ↓
  DELETE notifikasi lama
        ↓
  Hapus data untuk keep database clean
```

---

## 🗂️ File Structure

```
Andalib Backend/
├── controllers/
│   ├── peminjamanController.js        ✏️ MODIFIED
│   │   └── createPeminjaman()         ✏️ Add trigger notifikasi
│   │   └── createPengembalianNotification() called here
│   │
│   └── returnController.js            ✏️ MODIFIED
│       └── createReturn()             ✏️ Add trigger notifikasi
│       └── createPengembalianNotification() called here
│
├── utils/
│   ├── peminjamanNotificationHelper.js ✨ NEW
│   │   ├── createPeminjamanNotification()      → Type: PEMINJAMAN_BARU
│   │   ├── createPengembalianNotification()    → Type: PENGEMBALIAN_BUKU
│   │   ├── notifyUpcomingDueDate()            → Type: PEMINJAMAN_AKAN_JATUH_TEMPO
│   │   └── notifyOverduePeminjaman()          → Type: PEMINJAMAN_OVERDUE
│   │
│   └── notificationScheduler.js       ✨ NEW
│       ├── scheduleUpcomingDueDateReminder()  → Cron: 0 8 * * * (08:00 daily)
│       ├── scheduleOverdueReminder()          → Cron: 0 9 * * * (09:00 daily)
│       ├── startAllSchedulers()               → Start all above
│       ├── stopAllSchedulers()                → Stop all schedulers
│       ├── triggerUpcomingDueDateReminderManual()  → Manual trigger
│       └── triggerOverdueReminderManual()         → Manual trigger
│
├── routes/
│   ├── peminjaman.js                  (existing)
│   ├── return.js                      (existing)
│   ├── memberNotification.js           (existing)
│   └── peminjamanNotification.js       ✨ NEW
│       ├── GET /upcoming-due-date     → Manual trigger remind
│       └── GET /overdue               → Manual trigger overdue
│
├── prisma/
│   └── schema.prisma
│       └── model Notification (already exists, used by system)
│
├── docs/ (Documentation)
│   ├── NOTIFIKASI_PEMINJAMAN.md       ✨ NEW (lengkap)
│   ├── QUICK_START_NOTIFIKASI.md      ✨ NEW (quick start)
│   ├── CHECKLIST_IMPLEMENTASI.md      ✨ NEW (checklist)
│   ├── IMPLEMENTASI_SUMMARY.md        ✨ NEW (summary)
│   └── APP_EXAMPLE_WITH_NOTIFICATION.js ✨ NEW (example)
│
└── app.js
    ├── require('./utils/notificationScheduler')  ✏️ ADD THIS
    ├── require('./routes/peminjamanNotification') ✏️ ADD THIS
    ├── app.use('/api/notifications/peminjaman', ...) ✏️ ADD THIS
    └── startAllSchedulers()                     ✏️ ADD THIS in app.listen()
```

---

## 📋 Notification Types

| Type                          | Trigger            | When                  | Auto | Manual |
| ----------------------------- | ------------------ | --------------------- | ---- | ------ |
| `PEMINJAMAN_BARU`             | createPeminjaman() | When creating loan    | ✅   | ❌     |
| `PENGEMBALIAN_BUKU`           | createReturn()     | When returning book   | ✅   | ❌     |
| `PEMINJAMAN_AKAN_JATUH_TEMPO` | Cron job (08:00)   | Daily at 08:00        | ✅   | ✅     |
| `PEMINJAMAN_OVERDUE`          | Cron job (09:00)   | Daily at 09:00        | ✅   | ✅     |
| `RETURN_DAMAGE_PROOF`         | Upload bukti       | When uploading damage | ✅   | ❌     |

---

## 🔐 Database Schema

```sql
CREATE TABLE Notification (
  id INT PRIMARY KEY AUTO_INCREMENT,

  -- Relasi ke Admin
  adminId INT NOT NULL,
  FOREIGN KEY (adminId) REFERENCES Admin(id) ON DELETE CASCADE,

  -- Tipe notifikasi
  type VARCHAR(255) NOT NULL,  -- PEMINJAMAN_BARU, PENGEMBALIAN_BUKU, etc

  -- Konten
  title VARCHAR(255) NOT NULL,
  message LONGTEXT NOT NULL,

  -- Metadata (JSON string)
  metadata LONGTEXT,

  -- Status baca
  isRead BOOLEAN DEFAULT false,
  readAt DATETIME,

  -- Timestamp
  createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,

  -- Indexes untuk fast query
  KEY (adminId, isRead),
  KEY (type),
  KEY (createdAt)
) ENGINE=InnoDB;
```

---

## 🔗 API Endpoints

### Automatic Triggers (Tidak perlu manual call)

```
POST /api/peminjaman                    → Trigger: PEMINJAMAN_BARU
POST /api/returns                       → Trigger: PENGEMBALIAN_BUKU
```

### Manual Triggers (untuk testing / manual execution)

```
GET /api/notifications/peminjaman/upcoming-due-date
GET /api/notifications/peminjaman/overdue
```

### View Notifications (existing)

```
GET  /api/member-notifications/unread          → Get unread notifications
GET  /api/member-notifications                 → Get all notifications
GET  /api/member-notifications/count           → Count unread
PUT  /api/member-notifications/:id/read        → Mark as read
```

---

## 🧪 Testing Matrix

```
┌──────────────────────────────────────────────────────────────┐
│           TEST CASE              │  TRIGGER  │  EXPECTED      │
├──────────────────────────────────────────────────────────────┤
│ 1. Create peminjaman             │ POST      │ PEMINJAMAN_BARU│
│ 2. Return tepat waktu            │ POST      │ PENGEMBALIAN   │
│ 3. Return terlambat              │ POST      │ PENGEMBALIAN   │
│ 4. Return dengan kerusakan       │ POST      │ PENGEMBALIAN   │
│ 5. Manual trigger upcoming       │ GET       │ REMINDER       │
│ 6. Manual trigger overdue        │ GET       │ OVERDUE        │
│ 7. Mark notif as read            │ PUT       │ isRead = true  │
│ 8. Get unread count              │ GET       │ Count number   │
│ 9. Auto cleanup after 2 min      │ CRON      │ Deleted        │
│ 10. Cron upcoming at 08:00       │ CRON      │ Notification   │
│ 11. Cron overdue at 09:00        │ CRON      │ Notification   │
└──────────────────────────────────────────────────────────────┘
```

---

## ⚙️ Configuration

### Timezone

- Menggunakan UTC untuk consistency
- Server timezone untuk cron jobs

### Cron Schedule

- **08:00 setiap hari** - Upcoming due date reminder
- **09:00 setiap hari** - Overdue reminder
- **Setiap 2 menit** - Auto cleanup notifikasi (existing)

### Metadata Fields

Tersimpan sebagai JSON string untuk fleksibilitas future expansion

---

## 🚀 Implementation Checklist

- [ ] Install node-cron: `npm install node-cron`
- [ ] Copy files: peminjamanNotificationHelper.js, notificationScheduler.js
- [ ] Update files: peminjamanController.js, returnController.js
- [ ] Add routes: peminjamanNotification.js
- [ ] Update app.js: Add imports, routes, scheduler
- [ ] Run migration: `npx prisma migrate dev`
- [ ] Test: Run all test cases above
- [ ] Deploy: Push to production

---

**Architecture Version:** 1.0
**Last Updated:** 22 December 2024
**Status:** ✅ Production Ready
