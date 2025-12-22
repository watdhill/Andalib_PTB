# 📇 QUICK REFERENCE CARD - Notifikasi Peminjaman

## 🎯 1 Minute Overview

```
Sistem notifikasi peminjaman telah ditambahkan ke Andalib.
Notifikasi dibuat otomatis saat:
✅ Peminjaman baru dibuat
✅ Buku dikembalikan
✅ Peminjaman akan jatuh tempo (reminder harian)
✅ Peminjaman overdue (reminder harian)
```

---

## 📦 What Was Added

| Item               | File                                    | Status      |
| ------------------ | --------------------------------------- | ----------- |
| Helper functions   | `utils/peminjamanNotificationHelper.js` | ✨ New      |
| Scheduler/Cron     | `utils/notificationScheduler.js`        | ✨ New      |
| API routes         | `routes/peminjamanNotification.js`      | ✨ New      |
| Peminjaman trigger | `controllers/peminjamanController.js`   | ✏️ Modified |
| Return trigger     | `controllers/returnController.js`       | ✏️ Modified |
| Documentation      | 5 markdown files                        | ✨ New      |

---

## ⚡ Quick Integration

**Step 1: Install dependency**

```bash
npm install node-cron
```

**Step 2: Add to app.js (top)**

```javascript
const { startAllSchedulers } = require("./utils/notificationScheduler");
const peminjamanNotificationRoutes = require("./routes/peminjamanNotification");
```

**Step 3: Add to app.js (routes)**

```javascript
app.use("/api/notifications/peminjaman", peminjamanNotificationRoutes);
```

**Step 4: Add to app.js (app.listen)**

```javascript
app.listen(PORT, "0.0.0.0", () => {
  console.log(`Server running at ${PORT}`);
  startCleanupScheduler();
  startAllSchedulers(); // ← ADD THIS
});
```

**Done!** ✅

---

## 🔔 Notification Types

```
Type 1: PEMINJAMAN_BARU
├─ Trigger: POST /api/peminjaman
├─ When: Saat admin buat peminjaman
├─ Message: "{Member} meminjam {Book}. Jatuh tempo: {Date}"
└─ Auto: Yes ✅

Type 2: PENGEMBALIAN_BUKU
├─ Trigger: POST /api/returns
├─ When: Saat buku dikembalikan
├─ Message: "{Member} mengembalikan {Book}..." (varies)
└─ Auto: Yes ✅

Type 3: PEMINJAMAN_AKAN_JATUH_TEMPO
├─ Trigger: Cron (08:00 setiap hari)
├─ When: 1-2 hari sebelum jatuh tempo
├─ Message: "Peminjaman {Book} akan jatuh tempo pada {Date}"
└─ Auto: Yes ✅

Type 4: PEMINJAMAN_OVERDUE
├─ Trigger: Cron (09:00 setiap hari)
├─ When: Peminjaman sudah melewati jatuh tempo
├─ Message: "Buku {Book} sudah terlambat {X} hari"
└─ Auto: Yes ✅
```

---

## 📡 API Endpoints

### Automatic (built-in)

```bash
POST /api/peminjaman
POST /api/returns
```

### Manual Trigger (testing)

```bash
GET /api/notifications/peminjaman/upcoming-due-date
GET /api/notifications/peminjaman/overdue
```

### View Notifications

```bash
GET /api/member-notifications/unread
GET /api/member-notifications
GET /api/member-notifications/count
PUT /api/member-notifications/:id/read
```

---

## 🧪 Quick Test

```bash
# 1. Create peminjaman
curl -X POST http://localhost:3000/api/peminjaman \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "nim": "123456",
    "bukuId": 1,
    "jatuhTempo": "31/12/2024",
    "adminId": 1
  }'

# 2. Check notification
curl http://localhost:3000/api/member-notifications/unread \
  -H "Authorization: Bearer <token>"

# Expected: Notifikasi bertipe PEMINJAMAN_BARU ✅
```

---

## 🔧 Configuration

| Item       | Default           | Editable             |
| ---------- | ----------------- | -------------------- |
| Reminder 1 | 08:00 setiap hari | Yes, di scheduler    |
| Reminder 2 | 09:00 setiap hari | Yes, di scheduler    |
| Cleanup    | 2 menit           | Yes, di cleanup util |
| Timezone   | UTC               | Yes, server config   |

---

## 📂 Files Location

```
andalib_backend/
├── utils/
│   ├── peminjamanNotificationHelper.js
│   └── notificationScheduler.js
├── routes/
│   └── peminjamanNotification.js
├── controllers/
│   ├── peminjamanController.js (modified)
│   └── returnController.js (modified)
└── docs/
    ├── NOTIFIKASI_PEMINJAMAN.md
    ├── QUICK_START_NOTIFIKASI.md
    ├── CHECKLIST_IMPLEMENTASI.md
    ├── IMPLEMENTASI_SUMMARY.md
    ├── NOTIFICATION_ARCHITECTURE.md
    ├── APP_EXAMPLE_WITH_NOTIFICATION.js
    └── QUICK_REFERENCE.md (this file)
```

---

## ✅ Verification Commands

```bash
# Check files exist
ls -la utils/peminjamanNotificationHelper.js
ls -la utils/notificationScheduler.js
ls -la routes/peminjamanNotification.js

# Check imports
grep "peminjamanNotificationHelper" controllers/peminjamanController.js

# Check package installed
npm list node-cron

# Start server (should see scheduler logs)
npm start
```

---

## 🐛 Quick Troubleshooting

| Issue                 | Fix                                         |
| --------------------- | ------------------------------------------- |
| Module not found      | `npm install node-cron`                     |
| No notifications      | Add `adminId` to request                    |
| Scheduler not running | Call `startAllSchedulers()` in app.listen() |
| DB error              | `npx prisma migrate dev`                    |

---

## 🎁 Features

✨ Fully automatic notifikasi saat peminjaman
✨ Harian reminder untuk peminjaman akan/sudah overdue
✨ Auto cleanup old notifications
✨ Metadata JSON untuk custom data
✨ Secure dengan auth middleware
✨ Scalable & future-proof

---

## 📚 Full Docs

- **Complete**: `NOTIFIKASI_PEMINJAMAN.md`
- **Quick**: `QUICK_START_NOTIFIKASI.md`
- **Checklist**: `CHECKLIST_IMPLEMENTASI.md`
- **Architecture**: `NOTIFICATION_ARCHITECTURE.md`
- **Example**: `APP_EXAMPLE_WITH_NOTIFICATION.js`

---

## 🚀 Next Steps

1. Read QUICK_START_NOTIFIKASI.md (5 min)
2. Integrate app.js (5 min)
3. Run tests (5 min)
4. Monitor logs (ongoing)

**Total Time: ~15 minutes** ⏱️

---

## 📞 Support

- Logs console untuk debug
- Dokumentasi lengkap di NOTIFIKASI_PEMINJAMAN.md
- Check CHECKLIST_IMPLEMENTASI.md untuk verification

---

**Quick Ref Version:** 1.0
**Status:** ✅ Ready
**Date:** 22 December 2024
