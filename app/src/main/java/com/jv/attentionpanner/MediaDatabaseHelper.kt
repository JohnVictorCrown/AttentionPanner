package com.jv.attentionpanner

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.provider.MediaStore
import java.security.SecureRandom

class MediaDatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, "MediaDb", null, 1) {
    companion object {
        @Volatile private var instance: MediaDatabaseHelper? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) { instance ?: MediaDatabaseHelper(context.applicationContext).also { instance = it } }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE media_table (id INTEGER PRIMARY KEY AUTOINCREMENT, uri_string TEXT)")
        db.execSQL("CREATE INDEX index_media_id ON media_table(id)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS media_table"); onCreate(db)
    }

    fun getMediaCount(): Int {
        return readableDatabase.rawQuery("SELECT COUNT(*) FROM media_table", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun syncFromMediaStore(context: Context) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM media_table")
            db.execSQL("DELETE FROM sqlite_sequence WHERE name='media_table'")

            val selection = "${MediaStore.MediaColumns.SIZE} > 16384"
            val projection = arrayOf(MediaStore.MediaColumns._ID)

            val uris = listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)

            for (contentUri in uris) {
                context.contentResolver.query(contentUri, projection, selection, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    while (cursor.moveToNext()) {
                        val uri = Uri.withAppendedPath(contentUri, cursor.getLong(idCol).toString()).toString()
                        val values = ContentValues().apply { put("uri_string", uri) }
                        db.insert("media_table", null, values)
                    }
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) { e.printStackTrace() } finally {
            db.endTransaction()
            db.execSQL("VACUUM")
        }
    }

    fun getRandomMediaUri(secureRandom: SecureRandom): Uri? {
        val count = getMediaCount().toLong()
        if (count == 0L) return null
        val randomOffset = (secureRandom.nextDouble() * count).toLong()
        return readableDatabase.rawQuery("SELECT uri_string FROM media_table LIMIT 1 OFFSET ?", arrayOf(randomOffset.toString())).use {
            if (it.moveToFirst()) Uri.parse(it.getString(0)) else null
        }
    }
}
