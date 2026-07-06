package com.jv.attentionpanner

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.SecureRandom

class VerseDatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, "VerseDb", null, 2) {
    companion object {
        @Volatile private var instance: VerseDatabaseHelper? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) { instance ?: VerseDatabaseHelper(context.applicationContext).also { instance = it } }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE verses (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT, reference TEXT, book TEXT)")
        db.execSQL("CREATE INDEX index_id ON verses(id)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS verses"); onCreate(db)
    }

    fun getVerseCount(): Int {
        return readableDatabase.rawQuery("SELECT COUNT(*) FROM verses", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getRandomVerseSet(secureRandom: SecureRandom): Verse? {
        val count = getVerseCount().toLong()
        if (count == 0L) return null
        val randomOffset = (secureRandom.nextDouble() * count).toLong()
        val cursor = readableDatabase.rawQuery("SELECT text, reference, book FROM verses LIMIT 3 OFFSET ?", arrayOf(randomOffset.toString()))

        val verses = mutableListOf<Verse>()
        var firstBook: String? = null
        cursor.use {
            while (it.moveToNext()) {
                val book = it.getString(2)
                if (firstBook == null) firstBook = book
                if (book == firstBook) verses.add(Verse(it.getString(0), it.getString(1))) else break
            }
        }
        if (verses.isEmpty()) return null

        val combinedText = verses.joinToString(" ") { it.text }
        val firstRef = verses.first().reference
        val displayRef = if (verses.size > 1) {
            try {
                "${firstRef.substringBeforeLast(":")}:${firstRef.substringAfterLast(":")}-${verses.last().reference.substringAfterLast(":")}"
            } catch (e: Exception) { firstRef }
        } else firstRef
        return Verse(combinedText, displayRef)
    }
}
