package com.example

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.util.AppShareHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppShareHelperTest {

    @Before
    fun setUp() {
        AppShareHelper.resetDebounceForTesting()
    }

    @Test
    fun shareMessage_containsExactGitHubLatestReleaseUrlAndFormatting() {
        val expectedMessage = """
            Parcel Pro – Smart Parcel Entry App

            Download Parcel Pro:
            https://github.com/chat2milu2-web/Parcelpro/releases/latest

            Get the latest version directly from GitHub!
        """.trimIndent()

        assertEquals(expectedMessage, AppShareHelper.SHARE_MESSAGE)
        assertTrue(
            AppShareHelper.SHARE_MESSAGE.contains("https://github.com/chat2milu2-web/Parcelpro/releases/latest")
        )
    }

    @Test
    fun createShareSendIntent_configuresActionSendAndTextPlainMimeType() {
        val sendIntent = AppShareHelper.createShareSendIntent()

        assertEquals(Intent.ACTION_SEND, sendIntent.action)
        assertEquals("text/plain", sendIntent.type)
        assertEquals("Parcel Pro – Smart Parcel Entry App", sendIntent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals(AppShareHelper.SHARE_MESSAGE, sendIntent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun createShareChooserIntent_wrapsSendIntentInNativeAndroidSharesheet() {
        val chooserIntent = AppShareHelper.createShareChooserIntent()

        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)
        assertEquals(AppShareHelper.SHARE_CHOOSER_TITLE, chooserIntent.getStringExtra(Intent.EXTRA_TITLE))

        @Suppress("DEPRECATION")
        val targetIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent!!.action)
        assertEquals("text/plain", targetIntent.type)
        assertEquals(AppShareHelper.SHARE_MESSAGE, targetIntent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun shareApp_launchesAndroidSharesheetAndPreventsRapidDuplicateLaunches() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        val firstLaunch = AppShareHelper.shareApp(activity, enforceDebounce = true)
        assertTrue("First tap should launch Sharesheet", firstLaunch)

        // Immediate second tap within debounce window should be ignored
        val duplicateLaunch = AppShareHelper.shareApp(activity, enforceDebounce = true)
        assertFalse("Rapid duplicate tap should be debounced", duplicateLaunch)

        val startedIntent = shadowOf(activity).nextStartedActivity
        assertNotNull("Expected Chooser Intent to be started", startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)

        @Suppress("DEPRECATION")
        val innerIntent = startedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(innerIntent)
        assertEquals(Intent.ACTION_SEND, innerIntent!!.action)
        assertEquals("text/plain", innerIntent.type)
        assertTrue(
            innerIntent.getStringExtra(Intent.EXTRA_TEXT)!!
                .contains("https://github.com/chat2milu2-web/Parcelpro/releases/latest")
        )
    }

    @Test
    fun shareApp_handlesMissingCompatibleAppGracefullyWithoutCrashing() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val throwingContext = object : ContextWrapper(baseContext) {
            override fun startActivity(intent: Intent?) {
                throw ActivityNotFoundException("No Activity found to handle Intent")
            }
        }

        val launched = AppShareHelper.shareApp(throwingContext, enforceDebounce = false)
        assertFalse("Should return false cleanly without crashing when no app can handle share intent", launched)
    }
}
