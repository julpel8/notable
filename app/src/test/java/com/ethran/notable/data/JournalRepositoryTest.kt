package com.ethran.notable.data

import androidx.room.Room
import com.ethran.notable.data.db.AppDatabase
import com.ethran.notable.data.model.BackgroundType
import com.ethran.notable.data.model.isCheckedValue
import com.ethran.notable.data.model.parseDailyValues
import com.ethran.notable.data.model.taskValueKey
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Room-backed tests for the daily page lifecycle (in-memory database — the
 * production DatabaseModule writes to external storage and is bypassed here).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class JournalRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: JournalRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = JournalRepository(db.dailyPageDao(), db.pageDao(), db.folderDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `getOrCreateDailyPage is idempotent for a given date`() = runBlocking {
        val date = LocalDate.parse("2026-06-10")
        val first = repository.getOrCreateDailyPage(date)
        val second = repository.getOrCreateDailyPage(date)

        assertEquals(first.pageId, second.pageId)
        assertEquals("2026-06-10", first.date)
    }

    @Test
    fun `created page is a standalone daily page in the Journal folder`() = runBlocking {
        val date = LocalDate.parse("2026-06-10")
        val dailyPage = repository.getOrCreateDailyPage(date)

        val page = db.pageDao().getById(dailyPage.pageId)
        assertNotNull(page)
        assertNull(page!!.notebookId)
        assertEquals(BackgroundType.Daily.key, page.backgroundType)
        assertEquals("2026-06-10", page.background)

        val journal = db.folderDao().getByTitleInRoot(JOURNAL_FOLDER_TITLE)
        assertNotNull(journal)
        assertEquals(journal!!.id, page.parentFolderId)
    }

    @Test
    fun `multiple dates share a single Journal folder`() = runBlocking {
        repository.getOrCreateDailyPage(LocalDate.parse("2026-06-10"))
        repository.getOrCreateDailyPage(LocalDate.parse("2026-06-11"))
        repository.getOrCreateDailyPage(LocalDate.parse("2026-06-12"))

        val journalFolders = db.folderDao().getAll().filter { it.title == JOURNAL_FOLDER_TITLE }
        assertEquals(1, journalFolders.size)
    }

    @Test
    fun `deleting the page cascades to the DailyPage row`() = runBlocking {
        val date = LocalDate.parse("2026-06-10")
        val dailyPage = repository.getOrCreateDailyPage(date)

        db.pageDao().delete(dailyPage.pageId)

        assertNull(repository.getByDate(date))
        assertNull(repository.getByPageId(dailyPage.pageId))
    }

    @Test
    fun `getByPageId finds the daily page and ignores other pages`() = runBlocking {
        val dailyPage = repository.getOrCreateDailyPage(LocalDate.parse("2026-06-10"))

        assertEquals("2026-06-10", repository.getByPageId(dailyPage.pageId)?.date)
        assertNull(repository.getByPageId("not-a-daily-page-id"))
    }

    @Test
    fun `new daily pages start with empty interactive values`() = runBlocking {
        val dailyPage = repository.getOrCreateDailyPage(LocalDate.parse("2026-06-10"))
        assertEquals(emptyMap<String, Float>(), parseDailyValues(dailyPage.valuesJson))
    }

    @Test
    fun `toggleValue persists and a second toggle clears it`() = runBlocking {
        val dailyPage = repository.getOrCreateDailyPage(LocalDate.parse("2026-06-10"))
        val key = taskValueKey("Pay rent")

        repository.toggleValue(dailyPage.pageId, key)
        var values = parseDailyValues(repository.getByPageId(dailyPage.pageId)!!.valuesJson)
        assertTrue(isCheckedValue(values[key]))

        repository.toggleValue(dailyPage.pageId, key)
        values = parseDailyValues(repository.getByPageId(dailyPage.pageId)!!.valuesJson)
        assertFalse(isCheckedValue(values[key]))
    }

    @Test
    fun `toggleValue is a no-op for non-journal pages`() = runBlocking {
        // must not throw nor create rows
        repository.toggleValue("not-a-daily-page-id", taskValueKey("Anything"))
        assertNull(repository.getByPageId("not-a-daily-page-id"))
    }

    @Test
    fun `getOrCreateDailyPage does not reset existing values`() = runBlocking {
        val date = LocalDate.parse("2026-06-10")
        val dailyPage = repository.getOrCreateDailyPage(date)
        repository.toggleValue(dailyPage.pageId, taskValueKey("Pay rent"))

        val again = repository.getOrCreateDailyPage(date)
        assertTrue(
            isCheckedValue(parseDailyValues(again.valuesJson)[taskValueKey("Pay rent")])
        )
    }
}
