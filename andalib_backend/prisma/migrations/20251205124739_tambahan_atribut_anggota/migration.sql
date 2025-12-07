/*
  Warnings:

  - You are about to drop the column `address` on the `anggota` table. All the data in the column will be lost.
  - You are about to drop the column `memberId` on the `anggota` table. All the data in the column will be lost.
  - You are about to drop the column `phone` on the `anggota` table. All the data in the column will be lost.
  - A unique constraint covering the columns `[nim]` on the table `Anggota` will be added. If there are existing duplicate values, this will fail.
  - Added the required column `contact` to the `Anggota` table without a default value. This is not possible if the table is not empty.
  - Added the required column `email` to the `Anggota` table without a default value. This is not possible if the table is not empty.
  - Added the required column `faculty` to the `Anggota` table without a default value. This is not possible if the table is not empty.
  - Added the required column `gender` to the `Anggota` table without a default value. This is not possible if the table is not empty.
  - Added the required column `major` to the `Anggota` table without a default value. This is not possible if the table is not empty.
  - Added the required column `nim` to the `Anggota` table without a default value. This is not possible if the table is not empty.

*/
-- DropIndex
DROP INDEX `Anggota_memberId_key` ON `anggota`;

-- AlterTable
ALTER TABLE `anggota` DROP COLUMN `address`,
    DROP COLUMN `memberId`,
    DROP COLUMN `phone`,
    ADD COLUMN `contact` VARCHAR(191) NOT NULL,
    ADD COLUMN `email` VARCHAR(191) NOT NULL,
    ADD COLUMN `faculty` ENUM('HUKUM', 'PERTANIAN', 'KEDOKTERAN', 'MIPA', 'EKONOMI_BISNIS', 'PETERNAKAN', 'ILMU_BUDAYA', 'FISIP', 'TEKNIK', 'FARMASI', 'TEKNOLOGI_PERTANIAN', 'KESEHATAN_MASYARAKAT', 'KEPERAWATAN', 'KEDOKTERAN_GIGI', 'TEKNOLOGI_INFORMASI') NOT NULL,
    ADD COLUMN `gender` ENUM('LAKI_LAKI', 'PEREMPUAN') NOT NULL,
    ADD COLUMN `major` ENUM('ILMU_HUKUM', 'AGRIBISNIS', 'AGROTEKNOLOGI', 'ILMU_TANAH', 'PROTEKSI_TANAMAN', 'PENYULUHAN_PERTANIAN', 'AGROEKOTEKNOLOGI', 'PENDIDIKAN_DOKTER', 'KEBIDANAN', 'PSIKOLOGI', 'ILMU_BIOMEDIS', 'KIMIA', 'BIOLOGI', 'MATEMATIKA', 'FISIKA', 'EKONOMI', 'MANAJEMEN', 'AKUNTANSI', 'PRODUKSI_TERNAK', 'NUTRISI_DAN_MAKANAN_TERNAK', 'ILMU_SEJARAH', 'SASTRA_INGGRIS', 'SASTRA_INDONESIA', 'SASTRA_MINANGKABAU', 'SASTRA_JEPANG', 'ILMU_POLITIK', 'SOSIOLOGI', 'ANTROPOLOGI_SOSIAL', 'ILMU_HUBUNGAN_INTERNASIONAL', 'ILMU_KOMUNIKASI', 'ADMINISTRASI_PUBLIK', 'TEKNIK_MESIN', 'TEKNIK_SIPIL', 'TEKNIK_LINGKUNGAN', 'TEKNIK_INDUSTRI', 'TEKNIK_ELEKTRO', 'FARMASI', 'TEKNOLOGI_PANGAN_HASIL_PERTANIAN', 'TEKNIK_PERTANIAN_BIOSISTEM', 'TEKNOLOGI_INDUSTRI_PERTANIAN', 'KESEHATAN_MASYARAKAT', 'GIZI', 'KEPERAWATAN', 'PENDIDIKAN_DOKTER_GIGI', 'SISTEM_INFORMASI', 'INFORMATIKA', 'TEKNIK_KOMPUTER') NOT NULL,
    ADD COLUMN `nim` VARCHAR(191) NOT NULL,
    ADD COLUMN `photoPath` VARCHAR(191) NULL,
    ADD COLUMN `registrationDate` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);

-- CreateIndex
CREATE UNIQUE INDEX `Anggota_nim_key` ON `Anggota`(`nim`);
