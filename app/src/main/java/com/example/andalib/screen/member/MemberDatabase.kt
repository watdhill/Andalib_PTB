package com.example.andalib.screen.member

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class MemberDatabase(context: Context) :
    SQLiteOpenHelper(context, "members.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("""
            CREATE TABLE members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                nim TEXT NOT NULL UNIQUE,
                major TEXT NOT NULL,
                contact TEXT NOT NULL,
                email TEXT NOT NULL,
                photo_path TEXT,
                registration_date TEXT NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS members")
        onCreate(db)
    }

    fun insertMember(member: Member): Long {
        val values = ContentValues().apply {
            put("name", member.name)
            put("nim", member.nim)
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

        if (cursor.moveToFirst()) {
            do {
                members.add(Member(
                    id = cursor.getInt(0),
                    name = cursor.getString(1),
                    nim = cursor.getString(2),
                    major = cursor.getString(3),
                    faculty = cursor.getString(3),
                    contact = cursor.getString(5),
                    email = cursor.getString(6),
                    photoPath = cursor.getString(7) ?: "",
                    registrationDate = cursor.getString(8) ?: ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return members
    }

    fun searchMembers(query: String): List<Member> {
        val members = mutableListOf<Member>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM members WHERE name LIKE ? OR nim LIKE ? OR major LIKE ?",
            arrayOf("%$query%", "%$query%", "%$query%")
        )

        if (cursor.moveToFirst()) {
            do {
                members.add(Member(
                    id = cursor.getInt(0),
                    name = cursor.getString(1),
                    nim = cursor.getString(2),
                    major = cursor.getString(3),
                    faculty = cursor.getString(4),
                    contact = cursor.getString(5),
                    email = cursor.getString(6),
                    photoPath = cursor.getString(7) ?: "",
                    registrationDate = cursor.getString(8) ?: ""
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