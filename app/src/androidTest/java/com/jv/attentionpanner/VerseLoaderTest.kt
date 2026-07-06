package com.jv.attentionpanner

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VerseLoaderTest {
    private lateinit var dbHelper: VerseDatabaseHelper

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        dbHelper = VerseDatabaseHelper.getInstance(context)
    }

    @After
    fun tearDown() {
        val db = dbHelper.writableDatabase
        db.execSQL("DELETE FROM verses")
    }

    @Test
    fun loadFromAssets_populatesDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("Initial count should be 0", 0, dbHelper.getVerseCount())

        val success = VerseLoader.loadFromAssets(context, dbHelper)

        assertTrue("loadFromAssets should return true", success)
        assertTrue("Verse count should be > 0, was ${dbHelper.getVerseCount()}", dbHelper.getVerseCount() > 0)
    }

    @Test
    fun loadedVersesContainAllBooks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        VerseLoader.loadFromAssets(context, dbHelper)

        val cursor = dbHelper.readableDatabase.rawQuery("SELECT DISTINCT book FROM verses", null)
        val books = mutableSetOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                books.add(it.getString(0))
            }
        }
        assertTrue("Should contain Proverbs", books.contains("Proverbs"))
        assertTrue("Should contain Song of Solomon", books.contains("Song of Solomon"))
        assertTrue("Should contain Sirach", books.contains("Sirach"))
        assertEquals("Should have exactly 3 books", 3, books.size)
    }

    @Test
    fun getRandomVerseSet_returnsVerseWithReference() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        VerseLoader.loadFromAssets(context, dbHelper)

        val secureRandom = java.security.SecureRandom()
        val verse = dbHelper.getRandomVerseSet(secureRandom)

        assertNotNull("getRandomVerseSet should return a verse", verse)
        assertNotNull("Verse text should not be null", verse?.text)
        assertTrue("Verse text should not be empty", verse?.text?.isNotEmpty() == true)
        assertNotNull("Verse reference should not be null", verse?.reference)
        assertTrue("Verse reference should not be empty", verse?.reference?.isNotEmpty() == true)
    }

    @Test
    fun multiVerseReferencesContainDash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        VerseLoader.loadFromAssets(context, dbHelper)

        val secureRandom = java.security.SecureRandom()
        var foundMultiVerse = false

        repeat(100) {
            val verse = dbHelper.getRandomVerseSet(secureRandom)
            if (verse != null && verse.reference.contains("-")) {
                foundMultiVerse = true
            }
        }
        assertTrue("At least one multi-verse reference should appear in 100 samples", foundMultiVerse)
    }

    @Test
    fun countMatchesExpectedTotal() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        VerseLoader.loadFromAssets(context, dbHelper)

        assertEquals("Total verses should be 2393", 2393, dbHelper.getVerseCount())
    }
}
