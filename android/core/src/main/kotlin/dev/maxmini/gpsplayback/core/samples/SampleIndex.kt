package dev.maxmini.gpsplayback.core.samples

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.security.MessageDigest

/**
 * One downloadable example feed, as listed in the repo's `samples/index.json`.
 * [url] is always absolute once it has come through [SampleIndex.parse].
 */
@Serializable
data class SampleFeed(
    val id: String,
    val name: String,
    val url: String,
    val description: String? = null,
    val region: String? = null,
    val license: String? = null,
    val sizeBytes: Long? = null,
    /** Lowercase hex SHA-256 of the zip, when the file at [url] is fixed. */
    val sha256: String? = null,
) {
    /** Local file name for the downloaded zip. [id] is validated by [SampleIndex.parse]. */
    val fileName: String get() = "$id.zip"
}

@Serializable
private data class IndexFile(val version: Int = 1, val feeds: List<SampleFeed> = emptyList())

object SampleIndex {
    /** Highest index format this app understands. */
    const val VERSION = 1

    private val ID = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses `index.json` fetched from [indexUrl]. Relative feed urls are
     * resolved against [indexUrl], so a fork's index points at the fork's
     * files. Entries with an unusable id, a non-http(s) url or a malformed
     * checksum are dropped rather than failing the whole list, and so are
     * duplicate ids (the first one wins).
     */
    fun parse(text: String, indexUrl: String): List<SampleFeed> {
        val file = json.decodeFromString(IndexFile.serializer(), text)
        require(file.version <= VERSION) {
            "Example list format ${file.version} is newer than this app understands; update the app."
        }
        val base = URI(indexUrl)
        val seen = HashSet<String>()
        return file.feeds.mapNotNull { feed ->
            if (!ID.matches(feed.id) || !seen.add(feed.id)) return@mapNotNull null
            val resolved = runCatching { base.resolve(feed.url.trim()) }.getOrNull() ?: return@mapNotNull null
            if (resolved.scheme?.lowercase() !in setOf("http", "https") || resolved.host.isNullOrEmpty()) return@mapNotNull null
            val sha = feed.sha256?.lowercase()
            if (sha != null && !SHA256.matches(sha)) return@mapNotNull null
            feed.copy(url = resolved.toString(), sha256 = sha, sizeBytes = feed.sizeBytes?.takeIf { it >= 0 })
        }
    }

    fun sha256Hex(digest: MessageDigest): String = digest.digest().joinToString("") { "%02x".format(it) }

    /** "6.5 KB", "12.3 MB": decimal units, one decimal place. */
    fun formatSize(bytes: Long): String = when {
        bytes < 1_000 -> "$bytes B"
        bytes < 1_000_000 -> "%.1f KB".format(java.util.Locale.ROOT, bytes / 1_000.0)
        bytes < 1_000_000_000 -> "%.1f MB".format(java.util.Locale.ROOT, bytes / 1_000_000.0)
        else -> "%.1f GB".format(java.util.Locale.ROOT, bytes / 1_000_000_000.0)
    }
}
