// services/fcmService.js
const { getMessaging } = require("../config/firebaseAdmin");

const ADMIN_TOPIC = "andalib-admin";

async function notifyAdminsDamageProof({ peminjamanId, pengembalianId, buktiKerusakanUrl }) {
  const messaging = getMessaging();
  if (!messaging) {
    console.warn("[FCM] messaging null, skip send");
    return null;
  }

  const message = {
    topic: ADMIN_TOPIC,
    notification: {
      title: "Bukti Kerusakan Diunggah",
      body: `Peminjaman ID: ${peminjamanId} (Pengembalian ID: ${pengembalianId})`,
    },
    data: {
      type: "RETURN_DAMAGE_PROOF",
      peminjamanId: String(peminjamanId),
      pengembalianId: String(pengembalianId),
      buktiKerusakanUrl: buktiKerusakanUrl || "",
      // optional untuk fallback client:
      title: "Bukti Kerusakan Diunggah",
      body: `Peminjaman ID: ${peminjamanId} (Pengembalian ID: ${pengembalianId})`,
    },
    android: {
      priority: "high",
      notification: {
        channelId: "andalib_admin_channel",
      },
    },
  };

  return messaging.send(message);
}

module.exports = { notifyAdminsDamageProof, ADMIN_TOPIC };
