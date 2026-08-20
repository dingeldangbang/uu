package com.secureguard.enterprise.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object NotificationConstants {
    const val CHANNEL_ALERTS = "secureguard_alerts"
    const val CHANNEL_SERVICES = "secureguard_services"
    const val FGS_NOTIF_ID = 4201
}

fun Long.formatRelative(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "gerade eben"
        diff < TimeUnit.HOURS.toMillis(1)   -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min"
        diff < TimeUnit.DAYS.toMillis(1)    -> "${TimeUnit.MILLISECONDS.toHours(diff)} h"
        else -> SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(this))
    }
}

fun Double.formatCoords(): String =
    "%.5f, %.5f".format(this)

fun ByteArray.toHex(): String =
    joinToString(separator = "") { "%02X".format(it) }

/** One-shot-Toast-Helper aus einem Composable. */
@Composable
fun rememberToast(): (String, Boolean) -> Unit {
    val ctx = LocalContext.current
    return { msg, long ->
        Toast.makeText(ctx, msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun OnFirstComposition(block: () -> Unit) {
    LaunchedEffect(Unit) { block() }
}
