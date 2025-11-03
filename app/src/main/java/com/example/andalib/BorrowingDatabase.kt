package com.example.andalib

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BorrowingDatabase(context: Context) :
    SQLiteOpenHelper(context, "borrowings.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE borrowings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                borrower_name TEXT NOT NULL,
                nim TEXT NOT NULL,
                major TEXT,
                contact TEXT,
                book_title TEXT NOT NULL,
                author TEXT,
                isbn TEXT,
                identity_path TEXT,
                borrow_date TEXT NOT NULL,
                return_date TEXT NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS borrowings")
        onCreate(db)
    }

    fun insertBorrowing(borrowing: Borrowing): Long {
        val values = ContentValues().apply {
            put("borrower_name", borrowing.borrowerName)
            put("nim", borrowing.nim)
            put("major", borrowing.major)
            put("contact", borrowing.contact)
            put("book_title", borrowing.bookTitle)
            put("author", borrowing.author)
            put("isbn", borrowing.isbn)
            put("identity_path", borrowing.identityPath)
            put("borrow_date", borrowing.borrowDate)
            put("return_date", borrowing.returnDate)
        }
        return writableDatabase.insert("borrowings", null, values)
    }

    fun getAllActiveBorrowings(): List<Borrowing> {
        val borrowings = mutableListOf<Borrowing>()
        // Mengambil semua data peminjaman aktif
        val cursor = readableDatabase.rawQuery("SELECT * FROM borrowings ORDER BY id DESC", null)

        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex("id")
            val nameIndex = cursor.getColumnIndex("borrower_name")
            val nimIndex = cursor.getColumnIndex("nim")
            val majorIndex = cursor.getColumnIndex("major")
            val contactIndex = cursor.getColumnIndex("contact")
            val titleIndex = cursor.getColumnIndex("book_title")
            val authorIndex = cursor.getColumnIndex("author")
            val isbnIndex = cursor.getColumnIndex("isbn")
            val identityPathIndex = cursor.getColumnIndex("identity_path")
            val borrowDateIndex = cursor.getColumnIndex("borrow_date")
            val returnDateIndex = cursor.getColumnIndex("return_date")

            do {
                borrowings.add(
                    Borrowing(
                        id = cursor.getInt(idIndex),
                        borrowerName = cursor.getString(nameIndex),
                        nim = cursor.getString(nimIndex),
                        major = cursor.getString(majorIndex) ?: "",
                        contact = cursor.getString(contactIndex) ?: "",
                        bookTitle = cursor.getString(titleIndex),
                        author = cursor.getString(authorIndex) ?: "",
                        isbn = cursor.getString(isbnIndex) ?: "",
                        identityPath = cursor.getString(identityPathIndex) ?: "",
                        borrowDate = cursor.getString(borrowDateIndex),
                        returnDate = cursor.getString(returnDateIndex)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return borrowings
    }

    fun searchBorrowings(query: String): List<Borrowing> {
        val borrowings = mutableListOf<Borrowing>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM borrowings WHERE borrower_name LIKE ? OR nim LIKE ? OR book_title LIKE ?",
            arrayOf("%$query%", "%$query%", "%$query%")
        )

        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex("id")
            val nameIndex = cursor.getColumnIndex("borrower_name")
            val nimIndex = cursor.getColumnIndex("nim")
            val majorIndex = cursor.getColumnIndex("major")
            val contactIndex = cursor.getColumnIndex("contact")
            val titleIndex = cursor.getColumnIndex("book_title")
            val authorIndex = cursor.getColumnIndex("author")
            val isbnIndex = cursor.getColumnIndex("isbn")
            val identityPathIndex = cursor.getColumnIndex("identity_path")
            val borrowDateIndex = cursor.getColumnIndex("borrow_date")
            val returnDateIndex = cursor.getColumnIndex("return_date")

            do {
                borrowings.add(
                    Borrowing(
                        id = cursor.getInt(idIndex),
                        borrowerName = cursor.getString(nameIndex),
                        nim = cursor.getString(nimIndex),
                        major = cursor.getString(majorIndex) ?: "",
                        contact = cursor.getString(contactIndex) ?: "",
                        bookTitle = cursor.getString(titleIndex),
                        author = cursor.getString(authorIndex) ?: "",
                        isbn = cursor.getString(isbnIndex) ?: "",
                        identityPath = cursor.getString(identityPathIndex) ?: "",
                        borrowDate = cursor.getString(borrowDateIndex),
                        returnDate = cursor.getString(returnDateIndex)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return borrowings
    }

    fun updateBorrowing(borrowing: Borrowing): Int {
        val values = ContentValues().apply {
            put("borrower_name", borrowing.borrowerName)
            put("nim", borrowing.nim)
            put("major", borrowing.major)
            put("contact", borrowing.contact)
            put("book_title", borrowing.bookTitle)
            put("author", borrowing.author)
            put("isbn", borrowing.isbn)
            put("identity_path", borrowing.identityPath)
            put("borrow_date", borrowing.borrowDate)
            put("return_date", borrowing.returnDate)
        }
        return writableDatabase.update(
            "borrowings",
            values,
            "id = ?",
            arrayOf(borrowing.id.toString())
        )
    }

    fun deleteBorrowing(id: Int): Int {
        return writableDatabase.delete(
            "borrowings",
            "id = ?",
            arrayOf(id.toString())
        )
    }
}