package com.jv.attentionpanner

import android.content.ContentValues
import android.content.Context
import org.json.JSONArray

object VerseLoader {

    fun loadFromAssets(context: Context, dbHelper: VerseDatabaseHelper): Boolean {
        try {
            val files = listOf(
                "proverbs.json" to "Proverbs",
                "song_of_solomon.json" to "Song of Solomon",
                "sirach.json" to "Sirach"
            )
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                for ((fileName, bookName) in files) {
                    val json = context.assets.open(fileName).bufferedReader(charset = Charsets.UTF_8).use { it.readText() }
                    val arr = JSONArray(json)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val chapter = obj.getLong("chapter")
                        val verse = obj.getLong("verse")
                        val text = obj.getString("text")
                        val values = ContentValues().apply {
                            put("text", text)
                            put("reference", "$bookName $chapter:$verse")
                            put("book", bookName)
                        }
                        db.insert("verses", null, values)
                    }
                }
                db.setTransactionSuccessful()
                return true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            android.util.Log.e("VerseLoader", "Failed to load verses from assets", e)
            return false
        }
    }
}
