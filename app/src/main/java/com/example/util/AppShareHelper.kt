package com.example.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast

/**
 * Helper for sharing the Parcel Pro application download link via Android's native Sharesheet.
 */
object AppShareHelper {

    const val SHARE_RELEASE_URL = "https://github.com/chat2milu2-web/Parcelpro/releases/latest"

    const val SHARE_SUBJECT = "Parcel Pro – Smart Parcel Entry App"

    const val SHARE_CHOOSER_TITLE = "Share Parcel Pro"

    val SHARE_MESSAGE: String = """
        Parcel Pro – Smart Parcel Entry App

        Download Parcel Pro:
        $SHARE_RELEASE_URL

        Get the latest version directly from GitHub!
    """.trimIndent()

    private const val SHARE_DEBOUNCE_MS = 600L
    @Volatile
    private var lastShareClickElapsedMs: Long = -1L

    /**
     * Builds the underlying ACTION_SEND text/plain intent with the official Parcel Pro message.
     */
    fun createShareSendIntent(): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, SHARE_SUBJECT)
            putExtra(Intent.EXTRA_TEXT, SHARE_MESSAGE)
        }
    }

    /**
     * Wraps the ACTION_SEND intent in Android's native Sharesheet chooser via Intent.createChooser().
     */
    fun createShareChooserIntent(): Intent {
        val sendIntent = createShareSendIntent()
        return Intent.createChooser(sendIntent, SHARE_CHOOSER_TITLE)
    }

    /**
     * Launches Android's native Sharesheet safely using the provided Context.
     * Prevents duplicate rapid launches and handles missing sharing apps gracefully without crashing.
     */
    fun shareApp(context: Context, enforceDebounce: Boolean = true): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (
            enforceDebounce &&
            lastShareClickElapsedMs >= 0L &&
            now >= lastShareClickElapsedMs &&
            now - lastShareClickElapsedMs < SHARE_DEBOUNCE_MS
        ) {
            return false
        }
        lastShareClickElapsedMs = now

        val sendIntent = createShareSendIntent()
        val chooserIntent = Intent.createChooser(sendIntent, SHARE_CHOOSER_TITLE)

        val activity = context.findActivity()
        if (activity == null) {
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            val targetContext = activity ?: context
            targetContext.startActivity(chooserIntent)
            true
        } catch (e: ActivityNotFoundException) {
            showNoShareAppToast(context)
            false
        } catch (e: Exception) {
            showNoShareAppToast(context)
            false
        }
    }

    internal fun resetDebounceForTesting() {
        lastShareClickElapsedMs = -1L
    }

    private fun showNoShareAppToast(context: Context) {
        try {
            Toast.makeText(
                context.applicationContext ?: context,
                "No compatible sharing application found on this device.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            // Ignore Toast errors in headless unit test environments
        }
    }

    private tailrec fun Context.findActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext?.findActivity()
            else -> null
        }
    }
}
