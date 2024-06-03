package io.hex128.powernotifier

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText


class MainActivity : Activity() {
    private var sharedPref: SharedPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val serviceIntent = Intent(
            applicationContext,
            PowerNotifierService::class.java
        )
        ContextCompat.startForegroundService(this, serviceIntent)
        sharedPref = getSharedPreferences(
            getString(R.string.preference_file_key), MODE_PRIVATE
        )
        findViewById<EditText>(R.id.urlEditText).setText(sharedPref!!.getString("url", ""))
        findViewById<EditText>(R.id.urlEditText).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                with(sharedPref!!.edit()) {
                    putString("url", findViewById<EditText>(R.id.urlEditText).text.toString())
                    apply()
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }
}
