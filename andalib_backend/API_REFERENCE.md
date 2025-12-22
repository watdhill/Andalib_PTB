# 📚 API REFERENCE - Notification System

## Base URL

```
http://localhost:3000
```

## Authentication

Semua endpoint memerlukan JWT Bearer token di header:

```
Authorization: Bearer <your_jwt_token>
```

---

## 🔔 Notifikasi Peminjaman APIs

### 1. Manual Trigger: Upcoming Due Date Reminder

Menjalankan reminder untuk peminjaman yang akan jatuh tempo (1-2 hari ke depan).

```http
GET /api/notifications/peminjaman/upcoming-due-date
Authorization: Bearer <token>
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| adminId | number | No | ID admin tertentu (jika kosong, semua admin) |

**Example Request:**

```bash
curl -X GET "http://localhost:3000/api/notifications/peminjaman/upcoming-due-date" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

**Success Response (200):**

```json
{
  "success": true,
  "message": "Notifikasi reminder berhasil dibuat"
}
```

**Error Response (500):**

```json
{
  "success": false,
  "message": "Gagal membuat notifikasi reminder"
}
```

---

### 2. Manual Trigger: Overdue Reminder

Menjalankan reminder untuk peminjaman yang sudah melewati jatuh tempo.

```http
GET /api/notifications/peminjaman/overdue
Authorization: Bearer <token>
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| adminId | number | No | ID admin tertentu (jika kosong, semua admin) |

**Example Request:**

```bash
curl -X GET "http://localhost:3000/api/notifications/peminjaman/overdue" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

**Success Response (200):**

```json
{
  "success": true,
  "message": "Notifikasi overdue berhasil dibuat"
}
```

**Error Response (500):**

```json
{
  "success": false,
  "message": "Gagal membuat notifikasi overdue"
}
```

---

## 🔔 Member Notification APIs (Existing)

### 3. Get Unread Notifications

Mengambil semua notifikasi yang belum dibaca oleh admin yang login.

```http
GET /api/member-notifications/unread
Authorization: Bearer <token>
```

**Query Parameters:** None

**Example Request:**

```bash
curl -X GET "http://localhost:3000/api/member-notifications/unread" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

**Success Response (200):**

```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "admin_id": 1,
      "notification_type": "PEMINJAMAN_BARU",
      "title": "Peminjaman Baru Dicatat",
      "message": "John Doe (123456) meminjam buku \"Clean Code\". Jatuh tempo: 31/12/2024",
      "member_name": "John Doe",
      "member_nim": "123456",
      "is_read": false,
      "created_at": "2024-12-22T10:30:00.000Z"
    },
    {
      "id": 2,
      "admin_id": 1,
      "notification_type": "PENGEMBALIAN_BUKU",
      "title": "Pengembalian Terlambat",
      "message": "Jane Smith (654321) mengembalikan buku \"Design Patterns\" TERLAMBAT. Denda: Rp10.000",
      "member_name": "Jane Smith",
      "member_nim": "654321",
      "is_read": false,
      "created_at": "2024-12-22T09:15:00.000Z"
    }
  ]
}
```

---

### 4. Get All Notifications

Mengambil semua notifikasi (read dan unread).

```http
GET /api/member-notifications
Authorization: Bearer <token>
```

**Query Parameters:** None

**Example Request:**

```bash
curl -X GET "http://localhost:3000/api/member-notifications" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

**Success Response (200):**

```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "admin_id": 1,
      "notification_type": "PEMINJAMAN_BARU",
      "title": "Peminjaman Baru Dicatat",
      "message": "...",
      "is_read": false,
      "created_at": "2024-12-22T10:30:00.000Z"
    },
    {
      "id": 2,
      "admin_id": 1,
      "notification_type": "PEMINJAMAN_AKAN_JATUH_TEMPO",
      "title": "Reminder: Peminjaman Akan Jatuh Tempo",
      "message": "...",
      "is_read": true,
      "created_at": "2024-12-21T08:00:00.000Z"
    }
  ]
}
```

---

### 5. Get Unread Count

Menghitung jumlah notifikasi yang belum dibaca.

```http
GET /api/member-notifications/count
Authorization: Bearer <token>
```

**Query Parameters:** None

**Example Request:**

```bash
curl -X GET "http://localhost:3000/api/member-notifications/count" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

**Success Response (200):**

```json
{
  "success": true,
  "message": "Success",
  "count": 5
}
```

---

### 6. Mark Notification as Read

Menandai notifikasi sebagai sudah dibaca.

```http
PUT /api/member-notifications/:id/read
Authorization: Bearer <token>
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | number | Yes | ID notifikasi |

**Example Request:**

```bash
curl -X PUT "http://localhost:3000/api/member-notifications/1/read" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

**Success Response (200):**

```json
{
  "success": true,
  "message": "Notifikasi ditandai sebagai sudah dibaca"
}
```

**Error Response (404):**

```json
{
  "success": false,
  "message": "Notifikasi tidak ditemukan"
}
```

---

## 📝 Notification Types

### PEMINJAMAN_BARU

```json
{
  "type": "PEMINJAMAN_BARU",
  "title": "Peminjaman Baru Dicatat",
  "message": "{memberName} ({memberNim}) meminjam buku \"{bookTitle}\". Jatuh tempo: {dueDate}",
  "metadata": {
    "memberName": "John Doe",
    "memberNim": "123456",
    "bookTitle": "Clean Code",
    "bookId": 5,
    "peminjamanId": 10,
    "dueDate": "2024-12-31T00:00:00.000Z"
  }
}
```

### PENGEMBALIAN_BUKU (Tepat Waktu)

```json
{
  "type": "PENGEMBALIAN_BUKU",
  "title": "Buku Dikembalikan",
  "message": "{memberName} ({memberNim}) mengembalikan buku \"{bookTitle}\" tepat waktu.",
  "metadata": {
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
}
```

### PENGEMBALIAN_BUKU (Terlambat)

```json
{
  "type": "PENGEMBALIAN_BUKU",
  "title": "Pengembalian Terlambat",
  "message": "{memberName} ({memberNim}) mengembalikan buku \"{bookTitle}\" TERLAMBAT. Denda: Rp{denda}",
  "metadata": {
    "memberName": "Jane Smith",
    "memberNim": "654321",
    "bookTitle": "Design Patterns",
    "bookId": 8,
    "peminjamanId": 15,
    "pengembalianId": 5,
    "terlambat": true,
    "denda": 10000,
    "kondisiKerusakan": "Normal"
  }
}
```

### PEMINJAMAN_AKAN_JATUH_TEMPO

```json
{
  "type": "PEMINJAMAN_AKAN_JATUH_TEMPO",
  "title": "Reminder: Peminjaman Akan Jatuh Tempo",
  "message": "Peminjaman buku \"{bookTitle}\" oleh {memberName} ({memberNim}) akan jatuh tempo pada {dueDate}",
  "metadata": {
    "memberName": "John Doe",
    "memberNim": "123456",
    "bookTitle": "Clean Code",
    "bookId": 5,
    "peminjamanId": 10,
    "dueDate": "2024-12-31T00:00:00.000Z"
  }
}
```

### PEMINJAMAN_OVERDUE

```json
{
  "type": "PEMINJAMAN_OVERDUE",
  "title": "Peminjaman Overdue",
  "message": "Buku \"{bookTitle}\" dipinjam oleh {memberName} ({memberNim}) sudah terlambat {daysOverdue} hari. Jatuh tempo: {dueDate}",
  "metadata": {
    "memberName": "John Doe",
    "memberNim": "123456",
    "bookTitle": "Clean Code",
    "bookId": 5,
    "peminjamanId": 10,
    "daysOverdue": 3
  }
}
```

---

## 🔐 Authentication Errors

### 401 Unauthorized

Terjadi saat token tidak valid atau expired.

```json
{
  "success": false,
  "message": "Unauthorized - Invalid or expired token"
}
```

**Solution:**

- Pastikan token valid
- Re-login untuk mendapatkan token baru

---

## ⚠️ Common Errors

### 400 Bad Request

```json
{
  "success": false,
  "message": "Invalid request parameters"
}
```

### 404 Not Found

```json
{
  "success": false,
  "message": "Resource not found"
}
```

### 500 Internal Server Error

```json
{
  "success": false,
  "message": "Internal server error"
}
```

---

## 🔄 Workflow Example

### Complete Flow: Peminjaman hingga Notifikasi Cleanup

```
1. Admin membuat peminjaman
   POST /api/peminjaman
   ├─ Body: {nim, bukuId, jatuhTempo, adminId}
   └─ Response: 201 Created

2. Sistem otomatis trigger notifikasi
   ├─ Type: PEMINJAMAN_BARU
   └─ Inserted into Notification table

3. Admin cek notifikasi
   GET /api/member-notifications/unread
   ├─ Header: Authorization: Bearer <token>
   └─ Response: 200 OK dengan list notifikasi

4. Admin lihat count unread
   GET /api/member-notifications/count
   └─ Response: {count: 5}

5. Admin baca notifikasi (click)
   PUT /api/member-notifications/1/read
   └─ Response: 200 OK

6. Sistem otomatis hapus setelah 2 menit
   (DELETE query dijalankan otomatis)

7. Setiap hari jam 08:00
   CRON: notifyUpcomingDueDate()
   ├─ Find peminjaman jatuh tempo 1-2 hari lagi
   └─ Insert reminder notifications

8. Setiap hari jam 09:00
   CRON: notifyOverduePeminjaman()
   ├─ Find peminjaman overdue
   └─ Insert overdue notifications
```

---

## 📊 Response Status Codes

| Code | Meaning      | Example                |
| ---- | ------------ | ---------------------- |
| 200  | OK           | GET unread, PUT read   |
| 201  | Created      | POST peminjaman        |
| 400  | Bad Request  | Invalid parameters     |
| 401  | Unauthorized | Invalid token          |
| 404  | Not Found    | Notification not found |
| 500  | Server Error | Database error         |

---

## 🧪 Testing Examples

### Using cURL

**Test 1: Create Peminjaman (Trigger Notifikasi)**

```bash
curl -X POST http://localhost:3000/api/peminjaman \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "nim": "123456",
    "bukuId": 1,
    "jatuhTempo": "31/12/2024",
    "adminId": 1
  }'
```

**Test 2: Check Unread Notifications**

```bash
curl -X GET http://localhost:3000/api/member-notifications/unread \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Test 3: Mark as Read**

```bash
curl -X PUT http://localhost:3000/api/member-notifications/1/read \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Test 4: Manual Trigger Reminder**

```bash
curl -X GET http://localhost:3000/api/notifications/peminjaman/upcoming-due-date \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Using Postman

1. Set Authorization tab ke "Bearer Token"
2. Paste JWT token
3. Set headers:
   - Content-Type: application/json
4. Send request sesuai examples di atas

---

## 📞 Rate Limiting

Tidak ada rate limiting pada API notifikasi. Namun disarankan:

- Jangan trigger reminder manual terlalu sering
- Cron jobs sudah dijadwalkan otomatis

---

**API Reference Version:** 1.0
**Last Updated:** 22 December 2024
**Status:** ✅ Complete
