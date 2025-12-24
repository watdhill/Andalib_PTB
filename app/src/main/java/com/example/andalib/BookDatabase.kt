package com.example.andalib

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BookDatabase(context: Context) :
    SQLiteOpenHelper(context, "perpustakaan.db", null, 4) {

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
                stok INTEGER DEFAULT 0,
                server_id INTEGER
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            // Add stok column if upgrading from version 3
            try {
                db?.execSQL("ALTER TABLE books ADD COLUMN stok INTEGER DEFAULT 0")
            } catch (e: Exception) {
                // If ALTER fails, recreate table
                db?.execSQL("DROP TABLE IF EXISTS books")
                db?.execSQL("DROP TABLE IF EXISTS books_new")
                onCreate(db)
            }
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
                put("stok", book.stok)
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
            val cursor = readableDatabase.rawQuery("SELECT id, isbn, title, author, publisher, year, category, cover_path, stok, server_id FROM books ORDER BY id DESC", null)

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
                            stok = cursor.getInt(cursor.getColumnIndexOrThrow("stok")),
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
                "SELECT id, isbn, title, author, publisher, year, category, cover_path, stok, server_id FROM books WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ? ORDER BY id DESC",
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
                            stok = cursor.getInt(cursor.getColumnIndexOrThrow("stok")),
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
                put("stok", book.stok)
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

    fun getBookByIsbn(isbn: String): Book? {
        return try {
            val cursor = readableDatabase.rawQuery(
                "SELECT id, isbn, title, author, publisher, year, category, cover_path, stok, server_id FROM books WHERE isbn = ? LIMIT 1",
                arrayOf(isbn)
            )
            
            var book: Book? = null
            if (cursor.moveToFirst()) {
                book = Book(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    isbn = cursor.getString(cursor.getColumnIndexOrThrow("isbn")) ?: "",
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                    author = cursor.getString(cursor.getColumnIndexOrThrow("author")) ?: "",
                    publisher = cursor.getString(cursor.getColumnIndexOrThrow("publisher")) ?: "",
                    year = cursor.getString(cursor.getColumnIndexOrThrow("year")) ?: "",
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")) ?: "",
                    coverPath = cursor.getString(cursor.getColumnIndexOrThrow("cover_path")) ?: "",
                    stok = cursor.getInt(cursor.getColumnIndexOrThrow("stok")),
                    serverId = if (!cursor.isNull(cursor.getColumnIndexOrThrow("server_id"))) cursor.getInt(cursor.getColumnIndexOrThrow("server_id")) else null
                )
            }
            cursor.close()
            book
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun updateStok(isbn: String, addAmount: Int): Boolean {
        return try {
            val book = getBookByIsbn(isbn)
            if (book != null) {
                val newStok = (book.stok + addAmount).coerceAtLeast(0)
                android.util.Log.d("BookDatabase", "Updating stock for ISBN: $isbn, current: ${book.stok}, adding: $addAmount, new: $newStok")
                val values = ContentValues().apply {
                    put("stok", newStok)
                }
                val rows = writableDatabase.update("books", values, "isbn = ?", arrayOf(isbn))
                android.util.Log.d("BookDatabase", "Update result: $rows rows affected")
                rows > 0
            } else {
                android.util.Log.d("BookDatabase", "Book not found for ISBN: $isbn")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("BookDatabase", "Error updating stock", e)
            e.printStackTrace()
            false
        }
    }
}