
const jwt = require("jsonwebtoken");

const jwtSecret = process.env.JWT_SECRET;

/**
 * Middleware untuk memverifikasi JWT dari header Authorization
 */
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers["authorization"];
  const token = authHeader && authHeader.split(" ")[1];

  if (token == null) {
    return res
      .status(401)
      .json({ message: "Akses ditolak. Token tidak tersedia." });
  }

  jwt.verify(token, jwtSecret, (err, user) => {
    if (err) {

      return res
        .status(403)
        .json({ message: "Token tidak valid atau sudah kadaluarsa." });
    }

   
    req.user = user;
    next(); 
  });
};

/**
 * Middleware untuk memastikan pengguna adalah Admin
 */
const isAdmin = (req, res, next) => {
  
  if (req.user && req.user.role === "admin") {
    next();
  } else {
    
    res.status(403).json({ message: "Akses ditolak. Hanya untuk Admin." });
  }
};

module.exports = {
  authenticateToken,
  isAdmin,
};
