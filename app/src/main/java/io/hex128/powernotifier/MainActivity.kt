package io.hex128.powernotifier

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView


class MainActivity : Activity() {
    data class UiState(
        val enableSwitchChecked: Boolean,
        val insecureSwitchChecked: Boolean,
        val customSwitchChecked: Boolean,
        val telegramBotToken: String?,
        val telegramChatId: String?,
        val onMessageText: String?,
        val offMessageText: String?,
        val customUrlOn: String?,
        val customUrlOff: String?,
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

    private var notificationPermissionWarningLayout: LinearLayout? = null
    private var notificationPermissionWarningButton: Button? = null
    private var enableSwitch: Switch? = null
    private var lastResponseTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notificationPermissionWarningLayout = findViewById(R.id.notificationPermissionWarningLayout)
        notificationPermissionWarningButton = findViewById(R.id.notificationPermissionWarningButton)
        enableSwitch = findViewById(R.id.enableSwitch)

        val insecureSwitch = findViewById<Switch>(R.id.insecureSwitch)
        val customSwitch = findViewById<Switch>(R.id.customSwitch)
        val telegramBotTokenEditText = findViewById<EditText>(R.id.telegramBotTokenEditText)
        val telegramChatIdEditText = findViewById<EditText>(R.id.telegramChatIdEditText)
        val onMessageEditText = findViewById<EditText>(R.id.onMessageEditText)
        val offMessageEditText = findViewById<EditText>(R.id.offMessageEditText)
        val customUrlOnEditText = findViewById<EditText>(R.id.customOnUrlEditText)
        val customUrlOffEditText = findViewById<EditText>(R.id.customOffUrlEditText)

        val telegramInputsLayout = findViewById<LinearLayout>(R.id.telegramInputsLayout)
        val customUrlInputsLayout = findViewById<LinearLayout>(R.id.customInputsLayout)

        lastResponseTextView = findViewById(R.id.lastResponse)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PermissionChecker.PERMISSION_GRANTED
            ) {
                notificationPermissionWarningLayout?.visibility = View.VISIBLE
                enableSwitch?.isEnabled = false
                prefs.edit().putBoolean(PowerNotifierReceiver.PREF_ENABLED, false).apply()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }

        val uiState = UiState(
            prefs.getBoolean(PowerNotifierReceiver.PREF_ENABLED, false),
            prefs.getBoolean(PowerNotifierReceiver.PREF_INSECURE, false),
            prefs.getBoolean(PowerNotifierReceiver.PREF_CUSTOM, false),
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
            prefs.getString(PowerNotifierReceiver.PREF_CUSTOM_URL_ON, ""),
            prefs.getString(PowerNotifierReceiver.PREF_CUSTOM_URL_OFF, ""),
            prefs.getString(PowerNotifierReceiver.PREF_LAST_RESPONSE, "")
        )

        if (uiState.customSwitchChecked) {
            telegramInputsLayout.visibility = View.GONE
            customUrlInputsLayout.visibility = View.VISIBLE
        } else {
            telegramInputsLayout.visibility = View.VISIBLE
            customUrlInputsLayout.visibility = View.GONE
        }

        enableSwitch?.isChecked = uiState.enableSwitchChecked
        insecureSwitch.isChecked = uiState.insecureSwitchChecked
        customSwitch.isChecked = uiState.customSwitchChecked
        telegramBotTokenEditText.setText(uiState.telegramBotToken)
        telegramChatIdEditText.setText(uiState.telegramChatId)
        onMessageEditText.setText(uiState.onMessageText)
        offMessageEditText.setText(uiState.offMessageText)
        customUrlOnEditText.setText(uiState.customUrlOn)
        customUrlOffEditText.setText(uiState.customUrlOff)
        lastResponseTextView?.text = uiState.lastResponse

        prefs.registerOnSharedPreferenceChangeListener(listener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationPermissionWarningButton?.setOnClickListener { _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            }
        }

        enableSwitch?.setOnCheckedChangeListener { _, isChecked ->
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

        customSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PowerNotifierReceiver.PREF_CUSTOM, isChecked).apply()

            if (isChecked) {
                telegramInputsLayout.visibility = View.GONE
                customUrlInputsLayout.visibility = View.VISIBLE
            } else {
                telegramInputsLayout.visibility = View.VISIBLE
                customUrlInputsLayout.visibility = View.GONE
            }
        }

        arrayOf(
            telegramBotTokenEditText,
            telegramChatIdEditText,
            onMessageEditText,
            offMessageEditText,
            customUrlOnEditText,
            customUrlOffEditText
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
                R.id.customOnUrlEditText -> PowerNotifierReceiver.PREF_CUSTOM_URL_ON
                R.id.customOffUrlEditText -> PowerNotifierReceiver.PREF_CUSTOM_URL_OFF
                else -> throw IllegalArgumentException("Invalid EditText ID")
            }
        }

        if (enableSwitch!!.isChecked) {
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_CODE -> {
                if (
                    grantResults.isNotEmpty() &&
                    grantResults[0] == PermissionChecker.PERMISSION_GRANTED
                ) {
                    notificationPermissionWarningLayout?.visibility = View.GONE
                    enableSwitch?.isEnabled = true
                } else {
                    notificationPermissionWarningLayout?.visibility = View.VISIBLE
                    enableSwitch?.isEnabled = false
                }
            }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PermissionChecker.PERMISSION_GRANTED
            ) {
                notificationPermissionWarningLayout?.visibility = View.VISIBLE
                enableSwitch?.isEnabled = false
                prefs.edit().putBoolean(PowerNotifierReceiver.PREF_ENABLED, false).apply()
            } else {
                notificationPermissionWarningLayout?.visibility = View.GONE
                enableSwitch?.isEnabled = true
            }
        }
    }

    companion object {
        const val NOTIFICATION_PERMISSION_CODE = 101
    }
}
