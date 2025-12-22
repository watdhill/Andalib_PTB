const admin = require("firebase-admin");
const fs = require("fs");
const path = require("path");

let isInitialized = false;

function resolveCredentialPath(p) {
  if (!p) return null;
  // kalau relative, jadikan absolute berdasar root project (process.cwd())
  return path.isAbsolute(p) ? p : path.resolve(process.cwd(), p);
}

function initFirebaseAdmin() {
  if (isInitialized) return admin;

  try {
    // 1) Pakai GOOGLE_APPLICATION_CREDENTIALS (path file)
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      const credPath = resolveCredentialPath(process.env.GOOGLE_APPLICATION_CREDENTIALS);

      if (!credPath || !fs.existsSync(credPath)) {
        throw new Error(
          `GOOGLE_APPLICATION_CREDENTIALS menunjuk ke file yang tidak ada: ${credPath}`
        );
      }

      const serviceAccount = JSON.parse(fs.readFileSync(credPath, "utf8"));

      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
      });

      isInitialized = true;
      console.log("[FCM] Initialized using GOOGLE_APPLICATION_CREDENTIALS");
      return admin;
    }

    // 2) Alternatif: JSON langsung dari env
    if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);

      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
      });

      isInitialized = true;
      console.log("[FCM] Initialized using FIREBASE_SERVICE_ACCOUNT_JSON");
      return admin;
    }

    // 3) Alternatif: Base64
    if (process.env.FIREBASE_SERVICE_ACCOUNT_BASE64) {
      const jsonStr = Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64, "base64").toString("utf8");
      const serviceAccount = JSON.parse(jsonStr);

      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
      });

      isInitialized = true;
      console.log("[FCM] Initialized using FIREBASE_SERVICE_ACCOUNT_BASE64");
      return admin;
    }

    console.warn("[FCM] Firebase Admin belum bisa di-init. Set GOOGLE_APPLICATION_CREDENTIALS atau FIREBASE_SERVICE_ACCOUNT_JSON/BASE64.");
    return null;
  } catch (err) {
    console.error("[FCM] Init failed:", err.message);
    return null;
  }
}

function getMessaging() {
  const a = initFirebaseAdmin();
  return a ? a.messaging() : null;
}

module.exports = { initFirebaseAdmin, getMessaging };
