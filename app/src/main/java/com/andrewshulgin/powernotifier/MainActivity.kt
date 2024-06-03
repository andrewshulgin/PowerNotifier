package io.hex128.powernotifier

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Switch


class MainActivity : Activity() {
    private val serviceIntent by lazy {
        Intent(applicationContext, PowerNotifierService::class.java)
    }

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val enableSwitch = findViewById<Switch>(R.id.enableSwitch)
        val insecureSwitch = findViewById<Switch>(R.id.insecureSwitch)

        // val urlEditText = findViewById<EditText>(R.id.urlEditText)
        val telegramBotTokenEditText = findViewById<EditText>(R.id.telegramBotTokenEditText)
        val telegramChatIdEditText = findViewById<EditText>(R.id.telegramChatIdEditText)
        val onMessageEditText = findViewById<EditText>(R.id.onMessageEditText)
        val offMessageEditText = findViewById<EditText>(R.id.offMessageEditText)

        enableSwitch.isChecked = prefs.getBoolean("enabled", false)
        insecureSwitch.isChecked = prefs.getBoolean("insecure", false)

        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            with(prefs.edit()) {
                if (isChecked) {
                    ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                } else {
                    stopService(serviceIntent)
                }
                putBoolean("enabled", isChecked)
                apply()
            }
        }
        insecureSwitch.setOnCheckedChangeListener { _, isChecked ->
            with(prefs.edit()) {
                putBoolean("insecure", isChecked)
                apply()
            }
        }

        // urlEditText.setText(prefs.getString("url", ""))
        telegramBotTokenEditText.setText(prefs.getString("telegram_bot_token", ""))
        telegramChatIdEditText.setText(prefs.getString("telegram_chat_id", ""))
        onMessageEditText.setText(
            prefs.getString(
                "on_message_text",
                getString(R.string.default_on_message)
            )
        )
        offMessageEditText.setText(
            prefs.getString(
                "off_message_text",
                getString(R.string.default_off_message)
            )
        )

        arrayOf(
            // urlEditText,
            telegramBotTokenEditText,
            telegramChatIdEditText,
            onMessageEditText,
            offMessageEditText
        ).forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    with(prefs.edit()) {
                        putString(editText.tag.toString(), s.toString())
                        apply()
                    }
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
                // R.id.urlEditText -> "url"
                R.id.telegramBotTokenEditText -> "telegram_bot_token"
                R.id.telegramChatIdEditText -> "telegram_chat_id"
                R.id.onMessageEditText -> "on_message_text"
                R.id.offMessageEditText -> "off_message_text"
                else -> throw IllegalArgumentException("Invalid EditText ID")
            }
        }

        if (enableSwitch.isChecked) {
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }
}
