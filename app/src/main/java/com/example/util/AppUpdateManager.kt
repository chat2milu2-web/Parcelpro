package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

data class UpdateMetadata(
    val versionCode: Int,
    val versionName: String,
    val packageName: String,
    val apkDownloadUrl: String,
    val sha256Checksum: String,
    val releaseNotesUrl: String
)

sealed class AppUpdateUiState {
    data object Idle : AppUpdateUiState()
    data object Checking : AppUpdateUiState()
    data class UpToDate(
        val currentVersionName: String,
        val currentVersionCode: Int
    ) : AppUpdateUiState()
    data class UpdateAvailable(
        val metadata: UpdateMetadata
    ) : AppUpdateUiState()
    data class Downloading(
        val metadata: UpdateMetadata,
        val progressPercent: Int
    ) : AppUpdateUiState()
    data class Verifying(
        val metadata: UpdateMetadata
    ) : AppUpdateUiState()
    data class PermissionRequired(
        val metadata: UpdateMetadata,
        val apkFile: File
    ) : AppUpdateUiState()
    data class ReadyToInstall(
        val metadata: UpdateMetadata,
        val apkFile: File
    ) : AppUpdateUiState()
    data class Error(
        val message: String,
        val metadata: UpdateMetadata? = null
    ) : AppUpdateUiState()
}

object AppUpdateManager {

    private const val GITHUB_REPO = "chat2milu2-web/Parcelpro"
    private const val LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val DIRECT_UPDATE_JSON_URL =
        "https://github.com/$GITHUB_REPO/releases/latest/download/update.json"

    private val ALLOWED_APK_DOWNLOAD_HOSTS = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    )

    private val SHA256_HEX_REGEX = Regex("^[0-9a-fA-F]{64}$")

    val installedVersionName: String
        get() = BuildConfig.VERSION_NAME

    val installedVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    val installedPackageName: String
        get() = BuildConfig.APPLICATION_ID

    fun isUpdateAvailable(
        remoteVersionCode: Int,
        currentVersionCode: Int = installedVersionCode
    ): Boolean {
        return remoteVersionCode > currentVersionCode
    }

    fun validateSha256Format(checksum: String): String {
        val trimmed = checksum.trim()
        if (trimmed.isEmpty()) {
            throw SecurityException("Missing or empty sha256Checksum in update metadata.")
        }
        if (!SHA256_HEX_REGEX.matches(trimmed)) {
            throw SecurityException("Invalid sha256Checksum format: must be exactly 64 hexadecimal characters.")
        }
        return trimmed
    }

    fun validateApkDownloadUrl(urlString: String): URI {
        val trimmed = urlString.trim()
        if (trimmed.isEmpty()) {
            throw SecurityException("APK download URL is empty.")
        }

        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            throw SecurityException("Malformed APK download URL: ${e.message}")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "https") {
            throw SecurityException("Insecure APK download URL scheme '$scheme': only HTTPS is allowed.")
        }

        if (!uri.userInfo.isNullOrEmpty()) {
            throw SecurityException("APK download URL must not contain embedded user credentials.")
        }

        if (uri.port != -1 && uri.port != 443) {
            throw SecurityException("APK download URL uses disallowed port ${uri.port}.")
        }

        val host = uri.host?.lowercase()
            ?: throw SecurityException("APK download URL is missing a valid hostname.")

        if (host !in ALLOWED_APK_DOWNLOAD_HOSTS) {
            throw SecurityException("Untrusted APK download host '$host'. Allowed hosts: $ALLOWED_APK_DOWNLOAD_HOSTS")
        }

        return uri
    }

    fun resolveAndValidateRedirectUrl(currentUrlString: String, locationHeader: String?): String {
        if (locationHeader.isNullOrBlank()) {
            throw SecurityException("Redirect response is missing Location header.")
        }
        val baseUri = URI(currentUrlString)
        val resolvedUri = baseUri.resolve(locationHeader.trim())
        val validatedUri = validateApkDownloadUrl(resolvedUri.toString())
        return validatedUri.toString()
    }

    suspend fun fetchLatestUpdateMetadata(): Result<UpdateMetadata> = withContext(Dispatchers.IO) {
        try {
            val jsonString = fetchUpdateJsonString()
                ?: return@withContext Result.failure(
                    IllegalStateException("No published release found on GitHub yet.")
                )

            val metadata = parseUpdateMetadata(jsonString)
            Result.success(metadata)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseUpdateMetadata(
        jsonString: String,
        expectedPackageName: String = installedPackageName
    ): UpdateMetadata {
        val obj = JSONObject(jsonString)
        if (!obj.has("versionCode")) {
            throw IllegalArgumentException("Missing versionCode in update.json.")
        }
        val versionCode = obj.getInt("versionCode")
        if (versionCode <= 0) {
            throw IllegalArgumentException("Invalid versionCode ($versionCode) in update.json.")
        }

        val versionName = obj.optString("versionName", "").trim()
        if (versionName.isEmpty()) {
            throw IllegalArgumentException("Missing or empty versionName in update.json.")
        }

        val packageName = obj.optString("packageName", "").trim()
        if (packageName.isEmpty()) {
            throw SecurityException("Missing packageName in update.json.")
        }
        if (packageName != expectedPackageName) {
            throw SecurityException("Update package ID ($packageName) does not match installed app ($expectedPackageName).")
        }

        val apkDownloadUrl = obj.optString("apkDownloadUrl", "").trim()
        validateApkDownloadUrl(apkDownloadUrl)

        val rawChecksum = obj.optString("sha256Checksum", "")
        val validatedChecksum = validateSha256Format(rawChecksum)

        val releaseNotesUrl = obj.optString(
            "releaseNotesUrl",
            "https://github.com/$GITHUB_REPO/releases"
        ).trim()

        return UpdateMetadata(
            versionCode = versionCode,
            versionName = versionName,
            packageName = packageName,
            apkDownloadUrl = apkDownloadUrl,
            sha256Checksum = validatedChecksum,
            releaseNotesUrl = releaseNotesUrl
        )
    }

    private fun fetchUpdateJsonString(): String? {
        runCatching {
            downloadText(DIRECT_UPDATE_JSON_URL)
        }.getOrNull()?.let { if (it.trim().startsWith("{")) return it }

        val releaseApiText = runCatching {
            downloadText(LATEST_RELEASE_API_URL)
        }.getOrNull() ?: return null

        val releaseObj = JSONObject(releaseApiText)
        val assets = releaseObj.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == "update.json") {
                    val downloadUrl = asset.optString("browser_download_url")
                    if (downloadUrl.isNotBlank()) {
                        return downloadText(downloadUrl)
                    }
                }
            }
        }
        return null
    }

    private fun downloadText(urlString: String): String {
        var currentUrl = urlString
        var redirects = 0
        while (redirects < 5) {
            val uri = URI(currentUrl)
            if (uri.scheme?.lowercase() != "https") {
                throw SecurityException("Refusing non-HTTPS update metadata URL: $currentUrl")
            }
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json, application/vnd.github+json")
                setRequestProperty("User-Agent", "ParcelPro-Android-Updater/${BuildConfig.VERSION_NAME}")
            }
            try {
                val status = connection.responseCode
                if (status in 301..308) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IllegalStateException("Redirect missing Location header")
                    val resolved = uri.resolve(location.trim())
                    if (resolved.scheme?.lowercase() != "https") {
                        throw SecurityException("Refusing HTTPS-to-HTTP redirect for metadata URL.")
                    }
                    currentUrl = resolved.toString()
                    redirects++
                    continue
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("HTTP $status while checking for updates")
                }
                return connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
        throw IllegalStateException("Too many redirects while connecting to update server.")
    }

    suspend fun downloadAndVerifyApk(
        context: Context,
        metadata: UpdateMetadata,
        onProgress: (Int) -> Unit,
        onVerifying: () -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "app_updates").apply {
            if (!exists()) mkdirs()
        }
        val safeVersionName = metadata.versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val targetFile = File(updatesDir, "Parcel-Pro-$safeVersionName.apk")

        try {
            coroutineContext.ensureActive()
            val expectedChecksum = validateSha256Format(metadata.sha256Checksum)
            var currentUrl = validateApkDownloadUrl(metadata.apkDownloadUrl).toString()

            if (targetFile.exists()) {
                targetFile.delete()
            }

            var redirects = 0
            var downloaded = false

            while (redirects < 5 && !downloaded) {
                coroutineContext.ensureActive()
                validateApkDownloadUrl(currentUrl)

                val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "ParcelPro-Android-Updater/${BuildConfig.VERSION_NAME}")
                }
                try {
                    val status = connection.responseCode
                    if (status in 301..308) {
                        val location = connection.getHeaderField("Location")
                        currentUrl = resolveAndValidateRedirectUrl(currentUrl, location)
                        redirects++
                        continue
                    }
                    if (status != HttpURLConnection.HTTP_OK) {
                        throw IllegalStateException("Failed to download APK (HTTP $status)")
                    }

                    val totalBytes = connection.contentLengthLong
                    connection.inputStream.use { input ->
                        copyStreamWithCancellation(
                            input = input,
                            targetFile = targetFile,
                            totalBytes = totalBytes,
                            onProgress = onProgress
                        )
                    }
                    downloaded = true
                } finally {
                    connection.disconnect()
                }
            }

            if (!downloaded) {
                throw SecurityException("Too many redirects while downloading APK.")
            }

            coroutineContext.ensureActive()

            if (!targetFile.exists() || targetFile.length() == 0L) {
                targetFile.delete()
                return@withContext Result.failure(
                    IllegalStateException("Downloaded APK file is empty or corrupted.")
                )
            }

            onVerifying()
            coroutineContext.ensureActive()

            val actualHash = calculateSha256(targetFile)
            if (!actualHash.equals(expectedChecksum, ignoreCase = true)) {
                targetFile.delete()
                return@withContext Result.failure(
                    SecurityException(
                        "SHA-256 verification failed! Expected ${expectedChecksum.take(12)}..., got ${actualHash.take(12)}..."
                    )
                )
            }

            Result.success(targetFile)
        } catch (e: CancellationException) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            throw e
        } catch (e: Exception) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            Result.failure(e)
        }
    }

    suspend fun copyStreamWithCancellation(
        input: InputStream,
        targetFile: File,
        totalBytes: Long,
        onProgress: (Int) -> Unit
    ) {
        var bytesCopied = 0L
        var lastReportedProgress = 0
        FileOutputStream(targetFile).use { output ->
            val buffer = ByteArray(8 * 1024)
            var bytes = input.read(buffer)
            while (bytes >= 0) {
                coroutineContext.ensureActive()
                output.write(buffer, 0, bytes)
                bytesCopied += bytes
                if (totalBytes > 0) {
                    val progress = ((bytesCopied * 100L) / totalBytes).toInt().coerceIn(0, 100)
                    if (progress != lastReportedProgress) {
                        lastReportedProgress = progress
                        onProgress(progress)
                    }
                }
                bytes = input.read(buffer)
            }
            output.flush()
        }
    }

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createManageUnknownAppSourcesIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openManageUnknownAppSourcesSettings(context: Context): Result<Unit> {
        return try {
            context.startActivity(createManageUnknownAppSourcesIntent(context))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!canRequestPackageInstalls(context)) {
                return Result.failure(
                    SecurityException("Permission to install unknown apps is required. Please grant permission in Android Settings.")
                )
            }
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(
                    IllegalStateException("Verified APK file is no longer available. Please download the update again.")
                )
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
