@file:OptIn(ExperimentalLayoutApi::class)

package com.enerix.anicontrol.ui

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enerix.anicontrol.R
import com.enerix.anicontrol.android.Exporter
import com.enerix.anicontrol.core.Controller
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DataScreen(c: Controller, v: Int) {
    val ctx = LocalContext.current
    val ageS = if (c.lastRxMs == 0L) null else (SystemClock.elapsedRealtime() - c.lastRxMs) / 1000.0
    val learned = c.edges.entries.filter { it.value.passes > 0 }
        .sortedByDescending { it.value.blocks * 10 + it.value.correction }
    val signal = when {
        !c.isConnected -> "Not connected"
        c.linkStale -> "No signal"
        else -> "Good"
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---- 1. live connection
        Panel {
            @Suppress("UNUSED_VARIABLE") val redraw = v
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderRow(R.drawable.ic_bluetooth, "Live connection", "Messages coming from the robot right now")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("Signal", signal, warn = signal != "Good")
                    Metric("Robot", c.link?.name ?: "–")
                    Metric("Messages per second", "%.1f".format(c.rxPerSecond))
                    Metric("Last message", ageS?.let { "%.1f s ago".format(it) } ?: "–", warn = c.linkStale)
                    Metric("Battery", if (c.hasVoltage) "%.2f V (%d%%)".format(c.last!!.volts, c.battery.toInt()) else "not sent")
                }
                if (c.batteryHistory.size > 2) BatterySpark(c.batteryHistory.toList(), v)
            }
        }

        // ---- 2. learning
        Panel {
            @Suppress("UNUSED_VARIABLE") val redraw = v
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderRow(R.drawable.ic_robot, "What the robot has learned", "Every aisle it drives is timed and checked for blockages")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("Aisles driven", "${c.dataset.size}")
                    Metric("Normal time per aisle", c.baselineTravelS?.let { "%.2f s".format(it) } ?: "after 3 aisles")
                    Metric("Aisles known", "${learned.size} of ${c.edges.size}")
                }
                if (learned.isEmpty()) {
                    Small("Nothing learned yet. Send a route from the Live tab and come back here.")
                } else {
                    Text("Aisles it knows best", fontWeight = FontWeight.SemiBold)
                    learned.take(6).forEach { (k, e) ->
                        val slow = e.correction > 1.15
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(k.replace("|", " – ").replace(",", ", "), Modifier.width(120.dp), style = Mono)
                            Text(
                                when {
                                    e.blocks > 0 -> "blocked ${e.blocks} of ${e.passes} times"
                                    slow -> "slower than normal (×${"%.2f".format(e.correction)})"
                                    else -> "normal · ${e.passes} passes"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (e.blocks > 0) AniColors.Crit else if (slow) AniColors.Warn else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // ---- 3. dataset
        Panel {
            @Suppress("UNUSED_VARIABLE") val redraw = v
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderRow(R.drawable.ic_nav_data, "Data for retraining", "One row per aisle: conditions + real travel time")
                Text("${c.dataset.size} rows saved", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val has = c.dataset.isNotEmpty()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = has, shape = CardShape, onClick = {
                        Toast.makeText(ctx, Exporter.saveToDownloads(ctx, "ani_dataset_${stamp()}.csv", c.datasetCsv()), Toast.LENGTH_LONG).show()
                    }) { Text("Save to phone") }
                    OutlinedButton(enabled = has, shape = CardShape, onClick = { Exporter.share(ctx, "ani_dataset.csv", c.datasetCsv()) }) {
                        Text("Share")
                    }
                }
                Small("The file goes to the phone's Downloads folder. Use it with tools/export_model.py to retrain the model.")
            }
        }

        // ---- raw messages
        ExpandablePanel(
            title = "Raw robot messages",
            subtitle = "For checking the HC-05 link (newest at the top)",
            icon = R.drawable.ic_nav_data,
            v = v,
        ) { Console(c, it) }

        Spacer(Modifier.height(8.dp))
    }
}

private fun stamp() = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

@Composable
private fun HeaderRow(icon: Int, title: String, sub: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, AniColors.Safety, AniColors.Ink, 38)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Small(sub)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, warn: Boolean = false) {
    Column {
        Small(label)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            color = if (warn) AniColors.Crit else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Console(c: Controller, v: Int) {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
    val wallOffset = System.currentTimeMillis() - SystemClock.elapsedRealtime()
    Column(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(AniColors.Term, RoundedCornerShape(10.dp))
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
    ) {
        if (c.console.isEmpty()) Text("No messages yet", color = Color(0xFF9BA1A7), style = Mono)
        c.console.takeLast(150).asReversed().forEach { l ->
            val color = when {
                l.outgoing -> AniColors.Safety
                l.text.startsWith("#") -> Color(0xFF8C939A)
                else -> Color(0xFF9FD8B4)
            }
            Text(
                "${fmt.format(Date(l.atMs + wallOffset))} ${if (l.outgoing) "→" else "←"} ${l.text}",
                color = color,
                style = Mono.copy(fontSize = 11.sp),
            )
        }
    }
    Small("Yellow → = sent by the app.  Green ← = sent by the robot.")
}

@Composable
private fun BatterySpark(values: List<Double>, v: Int) {
    Column {
        Small("Battery over time")
        Canvas(Modifier.fillMaxWidth().height(40.dp).padding(top = 4.dp)) {
            @Suppress("UNUSED_VARIABLE") val redrawKey = v
            val lo = values.min()
            val span = maxOf(1.0, values.max() - lo)
            fun y(b: Double) = (size.height - 2 - ((b - lo) / span * (size.height - 4))).toFloat()
            val path = Path()
            values.forEachIndexed { i, b ->
                val x = i / (values.size - 1f) * size.width
                if (i == 0) path.moveTo(x, y(b)) else path.lineTo(x, y(b))
            }
            drawPath(path, AniColors.Safety, style = Stroke(width = 2.dp.toPx()))
            drawCircle(AniColors.Safety, 3.dp.toPx(), Offset(size.width, y(values.last())))
        }
    }
}
