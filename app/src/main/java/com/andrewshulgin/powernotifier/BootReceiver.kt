package io.hex128.powernotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val serviceIntent = Intent(
                context,
                ChargerStateService::class.java
            )
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
