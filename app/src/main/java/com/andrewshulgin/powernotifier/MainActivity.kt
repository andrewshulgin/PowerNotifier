package com.andrewshulgin.powernotifier

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView


class MainActivity : Activity() {
    data class UiState(
        val enableSwitchChecked: Boolean,
        val insecureSwitchChecked: Boolean,
        val telegramBotToken: String?,
        val telegramChatId: String?,
        val onMessageText: String?,
        val offMessageText: String?,
        val lastResponse: String?
    )

    private val serviceIntent by lazy {
        Intent(applicationContext, PowerNotifierService::class.java)
    }

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PowerNotifierReceiver.PREF_LAST_RESPONSE) {
            lastResponseTextView?.text = prefs.getString(key, "")
        }
    }

    private var lastResponseTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val enableSwitch = findViewById<Switch>(R.id.enableSwitch)
        val insecureSwitch = findViewById<Switch>(R.id.insecureSwitch)
        val telegramBotTokenEditText = findViewById<EditText>(R.id.telegramBotTokenEditText)
        val telegramChatIdEditText = findViewById<EditText>(R.id.telegramChatIdEditText)
        val onMessageEditText = findViewById<EditText>(R.id.onMessageEditText)
        val offMessageEditText = findViewById<EditText>(R.id.offMessageEditText)
        lastResponseTextView = findViewById(R.id.lastResponse)

        val uiState = UiState(
            prefs.getBoolean(PowerNotifierReceiver.PREF_ENABLED, false),
            prefs.getBoolean(PowerNotifierReceiver.PREF_INSECURE, false),
            prefs.getString(PowerNotifierReceiver.PREF_TELEGRAM_BOT_TOKEN, ""),
            prefs.getString(PowerNotifierReceiver.PREF_TELEGRAM_CHAT_ID, ""),
            prefs.getString(
                PowerNotifierReceiver.PREF_ON_MESSAGE_TEXT,
                getString(R.string.default_on_message)
            ),
            prefs.getString(
                PowerNotifierReceiver.PREF_OFF_MESSAGE_TEXT,
                getString(R.string.default_off_message)
            ),
            prefs.getString(PowerNotifierReceiver.PREF_LAST_RESPONSE, "")
        )

        enableSwitch.isChecked = uiState.enableSwitchChecked
        insecureSwitch.isChecked = uiState.insecureSwitchChecked
        telegramBotTokenEditText.setText(uiState.telegramBotToken)
        telegramChatIdEditText.setText(uiState.telegramChatId)
        onMessageEditText.setText(uiState.onMessageText)
        offMessageEditText.setText(uiState.offMessageText)
        lastResponseTextView?.text = uiState.lastResponse

        prefs.registerOnSharedPreferenceChangeListener(listener)

        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            with(prefs.edit()) {
                if (isChecked) {
                    ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                } else {
                    stopService(serviceIntent)
                    remove(PowerNotifierReceiver.PREF_WAS_CONNECTED)
                }
                putBoolean(PowerNotifierReceiver.PREF_ENABLED, isChecked)
                apply()
            }
        }

        insecureSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PowerNotifierReceiver.PREF_INSECURE, isChecked).apply()
        }

        arrayOf(
            telegramBotTokenEditText,
            telegramChatIdEditText,
            onMessageEditText,
            offMessageEditText
        ).forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    prefs.edit().putString(editText.tag.toString(), s.toString()).apply()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
            editText.tag = when (editText.id) {
                R.id.telegramBotTokenEditText -> PowerNotifierReceiver.PREF_TELEGRAM_BOT_TOKEN
                R.id.telegramChatIdEditText -> PowerNotifierReceiver.PREF_TELEGRAM_CHAT_ID
                R.id.onMessageEditText -> PowerNotifierReceiver.PREF_ON_MESSAGE_TEXT
                R.id.offMessageEditText -> PowerNotifierReceiver.PREF_OFF_MESSAGE_TEXT
                else -> throw IllegalArgumentException("Invalid EditText ID")
            }
        }

        if (enableSwitch.isChecked) {
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    override fun onResume() {
        super.onResume()
        lastResponseTextView?.text = prefs.getString(PowerNotifierReceiver.PREF_LAST_RESPONSE, "")
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }
}
