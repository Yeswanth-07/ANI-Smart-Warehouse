@file:OptIn(ExperimentalLayoutApi::class)

package com.enerix.anicontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.enerix.anicontrol.R
import com.enerix.anicontrol.core.ConditionMode
import com.enerix.anicontrol.core.Controller
import com.enerix.anicontrol.core.HEADINGS
import com.enerix.anicontrol.core.Node

@Composable
fun LiveScreen(c: Controller, v: Int, onConnect: () -> Unit) {
    var editNode by remember { mutableStateOf<Node?>(null) }
    val running = c.run != null

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StatusCard(c, v, onConnect)

        // ---- map
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, CardShape).padding(8.dp),
            ) { WarehouseMap(c, v) { if (!running) editNode = it } }
            MapLegend()
        }

        // ---- step 1: destination
        SectionTitle(if (running) "Going to" else "1. Where should the robot go?")
        DestinationGrid(c, v)

        // ---- step 2: go / pause / stop
        if (!running) {
            SectionTitle("2. Start")
            RouteSummary(c, v)
            val label = startLabel(c)
            Button(
                onClick = { c.startRun() },
                enabled = c.isConnected && c.preview != null,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = CardShape,
                colors = ButtonDefaults.buttonColors(containerColor = AniColors.Safety, contentColor = AniColors.Ink),
            ) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            if (!c.isConnected) Small("Connect a robot first (button at the top of the screen).")
        } else {
            RunControls(c, v)
        }

        // ---- optional sections
        ExpandablePanel(
            title = "Mark busy or blocked aisles",
            subtitle = "Tell the robot about traffic and obstacles",
            icon = R.drawable.ic_warning,
            v = v,
        ) { ConditionsBody(c, it) }

        ExpandablePanel(
            title = "More controls",
            subtitle = "Drive by hand, reset position, battery",
            icon = R.drawable.ic_robot,
            v = v,
        ) { MoreControlsBody(c, it) }

        Spacer(Modifier.height(8.dp))
    }

    editNode?.let { n ->
        AlertDialog(
            onDismissRequest = { editNode = null },
            title = { Text("Junction $n") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Small("Choose what this junction on the floor is.")
                    c.layout.targets.values.forEach { t ->
                        TextButton(onClick = { c.setTargetAt(t.id, n); editNode = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("${t.label} is here", modifier = Modifier.fillMaxWidth())
                        }
                    }
                    (0..3).forEach { h ->
                        TextButton(onClick = { c.setStart(n, h); editNode = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("Robot starts here, facing ${listOf("up", "right", "down", "left")[h]} (${HEADINGS[h]})",
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editNode = null }) { Text("Close") } },
        )
    }
}

private fun startLabel(c: Controller): String {
    val p = c.preview
    val name = c.layout.targets[c.selectedTarget]?.label ?: ""
    return when {
        !c.isConnected -> "Start"
        p == null -> "Already at $name"
        else -> "Go to $name"
    }
}

// ------------------------------------------------------------------ status card

private data class StatusLook(val title: String, val sub: String, val icon: Int, val bg: Color, val fg: Color)

private fun statusLook(c: Controller): StatusLook {
    val r = c.run
    val target = r?.let { c.layout.targets[it.targetId]?.label }
    return when {
        !c.isConnected && c.status.name == "CONNECTING" ->
            StatusLook("Connecting…", "Opening the Bluetooth link to the robot.", R.drawable.ic_bluetooth, AniColors.Warn, Color.White)
        !c.isConnected ->
            StatusLook("Robot not connected", "Connect your HC-05 robot, or try the demo robot.", R.drawable.ic_bluetooth, Color(0xFF5C6166), Color.White)
        c.linkStale ->
            StatusLook("No signal from robot", "No message for over 2 seconds. Is it switched on and in range?", R.drawable.ic_warning, AniColors.Warn, Color.White)
        c.last == null ->
            StatusLook("Waiting for robot…", "Connected. Waiting for the first message.", R.drawable.ic_bluetooth, AniColors.Warn, Color.White)
        c.robotState == "BLOCKED" ->
            StatusLook("Something is in the way", "The robot is waiting. If it stays blocked it will back up and go around.", R.drawable.ic_warning, AniColors.Crit, Color.White)
        c.robotState == "PAUSED" ->
            StatusLook("Paused", "Press Resume to continue.", R.drawable.ic_pause, AniColors.Warn, Color.White)
        c.robotState == "LOST" ->
            StatusLook("Robot lost the line", "Put it back on a junction, then use More controls → Robot is at start.", R.drawable.ic_warning, AniColors.Crit, Color.White)
        r != null && r.reversing ->
            StatusLook("Backing out of a blocked aisle", "Then taking another way to $target.", R.drawable.ic_nav_route, AniColors.Safety, AniColors.Ink)
        r != null ->
            StatusLook("Driving to $target", "Aisle ${minOf(r.idx + 1, r.nodes.size - 1)} of ${r.nodes.size - 1}" +
                (if (r.reroutes + r.reverses > 0) " · changed route ${r.reroutes + r.reverses}×" else ""), R.drawable.ic_nav_route, AniColors.Safety, AniColors.Ink)
        c.robotState == "REACHED" ->
            StatusLook("Arrived", "The robot reached its destination. Pick the next one.", R.drawable.ic_check, AniColors.Ok, Color.White)
        else ->
            StatusLook("Ready", "Robot is waiting at junction ${c.robotPos}. Pick a destination below.", R.drawable.ic_robot, AniColors.Ok, Color.White)
    }
}

@Composable
private fun StatusCard(c: Controller, v: Int, onConnect: () -> Unit) {
    val look = statusLook(c)
    Panel {
        @Suppress("UNUSED_VARIABLE") val redraw = v
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(look.icon, look.bg, look.fg)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(look.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Small(look.sub)
                }
            }
            if (!c.isConnected) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onConnect, modifier = Modifier.weight(1f), shape = CardShape) { Text("Connect robot") }
                    OutlinedButton(onClick = { c.connectDemo() }, modifier = Modifier.weight(1f), shape = CardShape) { Text("Try demo robot") }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val bat = c.battery
                    InfoPill("Battery", "${bat.toInt()}%${if (!c.hasVoltage) " (set)" else ""}",
                        if (bat < 25) AniColors.Crit else MaterialTheme.colorScheme.onSurface)
                    val d = c.last?.distanceCm
                    InfoPill("Ahead", when { d == null -> "–"; d >= 999 -> "clear"; else -> "$d cm" },
                        if (d != null && d < 15) AniColors.Crit else MaterialTheme.colorScheme.onSurface)
                    InfoPill("At", "${c.robotPos} ${HEADINGS[c.robotHeading]}")
                }
            }
            if (c.notice.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 6.dp).size(8.dp).background(if (c.noticeIsWarning) AniColors.Crit else AniColors.Safety, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(c.notice, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ map legend

@Composable
private fun MapLegend() {
    @Composable
    fun item(color: Color, label: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(16.dp).height(5.dp).background(color, CircleShape))
            Spacer(Modifier.width(5.dp))
            Small(label)
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item(AniColors.Safety, "Smart route")
        item(MaterialTheme.colorScheme.outline, "Shortest (dashed)")
        item(AniColors.Warn, "Busy aisle")
        item(AniColors.Crit, "Blocked aisle")
    }
}

// ------------------------------------------------------------------ destinations

@Composable
private fun DestinationGrid(c: Controller, v: Int) {
    val running = c.run != null
    val active = c.run?.targetId ?: c.selectedTarget
    val targets = c.layout.targets.values.toList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        targets.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    ChoiceTile(
                        letter = t.id,
                        label = t.label,
                        sub = if (t.node == c.robotPos && !running) "robot is here" else "junction ${t.node}",
                        selected = t.id == active,
                        enabled = !running,
                        modifier = Modifier.weight(1f),
                    ) { c.selectTarget(t.id) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RouteSummary(c: Controller, v: Int) {
    val p = c.preview ?: return
    val saved = p.shortCost - p.mlCost
    val text = if (saved > 0.01)
        "Smart route: ${p.mlPath.size - 1} aisles, ${(saved / p.shortCost * 100).toInt()}% cheaper than the shortest path. See the Route tab for details."
    else "Smart route: ${p.mlPath.size - 1} aisles. The shortest path is also the cheapest right now."
    Small(text)
}

// ------------------------------------------------------------------ while driving

@Composable
private fun RunControls(c: Controller, v: Int) {
    val paused = c.robotState == "PAUSED"
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { c.pauseResume() },
            modifier = Modifier.weight(1f).height(58.dp),
            shape = CardShape,
            colors = ButtonDefaults.buttonColors(containerColor = AniColors.Safety, contentColor = AniColors.Ink),
        ) {
            Icon(painterResource(if (paused) R.drawable.ic_play else R.drawable.ic_pause), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (paused) "Resume" else "Pause", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { c.emergencyStop() },
            modifier = Modifier.weight(1f).height(58.dp),
            shape = CardShape,
            colors = ButtonDefaults.buttonColors(containerColor = AniColors.Crit, contentColor = Color.White),
        ) {
            Icon(painterResource(R.drawable.ic_stop), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Stop", fontWeight = FontWeight.Bold)
        }
    }
    if (c.isDemo) {
        OutlinedButton(onClick = { c.dropDemoObstacle() }, modifier = Modifier.fillMaxWidth(), shape = CardShape) {
            Text("Demo: put an obstacle in front of the robot")
        }
    }
    TextButton(onClick = { c.abandonTask() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel this task") }
}

// ------------------------------------------------------------------ conditions

@Composable
private fun ConditionsBody(c: Controller, v: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Small("1. Choose what to mark.  2. Tap an aisle (a line between two dots) on the map. Each tap makes it stronger, from 0 to 4; the fifth tap clears it.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = c.conditionMode == ConditionMode.TRAFFIC,
                onClick = { c.conditionMode = ConditionMode.TRAFFIC; c.tickUi() },
                label = { Text("Busy (traffic)") },
            )
            FilterChip(
                selected = c.conditionMode == ConditionMode.OBSTACLE,
                onClick = { c.conditionMode = ConditionMode.OBSTACLE; c.tickUi() },
                label = { Text("Blocked (obstacle)") },
            )
        }
        Small("Obstacles the robot's sensor finds are marked automatically.")
        OutlinedButton(onClick = { c.clearConditions() }, shape = CardShape) { Text("Clear all marks") }
    }
}

// ------------------------------------------------------------------ more controls

@Composable
private fun MoreControlsBody(c: Controller, v: Int) {
    val running = c.run != null
    val startText = "Robot is at the start square ${c.layout.start}"
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = { c.resetPosition() },
            enabled = c.isConnected && !running,
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
        ) { Text(startText) }
        Small("Use this after placing the robot back on the start square, facing ${listOf("up", "right", "down", "left")[c.layout.startHeading]}.")

        if (!c.hasVoltage) {
            Text("Battery level", fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = c.manualBattery.toFloat(),
                    onValueChange = { c.setManualBatteryLevel(it.toDouble()) },
                    valueRange = 5f..100f,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text("${c.manualBattery.toInt()}%")
            }
            Small("The robot is not sending its battery voltage, so set it here.")
        }

        Text("Drive by hand", fontWeight = FontWeight.SemiBold)
        Small("Hold a button to move, let go to stop.")
        val enabled = c.isConnected && !running
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HoldPad("▲", "F", enabled) { c.manual(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(60.dp)) {
                HoldPad("⟲", "L", enabled) { c.manual(it) }
                HoldPad("⟳", "R", enabled) { c.manual(it) }
            }
            HoldPad("▼", "B", enabled) { c.manual(it) }
        }
    }
}

/** Sends M:<cmd> while pressed and M:S when released. */
@Composable
private fun HoldPad(glyph: String, cmd: String, enabled: Boolean, send: (String) -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (enabled) AniColors.Safety else MaterialTheme.colorScheme.surface,
        contentColor = AniColors.Ink,
        modifier = Modifier
            .padding(4.dp)
            .size(60.dp)
            .pointerInput(enabled, cmd) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    send(cmd)
                    tryAwaitRelease()
                    send("S")
                })
            },
    ) {
        Box(contentAlignment = Alignment.Center) { Text(glyph, style = MaterialTheme.typography.titleLarge) }
    }
}
