package com.example.tourify_v2

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Contoh tes yang berjalan di perangkat Android.
 *
 * Lihat [panduan tes](http://d.android.com/tools/testing) untuk detailnya.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Ambil context aplikasi yang sedang diuji.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.tourify_v2", appContext.packageName)
    }
}
