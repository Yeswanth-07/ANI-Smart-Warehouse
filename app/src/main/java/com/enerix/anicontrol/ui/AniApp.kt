@file:OptIn(ExperimentalMaterial3Api::class)

package com.enerix.anicontrol.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.enerix.anicontrol.R
import com.enerix.anicontrol.core.Controller
import com.enerix.anicontrol.core.LinkStatus

private data class Tab(val label: String, val icon: Int)

private val tabs = listOf(
    Tab("Live", R.drawable.ic_nav_live),
    Tab("Route", R.drawable.ic_nav_route),
    Tab("Data", R.drawable.ic_nav_data),
    Tab("Trials", R.drawable.ic_nav_trials),
)

@Composable
fun AniApp(vm: AppViewModel) {
    val v = vm.version // read so every change redraws
    val c = vm.c
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showConnect by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }

    // Bluetooth permission (Android 12+ asks for "Nearby devices")
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        permLauncher.launch(perms)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(AniColors.Ink),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.ic_nav_route), contentDescription = null, tint = AniColors.Safety,
                                modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("ANI Control", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Zero Origin Warehouse", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    ConnectionPill(c, v) { showConnect = true }
                    IconButton(onClick = { showHelp = true }) {
                        Icon(painterResource(R.drawable.ic_help), contentDescription = "How to use")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(painterResource(t.icon), contentDescription = null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AniColors.Ink,
                            indicatorColor = AniColors.Safety,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> LiveScreen(c, v) { showConnect = true }
                1 -> RouteScreen(c, v)
                2 -> DataScreen(c, v)
                else -> TrialsScreen(c, v)
            }
        }
    }

    if (showConnect) ConnectDialog(vm, v) { showConnect = false }
    if (showSettings) SettingsDialog(c, v) { showSettings = false }
    if (showHelp) HelpDialog { showHelp = false }
}

@Composable
private fun ConnectionPill(c: Controller, v: Int, onClick: () -> Unit) {
    val stale = c.linkStale
    val color = when {
        !c.isConnected -> MaterialTheme.colorScheme.outline
        stale -> AniColors.Warn
        else -> AniColors.Ok
    }
    val label = when (c.status) {
        LinkStatus.CONNECTED -> if (stale) "No signal" else if (c.isDemo) "Demo" else "Connected"
        LinkStatus.CONNECTING -> "Connecting…"
        LinkStatus.DISCONNECTED -> "Connect"
    }
    Row(
        Modifier
            .widthIn(max = 140.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    val steps = listOf(
        "Connect" to "Tap Connect at the top. Pick HC-05 for the real robot (pair it in Android Bluetooth settings first, PIN 1234), or Demo robot to practise.",
        "Place the robot" to "Put it on the green start square, facing up on the map. If you moved it, use Live → More controls → Robot is at the start square.",
        "Pick a destination" to "Tap a rack on the Live tab. The yellow line is the smart route; the dashed line is the shortest path.",
        "Press Go" to "The robot drives the route. If an aisle gets busy or blocked, the app finds a better way by itself.",
        "Check results" to "Route shows the turn-by-turn plan. Data shows live messages and what the robot learned. Trials lists every trip.",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How to use ANI Control") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { i, (t, d) ->
                    Row {
                        Box(Modifier.size(26.dp).background(AniColors.Safety, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = AniColors.Ink, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(t, fontWeight = FontWeight.Bold)
                            Small(d)
                        }
                    }
                }
                HorizontalDivider()
                Text("Map colours", fontWeight = FontWeight.Bold)
                Small("Yellow line = smart route · Dashed grey = shortest path · Orange aisle = busy · Red dotted aisle = blocked · Orange dot = aisle the robot found slow · Green square = start")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
    )
}

@Composable
private fun ConnectDialog(vm: AppViewModel, v: Int, onDismiss: () -> Unit) {
    val c = vm.c
    val devices: List<BluetoothDevice> = remember { vm.pairedDevices() } // read each time the dialog opens
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to robot") },
        text = {
            @Suppress("UNUSED_VARIABLE") val redraw = v
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your robot", fontWeight = FontWeight.Bold)
                if (devices.isEmpty()) {
                    Small("No paired Bluetooth devices. Open Android Settings → Bluetooth, pair HC-05 (PIN 1234 or 0000), then open this again. Also allow \"Nearby devices\" for this app.")
                }
                devices.forEach { d ->
                    DeviceRow(R.drawable.ic_bluetooth, vm.deviceName(d), d.address) { onDismiss(); vm.connect(d) }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("No robot with you?", fontWeight = FontWeight.Bold)
                DeviceRow(R.drawable.ic_robot, "Demo robot", "A pretend robot that behaves like the real one") {
                    onDismiss(); c.connectDemo()
                }
                if (c.isConnected) {
                    OutlinedButton(onClick = { onDismiss(); c.disconnect() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Disconnect from ${c.link?.name}")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun DeviceRow(icon: Int, name: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, AniColors.Safety, AniColors.Ink, 36)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, fontWeight = FontWeight.SemiBold)
            Small(sub)
        }
    }
}

@Composable
private fun SettingsDialog(c: Controller, v: Int, onDismiss: () -> Unit) {
    val running = c.run != null
    var seg by remember { mutableStateOf(c.layout.segmentM.toString()) }
    var vLo by remember { mutableStateOf(c.vMin.toString()) }
    var vHi by remember { mutableStateOf(c.vMax.toString()) }
    var armed by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            @Suppress("UNUSED_VARIABLE") val redraw = v
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Floor grid", fontWeight = FontWeight.Bold)
                Small("Count the tape crossings on your floor and set them here.")
                Stepper("Crossings left to right", c.layout.cols, !running) { c.setGrid(it, c.layout.rows) }
                Stepper("Crossings bottom to top", c.layout.rows, !running) { c.setGrid(c.layout.cols, it) }
                OutlinedTextField(seg, { seg = it }, label = { Text("Size of one square (metres)") }, singleLine = true)
                Small("Tip: long-press a crossing on the map to move a rack or the start square.")
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Battery", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(vLo, { vLo = it }, label = { Text("Empty (V)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(vHi, { vHi = it }, label = { Text("Full (V)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SwitchRow("Find better routes automatically", "Change route when an aisle gets busy", c.autoReroute) {
                    c.autoReroute = it; c.tickUi()
                }
                SwitchRow("Back out of blocked aisles", "If stuck for 3 s, reverse and go around", c.autoEscape) {
                    c.autoEscape = it; c.tickUi()
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Reset", fontWeight = FontWeight.Bold)
                listOf(
                    "Clear busy / blocked marks" to { c.clearConditions() },
                    "Forget what the robot learned" to { c.clearLearning() },
                    "Delete all trials and data" to { c.clearRecords() },
                ).forEach { (label, action) ->
                    OutlinedButton(
                        onClick = { if (armed == label) { action(); armed = null } else armed = label },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (armed == label) "Tap again to confirm" else label) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                seg.replace(',', '.').toDoubleOrNull()?.let { c.setSegmentLength(it) }
                val lo = vLo.replace(',', '.').toDoubleOrNull()
                val hi = vHi.replace(',', '.').toDoubleOrNull()
                if (lo != null && hi != null) c.setBatteryRange(lo, hi)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Stepper(label: String, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        TextButton(onClick = { onChange(value - 1) }, enabled = enabled && value > 2) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text("$value", fontWeight = FontWeight.Bold)
        TextButton(onClick = { onChange(value + 1) }, enabled = enabled && value < 12) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Small(sub)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
