package io.hex128.powernotifier

import android.annotation.SuppressLint
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
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class PowerNotifierReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Activity.MODE_PRIVATE
        )
        val enabled = prefs.getBoolean("enabled", false)
        val insecure = prefs.getBoolean("insecure", false)
        val telegramBotToken = prefs.getString("telegram_bot_token", "")
        val telegramChatId = prefs.getString("telegram_chat_id", "")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && enabled) {
            val serviceIntent = Intent(
                context,
                PowerNotifierService::class.java
            )
            ContextCompat.startForegroundService(context, serviceIntent)
        }
        if (intent.action == Intent.ACTION_BATTERY_CHANGED && enabled) {
            val isConnected = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) > 0
            if (!prefs.contains("was_connected") || prefs.getBoolean(
                    "was_connected",
                    false
                ) != isConnected
            ) {
                with(prefs!!.edit()) {
                    putBoolean("was_connected", isConnected)
                    apply()
                }
                val message =
                    prefs.getString((if (isConnected) "on_message" else "off_message"), "")
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
    }

    private fun sendChargerState(url: String, insecure: Boolean) {
        Thread {
            try {
                val client = URL(url).openConnection() as HttpsURLConnection
                client.connectTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
                client.readTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
                if (insecure) {
                    client.sslSocketFactory = SSLContext.getInstance("SSL").apply {
                        init(null, arrayOf<TrustManager>(
                            @SuppressLint("CustomX509TrustManager")
                            object : X509TrustManager {
                                override fun getAcceptedIssuers(): Array<X509Certificate>? = null

                                override fun checkClientTrusted(
                                    chain: Array<X509Certificate>,
                                    authType: String
                                ) = Unit

                                override fun checkServerTrusted(
                                    chain: Array<X509Certificate>,
                                    authType: String
                                ) = Unit
                            }), java.security.SecureRandom()
                        )
                    }.socketFactory
                    client.hostnameVerifier = HostnameVerifier { _, _ -> true }
                }
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
}
