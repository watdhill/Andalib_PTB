
const express = require('express');
const router = express.Router();
const dashboardController = require('../controllers/dashboardController');
const { authenticateToken } = require('../middlewares/authMiddleware');


router.use(authenticateToken);
router.get('/stats', dashboardController.getDashboardStats);

module.exports = router;
