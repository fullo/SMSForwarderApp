// app/src/main/java/com/apropos/smsforwarder/LogActivity.kt
package com.apropos.smsforwarder

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogActivity : AppCompatActivity() {
    private lateinit var logListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        logListView = findViewById(R.id.logListView)

        loadLogs()
    }

    private fun loadLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            val logs = SmsLog.getLogs(this@LogActivity)
            val logStrings = logs.map { "${it.time} - From: ${it.sender}\n${it.body}" }

            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(this@LogActivity, android.R.layout.simple_list_item_1, logStrings)
                logListView.adapter = adapter
            }
        }
    }
}