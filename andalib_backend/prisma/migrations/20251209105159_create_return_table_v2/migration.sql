/*
  Warnings:

  - You are about to drop the column `quantity` on the `buku` table. All the data in the column will be lost.
  - You are about to drop the column `borrowDate` on the `peminjaman` table. All the data in the column will be lost.
  - You are about to drop the column `dueDate` on the `peminjaman` table. All the data in the column will be lost.
  - You are about to drop the column `isReturned` on the `peminjaman` table. All the data in the column will be lost.
  - You are about to drop the column `returnDate` on the `peminjaman` table. All the data in the column will be lost.
  - You are about to drop the `denda` table. If the table is not empty, all the data it contains will be lost.
  - Added the required column `jatuhTempo` to the `Peminjaman` table without a default value. This is not possible if the table is not empty.

*/
-- DropForeignKey
ALTER TABLE `denda` DROP FOREIGN KEY `Denda_peminjamanId_fkey`;

-- DropForeignKey
ALTER TABLE `peminjaman` DROP FOREIGN KEY `Peminjaman_adminId_fkey`;

-- AlterTable
ALTER TABLE `buku` DROP COLUMN `quantity`,
    ADD COLUMN `stok` INTEGER NOT NULL DEFAULT 1;

-- AlterTable
ALTER TABLE `peminjaman` DROP COLUMN `borrowDate`,
    DROP COLUMN `dueDate`,
    DROP COLUMN `isReturned`,
    DROP COLUMN `returnDate`,
    ADD COLUMN `jatuhTempo` DATETIME(3) NOT NULL,
    ADD COLUMN `status` ENUM('DIPINJAM', 'DIKEMBALIKAN') NOT NULL DEFAULT 'DIPINJAM',
    ADD COLUMN `tanggalPinjam` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    MODIFY `adminId` INTEGER NULL;

-- DropTable
DROP TABLE `denda`;

-- CreateTable
CREATE TABLE `Pengembalian` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `tanggalPengembalian` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `denda` INTEGER NOT NULL DEFAULT 0,
    `keterangan` VARCHAR(191) NULL,
    `peminjamanId` INTEGER NOT NULL,

    UNIQUE INDEX `Pengembalian_peminjamanId_key`(`peminjamanId`),
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE `Peminjaman` ADD CONSTRAINT `Peminjaman_adminId_fkey` FOREIGN KEY (`adminId`) REFERENCES `Admin`(`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `Pengembalian` ADD CONSTRAINT `Pengembalian_peminjamanId_fkey` FOREIGN KEY (`peminjamanId`) REFERENCES `Peminjaman`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;
