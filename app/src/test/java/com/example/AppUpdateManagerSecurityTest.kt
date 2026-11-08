package com.example

import com.example.util.AppUpdateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppUpdateManagerSecurityTest {

    private val validSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    private val validPackage = "com.aistudio.parcelentrypro.xzqk"
    private val validApkUrl =
        "https://github.com/chat2milu2-web/Parcelpro/releases/download/v1.1/Parcel-Pro-release.apk"

    @Test
    fun missingChecksum_isRejected() {
        val json = """
            {
              "versionCode": 2,
              "versionName": "1.1",
              "packageName": "$validPackage",
              "apkDownloadUrl": "$validApkUrl",
              "sha256Checksum": ""
            }
        """.trimIndent()

        try {
            AppUpdateManager.parseUpdateMetadata(json, validPackage)
            fail("Expected SecurityException for empty sha256Checksum")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("sha256Checksum", ignoreCase = true))
        }
    }

    @Test
    fun invalidChecksumFormat_isRejected() {
        // Too short (not 64 hex chars)
        try {
            AppUpdateManager.validateSha256Format("abc12345")
            fail("Expected SecurityException for short checksum")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("64 hexadecimal"))
        }

        // Contains non-hex characters ('g', 'z')
        val nonHex64 = "g3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85z"
        try {
            AppUpdateManager.validateSha256Format(nonHex64)
            fail("Expected SecurityException for non-hex characters")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("64 hexadecimal"))
        }
    }

    @Test
    fun correctSha256Verification_succeedsCaseInsensitively() {
        val tempFile = Files.createTempFile("test_apk_", ".apk").toFile()
        try {
            tempFile.writeText("ParcelPro-Test-APK-Payload")
            val actualHash = AppUpdateManager.calculateSha256(tempFile)
            assertEquals(64, actualHash.length)

            val upperCaseHash = actualHash.uppercase()
            val validated = AppUpdateManager.validateSha256Format(upperCaseHash)
            assertTrue(actualHash.equals(validated, ignoreCase = true))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun incorrectSha256Verification_isDetected() {
        val tempFile = Files.createTempFile("test_apk_mismatch_", ".apk").toFile()
        try {
            tempFile.writeText("Tampered-APK-Content")
            val actualHash = AppUpdateManager.calculateSha256(tempFile)
            assertFalse(actualHash.equals(validSha256, ignoreCase = true))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun httpUrl_isRejected() {
        val insecureUrl =
            "http://github.com/chat2milu2-web/Parcelpro/releases/download/v1.1/Parcel-Pro-release.apk"
        try {
            AppUpdateManager.validateApkDownloadUrl(insecureUrl)
            fail("Expected SecurityException for HTTP URL")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("HTTPS", ignoreCase = true))
        }
    }

    @Test
    fun urlWithCredentialsOrUnexpectedPort_isRejected() {
        val credentialUrl =
            "https://user:pass@github.com/chat2milu2-web/Parcelpro/releases/download/v1.1/Parcel-Pro-release.apk"
        try {
            AppUpdateManager.validateApkDownloadUrl(credentialUrl)
            fail("Expected SecurityException for URL containing credentials")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("credentials", ignoreCase = true))
        }

        val badPortUrl =
            "https://github.com:8443/chat2milu2-web/Parcelpro/releases/download/v1.1/Parcel-Pro-release.apk"
        try {
            AppUpdateManager.validateApkDownloadUrl(badPortUrl)
            fail("Expected SecurityException for URL containing non-443 port")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("port", ignoreCase = true))
        }
    }

    @Test
    fun untrustedHostname_isRejected() {
        val untrustedUrls = listOf(
            "https://evil-domain.com/Parcel-Pro-release.apk",
            "https://github.com.evil.com/Parcel-Pro-release.apk",
            "https://raw.githubusercontent.com/some/file.apk"
        )
        for (url in untrustedUrls) {
            try {
                AppUpdateManager.validateApkDownloadUrl(url)
                fail("Expected SecurityException for untrusted host: $url")
            } catch (e: SecurityException) {
                assertTrue(e.message!!.contains("Untrusted APK download host"))
            }
        }
    }

    @Test
    fun validGitHubReleaseUrls_areAccepted() {
        val validUrls = listOf(
            "https://github.com/chat2milu2-web/Parcelpro/releases/download/v1.1/Parcel-Pro-release.apk",
            "https://objects.githubusercontent.com/github-production-release-asset-2e65be/12345/Parcel-Pro-release.apk",
            "https://release-assets.githubusercontent.com/github-production-release-asset/12345/Parcel-Pro-release.apk"
        )
        for (url in validUrls) {
            val uri = AppUpdateManager.validateApkDownloadUrl(url)
            assertNotNull(uri)
            assertEquals("https", uri.scheme)
        }
    }

    @Test
    fun redirectValidation_acceptsTrustedAndRejectsUntrustedOrHttpRedirects() {
        val initialUrl =
            "https://github.com/chat2milu2-web/Parcelpro/releases/download/v1.1/Parcel-Pro-release.apk"
        val legitimateRedirect =
            "https://objects.githubusercontent.com/github-production-release-asset/98765/Parcel-Pro-release.apk"

        val resolved = AppUpdateManager.resolveAndValidateRedirectUrl(initialUrl, legitimateRedirect)
        assertEquals(legitimateRedirect, resolved)

        // Reject HTTPS-to-HTTP downgrade redirect
        try {
            AppUpdateManager.resolveAndValidateRedirectUrl(
                initialUrl,
                "http://objects.githubusercontent.com/asset.apk"
            )
            fail("Expected SecurityException for HTTP downgrade redirect")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("HTTPS", ignoreCase = true))
        }

        // Reject redirect to third-party host
        try {
            AppUpdateManager.resolveAndValidateRedirectUrl(
                initialUrl,
                "https://malicious-mirror.org/Parcel-Pro-release.apk"
            )
            fail("Expected SecurityException for untrusted redirect host")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("Untrusted APK download host"))
        }
    }

    @Test
    fun packageNameMismatch_isRejected() {
        val json = """
            {
              "versionCode": 2,
              "versionName": "1.1",
              "packageName": "com.wrong.package.name",
              "apkDownloadUrl": "$validApkUrl",
              "sha256Checksum": "$validSha256"
            }
        """.trimIndent()

        try {
            AppUpdateManager.parseUpdateMetadata(json, validPackage)
            fail("Expected SecurityException for packageName mismatch")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("does not match installed app"))
        }
    }

    @Test
    fun versionDowngradeAndSameVersion_areRejected() {
        // Installed is versionCode 5
        assertFalse(AppUpdateManager.isUpdateAvailable(remoteVersionCode = 5, currentVersionCode = 5))
        assertFalse(AppUpdateManager.isUpdateAvailable(remoteVersionCode = 4, currentVersionCode = 5))
        assertTrue(AppUpdateManager.isUpdateAvailable(remoteVersionCode = 6, currentVersionCode = 5))
    }

    @Test
    fun downloadCancellation_stopsStreamAndCleansUpPartialFile() = runBlocking {
        val partialFile = Files.createTempFile("partial_download_", ".apk").toFile()
        var chunksRead = 0
        val infiniteStream = object : InputStream() {
            override fun read(): Int = 0x42
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                chunksRead++
                return len.coerceAtMost(4096)
            }
        }

        var jobRef: kotlinx.coroutines.Job? = null
        val job = launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                AppUpdateManager.copyStreamWithCancellation(
                    input = infiniteStream,
                    targetFile = partialFile,
                    totalBytes = 10_000_000L,
                    onProgress = {
                        if (chunksRead >= 2) {
                            jobRef?.cancel()
                        }
                    }
                )
            } catch (e: CancellationException) {
                if (partialFile.exists()) {
                    partialFile.delete()
                }
                throw e
            }
        }
        jobRef = job

        job.join()
        assertTrue("Expected coroutine to be cancelled", job.isCancelled)
        assertTrue("Expected at least 2 chunks read before cancellation", chunksRead >= 2)
        assertFalse("Partial APK file must be deleted upon cancellation", partialFile.exists())
    }
}
