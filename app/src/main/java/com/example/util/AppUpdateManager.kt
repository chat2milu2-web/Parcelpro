package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

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

    val installedVersionName: String
        get() = BuildConfig.VERSION_NAME

    val installedVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    val installedPackageName: String
        get() = BuildConfig.APPLICATION_ID

    suspend fun fetchLatestUpdateMetadata(): Result<UpdateMetadata> = withContext(Dispatchers.IO) {
        try {
            val jsonString = fetchUpdateJsonString()
                ?: return@withContext Result.failure(
                    IllegalStateException("No published release found on GitHub yet.")
                )

            val metadata = parseUpdateMetadata(jsonString)
            if (metadata.packageName.isNotBlank() && metadata.packageName != installedPackageName) {
                return@withContext Result.failure(
                    SecurityException("Update package ID (${metadata.packageName}) does not match installed app ($installedPackageName).")
                )
            }
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseUpdateMetadata(jsonString: String): UpdateMetadata {
        val obj = JSONObject(jsonString)
        val versionCode = obj.getInt("versionCode")
        val versionName = obj.getString("versionName")
        val packageName = obj.optString("packageName", installedPackageName)
        val apkDownloadUrl = obj.getString("apkDownloadUrl")
        val sha256Checksum = obj.optString("sha256Checksum", "")
        val releaseNotesUrl = obj.optString(
            "releaseNotesUrl",
            "https://github.com/$GITHUB_REPO/releases"
        )
        return UpdateMetadata(
            versionCode = versionCode,
            versionName = versionName,
            packageName = packageName,
            apkDownloadUrl = apkDownloadUrl,
            sha256Checksum = sha256Checksum,
            releaseNotesUrl = releaseNotesUrl
        )
    }

    private fun fetchUpdateJsonString(): String? {
        // 1. Try direct latest update.json asset URL
        runCatching {
            downloadText(DIRECT_UPDATE_JSON_URL)
        }.getOrNull()?.let { if (it.trim().startsWith("{")) return it }

        // 2. Fallback: Query GitHub Releases API for update.json asset or tag info
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
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json, application/vnd.github+json")
                setRequestProperty("User-Agent", "ParcelPro-Android-Updater/${BuildConfig.VERSION_NAME}")
            }
            try {
                val status = connection.responseCode
                if (status in 301..308) {
                    val location = connection.getHeaderField("Location") ?: break
                    currentUrl = location
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
        try {
            val updatesDir = File(context.cacheDir, "app_updates").apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(updatesDir, "Parcel-Pro-${metadata.versionName}.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            var currentUrl = metadata.apkDownloadUrl
            var redirects = 0
            var downloaded = false

            while (redirects < 5 && !downloaded) {
                val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "ParcelPro-Android-Updater/${BuildConfig.VERSION_NAME}")
                }
                try {
                    val status = connection.responseCode
                    if (status in 301..308) {
                        val location = connection.getHeaderField("Location")
                            ?: throw IllegalStateException("Redirect missing Location header")
                        currentUrl = location
                        redirects++
                        continue
                    }
                    if (status != HttpURLConnection.HTTP_OK) {
                        throw IllegalStateException("Failed to download APK (HTTP $status)")
                    }

                    val totalBytes = connection.contentLengthLong
                    var bytesCopied = 0L
                    var lastReportedProgress = 0

                    connection.inputStream.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var bytes = input.read(buffer)
                            while (bytes >= 0) {
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
                    downloaded = true
                } finally {
                    connection.disconnect()
                }
            }

            if (!targetFile.exists() || targetFile.length() == 0L) {
                return@withContext Result.failure(
                    IllegalStateException("Downloaded APK file is empty or corrupted.")
                )
            }

            onVerifying()

            // Verify SHA-256 checksum if provided in update.json
            if (metadata.sha256Checksum.isNotBlank()) {
                val actualHash = calculateSha256(targetFile)
                if (!actualHash.equals(metadata.sha256Checksum.trim(), ignoreCase = true)) {
                    targetFile.delete()
                    return@withContext Result.failure(
                        SecurityException("SHA-256 verification failed! Expected ${metadata.sha256Checksum.take(12)}..., got ${actualHash.take(12)}...")
                    )
                }
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
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

    fun launchApkInstaller(context: Context, apkFile: File): Result<Unit> {
        return try {
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
