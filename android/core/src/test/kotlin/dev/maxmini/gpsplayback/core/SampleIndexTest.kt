package dev.maxmini.gpsplayback.core

import dev.maxmini.gpsplayback.core.gtfs.GtfsParser
import dev.maxmini.gpsplayback.core.samples.SampleIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class SampleIndexTest {
    private val indexUrl = "https://raw.githubusercontent.com/o/r/main/samples/index.json"

    @Test
    fun resolvesRelativeUrlsAgainstTheIndex() {
        val feeds = SampleIndex.parse(
            """{"version":1,"feeds":[
                {"id":"a","name":"A","url":"feeds/a.zip","sizeBytes":10,"sha256":"${"AB".repeat(32)}"},
                {"id":"b","name":"B","url":"https://github.com/o/r/releases/download/samples/b.zip","extra":true}
            ]}""",
            indexUrl,
        )
        assertEquals(listOf("a", "b"), feeds.map { it.id })
        assertEquals("https://raw.githubusercontent.com/o/r/main/samples/feeds/a.zip", feeds[0].url)
        assertEquals("ab".repeat(32), feeds[0].sha256)
        assertEquals("https://github.com/o/r/releases/download/samples/b.zip", feeds[1].url)
        assertNull(feeds[1].sha256)
        assertEquals("a.zip", feeds[0].fileName)
    }

    @Test
    fun dropsUnsafeOrBrokenEntries() {
        val feeds = SampleIndex.parse(
            """{"feeds":[
                {"id":"../evil","name":"x","url":"x.zip"},
                {"id":"file","name":"x","url":"file:///etc/passwd"},
                {"id":"badsha","name":"x","url":"x.zip","sha256":"nothex"},
                {"id":"ok","name":"x","url":"x.zip"},
                {"id":"ok","name":"dupe","url":"y.zip"}
            ]}""",
            indexUrl,
        )
        assertEquals(listOf("ok"), feeds.map { it.id })
        assertEquals("x", feeds.single().name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNewerFormat() {
        SampleIndex.parse("""{"version":99,"feeds":[]}""", indexUrl)
    }

    @Test
    fun formatsSizes() {
        assertEquals("999 B", SampleIndex.formatSize(999))
        assertEquals("6.6 KB", SampleIndex.formatSize(6_640))
        assertEquals("12.3 MB", SampleIndex.formatSize(12_345_678))
    }

    /** The checked-in index and zips must agree and parse, since the app downloads them as-is. */
    @Test
    fun repoSamplesMatchTheirIndex() {
        val samples = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "samples") }
            .firstOrNull { File(it, "index.json").isFile } ?: return // not in a checkout
        val base = "https://samples.test/samples/"
        val feeds = SampleIndex.parse(File(samples, "index.json").readText(), base + "index.json")
        assertTrue(feeds.isNotEmpty())
        for (feed in feeds.filter { it.url.startsWith(base) }) {
            val zip = File(samples, feed.url.removePrefix(base))
            val bytes = zip.readBytes()
            assertEquals(feed.id, feed.sizeBytes, bytes.size.toLong())
            val sha = SampleIndex.sha256Hex(MessageDigest.getInstance("SHA-256").apply { update(bytes) })
            assertEquals(feed.id, feed.sha256, sha)
            val data = GtfsParser.parse({ zip.inputStream() }, feedName = feed.name)
            assertFalse(feed.id, data.routes.isEmpty())
            assertTrue(feed.id, data.trips.all { t -> t.shapeId?.let { data.shapes[it] } != null })
        }
    }
}
