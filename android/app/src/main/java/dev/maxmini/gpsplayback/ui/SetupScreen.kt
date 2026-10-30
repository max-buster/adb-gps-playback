package dev.maxmini.gpsplayback.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.maxmini.gpsplayback.AppStore
import dev.maxmini.gpsplayback.core.model.LatLon
import dev.maxmini.gpsplayback.core.playback.Fix
import dev.maxmini.gpsplayback.mock.MockLocationSink
import dev.maxmini.gpsplayback.ui.theme.AppTokens

private fun granted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun locationOk(context: Context) = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)

private fun notificationsOk(context: Context) =
    Build.VERSION.SDK_INT < 33 || granted(context, Manifest.permission.POST_NOTIFICATIONS)

/** How many Setup steps still need doing; drives the badge on the Setup tab. */
internal fun setupStepsRemaining(context: Context): Int =
    listOf(locationOk(context), notificationsOk(context), MockLocationSink.isSelectedMockApp(context)).count { !it }

/** Open Developer options, or About phone if they aren't enabled yet. */
private fun openDeveloperOptions(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
    }
}

@Composable
fun SetupScreen(resumeCount: Int) {
    val context = LocalContext.current
    val settings by AppStore.settings.collectAsState()
    // Re-evaluated on every recomposition triggered by resumeCount / permission results.
    var permissionResults by remember { mutableIntStateOf(0) }
    val recheck = resumeCount + permissionResults

    val locationOk = remember(recheck) { locationOk(context) }
    val notifOk = remember(recheck) { notificationsOk(context) }
    val mockOk = remember(recheck) { MockLocationSink.isSelectedMockApp(context) }
    val hasNotifStep = Build.VERSION.SDK_INT >= 33
    val steps = listOfNotNull(locationOk, notifOk.takeIf { hasNotifStep }, mockOk)
    // The first unfinished step is highlighted and gets the only filled button.
    val current = steps.indexOfFirst { !it }

    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissionResults++ }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Muted(
            if (current < 0) "All set. Load a feed to start." else "${steps.count { it }} of ${steps.size} steps done",
        )
        StepProgress(steps)

        var n = 0
        Step(
            number = ++n,
            title = "Location permission",
            done = locationOk,
            current = current == n - 1,
            detail = "Android requires it for a location foreground service, even though this app only writes locations.",
            action = "Grant permission",
        ) {
            requestPermissions.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
        if (hasNotifStep) {
            Step(
                number = ++n,
                title = "Notifications",
                done = notifOk,
                current = current == n - 1,
                detail = "Shows playback progress and Play/Pause/Stop while you're in another app.",
                action = "Allow notifications",
            ) {
                requestPermissions.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
        Step(
            number = ++n,
            title = "Select GPS Playback as mock location app",
            done = mockOk,
            current = current == n - 1,
            detail = "1. Turn on Developer options: Settings → About phone → tap Build number 7 times.\n" +
                "2. Developer options → Select mock location app → GPS Playback, then come back.",
            action = "Open Developer options",
        ) { openDeveloperOptions(context) }

        Section("Options") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mock Play Services fused location", style = MaterialTheme.typography.titleSmall)
                    Muted("Google Maps and most apps read location through Play Services.")
                }
                Switch(
                    checked = settings.mockFused,
                    onCheckedChange = { v -> AppStore.setSettings { it.copy(mockFused = v) } },
                )
            }
        }
        TestFix(enabled = mockOk)
    }
}

/** One segment per step, green when done. */
@Composable
private fun StepProgress(steps: List<Boolean>) {
    val ok = AppTokens.colors.ok
    val todo = MaterialTheme.colorScheme.outlineVariant
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEach { done ->
            Box(Modifier.weight(1f).height(4.dp).background(if (done) ok else todo, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun Step(
    number: Int,
    title: String,
    done: Boolean,
    current: Boolean,
    detail: String,
    action: String,
    onAction: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val ok = AppTokens.colors.ok
    FlatCard(
        border = if (current) BorderStroke(2.dp, scheme.primary) else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val badge = Modifier.size(28.dp)
            Box(
                if (done) badge.background(ok, CircleShape) else badge.border(2.dp, if (current) scheme.primary else scheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (done) "✓" else "$number",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (done) Color.White else if (current) scheme.primary else scheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (done) {
                    Text("Done", style = MaterialTheme.typography.labelMedium, color = ok)
                } else {
                    Muted(detail)
                    if (current) Button(onClick = onAction) { Text(action) }
                    else OutlinedButton(onClick = onAction) { Text(action) }
                }
            }
        }
    }
}

/**
 * Phase-1 smoke test: push one fixed position so you can confirm, in any maps
 * app, that mocking works on this device before playing a whole route.
 */
@Composable
private fun TestFix(enabled: Boolean) {
    val context = LocalContext.current
    val settings by AppStore.settings.collectAsState()
    var sink by remember { mutableStateOf<MockLocationSink?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onDispose { sink?.stop() }
    }
    Section("Test fix") {
        Muted("Pins the device to Apple Park, Cupertino. Open a maps app to check, then Clear.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                enabled = enabled && sink == null,
                onClick = {
                    val s = MockLocationSink(context, settings.mockFused) { message = it }
                    message = try {
                        s.start()
                        s.push(Fix(LatLon(37.3349, -122.0090), bearingDegrees = null, speedMps = 0.0, accuracyMeters = 3.0))
                        sink = s
                        "Sent. Keep this screen open while you check."
                    } catch (e: SecurityException) {
                        "Not allowed: ${e.message}"
                    }
                },
            ) { Text("Send test fix") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(enabled = sink != null, onClick = {
                sink?.stop()
                sink = null
                message = "Cleared."
            }) { Text("Clear") }
        }
        if (!enabled) Muted("Finish the mock location step first.")
        message?.let { Muted(it) }
    }
}
