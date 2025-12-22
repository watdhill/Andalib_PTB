const express = require("express");
const router = express.Router();
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const returnController = require("../controllers/returnController");
const { authenticateToken, isAdmin } = require("../middlewares/authMiddleware");

// folder upload
const damageProofUploadDir = path.join(__dirname, "..", "uploads", "kerusakan");
if (!fs.existsSync(damageProofUploadDir)) {
  fs.mkdirSync(damageProofUploadDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, damageProofUploadDir),
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + "-" + Math.round(Math.random() * 1e9);
    const ext = path.extname(file.originalname);
    cb(null, `kerusakan-${uniqueSuffix}${ext}`);
  },
});

const fileFilter = (req, file, cb) => {
  const allowed = ["image/jpeg", "image/jpg", "image/png", "image/webp"];
  if (allowed.includes(file.mimetype)) cb(null, true);
  else cb(new Error("Hanya gambar (JPEG, PNG, WEBP)"), false);
};

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: 5 * 1024 * 1024 },
}).single("buktiKerusakan");

// SEARCH + ACTIVE LOANS
router.get("/members/search", authenticateToken, isAdmin, returnController.searchMembers);
router.get("/borrowings/active/:nim", authenticateToken, isAdmin, returnController.getActiveBorrowings);

// UPLOAD BUKTI SAJA (tanpa membuat pengembalian)
router.post(
  "/damage-proof",
  authenticateToken,
  isAdmin,
  returnController.uploadDamageProof,
  returnController.uploadDamageProofOnly
);

// CREATE RETURN (boleh JSON berisi buktiKerusakanUrl, atau multipart bawa file)
router.post(
  "/process",
  authenticateToken,
  isAdmin,
  returnController.uploadDamageProof,
  returnController.createReturn
);

// UPDATE FILE BUKTI (ganti bukti pada pengembalian yang sudah ada)
router.put(
  "/update-damage-proof/:returnId",
  authenticateToken,
  isAdmin,
  returnController.uploadDamageProof,
  returnController.updateDamageProof
);

// HISTORY
router.get("/history", authenticateToken, isAdmin, returnController.getReturnHistory);

// UPDATE RETURN DATA
router.post("/update/:returnId", authenticateToken, isAdmin, returnController.updateReturn);

// DELETE RETURN
router.delete("/history/:id", authenticateToken, isAdmin, returnController.deleteReturn);

module.exports = router;