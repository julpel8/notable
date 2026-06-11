package com.ethran.notable.data

import androidx.room.Room
import com.ethran.notable.data.db.AppDatabase
import com.ethran.notable.data.model.BackgroundType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
}
