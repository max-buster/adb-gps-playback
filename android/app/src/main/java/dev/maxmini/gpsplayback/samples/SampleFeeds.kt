package dev.maxmini.gpsplayback.samples

import android.content.Context
import dev.maxmini.gpsplayback.BuildConfig
import dev.maxmini.gpsplayback.core.samples.SampleFeed
import dev.maxmini.gpsplayback.core.samples.SampleIndex
import dev.maxmini.gpsplayback.ui.map.USER_AGENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Example GTFS feeds hosted outside the APK (the repo's `samples/` folder,
 * served from GitHub). The list comes from [BuildConfig.SAMPLES_INDEX_URL];
 * downloaded zips live in `filesDir/samples/<id>.zip` and stay until deleted.
 */
object SampleFeeds {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
            }
            .build()
    }

    private fun dir(context: Context) = File(context.filesDir, "samples")

    fun localFile(context: Context, feed: SampleFeed) = File(dir(context), feed.fileName)

    /** Ids of the feeds already on the device. */
    fun downloadedIds(context: Context): Set<String> =
        dir(context).listFiles { f -> f.isFile && f.name.endsWith(".zip") }
            ?.map { it.name.removeSuffix(".zip") }?.toSet() ?: emptySet()

    suspend fun fetchIndex(): List<SampleFeed> = withContext(Dispatchers.IO) {
        val url = BuildConfig.SAMPLES_INDEX_URL
        val body = client.newCall(Request.Builder().url(url).build()).execute().use { res ->
            if (!res.isSuccessful) throw IOException("HTTP ${res.code} for the example list")
            res.body?.string() ?: throw IOException("Empty example list")
        }
        SampleIndex.parse(body, url)
    }

    /**
     * Downloads [feed] into app storage, checking its SHA-256 when the index
     * gives one. Writes to a temp file first, so a cancelled or failed
     * download never leaves a half-written zip behind.
     * [onProgress] gets a 0..1 fraction, or null when the size is unknown.
     */
    suspend fun download(context: Context, feed: SampleFeed, onProgress: (Float?) -> Unit): File =
        withContext(Dispatchers.IO) {
            val target = localFile(context, feed)
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, target.name + ".part")
            try {
                client.newCall(Request.Builder().url(feed.url).build()).execute().use { res ->
                    if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
                    val body = res.body ?: throw IOException("Empty response")
                    val total = body.contentLength().takeIf { it > 0 } ?: feed.sizeBytes
                    val digest = MessageDigest.getInstance("SHA-256")
                    var read = 0L
                    body.byteStream().use { input ->
                        tmp.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                coroutineContext.ensureActive()
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                digest.update(buf, 0, n)
                                read += n
                                onProgress(total?.let { (read.toFloat() / it).coerceIn(0f, 1f) })
                            }
                        }
                    }
                    feed.sha256?.let { want ->
                        val got = SampleIndex.sha256Hex(digest)
                        if (got != want) throw IOException("Checksum mismatch; the file on the server may have changed.")
                    }
                }
                if (!tmp.renameTo(target)) throw IOException("Couldn't save ${target.name}")
                target
            } finally {
                tmp.delete()
            }
        }

    fun delete(context: Context, feed: SampleFeed) {
        localFile(context, feed).delete()
    }
}
