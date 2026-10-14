package com.enerix.anicontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enerix.anicontrol.R
import com.enerix.anicontrol.core.Controller
import com.enerix.anicontrol.core.Node
import com.enerix.anicontrol.core.toActions

@Composable
fun RouteScreen(c: Controller, v: Int) {
    val r = c.run
    val p = c.preview
    val path: List<Node>? = r?.remaining ?: p?.mlPath
    if (path == null || path.size < 2) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(R.drawable.ic_nav_route, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface, 64)
                Text("No route yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("On the Live tab, pick a destination that is not where the robot is now. The route appears here.",
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    val targetId = r?.targetId ?: c.selectedTarget
    val targetName = c.layout.targets[targetId]?.label ?: "the destination"
    val shortest = r?.shortestCost ?: p!!.shortCost
    val planned = r?.plannedCost ?: p!!.mlCost
    val savedPct = if (shortest > 0) ((shortest - planned) / shortest * 100).toInt() else 0
    val heading = c.robotHeading
    val actions = if (r != null && r.reversing) null else toActions(path, heading)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---- headline
        Panel {
            @Suppress("UNUSED_VARIABLE") val redraw = v
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (savedPct > 0) "Smart route saves $savedPct%" else "Shortest path is the best right now",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                )
                Small(
                    if (savedPct > 0) "It avoids busy or blocked aisles, so it costs less even if it is a bit longer."
                    else "No busy or blocked aisles on the way, so the shortest path is also the cheapest.",
                )
                val max = maxOf(shortest, planned)
                CompareBar("Shortest path", shortest, max, MaterialTheme.colorScheme.outline, bold = false)
                CompareBar("Smart route (ML + UCS)", planned, max, AniColors.Safety, bold = true)
                Small(if (r != null) "Costs when the robot set off. Lower is better." else "Predicted cost. Lower is better.")
            }
        }

        // ---- directions
        SectionTitle(if (r != null) "What the robot does next" else "What the robot will do")
        if (actions == null) {
            Small("It is backing out of a blocked aisle. New directions appear when it reaches the junction.")
        } else {
            val steps = directions(path, actions, targetName)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { i, s -> StepRow(i + 1, s, last = i == steps.lastIndex) }
            }
        }

        // ---- details
        ExpandablePanel(
            title = "Cost of each aisle",
            subtitle = "How the smart route was chosen",
            icon = R.drawable.ic_nav_data,
            v = v,
        ) { CostDetails(c, it, path, actions) }

        Spacer(Modifier.height(8.dp))
    }
}

/** Turns the robot's letter commands into plain sentences, grouping straight runs. */
private fun directions(path: List<Node>, actions: String, target: String): List<String> {
    val out = ArrayList<String>()
    var i = 0
    while (i < actions.length) {
        var j = i
        while (j + 1 < actions.length && actions[j + 1] == 'F') j++
        val count = j - i + 1
        val to = path[j + 1]
        val aisles = if (count == 1) "1 aisle" else "$count aisles"
        out += when (actions[i]) {
            'L' -> "Turn left, then drive $aisles to junction $to"
            'R' -> "Turn right, then drive $aisles to junction $to"
            'U' -> "Turn around, then drive $aisles to junction $to"
            else -> "Drive straight $aisles to junction $to"
        }
        i = j + 1
    }
    out += "Stop at $target"
    return out
}

@Composable
private fun StepRow(n: Int, text: String, last: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).background(if (last) AniColors.Ok else AniColors.Safety, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", fontWeight = FontWeight.Bold, color = if (last) Color.White else AniColors.Ink)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CostDetails(c: Controller, v: Int, path: List<Node>, actions: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            TableRow(listOf("Aisle", "Blocked", "Busy", "ML cost", "× learned", "Cost"), header = true)
            for (i in 0 until path.size - 1) {
                val a = path[i]
                val b = path[i + 1]
                val e = c.edge(a, b)
                val o = c.obstacleOf(a, b)
                val ml = c.predictedCost(a, b)
                TableRow(
                    listOf("$a→$b", "$o", "${e.traffic}", "%.2f".format(ml), "%.2f".format(e.correction), "%.2f".format(ml * e.correction)),
                    colors = mapOf(1 to levelColor(o), 2 to levelColor(e.traffic)),
                )
            }
        }
        Text("Total ${"%.1f".format(c.pathCost(path))} over ${path.size - 1} aisles " +
            "(${"%.2f".format((path.size - 1) * c.layout.segmentM)} m) · battery ${c.battery.toInt()}%")
        Small(
            (if (c.model.isTrained) "ML cost comes from your ${c.model.name}. "
            else "ML cost uses the built-in stand-in model until you add your trained Random Forest. ") +
                "\"× learned\" goes above 1 for aisles the robot found slower than normal.",
        )
        if (actions != null) {
            Text("Command sent to the robot", fontWeight = FontWeight.SemiBold)
            SelectionContainer {
                Text(
                    "R:$actions",
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(10.dp),
                    style = Mono.copy(fontSize = 15.sp),
                )
            }
            Small("One letter per junction: F straight, L left, R right, U turn around.")
        }
    }
}

private fun levelColor(l: Int): Color? = when {
    l >= 3 -> AniColors.Crit
    l >= 1 -> AniColors.Warn
    else -> null
}

private val colWidths = listOf(112, 60, 48, 66, 72, 56)

@Composable
private fun TableRow(cells: List<String>, header: Boolean = false, colors: Map<Int, Color?> = emptyMap()) {
    Row(Modifier.padding(vertical = 5.dp)) {
        cells.forEachIndexed { i, s ->
            Text(
                s,
                Modifier.width(colWidths[i].dp),
                style = if (header) MaterialTheme.typography.labelSmall else Mono,
                color = colors[i] ?: if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                fontWeight = if (header) FontWeight.SemiBold else null,
            )
        }
    }
}
