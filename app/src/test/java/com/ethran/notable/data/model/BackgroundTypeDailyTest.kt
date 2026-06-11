package com.ethran.notable.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The "daily" key is persisted in Page.backgroundType — its stability is part
 * of the database contract. Robolectric because fromKey's fallback path logs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BackgroundTypeDailyTest {

    @Test
    fun `daily key is stable`() {
        assertEquals("daily", BackgroundType.Daily.key)
        assertEquals("", BackgroundType.Daily.folderName)
    }

    @Test
    fun `fromKey resolves daily`() {
        assertSame(BackgroundType.Daily, BackgroundType.fromKey("daily"))
    }

    @Test
    fun `fromKey keeps resolving the existing types`() {
        assertSame(BackgroundType.Native, BackgroundType.fromKey("native"))
        assertSame(BackgroundType.Image, BackgroundType.fromKey("image"))
        assertEquals(BackgroundType.Pdf(3), BackgroundType.fromKey("pdf3"))
    }

    @Test
    fun `unknown keys still fall back to Native`() {
        assertSame(BackgroundType.Native, BackgroundType.fromKey("definitely-not-a-key"))
    }
}
