package io.hex128.powernotifier

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.support.v4.content.ContextCompat
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class PowerNotifierReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Activity.MODE_PRIVATE
        )
        val enabled = prefs.getBoolean(PREF_ENABLED, false)
        val insecure = prefs.getBoolean(PREF_INSECURE, false)
        val telegramBotToken = prefs.getString(PREF_TELEGRAM_BOT_TOKEN, "")
        val telegramChatId = prefs.getString(PREF_TELEGRAM_CHAT_ID, "")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && enabled) {
            val serviceIntent = Intent(
                context,
                PowerNotifierService::class.java
            )
            ContextCompat.startForegroundService(context, serviceIntent)
        }
        if (intent.action == Intent.ACTION_BATTERY_CHANGED && enabled) {
            val isConnected = intent.getIntExtra(
                BatteryManager.EXTRA_PLUGGED, -1
            ) > 0
            if (prefs.contains(PREF_WAS_CONNECTED)) {
                if (prefs.getBoolean(PREF_WAS_CONNECTED, false) != isConnected) {
                    val message = if (isConnected) {
                        prefs.getString(
                            PREF_ON_MESSAGE_TEXT,
                            context.getString(R.string.default_on_message)
                        )
                    } else {
                        prefs.getString(
                            PREF_OFF_MESSAGE_TEXT,
                            context.getString(R.string.default_off_message)
                        )
                    }
                    sendChargerState(
                        "https://api.telegram.org/bot$telegramBotToken/sendMessage?chat_id=$telegramChatId&text=${
                            URLEncoder.encode(
                                message,
                                "UTF-8"
                            )
                        }",
                        insecure
                    )
                }
            }
            with(prefs.edit()) {
                putBoolean(PREF_WAS_CONNECTED, isConnected)
                apply()
            }
        }
    }

    private fun sendChargerState(url: String, insecure: Boolean) {
        Thread {
            try {
                val client = URL(url).openConnection() as HttpsURLConnection
                client.connectTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
                client.readTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
                client.sslSocketFactory = TLSv12SocketFactory(insecure)
                client.requestMethod = "GET"
                client.connect()
                val reader = BufferedReader(InputStreamReader(client.inputStream))
                val responseBody: String = reader.readText()
                Log.d("ChargerStateReceiver", "Response: $responseBody")
                client.disconnect()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    companion object {
        const val PREF_INSECURE = "insecure"
        const val PREF_ENABLED = "enabled"
        const val PREF_TELEGRAM_BOT_TOKEN = "telegram_bot_token"
        const val PREF_TELEGRAM_CHAT_ID = "telegram_chat_id"
        const val PREF_WAS_CONNECTED = "was_connected"
        const val PREF_ON_MESSAGE_TEXT = "on_message_text"
        const val PREF_OFF_MESSAGE_TEXT = "off_message_text"
    }
}
