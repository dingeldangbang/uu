package com.secureguard.enterprise.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observe Akkuzustand & Lade-Status via System-Broadcast (ACTION_BATTERY_CHANGED).
 *
 * Liefert:
 * - `level`    0..100 %
 * - `plugged`  true, wenn am Ladegerät
 */
@Singleton
class DeviceBatteryProvider @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val _level = MutableStateFlow(initialLevel())
    val level: StateFlow<Int> = _level.asStateFlow()

    private val _plugged = MutableStateFlow(initialPlugged())
    val plugged: StateFlow<Boolean> = _plugged.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            intent ?: return
            val raw  = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val sc   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (raw >= 0 && sc > 0) _level.value = (raw * 100) / sc
            _plugged.value = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        }
    }

    init {
        // Sticky-Broadcast → Initialwerte
        val sticky = safeRegister(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { receiver.onReceive(ctx, it) }
        // Live-Updates
        safeRegister(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun safeRegister(r: BroadcastReceiver?, f: IntentFilter): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            ctx.registerReceiver(r, f)
        }
    }

    private fun initialLevel(): Int = runCatching {
        val bm = ctx.getSystemService(BatteryManager::class.java)
        bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 50
    }.getOrDefault(50)

    private fun initialPlugged(): Boolean = runCatching {
        val sticky = safeRegister(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }.getOrDefault(false)
}
