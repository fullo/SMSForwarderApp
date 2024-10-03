package com.apropos.smsforwarder

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogActivity : AppCompatActivity() {
    private lateinit var logListView: ListView
    private lateinit var clearLogsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        logListView = findViewById(R.id.logListView)
        clearLogsButton = findViewById(R.id.clearLogsButton)

        loadLogs()

        clearLogsButton.setOnClickListener {
            showClearLogsConfirmationDialog()
        }
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

    private fun showClearLogsConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Logs")
            .setMessage("Are you sure you want to clear all logs?")
            .setPositiveButton("Yes") { _, _ ->
                clearLogs()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun clearLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            SmsLog.clearLogs(this@LogActivity)
            withContext(Dispatchers.Main) {
                loadLogs()
                Toast.makeText(this@LogActivity, "Logs cleared", Toast.LENGTH_SHORT).show()
            }
        }
    }
}