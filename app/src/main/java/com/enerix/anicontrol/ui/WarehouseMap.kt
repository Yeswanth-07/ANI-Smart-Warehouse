package com.enerix.anicontrol.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.enerix.anicontrol.core.Controller
import com.enerix.anicontrol.core.Node
import com.enerix.anicontrol.core.WarehouseLayout
import kotlin.math.hypot
import kotlin.math.min

/** Screen geometry of the junction grid. */
private class Geo(w: Float, h: Float, val l: WarehouseLayout) {
    val s = min(w / l.cols, h / l.rows)
    val ox = (w - s * (l.cols - 1)) / 2
    val oy = (h - s * (l.rows - 1)) / 2
    fun p(n: Node) = Offset(ox + n.x * s, oy + (l.rows - 1 - n.y) * s)

    fun edgeAt(q: Offset): Pair<Node, Node>? {
        var best: Pair<Node, Node>? = null
        var bd = s * 0.28f
        for (e in l.edges()) {
            val d = distToSeg(q, p(e.first), p(e.second))
            if (d < bd) { bd = d; best = e }
        }
        return best
    }

    fun nodeAt(q: Offset): Node? {
        var best: Node? = null
        var bd = s * 0.4f
        for (x in 0 until l.cols) for (y in 0 until l.rows) {
            val d = (p(Node(x, y)) - q).getDistance()
            if (d < bd) { bd = d; best = Node(x, y) }
        }
        return best
    }

    private fun distToSeg(q: Offset, a: Offset, b: Offset): Float {
        val abx = b.x - a.x; val aby = b.y - a.y
        val len2 = abx * abx + aby * aby
        if (len2 == 0f) return (q - a).getDistance()
        val t = (((q.x - a.x) * abx + (q.y - a.y) * aby) / len2).coerceIn(0f, 1f)
        return hypot(q.x - (a.x + abx * t), q.y - (a.y + aby * t))
    }
}

/**
 * Live warehouse floor. Tap an aisle to change its traffic/obstacle level,
 * long-press a junction to place a rack pickup point or the start square.
 */
@Composable
fun WarehouseMap(c: Controller, v: Int, onLongPressNode: (Node) -> Unit) {
    val l = c.layout
    val cs = MaterialTheme.colorScheme
    val tape = cs.onSurface.copy(alpha = 0.6f)
    val outline = cs.outline
    val onSurface = cs.onSurface
    val surface = cs.surface
    val longPress by rememberUpdatedState(onLongPressNode)

    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(l.cols.toFloat() / l.rows)
            .pointerInput(l.cols, l.rows) {
                detectTapGestures(
                    onTap = { o ->
                        Geo(size.width.toFloat(), size.height.toFloat(), c.layout).edgeAt(o)?.let { c.bumpEdge(it.first, it.second) }
                    },
                    onLongPress = { o ->
                        Geo(size.width.toFloat(), size.height.toFloat(), c.layout).nodeAt(o)?.let { longPress(it) }
                    },
                )
            },
    ) {
        @Suppress("UNUSED_VARIABLE") val redrawKey = v // makes the canvas redraw on every controller change
        val u = density // 1 dp in pixels
        val g = Geo(size.width, size.height, l)

        // aisles
        for ((a, b) in l.edges()) {
            val e = c.edge(a, b)
            val o = c.obstacleOf(a, b)
            val pa = g.p(a); val pb = g.p(b)
            when {
                o > 0 -> drawLine(AniColors.Crit, pa, pb, (3f + o * 1.4f) * u, StrokeCap.Round,
                    PathEffect.dashPathEffect(floatArrayOf(3f * u, 6f * u)))
                e.traffic > 0 -> drawLine(AniColors.Warn, pa, pb, (3f + e.traffic * 1.6f) * u, StrokeCap.Round)
                else -> drawLine(tape, pa, pb, 2.5f * u)
            }
            if (e.passes > 0 && e.correction > 1.25) drawCircle(AniColors.Warn, 3.5f * u, Offset((pa.x + pb.x) / 2, (pa.y + pb.y) / 2))
        }

        // routes
        val r = c.run
        val p = c.preview
        if (r != null) {
            if (r.idx > 0) poly(g, r.traveled, AniColors.Safety.copy(alpha = 0.35f), 4f * u)
            poly(g, r.remaining, AniColors.Ink, 9f * u)
            poly(g, r.remaining, AniColors.Safety, 5f * u)
        } else if (p != null) {
            for (i in 0 until p.shortPath.size - 1) {
                drawLine(outline, g.p(p.shortPath[i]), g.p(p.shortPath[i + 1]), 3f * u,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * u, 6f * u)))
            }
            poly(g, p.mlPath, AniColors.Ink, 9f * u)
            poly(g, p.mlPath, AniColors.Safety, 5f * u)
        }

        // junctions
        for (x in 0 until l.cols) for (y in 0 until l.rows) drawCircle(tape, 3f * u, g.p(Node(x, y)))

        // start square
        val sp = g.p(l.start)
        drawRect(AniColors.Ok, Offset(sp.x - 9f * u, sp.y - 9f * u), Size(18f * u, 18f * u), style = Stroke(2f * u))

        // targets
        val active = r?.targetId ?: c.selectedTarget
        for (t in l.targets.values) {
            val o = g.p(t.node)
            val sel = t.id == active
            val rad = (if (sel) 12f else 9f) * u
            drawCircle(if (sel) AniColors.Safety else surface, rad, o)
            drawCircle(onSurface, rad, o, style = Stroke(2f * u))
            text(t.id, o, if (sel) AniColors.Ink else onSurface, 11f * u)
        }

        // robot: last junction from telemetry, nudged toward the next one while moving
        val tl = c.last
        if (tl != null || !c.isConnected) {
            var center = g.p(c.robotPos)
            if (tl != null && tl.state == "MOVING" && l.contains(tl.next)) {
                val n = g.p(tl.next)
                center = Offset(center.x + (n.x - center.x) * 0.35f, center.y + (n.y - center.y) * 0.35f)
            }
            val ring = if (c.isConnected) AniColors.Safety else outline
            drawCircle(AniColors.Ink, 14f * u, center)
            drawCircle(ring, 14f * u, center, style = Stroke(3f * u))
            translate(center.x, center.y) {
                rotate(c.robotHeading * 90f, pivot = Offset.Zero) {
                    val arrow = Path().apply {
                        moveTo(0f, -8f * u); lineTo(6f * u, 5f * u); lineTo(0f, 2f * u); lineTo(-6f * u, 5f * u); close()
                    }
                    drawPath(arrow, ring)
                }
            }
            if (tl?.state == "BLOCKED") text("!", Offset(center.x + 16f * u, center.y - 16f * u), AniColors.Crit, 18f * u)
        }
    }
}

private fun DrawScope.poly(g: Geo, nodes: List<Node>, color: Color, w: Float) {
    if (nodes.size < 2) return
    val path = Path().apply {
        val s = g.p(nodes.first()); moveTo(s.x, s.y)
        for (n in nodes.drop(1)) { val q = g.p(n); lineTo(q.x, q.y) }
    }
    drawPath(path, color, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    isFakeBoldText = true
}

private fun DrawScope.text(s: String, center: Offset, color: Color, sizePx: Float) {
    textPaint.color = color.toArgb()
    textPaint.textSize = sizePx
    val fm = textPaint.fontMetrics
    drawContext.canvas.nativeCanvas.drawText(s, center.x, center.y - (fm.ascent + fm.descent) / 2, textPaint)
}
