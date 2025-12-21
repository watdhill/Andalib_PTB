// routes/returnNotification.js
const express = require("express");
const router = express.Router();

const { authenticateToken, isAdmin } = require("../middlewares/authMiddleware");
const returnNotifController = require("../controllers/returnNotifController");

// List notif milik admin yang login
router.get("/mine", authenticateToken, isAdmin, returnNotifController.listMine);

// Tandai notif dibaca
router.patch("/:id/read", authenticateToken, isAdmin, returnNotifController.markRead);

// Hapus notif milik sendiri
router.delete("/:id", authenticateToken, isAdmin, returnNotifController.deleteMine);

module.exports = router;
