package com.ethran.notable.data

import com.ethran.notable.data.db.DailyPage
import com.ethran.notable.data.db.DailyPageDao
import com.ethran.notable.data.db.Folder
import com.ethran.notable.data.db.FolderDao
import com.ethran.notable.data.db.Page
import com.ethran.notable.data.db.PageDao
import com.ethran.notable.data.model.BackgroundType
import io.shipbook.shipbooksdk.ShipBook
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val log = ShipBook.getLogger("JournalRepository")

const val JOURNAL_FOLDER_TITLE = "Journal"

/**
 * Daily journal pages: one standalone Page per local date, kept in a root
 * "Journal" folder. The page's background field carries the ISO date and its
 * backgroundType is [BackgroundType.Daily], which the renderer turns into the
 * calendar template.
 */
@Singleton
class JournalRepository @Inject constructor(
    private val dailyPageDao: DailyPageDao,
    private val pageDao: PageDao,
    private val folderDao: FolderDao,
) {
    suspend fun getByDate(date: LocalDate): DailyPage? = dailyPageDao.getByDate(date.toString())

    suspend fun getByPageId(pageId: String): DailyPage? = dailyPageDao.getByPageId(pageId)

    suspend fun setExportedAt(dailyPage: DailyPage, exportedAt: java.util.Date) {
        dailyPageDao.update(dailyPage.copy(exportedAt = exportedAt))
    }

    /**
     * Idempotent: returns the existing page for [date] or creates it (and the
     * "Journal" folder on first use). Safe against concurrent creation — the
     * date primary key plus OnConflictStrategy.IGNORE decides the winner, and
     * the loser's orphan page is removed.
     */
    suspend fun getOrCreateDailyPage(date: LocalDate): DailyPage {
        val iso = date.toString()
        dailyPageDao.getByDate(iso)?.let { return it }

        val journalFolder = ensureJournalFolder()
        val page = Page(
            notebookId = null,
            parentFolderId = journalFolder.id,
            background = iso,
            backgroundType = BackgroundType.Daily.key,
        )
        pageDao.create(page)

        val inserted = dailyPageDao.insert(DailyPage(date = iso, pageId = page.id))
        if (inserted == -1L) {
            // Lost a creation race: drop our orphan page, return the winner's row.
            log.w("Daily page for $iso created concurrently, discarding duplicate page")
            pageDao.delete(page.id)
            return dailyPageDao.getByDate(iso)
                ?: throw IllegalStateException("DailyPage for $iso vanished after conflict")
        }
        log.i("Created daily page for $iso (page ${page.id})")
        return DailyPage(date = iso, pageId = page.id)
    }

    private suspend fun ensureJournalFolder(): Folder {
        folderDao.getByTitleInRoot(JOURNAL_FOLDER_TITLE)?.let { return it }
        val folder = Folder(title = JOURNAL_FOLDER_TITLE)
        folderDao.create(folder)
        // If another writer created it in between, prefer the persisted one.
        return folderDao.getByTitleInRoot(JOURNAL_FOLDER_TITLE) ?: folder
    }
}
