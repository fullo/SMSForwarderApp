// app/src/main/java/com/apropos/smsforwarder/SmsLogEntry.kt
package com.apropos.smsforwarder

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "sms_logs")
data class SmsLogEntry(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "sender") var sender: String,
    @ColumnInfo(name = "body") var body: String,
    @ColumnInfo(name = "time") var time: String,
    @ColumnInfo(name = "sent") var sent: Boolean
) {
    companion object {
        fun create(sender: String, body: String, time: String, sent: Boolean): SmsLogEntry {
            return SmsLogEntry(0, sender, body, time, sent)
        }
    }
}