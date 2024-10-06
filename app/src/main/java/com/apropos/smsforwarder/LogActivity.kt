// app/src/main/java/com/apropos/smsforwarder/LogActivity.kt
package com.apropos.smsforwarder

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogActivity : AppCompatActivity() {
    private lateinit var logRecyclerView: RecyclerView
    private lateinit var adapter: LogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        logRecyclerView = findViewById(R.id.logRecyclerView)
        logRecyclerView.layoutManager = LinearLayoutManager(this)

        val clearLogsButton: FloatingActionButton = findViewById(R.id.clearLogsButton)
        clearLogsButton.setOnClickListener {
            showClearLogsConfirmationDialog()
        }

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        loadLogs()
    }

    private fun loadLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val logs = SmsLog.getLogs(this@LogActivity)
                withContext(Dispatchers.Main) {
                    adapter = LogAdapter(logs) { logEntry ->
                        resendMessage(logEntry)
                    }
                    logRecyclerView.adapter = adapter
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LogActivity, "Error loading logs: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun resendMessage(logEntry: SmsLogEntry) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = EmailSender.sendEmail(this@LogActivity, logEntry.sender, logEntry.body, logEntry.time)
                if (result.isSuccess) {
                    Toast.makeText(this@LogActivity, "Message resent successfully", Toast.LENGTH_SHORT).show()
                    SmsLog.updateLogStatus(this@LogActivity, logEntry.id, true)
                    loadLogs() // Reload logs to reflect the updated status
                } else {
                    Toast.makeText(this@LogActivity, "Failed to resend message: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LogActivity, "Error resending message: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showClearLogsConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Logs")
            .setMessage("Are you sure you want to clear all logs? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                clearLogs()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SmsLog.clearAllLogs(this@LogActivity)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LogActivity, "All logs cleared", Toast.LENGTH_SHORT).show()
                    loadLogs() // Reload (empty) logs
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LogActivity, "Error clearing logs: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    inner class LogAdapter(
        private val logs: List<SmsLogEntry>,
        private val onResendClick: (SmsLogEntry) -> Unit
    ) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

        inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val timeTextView: TextView = itemView.findViewById(R.id.logItemTime)
            val senderTextView: TextView = itemView.findViewById(R.id.logItemSender)
            val bodyTextView: TextView = itemView.findViewById(R.id.logItemBody)
            val resendButton: Button = itemView.findViewById(R.id.resendButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.log_item, parent, false)
            return LogViewHolder(view)
        }

        override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
            val logEntry = logs[position]
            holder.timeTextView.text = logEntry.time
            holder.senderTextView.text = "From: ${logEntry.sender}"
            holder.bodyTextView.text = logEntry.body
            holder.resendButton.setOnClickListener { onResendClick(logEntry) }

            if (!logEntry.sent) {
                holder.itemView.setBackgroundColor(Color.parseColor("#FFCCCC"))
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        override fun getItemCount() = logs.size
    }
}