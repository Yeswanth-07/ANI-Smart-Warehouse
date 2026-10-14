package com.enerix.anicontrol.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.enerix.anicontrol.R

/*
 * Small building blocks shared by the screens.
 * Note: every block that shows live robot data takes `v` (the change counter), so it redraws on each update.
 */

val CardShape = RoundedCornerShape(16.dp)

@Composable
fun Small(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

/** A rounded panel. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
    ) { content() }
}

/** A panel with a title row that opens and closes. Closed by default. */
@Composable
fun ExpandablePanel(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    v: Int,
    startOpen: Boolean = false,
    content: @Composable (Int) -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(startOpen) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Small(subtitle)
            }
            Icon(
                painterResource(R.drawable.ic_expand),
                contentDescription = if (open) "Close" else "Open",
                modifier = Modifier.size(22.dp).rotate(if (open) 180f else 0f),
            )
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) { content(v) }
        }
    }
}

/** Small rounded label: "Battery 82%". */
@Composable
fun InfoPill(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

/** Coloured circle with an icon, used at the start of status lines. */
@Composable
fun IconBadge(@DrawableRes icon: Int, bg: Color, fg: Color, diameter: Int = 44) {
    Box(
        Modifier.size(diameter.dp).background(bg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = fg, modifier = Modifier.size((diameter * 0.5f).dp))
    }
}

/** Big selectable tile, used for destinations. */
@Composable
fun ChoiceTile(
    letter: String,
    label: String,
    sub: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val border = if (selected) AniColors.Safety else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier
            .clip(CardShape)
            .background(if (selected) AniColors.Safety.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(if (selected) 2.dp else 1.dp, border, CardShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).background(if (selected) AniColors.Safety else MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, fontWeight = FontWeight.Bold, color = if (selected) AniColors.Ink else MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontWeight = FontWeight.SemiBold)
            Small(sub)
        }
    }
}

/** Horizontal bar comparing two values. */
@Composable
fun CompareBar(label: String, value: Double, max: Double, color: Color, bold: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
            Text("%.1f".format(value), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        }
        Box(
            Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(
                Modifier.fillMaxWidth((value / max).toFloat().coerceIn(0.03f, 1f)).height(12.dp)
                    .clip(RoundedCornerShape(6.dp)).background(color),
            )
        }
    }
}
