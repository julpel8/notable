package com.ethran.notable.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import java.util.Date

/**
 * One row per journal day: maps a local calendar date to the standalone Page
 * holding that day's handwriting. The date is the page's identity — it never
 * changes once created, regardless of timezone changes later.
 */
@Entity(
    foreignKeys = [ForeignKey(
        entity = Page::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("pageId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class DailyPage(
    // ISO local date, yyyy-MM-dd
    @PrimaryKey val date: String,
    @ColumnInfo(index = true) val pageId: String,
    // When this day was last exported to Markdown (future export pipeline)
    val exportedAt: Date? = null,
)

@Dao
interface DailyPageDao {
    @Query("SELECT * FROM dailypage WHERE date = :date")
    suspend fun getByDate(date: String): DailyPage?

    @Query("SELECT * FROM dailypage WHERE pageId = :pageId")
    suspend fun getByPageId(pageId: String): DailyPage?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(dailyPage: DailyPage): Long

    @Update
    suspend fun update(dailyPage: DailyPage)
}
