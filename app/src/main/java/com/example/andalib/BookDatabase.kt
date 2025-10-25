package com.example.andalib

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BookDatabase(context: Context) :
    SQLiteOpenHelper(context, "perpustakaan.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE books (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                author TEXT NOT NULL,
                publisher TEXT,
                year TEXT,
                category TEXT,
                cover_path TEXT
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS books")
        onCreate(db)
    }

    fun insertBook(book: Book): Long {
        val values = ContentValues().apply {
            put("title", book.title)
            put("author", book.author)
            put("publisher", book.publisher)
            put("year", book.year)
            put("category", book.category)
            put("cover_path", book.coverPath)
        }
        return writableDatabase.insert("books", null, values)
    }

    fun getAllBooks(): List<Book> {
        val books = mutableListOf<Book>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM books ORDER BY id DESC", null)

        if (cursor.moveToFirst()) {
            do {
                books.add(Book(
                    id = cursor.getInt(0),
                    title = cursor.getString(1),
                    author = cursor.getString(2),
                    publisher = cursor.getString(3) ?: "",
                    year = cursor.getString(4) ?: "",
                    category = cursor.getString(5) ?: "",
                    coverPath = cursor.getString(6) ?: ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return books
    }

    fun searchBooks(query: String): List<Book> {
        val books = mutableListOf<Book>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM books WHERE title LIKE ? OR author LIKE ?",
            arrayOf("%$query%", "%$query%")
        )

        if (cursor.moveToFirst()) {
            do {
                books.add(Book(
                    id = cursor.getInt(0),
                    title = cursor.getString(1),
                    author = cursor.getString(2),
                    publisher = cursor.getString(3) ?: "",
                    year = cursor.getString(4) ?: "",
                    category = cursor.getString(5) ?: "",
                    coverPath = cursor.getString(6) ?: ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return books
    }

    fun updateBook(book: Book): Int {
        val values = ContentValues().apply {
            put("title", book.title)
            put("author", book.author)
            put("publisher", book.publisher)
            put("year", book.year)
            put("category", book.category)
            put("cover_path", book.coverPath)
        }
        return writableDatabase.update("books", values, "id = ?", arrayOf(book.id.toString()))
    }

    fun deleteBook(id: Int): Int {
        return writableDatabase.delete("books", "id = ?", arrayOf(id.toString()))
    }
}