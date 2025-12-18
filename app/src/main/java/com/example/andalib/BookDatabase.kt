package com.example.andalib

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BookDatabase(context: Context) :
    SQLiteOpenHelper(context, "perpustakaan.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE books (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                isbn TEXT UNIQUE NOT NULL,
                title TEXT NOT NULL,
                author TEXT NOT NULL,
                publisher TEXT,
                year TEXT,
                category TEXT,
                cover_path TEXT,
                server_id INTEGER
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            // Jika dari versi lama atau ada error, reset database
            try {
                db?.execSQL("DROP TABLE IF EXISTS books")
                db?.execSQL("DROP TABLE IF EXISTS books_new")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onCreate(db)
        }
    }

    fun insertBook(book: Book): Long {
        return try {
            val values = ContentValues().apply {
                put("isbn", book.isbn)
                put("title", book.title.ifEmpty { "Untitled" })
                put("author", book.author.ifEmpty { "Unknown" })
                put("publisher", book.publisher)
                put("year", book.year)
                put("category", book.category)
                put("cover_path", book.coverPath)
                if (book.serverId != null) put("server_id", book.serverId)
            }
            writableDatabase.insert("books", null, values)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun getAllBooks(): List<Book> {
        val books = mutableListOf<Book>()
        try {
            val cursor = readableDatabase.rawQuery("SELECT id, isbn, title, author, publisher, year, category, cover_path, server_id FROM books ORDER BY id DESC", null)

            if (cursor.moveToFirst()) {
                do {
                    try {
                        books.add(Book(
                            id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                            isbn = cursor.getString(cursor.getColumnIndexOrThrow("isbn")) ?: "",
                            title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                            author = cursor.getString(cursor.getColumnIndexOrThrow("author")) ?: "",
                            publisher = cursor.getString(cursor.getColumnIndexOrThrow("publisher")) ?: "",
                            year = cursor.getString(cursor.getColumnIndexOrThrow("year")) ?: "",
                            category = cursor.getString(cursor.getColumnIndexOrThrow("category")) ?: "",
                            coverPath = cursor.getString(cursor.getColumnIndexOrThrow("cover_path")) ?: "",
                            serverId = if (!cursor.isNull(cursor.getColumnIndexOrThrow("server_id"))) cursor.getInt(cursor.getColumnIndexOrThrow("server_id")) else null
                        ))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return books
    }

    fun searchBooks(query: String): List<Book> {
        val books = mutableListOf<Book>()
        try {
            val cursor = readableDatabase.rawQuery(
                "SELECT id, isbn, title, author, publisher, year, category, cover_path, server_id FROM books WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ? ORDER BY id DESC",
                arrayOf("%$query%", "%$query%", "%$query%")
            )

            if (cursor.moveToFirst()) {
                do {
                    try {
                        books.add(Book(
                            id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                            isbn = cursor.getString(cursor.getColumnIndexOrThrow("isbn")) ?: "",
                            title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                            author = cursor.getString(cursor.getColumnIndexOrThrow("author")) ?: "",
                            publisher = cursor.getString(cursor.getColumnIndexOrThrow("publisher")) ?: "",
                            year = cursor.getString(cursor.getColumnIndexOrThrow("year")) ?: "",
                            category = cursor.getString(cursor.getColumnIndexOrThrow("category")) ?: "",
                            coverPath = cursor.getString(cursor.getColumnIndexOrThrow("cover_path")) ?: "",
                            serverId = if (!cursor.isNull(cursor.getColumnIndexOrThrow("server_id"))) cursor.getInt(cursor.getColumnIndexOrThrow("server_id")) else null
                        ))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return books
    }

    fun updateBook(book: Book): Int {
        return try {
            val values = ContentValues().apply {
                put("isbn", book.isbn)
                put("title", book.title.ifEmpty { "Untitled" })
                put("author", book.author.ifEmpty { "Unknown" })
                put("publisher", book.publisher)
                put("year", book.year)
                put("category", book.category)
                put("cover_path", book.coverPath)
                if (book.serverId != null) put("server_id", book.serverId)
            }
            writableDatabase.update("books", values, "id = ?", arrayOf(book.id.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun deleteBook(id: Int): Int {
        return try {
            writableDatabase.delete("books", "id = ?", arrayOf(id.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun setServerId(localId: Int, serverId: Int): Int {
        return try {
            val values = ContentValues().apply { put("server_id", serverId) }
            writableDatabase.update("books", values, "id = ?", arrayOf(localId.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun isbnExists(isbn: String, excludeId: Int = -1): Boolean {
        return try {
            val query = if (excludeId > 0) {
                "SELECT COUNT(*) FROM books WHERE isbn = ? AND id != ?"
            } else {
                "SELECT COUNT(*) FROM books WHERE isbn = ?"
            }
            
            val cursor = readableDatabase.rawQuery(query, arrayOf(isbn, excludeId.toString()))
            var exists = false
            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0
            }
            cursor.close()
            exists
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}