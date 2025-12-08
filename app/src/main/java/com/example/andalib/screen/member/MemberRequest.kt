package com.example.andalib.screen.member

import androidx.compose.ui.graphics.Color

enum class Gender {
    LAKI_LAKI,
    PEREMPUAN
}

enum  class Faculty {
    HUKUM,
    PERTANIAN,
    KEDOKTERAN,
    MIPA,
    EKONOMI_BISNIS,
    PETERNAKAN,
    ILMU_BUDAYA,
    FISIP,
    TEKNIK,
    FARMASI,
    TEKNOLOGI_PERTANIAN,
    KESEHATAN_MASYARAKAT,
    KEPERAWATAN,
    KEDOKTERAN_GIGI,
    TEKNOLOGI_INFORMASI
}

enum class Major {
    ILMU_HUKUM,
    AGRIBISNIS,
    AGROTEKNOLOGI,
    ILMU_TANAH,
    PROTEKSI_TANAMAN,
    PENYULUHAN_PERTANIAN,
    AGROEKOTEKNOLOGI,
    PENDIDIKAN_DOKTER,
    KEBIDANAN,
    PSIKOLOGI,
    ILMU_BIOMEDIS,
    KIMIA,
    BIOLOGI,
    MATEMATIKA,
    FISIKA,
    EKONOMI,
    MANAJEMEN,
    AKUNTANSI,
    PETERNAKAN,
    ILMU_SEJARAH,
    SASTRA_INGGRIS,
    SASTRA_INDONESIA,
    SASTRA_MINANGKABAU,
    SASTRA_JEPANG,
    ILMU_POLITIK,
    SOSIOLOGI,
    ANTROPOLOGI_SOSIAL,
    ILMU_HUBUNGAN_INTERNASIONAL,
    ILMU_KOMUNIKASI,
    ADMINISTRASI_PUBLIK,
    TEKNIK_MESIN,
    TEKNIK_SIPIL,
    TEKNIK_LINGKUNGAN,
    TEKNIK_INDUSTRI,
    TEKNIK_ELEKTRO,
    FARMASI,
    TEKNOLOGI_PANGAN_HASIL_PERTANIAN,
    TEKNIK_PERTANIAN_BIOSISTEM,
    TEKNOLOGI_INDUSTRI_PERTANIAN,
    KESEHATAN_MASYARAKAT,
    GIZI,
    KEPERAWATAN,
    PENDIDIKAN_DOKTER_GIGI,
    SISTEM_INFORMASI,
    INFORMATIKA,
    TEKNIK_KOMPUTER
}

fun Faculty.getMajors(): List<Major> {
    return when(this) {
        Faculty.HUKUM -> listOf(Major.ILMU_HUKUM)
        Faculty.PERTANIAN -> listOf(
            Major.AGRIBISNIS, Major.AGROTEKNOLOGI, Major.ILMU_TANAH,
            Major.PROTEKSI_TANAMAN, Major.PENYULUHAN_PERTANIAN, Major.AGROEKOTEKNOLOGI
        )
        Faculty.KEDOKTERAN -> listOf(
            Major.PENDIDIKAN_DOKTER, Major.KEBIDANAN, Major.PSIKOLOGI, Major.ILMU_BIOMEDIS
        )
        Faculty.MIPA -> listOf(
            Major.KIMIA, Major.BIOLOGI, Major.MATEMATIKA, Major.FISIKA
        )
        Faculty.EKONOMI_BISNIS -> listOf(
            Major.EKONOMI, Major.MANAJEMEN, Major.AKUNTANSI
        )
        Faculty.PETERNAKAN -> listOf(Major.PETERNAKAN)
        Faculty.ILMU_BUDAYA -> listOf(
            Major.ILMU_SEJARAH, Major.SASTRA_INGGRIS, Major.SASTRA_INDONESIA,
            Major.SASTRA_MINANGKABAU, Major.SASTRA_JEPANG
        )
        Faculty.FISIP -> listOf(
            Major.ILMU_POLITIK, Major.SOSIOLOGI, Major.ANTROPOLOGI_SOSIAL,
            Major.ILMU_HUBUNGAN_INTERNASIONAL, Major.ILMU_KOMUNIKASI, Major.ADMINISTRASI_PUBLIK
        )
        Faculty.TEKNIK -> listOf(
            Major.TEKNIK_MESIN, Major.TEKNIK_SIPIL, Major.TEKNIK_LINGKUNGAN,
            Major.TEKNIK_INDUSTRI, Major.TEKNIK_ELEKTRO
        )
        Faculty.FARMASI -> listOf(Major.FARMASI)
        Faculty.TEKNOLOGI_PERTANIAN -> listOf(
            Major.TEKNOLOGI_PANGAN_HASIL_PERTANIAN, Major.TEKNIK_PERTANIAN_BIOSISTEM,
            Major.TEKNOLOGI_INDUSTRI_PERTANIAN
        )
        Faculty.KESEHATAN_MASYARAKAT -> listOf(
            Major.KESEHATAN_MASYARAKAT, Major.GIZI
        )
        Faculty.KEPERAWATAN -> listOf(Major.KEPERAWATAN)
        Faculty.KEDOKTERAN_GIGI -> listOf(Major.PENDIDIKAN_DOKTER_GIGI)
        Faculty.TEKNOLOGI_INFORMASI -> listOf(
            Major.SISTEM_INFORMASI, Major.INFORMATIKA, Major.TEKNIK_KOMPUTER
        )
    }
}

fun Faculty.getBadgeColor(): Color {
    return when(this) {
        Faculty.HUKUM -> Color(0xFFB0BEC5) // Gray
        Faculty.PERTANIAN -> Color(0xFF66BB6A) // Green
        Faculty.KEDOKTERAN -> Color(0xFFEF5350) // Red
        Faculty.MIPA -> Color(0xFF42A5F5) // Blue
        Faculty.EKONOMI_BISNIS -> Color(0xFF81C784) // Light Green
        Faculty.PETERNAKAN -> Color(0xFFFF7043) // Deep Orange
        Faculty.ILMU_BUDAYA -> Color(0xFFAB47BC) // Purple
        Faculty.FISIP -> Color(0xFF26C6DA) // Cyan
        Faculty.TEKNIK -> Color(0xFF5C6BC0) // Indigo
        Faculty.FARMASI -> Color(0xFFEC407A) // Pink
        Faculty.TEKNOLOGI_PERTANIAN -> Color(0xFF9CCC65) // Light Green
        Faculty.KESEHATAN_MASYARAKAT -> Color(0xFF26A69A) // Teal
        Faculty.KEPERAWATAN -> Color(0xFFFF7043) // Deep Orange
        Faculty.KEDOKTERAN_GIGI -> Color(0xFFFFA726) // Orange
        Faculty.TEKNOLOGI_INFORMASI -> Color(0xFF29B6F6) // Light Blue
    }
}

data class MemberRequest(
    val id: Int = 0,
    val name: String,
    val nim: String,
    val gender: Gender,
    val faculty: Faculty,
    val major: Major,
    val contact: String,
    val email: String = "",
    val photoPath: String = "",
    val registrationDate: String = ""
)