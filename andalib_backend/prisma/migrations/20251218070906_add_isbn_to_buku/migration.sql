/*
  Warnings:

  - A unique constraint covering the columns `[isbn]` on the table `Buku` will be added. If there are existing duplicate values, this will fail.
  - Added the required column `isbn` to the `Buku` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE `buku` ADD COLUMN `isbn` VARCHAR(191) NOT NULL;

-- AlterTable
ALTER TABLE `peminjaman` ADD COLUMN `krsImagePath` VARCHAR(191) NULL;

-- CreateIndex
CREATE UNIQUE INDEX `Buku_isbn_key` ON `Buku`(`isbn`);
