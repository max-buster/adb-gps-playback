package dev.maxmini.gpsplayback.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.maxmini.gpsplayback.AppStore
import dev.maxmini.gpsplayback.R
import dev.maxmini.gpsplayback.ui.components.LiveStatusPill
import dev.maxmini.gpsplayback.ui.components.liveStateOf
import dev.maxmini.gpsplayback.ui.theme.AppTheme

private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    Setup("Setup", R.drawable.ic_nav_setup),
    Load("Load", R.drawable.ic_nav_load),
    Edit("Edit", R.drawable.ic_nav_edit),
    Play("Play", R.drawable.ic_nav_play),
}

class MainActivity : ComponentActivity() {
    // Bumped on every resume so screens re-check permissions / mock-app selection
    // after the user comes back from system settings.
    private var resumeCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                AppContent(resumeCount)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount++
    }
}

@Composable
private fun AppContent(resumeCount: Int) {
    val context = LocalContext.current
    val start = if (AppStore.routes.value.isEmpty()) Tab.Setup else Tab.Play
    var tab by rememberSaveable { mutableStateOf(start) }
    // Re-checked on resume and on tab change, so the badge clears after fixing a step.
    val setupReady = remember(resumeCount, tab) { setupStepsRemaining(context) == 0 }

    Scaffold(
        topBar = { TopBar(tab.label) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { tab = t },
                        icon = {
                            BadgedBox(badge = { if (t == Tab.Setup && !setupReady) Badge() }) {
                                Icon(painterResource(t.icon), contentDescription = null)
                            }
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.Setup -> SetupScreen(resumeCount)
                Tab.Load -> LoadScreen(onStaged = { tab = Tab.Edit })
                Tab.Edit -> EditScreen(onNeedRoute = { tab = Tab.Load }, onPlay = { tab = Tab.Play })
                Tab.Play -> PlayScreen(onNeedRoute = { tab = Tab.Load })
            }
        }
    }
}

/** Screen title plus the live status pill, which every tab shows. */
@Composable
private fun TopBar(title: String) {
    val status by AppStore.status.collectAsState()
    val player by AppStore.player.collectAsState()
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
        LiveStatusPill(liveStateOf(status, player))
    }
}
