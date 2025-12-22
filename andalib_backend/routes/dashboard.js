// routes/dashboard.js
const express = require('express');
const router = express.Router();
const dashboardController = require('../controllers/dashboardController');
const { authenticateToken } = require('../middlewares/authMiddleware');

// Apply auth middleware
router.use(authenticateToken);

// GET /api/dashboard/stats
// Get dashboard statistics
router.get('/stats', dashboardController.getDashboardStats);

module.exports = router;
