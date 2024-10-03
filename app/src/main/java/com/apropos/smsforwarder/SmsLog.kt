package com.apropos.smsforwarder

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "sms_logs")
data class SmsLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "sender") val sender: String,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "time") val time: String
)

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY id DESC")
    fun getAll(): List<SmsLogEntry>

    @Insert
    fun insert(log: SmsLogEntry)

    @Query("DELETE FROM sms_logs")
    fun deleteAll()
}

@Database(entities = [SmsLogEntry::class], version = 1)
abstract class SmsLogDatabase : RoomDatabase() {
    abstract fun smsLogDao(): SmsLogDao

    companion object {
        private var instance: SmsLogDatabase? = null

        fun getInstance(context: Context): SmsLogDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsLogDatabase::class.java,
                    "sms_log_database"
                ).build()
            }
            return instance!!
        }
    }
}

object SmsLog {
    fun addLog(context: Context, sender: String, body: String, time: String) {
        val logEntry = SmsLogEntry(sender = sender, body = body, time = time)
        CoroutineScope(Dispatchers.IO).launch {
            SmsLogDatabase.getInstance(context).smsLogDao().insert(logEntry)
        }
    }

    fun getLogs(context: Context): List<SmsLogEntry> {
        return SmsLogDatabase.getInstance(context).smsLogDao().getAll()
    }

    fun clearLogs(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            SmsLogDatabase.getInstance(context).smsLogDao().deleteAll()
        }
    }
}