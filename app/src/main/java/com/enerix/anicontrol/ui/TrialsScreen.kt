package com.enerix.anicontrol.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.enerix.anicontrol.R
import com.enerix.anicontrol.android.Exporter
import com.enerix.anicontrol.core.Controller
import com.enerix.anicontrol.core.Trial

@Composable
fun TrialsScreen(c: Controller, v: Int) {
    val ctx = LocalContext.current
    val trials = c.trials.toList()

    if (trials.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(R.drawable.ic_nav_trials, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface, 64)
                Text("No tasks yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Every trip you send from the Live tab is recorded here, with its result.",
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val ok = trials.filter { it.success }
    val rate = "${ok.size * 100 / trials.size}%"
    val withShort = ok.filter { it.shortestCost > 0 }
    val saved = if (withShort.isEmpty()) "–"
    else "${(withShort.sumOf { (it.shortestCost - it.plannedCost) / it.shortestCost } / withShort.size * 100).toInt()}%"
    val adapts = trials.sumOf { it.reroutes + it.reverses }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(rate, "tasks reached", Modifier.weight(1f))
            Stat(saved, "cheaper than shortest", Modifier.weight(1f))
            Stat("$adapts", "times it changed route", Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${trials.size} tasks", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(shape = CardShape, onClick = {
                Toast.makeText(ctx, Exporter.saveToDownloads(ctx, "ani_trials.csv", c.trialsCsv()), Toast.LENGTH_LONG).show()
            }) { Text("Save to phone") }
        }
        trials.forEach { TrialCard(it) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TrialCard(t: Trial) {
    Panel {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(
                if (t.success) R.drawable.ic_check else R.drawable.ic_warning,
                if (t.success) AniColors.Ok else AniColors.Crit, Color.White, 38,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("#${t.id} · ${t.target} · ${if (t.success) "Reached" else "Not reached"}", fontWeight = FontWeight.Bold)
                Small(t.note)
                Small(
                    "${t.segments} aisles · ${"%.1f".format(t.durationS)} s" +
                        (if (t.reroutes > 0) " · re-routed ${t.reroutes}×" else "") +
                        (if (t.reverses > 0) " · backed out ${t.reverses}×" else "") +
                        (if (t.blockedS > 0.5) " · waited ${"%.1f".format(t.blockedS)} s" else ""),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("%.1f".format(t.plannedCost), fontWeight = FontWeight.Bold)
                Small("vs %.1f".format(t.shortestCost))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Panel(modifier) {
        Column {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Small(label)
        }
    }
}
