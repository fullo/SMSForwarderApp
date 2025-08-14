// app/src/main/java/com/apropos/smsforwarder/SecurePreferencesManager.kt
package com.apropos.smsforwarder

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.IOException

class SecurePreferencesManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SecurePreferencesManager"
        private const val PREFS_NAME = "SMSForwarderPrefs"
        private const val ENCRYPTED_PREFS_NAME = "SMSForwarderSecurePrefs"
        private const val MIGRATION_STATE_FILE = "migration_state"

        // Stati di migrazione
        private const val MIGRATION_NONE = "none"
        private const val MIGRATION_STARTED = "started"
        private const val MIGRATION_COMPLETED = "completed"

        // Chiavi da migrare
        private val KEYS_TO_MIGRATE = setOf(
            "email", "password", "recipient",
            "subjectFormat", "bodyFormat"
        )

        // Chiavi non sensibili che rimangono in chiaro
        private val PLAIN_KEYS = setOf("isServiceRunning")

        @Volatile
        private var INSTANCE: SecurePreferencesManager? = null

        fun getInstance(context: Context): SecurePreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurePreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Lazy initialization delle SharedPreferences
    private val plainPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val encryptedPrefs: SharedPreferences? by lazy {
        createEncryptedPreferences()
    }

    private val migrationStateFile: File by lazy {
        File(context.filesDir, MIGRATION_STATE_FILE)
    }

    @Volatile
    private var migrationChecked = false

    /**
     * Ottiene un valore stringa con migrazione automatica se necessaria
     */
    fun getString(key: String, defaultValue: String = ""): String {
        ensureMigrationChecked()

        return when {
            key in PLAIN_KEYS -> plainPrefs.getString(key, defaultValue) ?: defaultValue
            encryptedPrefs != null -> encryptedPrefs!!.getString(key, defaultValue) ?: defaultValue
            else -> {
                Log.w(TAG, "Fallback to plain preferences for key: $key")
                plainPrefs.getString(key, defaultValue) ?: defaultValue
            }
        }
    }

    /**
     * Salva un valore stringa
     */
    fun putString(key: String, value: String) {
        ensureMigrationChecked()

        when {
            key in PLAIN_KEYS -> {
                plainPrefs.edit().putString(key, value).apply()
            }
            encryptedPrefs != null -> {
                encryptedPrefs!!.edit().putString(key, value).apply()
            }
            else -> {
                Log.w(TAG, "Fallback to plain preferences for saving key: $key")
                plainPrefs.edit().putString(key, value).apply()
            }
        }
    }

    /**
     * Ottiene un valore booleano
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        ensureMigrationChecked()

        return when {
            key in PLAIN_KEYS -> plainPrefs.getBoolean(key, defaultValue)
            encryptedPrefs != null -> encryptedPrefs!!.getBoolean(key, defaultValue)
            else -> plainPrefs.getBoolean(key, defaultValue)
        }
    }

    /**
     * Salva un valore booleano
     */
    fun putBoolean(key: String, value: Boolean) {
        ensureMigrationChecked()

        when {
            key in PLAIN_KEYS -> {
                plainPrefs.edit().putBoolean(key, value).apply()
            }
            encryptedPrefs != null -> {
                encryptedPrefs!!.edit().putBoolean(key, value).apply()
            }
            else -> {
                plainPrefs.edit().putBoolean(key, value).apply()
            }
        }
    }

    /**
     * Verifica se la configurazione email è completa
     */
    fun isEmailConfigured(): Boolean {
        val email = getString("email")
        val password = getString("password")
        val recipient = getString("recipient")
        return email.isNotEmpty() && password.isNotEmpty() && recipient.isNotEmpty()
    }

    /**
     * Verifica se l'app sta usando storage sicuro
     */
    fun isUsingSecureStorage(): Boolean {
        return encryptedPrefs != null
    }

    /**
     * Ottiene un messaggio di stato della sicurezza per l'UI
     */
    fun getSecurityStatusMessage(): String {
        return if (encryptedPrefs != null) {
            context.getString(R.string.secure_storage_enabled)
        } else {
            context.getString(R.string.secure_storage_disabled)
        }
    }

    /**
     * Verifica se dovremmo mostrare un avviso di sicurezza all'utente
     */
    fun shouldShowSecurityWarning(): Boolean {
        return encryptedPrefs == null && isEmailConfigured()
    }

    /**
     * Assicura che la migrazione sia stata controllata
     */
    private fun ensureMigrationChecked() {
        if (!migrationChecked) {
            synchronized(this) {
                if (!migrationChecked) {
                    checkAndPerformMigration()
                    migrationChecked = true
                }
            }
        }
    }

    /**
     * Crea EncryptedSharedPreferences con fallback graceful
     */
    private fun createEncryptedPreferences(): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences", e)
            null
        }
    }

    /**
     * Controlla e esegue la migrazione se necessaria
     */
    private fun checkAndPerformMigration() {
        if (encryptedPrefs == null) {
            Log.w(TAG, "EncryptedSharedPreferences not available, using plain preferences")
            return
        }

        val migrationState = getMigrationState()
        Log.d(TAG, "Migration state: $migrationState")

        when (migrationState) {
            MIGRATION_COMPLETED -> {
                Log.d(TAG, "Migration already completed")
                return
            }

            MIGRATION_STARTED -> {
                Log.w(TAG, "Incomplete migration detected, retrying...")
                retryMigration()
            }

            MIGRATION_NONE -> {
                if (needsMigration()) {
                    Log.i(TAG, "Starting migration...")
                    performMigration()
                } else {
                    // Nessun dato da migrare, marca come completata
                    setMigrationState(MIGRATION_COMPLETED)
                }
            }
        }
    }

    /**
     * Verifica se ci sono dati da migrare
     */
    private fun needsMigration(): Boolean {
        return KEYS_TO_MIGRATE.any { key ->
            plainPrefs.contains(key) && plainPrefs.getString(key, "")?.isNotEmpty() == true
        }
    }

    /**
     * Esegue la migrazione completa
     */
    private fun performMigration() {
        try {
            // Fase 1: Marca migrazione iniziata
            setMigrationState(MIGRATION_STARTED)

            // Fase 2: Copia tutti i dati sensibili
            val migratedKeys = mutableSetOf<String>()

            KEYS_TO_MIGRATE.forEach { key ->
                val value = plainPrefs.getString(key, null)
                if (value != null) {
                    encryptedPrefs!!.edit().putString(key, value).apply()
                    migratedKeys.add(key)
                    Log.d(TAG, "Migrated key: $key")
                }
            }

            // Fase 3: Verifica che i dati siano stati salvati correttamente
            val verificationFailed = migratedKeys.any { key ->
                val originalValue = plainPrefs.getString(key, null)
                val migratedValue = encryptedPrefs!!.getString(key, null)
                originalValue != migratedValue
            }

            if (verificationFailed) {
                throw IOException("Data verification failed after migration")
            }

            // Fase 4: Marca migrazione completata
            setMigrationState(MIGRATION_COMPLETED)

            // Fase 5: SOLO ORA cancella i dati originali (conservativo!)
            cleanupOriginalData(migratedKeys)

            Log.i(TAG, "Migration completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            // Non cambiamo lo stato, così al prossimo avvio riprova
        }
    }

    /**
     * Ritenta la migrazione da stato inconsistente
     */
    private fun retryMigration() {
        try {
            // Verifica se i dati crittografati sono consistenti
            val encryptedDataValid = KEYS_TO_MIGRATE.all { key ->
                val plainValue = plainPrefs.getString(key, null)
                val encryptedValue = encryptedPrefs!!.getString(key, null)

                // Se non c'è valore plain, va bene qualsiasi cosa encrypted
                if (plainValue == null) return@all true

                // Se c'è valore plain, deve corrispondere a quello encrypted
                plainValue == encryptedValue
            }

            if (encryptedDataValid) {
                // Dati consistenti, completa la migrazione
                setMigrationState(MIGRATION_COMPLETED)
                cleanupOriginalData(KEYS_TO_MIGRATE)
                Log.i(TAG, "Migration recovery completed")
            } else {
                // Dati inconsistenti, ripeti migrazione
                Log.w(TAG, "Inconsistent data detected, restarting migration")
                performMigration()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Migration retry failed", e)
        }
    }

    /**
     * Cancella i dati originali dopo migrazione verificata
     */
    private fun cleanupOriginalData(keys: Set<String>) {
        try {
            val editor = plainPrefs.edit()
            keys.forEach { key ->
                editor.remove(key)
            }
            editor.apply()
            Log.d(TAG, "Original data cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup original data", e)
            // Non è critico, i dati sono comunque al sicuro
        }
    }

    /**
     * Ottiene lo stato di migrazione dal file
     */
    private fun getMigrationState(): String {
        return try {
            if (migrationStateFile.exists()) {
                migrationStateFile.readText().trim()
            } else {
                MIGRATION_NONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read migration state", e)
            MIGRATION_NONE
        }
    }

    /**
     * Salva lo stato di migrazione nel file
     */
    private fun setMigrationState(state: String) {
        try {
            migrationStateFile.writeText(state)
            Log.d(TAG, "Migration state set to: $state")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write migration state", e)
        }
    }
}