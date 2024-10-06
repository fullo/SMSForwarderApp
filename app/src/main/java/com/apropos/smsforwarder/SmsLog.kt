// app/src/main/java/com/apropos/smsforwarder/SmsLog.kt
package com.apropos.smsforwarder

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY id DESC")
    fun getAll(): List<SmsLogEntry>

    @Insert
    fun insert(log: SmsLogEntry)

    @Query("UPDATE sms_logs SET sent = :sent WHERE id = :id")
    fun updateSentStatus(id: Int, sent: Boolean)

    @Query("DELETE FROM sms_logs")
    fun deleteAll()
}

@Database(entities = [SmsLogEntry::class], version = 2)
abstract class SmsLogDatabase : RoomDatabase() {
    abstract fun smsLogDao(): SmsLogDao

    companion object {
        private var instance: SmsLogDatabase? = null

        fun getInstance(context: Context): SmsLogDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): SmsLogDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SmsLogDatabase::class.java,
                "sms_log_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

object SmsLog {
    suspend fun addLog(context: Context, sender: String, body: String, time: String, sent: Boolean) {
        val logEntry = SmsLogEntry.create(sender, body, time, sent)
        withContext(Dispatchers.IO) {
            SmsLogDatabase.getInstance(context).smsLogDao().insert(logEntry)
        }
    }

    suspend fun getLogs(context: Context): List<SmsLogEntry> = withContext(Dispatchers.IO) {
        SmsLogDatabase.getInstance(context).smsLogDao().getAll()
    }

    suspend fun updateLogStatus(context: Context, id: Int, sent: Boolean) {
        withContext(Dispatchers.IO) {
            SmsLogDatabase.getInstance(context).smsLogDao().updateSentStatus(id, sent)
        }
    }

    suspend fun clearAllLogs(context: Context) {
        withContext(Dispatchers.IO) {
            SmsLogDatabase.getInstance(context).smsLogDao().deleteAll()
        }
    }
}