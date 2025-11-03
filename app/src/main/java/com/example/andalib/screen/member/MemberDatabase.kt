package com.example.andalib.screen.member

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class MemberDatabase(context: Context) :
// PERBAIKAN 1: Naikkan versi DB (misal dari 2 ke 3)
    SQLiteOpenHelper(context, "members.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                nim TEXT NOT NULL UNIQUE,
                gender TEXT NOT NULL, 
                faculty TEXT NOT NULL,
                major TEXT NOT NULL,
                contact TEXT NOT NULL,
                email TEXT NOT NULL,
                photo_path TEXT,
                registration_date TEXT NOT NULL
            )
        """) // <-- PERBAIKAN 2: Tambahkan "gender TEXT NOT NULL"
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Skema lama akan dihapus dan dibuat ulang
        db?.execSQL("DROP TABLE IF EXISTS members")
        onCreate(db)
    }

    fun insertMember(member: Member): Long {
        val values = ContentValues().apply {
            put("name", member.name)
            put("nim", member.nim)
            put("gender", member.gender) // <-- PERBAIKAN 3: Tambahkan gender
            put("faculty", member.faculty)
            put("major", member.major)
            put("contact", member.contact)
            put("email", member.email)
            put("photo_path", member.photoPath)
            put("registration_date", getCurrentDate())
        }
        return writableDatabase.insert("members", null, values)
    }

    fun getAllMembers(): List<Member> {
        val members = mutableListOf<Member>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM members ORDER BY id DESC",
            null
        )

        // PERBAIKAN 4: Perbarui indeks kolom
        if (cursor.moveToFirst()) {
            do {
                members.add(Member(
                    id = cursor.getInt(0),
                    name = cursor.getString(1),
                    nim = cursor.getString(2),
                    gender = cursor.getString(3),           // <-- Indeks 3
                    faculty = cursor.getString(4),          // <-- Indeks 4
                    major = cursor.getString(5),            // <-- Indeks 5
                    contact = cursor.getString(6),          // <-- Indeks 6
                    email = cursor.getString(7),            // <-- Indeks 7
                    photoPath = cursor.getString(8) ?: "",  // <-- Indeks 8
                    registrationDate = cursor.getString(9) ?: "" // <-- Indeks 9
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return members
    }

    fun searchMembers(query: String): List<Member> {
        val members = mutableListOf<Member>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM members WHERE name LIKE ? OR nim LIKE ? OR faculty LIKE ? OR major LIKE ?",
            arrayOf("%$query%", "%$query%", "%$query%", "%$query%")
        )

        // PERBAIKAN 5: Perbarui indeks kolom
        if (cursor.moveToFirst()) {
            do {
                members.add(Member(
                    id = cursor.getInt(0),
                    name = cursor.getString(1),
                    nim = cursor.getString(2),
                    gender = cursor.getString(3),           // <-- Indeks 3
                    faculty = cursor.getString(4),          // <-- Indeks 4
                    major = cursor.getString(5),            // <-- Indeks 5
                    contact = cursor.getString(6),          // <-- Indeks 6
                    email = cursor.getString(7),            // <-- Indeks 7
                    photoPath = cursor.getString(8) ?: "",  // <-- Indeks 8
                    registrationDate = cursor.getString(9) ?: "" // <-- Indeks 9
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return members
    }

    fun updateMember(member: Member): Int {
        val values = ContentValues().apply {
            put("name", member.name)
            put("nim", member.nim)
            put("gender", member.gender) // <-- PERBAIKAN 6: Tambahkan gender
            put("faculty", member.faculty)
            put("major", member.major)
            put("contact", member.contact)
            put("email", member.email)
            put("photo_path", member.photoPath)
        }
        return writableDatabase.update(
            "members",
            values,
            "id = ?",
            arrayOf(member.id.toString())
        )
    }

    fun deleteMember(id: Int): Int {
        return writableDatabase.delete(
            "members",
            "id = ?",
            arrayOf(id.toString())
        )
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date())
    }
}