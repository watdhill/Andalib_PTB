/*
  Warnings:

  - You are about to alter the column `isRead` on the `notification` table. The data in that column could be lost. The data in that column will be cast from `Int` to `TinyInt`.

*/
-- AlterTable
ALTER TABLE `notification` MODIFY `isRead` BOOLEAN NOT NULL DEFAULT false;
