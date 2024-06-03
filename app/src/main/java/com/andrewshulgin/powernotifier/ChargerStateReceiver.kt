package io.hex128.powernotifier

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ChargerStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
            val prefs = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Activity.MODE_PRIVATE
            )
            val isConnected = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) > 0
            if (!prefs.contains("was_connected") || prefs.getBoolean("was_connected", false) != isConnected) {
                with(prefs!!.edit()) {
                    putBoolean("was_connected", isConnected)
                    apply()
                }
                val url = prefs.getString("url", "")
                url?.let { sendChargerState(it, isConnected) }
            }
        }
    }

    private fun sendChargerState(url: String, isConnected: Boolean) {
        Thread {
            try {
                val client: HttpURLConnection
                val completeUrl = URL("$url?connected=$isConnected")
                Log.d("ChargerStateReceiver", "URL: $completeUrl")
                client = completeUrl.openConnection() as HttpURLConnection
                client.connectTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
                client.readTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
                client.requestMethod = "GET"
                client.connect()
                val reader = BufferedReader(InputStreamReader(client.inputStream))
                val responseBody: String = reader.readLine()
                Log.d("ChargerStateReceiver", "Response: $responseBody")
                client.disconnect()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }
}
