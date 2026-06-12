package com.ethran.notable.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ethran.notable.data.db.AppDatabase
import com.ethran.notable.data.db.MIGRATION_35_36
import com.ethran.notable.data.db.MIGRATION_36_37
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @Test
    fun simpleTest() {
        assertTrue(true)
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf(), // Add AutoMigrationSpecs here if any
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate30To31_autoMigration() {
        val dbName = "migration-test"

        // 1. Create DB with version 30 schema
        val db = helper.createDatabase(dbName, 30)

// Insert required parent data first
        db.execSQL(
            """
    INSERT INTO Notebook (
        id,
        title,
        openPageId,
        pageIds,
        parentFolderId,
        defaultNativeTemplate,
        createdAt,
        updatedAt
    ) VALUES (
        'notebook1',
        'Test Notebook',
        NULL,
        '[]',
        NULL,
        'blank',
        1620000000000,
        1620000000000
    )
    """.trimIndent()
        )


        db.execSQL(
            """
    INSERT INTO Folder (id, title, createdAt, updatedAt)
    VALUES ('TEST_FOLDER_ID', 'Test Folder', 1620000000, 1620000000)
    """.trimIndent()
        )

        // Insert with column name from version 30: 'nativeTemplate'
        db.execSQL(
            """
    INSERT INTO Page (
        id,
        notebookId,
        nativeTemplate,
        parentFolderId,
        scroll,
        createdAt,
        updatedAt
    ) VALUES (
        'page1',
        'notebook1',
        'grid',
        'TEST_FOLDER_ID',
        0.0,
        1620000000,
        1620000000
    )
    """.trimIndent()
        )


        db.close()

        // 2. Reopen DB with version 31 to trigger migration
        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .build().openHelper.writableDatabase

        // 3. Verify renamed column exists with expected data
        val cursor = migratedDb.query("SELECT background FROM Page WHERE id = 'page1'")
        cursor.use {
            assertTrue(it.moveToFirst())
            val background = it.getString(0)
            assertEquals("grid", background)
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate34ToCurrent_createsDailyPageTable_withDefaultValues() {
        val dbName = "migration-test-34-current"

        // 1. Create DB with version 34 schema and seed a standalone page
        val db = helper.createDatabase(dbName, 34)
        db.execSQL(
            """
            INSERT INTO Folder (id, title, createdAt, updatedAt)
            VALUES ('journal-folder', 'Journal', 1620000000, 1620000000)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO Page (
                id, scroll, notebookId, background, backgroundType,
                parentFolderId, createdAt, updatedAt
            ) VALUES (
                'daily-page-1', 0, NULL, '2026-06-10', 'daily',
                'journal-folder', 1620000000, 1620000000
            )
            """.trimIndent()
        )
        db.close()

        // 2. Reopen at the current version: AutoMigration(34, 35), then the
        // manual 35->36 (DailyPage table) and 36->37 (valuesJson column)
        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_35_36, MIGRATION_36_37)
            .build().openHelper.writableDatabase

        // 3. Existing data is preserved
        migratedDb.query(
            "SELECT background, backgroundType FROM Page WHERE id = 'daily-page-1'"
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals("2026-06-10", it.getString(0))
            assertEquals("daily", it.getString(1))
        }

        // 4. The new DailyPage table exists, defaults valuesJson to '{}',
        // and enforces its FK (CASCADE)
        migratedDb.execSQL(
            "INSERT INTO DailyPage (date, pageId, exportedAt) VALUES ('2026-06-10', 'daily-page-1', NULL)"
        )
        migratedDb.query(
            "SELECT pageId, valuesJson FROM DailyPage WHERE date = '2026-06-10'"
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals("daily-page-1", it.getString(0))
            assertEquals("{}", it.getString(1))
        }
        migratedDb.execSQL("DELETE FROM Page WHERE id = 'daily-page-1'")
        migratedDb.query("SELECT COUNT(*) FROM DailyPage").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate36To37_addsValuesJsonToExistingRows() {
        val dbName = "migration-test-36-37"

        // 1. Create DB at version 36 with a daily page row (pre-valuesJson)
        val db = helper.createDatabase(dbName, 36)
        db.execSQL(
            """
            INSERT INTO Page (
                id, scroll, notebookId, background, backgroundType,
                parentFolderId, createdAt, updatedAt
            ) VALUES (
                'daily-page-1', 0, NULL, '2026-06-10', 'daily',
                NULL, 1620000000, 1620000000
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO DailyPage (date, pageId, exportedAt) VALUES ('2026-06-10', 'daily-page-1', NULL)"
        )
        db.close()

        // 2. Run the manual migration alone and validate the schema
        helper.runMigrationsAndValidate(dbName, 37, true, MIGRATION_36_37).use { migratedDb ->
            // 3. Existing rows pick up the '{}' default
            migratedDb.query("SELECT valuesJson FROM DailyPage WHERE date = '2026-06-10'").use {
                assertTrue(it.moveToFirst())
                assertEquals("{}", it.getString(0))
            }
        }
    }
}
