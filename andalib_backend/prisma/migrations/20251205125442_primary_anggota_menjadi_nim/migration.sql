/*
  Warnings:

  - The primary key for the `anggota` table will be changed. If it partially fails, the table could be left without primary key constraint.
  - You are about to drop the column `id` on the `anggota` table. All the data in the column will be lost.
  - You are about to drop the column `anggotaId` on the `peminjaman` table. All the data in the column will be lost.
  - Added the required column `anggotaNim` to the `Peminjaman` table without a default value. This is not possible if the table is not empty.

*/
-- DropForeignKey
ALTER TABLE `peminjaman` DROP FOREIGN KEY `Peminjaman_anggotaId_fkey`;

-- DropIndex
DROP INDEX `Anggota_nim_key` ON `anggota`;

-- AlterTable
ALTER TABLE `anggota` DROP PRIMARY KEY,
    DROP COLUMN `id`,
    ADD PRIMARY KEY (`nim`);

-- AlterTable
ALTER TABLE `peminjaman` DROP COLUMN `anggotaId`,
    ADD COLUMN `anggotaNim` VARCHAR(191) NOT NULL;

-- AddForeignKey
ALTER TABLE `Peminjaman` ADD CONSTRAINT `Peminjaman_anggotaNim_fkey` FOREIGN KEY (`anggotaNim`) REFERENCES `Anggota`(`nim`) ON DELETE RESTRICT ON UPDATE CASCADE;
