package com.enerix.anicontrol.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.enerix.anicontrol.android.BluetoothRobotLink
import com.enerix.anicontrol.android.MainScheduler
import com.enerix.anicontrol.android.ModelLoader
import com.enerix.anicontrol.android.Storage
import com.enerix.anicontrol.core.Cancellable
import com.enerix.anicontrol.core.Controller

/** Owns the controller for the lifetime of the app and tells Compose when to redraw. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    val scheduler = MainScheduler()
    val c = Controller(scheduler)

    /** Bumped on every controller change; screens read it so they redraw. */
    var version by mutableIntStateOf(0)
        private set

    private val storage = Storage(app)
    private var saveTask: Cancellable? = null
    private var ticker: Cancellable? = null

    init {
        storage.load(c)
        c.model = ModelLoader.load(app)
        c.onChange = { version++ }
        c.onPersist = {
            saveTask?.cancel()
            saveTask = scheduler.post(1000) { storage.save(c) }
        }
        c.refreshPreview()
        tick()
    }

    // refresh "last message x s ago" and link health once a second
    private fun tick() {
        ticker = scheduler.post(1000) { c.tickUi(); tick() }
    }

    fun pairedDevices(): List<BluetoothDevice> = BluetoothRobotLink.pairedDevices(getApplication())

    fun deviceName(d: BluetoothDevice) = BluetoothRobotLink.displayName(d)

    fun connect(d: BluetoothDevice) {
        c.disconnect(silent = true)
        c.setConnecting()
        BluetoothRobotLink.connectAsync(getApplication(), d, scheduler.handler) { res ->
            res.onSuccess { c.attach(it) }
                .onFailure { c.connectFailed(it.message ?: "Could not connect to the HC-05.") }
        }
    }

    fun saveNow() = storage.save(c)

    override fun onCleared() {
        ticker?.cancel()
        storage.save(c)
        c.disconnect(silent = true)
    }
}
