package dev.maxmini.gpsplayback.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.maxmini.gpsplayback.core.samples.SampleFeed
import dev.maxmini.gpsplayback.core.samples.SampleIndex
import dev.maxmini.gpsplayback.samples.SampleFeeds
import dev.maxmini.gpsplayback.ui.theme.AppType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * Lists the example feeds published in the repo's `samples/index.json`.
 * Each one can be downloaded into app storage, then opened like a picked zip.
 */
@Composable
fun ExampleFeedsDialog(onOpen: (File, SampleFeed) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var feeds by remember { mutableStateOf<List<SampleFeed>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableStateOf(0) }
    var downloaded by remember { mutableStateOf(SampleFeeds.downloadedIds(context)) }
    // Running downloads: id → progress (null = size unknown).
    val progress = remember { mutableStateMapOf<String, Float?>() }
    val jobs = remember { HashMap<String, Job>() }

    LaunchedEffect(attempt) {
        error = null
        try {
            feeds = SampleFeeds.fetchIndex()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Couldn't load the example list: ${e.message ?: e}"
        }
    }

    fun download(feed: SampleFeed) {
        progress[feed.id] = 0f
        error = null
        jobs[feed.id] = scope.launch {
            try {
                SampleFeeds.download(context, feed) { p -> progress[feed.id] = p }
                downloaded = downloaded + feed.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "${feed.name}: download failed (${e.message ?: e})"
            } finally {
                progress.remove(feed.id)
                jobs.remove(feed.id)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Example feeds") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Muted("Hosted on GitHub, not bundled with the app. Downloads stay on this device until you delete them.")
                error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                val list = feeds
                when {
                    list == null && error == null -> Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                    list == null -> TextButton(onClick = { attempt++ }) { Text("Retry") }
                    list.isEmpty() -> Muted("No examples are published right now.")
                    else -> LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(list, key = { it.id }) { feed ->
                            HorizontalDivider()
                            ExampleRow(
                                feed = feed,
                                isDownloaded = feed.id in downloaded,
                                progress = progress[feed.id],
                                isDownloading = feed.id in progress,
                                onDownload = { download(feed) },
                                onCancel = { jobs[feed.id]?.cancel() },
                                onOpen = { onOpen(SampleFeeds.localFile(context, feed), feed) },
                                onDelete = {
                                    SampleFeeds.delete(context, feed)
                                    downloaded = downloaded - feed.id
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ExampleRow(
    feed: SampleFeed,
    isDownloaded: Boolean,
    progress: Float?,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(feed.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        feed.description?.let { Muted(it) }
        val meta = listOfNotNull(feed.region, feed.sizeBytes?.let(SampleIndex::formatSize), feed.license).joinToString(" · ")
        if (meta.isNotEmpty()) Text(meta, style = AppType.data, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when {
            isDownloading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f))
                else LinearProgressIndicator(Modifier.weight(1f))
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
            isDownloaded -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen) { Text("Open") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            else -> FilledTonalButton(onClick = onDownload, contentPadding = ButtonDefaults.TextButtonContentPadding) {
                Text("Download")
            }
        }
    }
}
