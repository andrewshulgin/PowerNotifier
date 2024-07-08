package io.hex128.powernotifier

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.support.v4.content.ContextCompat
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlin.math.pow
import kotlin.math.roundToLong


class PowerNotifierReceiver : BroadcastReceiver() {
    private var thread: Thread? = null

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Activity.MODE_PRIVATE
        )
        val enabled = prefs.getBoolean(PREF_ENABLED, false)
        val insecure = prefs.getBoolean(PREF_INSECURE, false)
        val custom = prefs.getBoolean(PREF_CUSTOM, false)
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
                    val url = if (custom) {
                        prefs.getString(
                            if (isConnected) PREF_CUSTOM_URL_ON else PREF_CUSTOM_URL_OFF,
                            ""
                        )
                    } else {
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
                        "https://api.telegram.org/bot$telegramBotToken/sendMessage?chat_id=$telegramChatId&text=${
                            URLEncoder.encode(
                                message,
                                "UTF-8"
                            )
                        }"
                    }
                    if (url != null) {
                        sendChargerState(url, insecure, prefs)
                    }
                }
            }
            prefs.edit().putBoolean(PREF_WAS_CONNECTED, isConnected).apply()
        }
    }

    @Synchronized
    private fun sendChargerState(url: String, insecure: Boolean, prefs: SharedPreferences) {
        if (thread != null) {
            Log.d(TAG, "Cancelling previous thread")
            thread?.interrupt()
        }
        thread = Thread {
            var delay: Double
            var attempt = 0
            while (prefs.getBoolean(PREF_ENABLED, false)) {
                attempt++
                if (Thread.interrupted()) {
                    return@Thread
                }
                val client = URL(url).openConnection() as HttpsURLConnection
                var retryRequired = true
                try {
                    client.defaultUseCaches = false
                    client.useCaches = false
                    client.setRequestProperty("Connection", "close")
                    client.connectTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
                    client.readTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
                    client.sslSocketFactory = TLSv12SocketFactory(insecure)
                    client.requestMethod = "GET"
                    client.connect()
                    var inputStream: InputStream? = client.errorStream
                    if (inputStream == null) {
                        inputStream = client.inputStream
                    }
                    val reader = BufferedReader(inputStream?.reader())
                    var responseBody: String = reader.readText()
                    Log.d(TAG, "Response: ${client.responseCode} $responseBody")
                    try {
                        responseBody = JSONObject(responseBody).toString(2)
                    } catch (_: JSONException) {
                    }
                    prefs.edit().putString(
                        PREF_LAST_RESPONSE,
                        "${client.responseCode} ${client.responseMessage}\n$responseBody"
                    ).apply()
                    thread = null
                    if (client.responseCode < 500) {
                        retryRequired = false
                    }
                } catch (e: IOException) {
                    Log.d(
                        TAG,
                        "Got an exception trying to send a request (attempt $attempt): ${e.message}.",
                        e
                    )
                    prefs.edit().putString(PREF_LAST_RESPONSE, e.stackTraceToString()).apply()
                } catch (_: InterruptedIOException) {
                } finally {
                    client.disconnect()
                }
                if (retryRequired) {
                    delay = (INITIAL_RETRY_DELAY * 2.0.pow(attempt - 1))
                        .coerceAtMost(MAX_RETRY_DELAY)
                    Log.d(TAG, "Retrying in ${delay.roundToLong()} ms.")
                    try {
                        Thread.sleep(delay.roundToLong())
                    } catch (ie: InterruptedException) {
                        Log.d(TAG, "Retry interrupted")
                        return@Thread
                    }
                } else {
                    break
                }
            }
        }
        thread?.start()
    }

    companion object {
        const val TAG = "PowerNotifier"
        const val INITIAL_RETRY_DELAY = 1000.0
        const val MAX_RETRY_DELAY = 32000.0
        const val PREF_INSECURE = "insecure"
        const val PREF_ENABLED = "enabled"
        const val PREF_CUSTOM = "custom"
        const val PREF_CUSTOM_URL_ON = "custom_url_on"
        const val PREF_CUSTOM_URL_OFF = "custom_url_off"
        const val PREF_TELEGRAM_BOT_TOKEN = "telegram_bot_token"
        const val PREF_TELEGRAM_CHAT_ID = "telegram_chat_id"
        const val PREF_WAS_CONNECTED = "was_connected"
        const val PREF_ON_MESSAGE_TEXT = "on_message_text"
        const val PREF_OFF_MESSAGE_TEXT = "off_message_text"
        const val PREF_LAST_RESPONSE = "last_response"
    }
}
