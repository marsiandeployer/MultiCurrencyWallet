package com.swaponline.wallet.wallet

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class WalletStorage(context: Context) {
    private val appContext = context.applicationContext

    private val masterKey: MasterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        appContext,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(snapshot: WalletSnapshot) {
        preferences.edit()
            .putString(KEY_MNEMONIC, snapshot.mnemonicWords.joinToString(" "))
            .putString(KEY_PRIVATE_KEY, snapshot.privateKeyHex)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    fun load(): StoredWallet? {
        val mnemonic = preferences.getString(KEY_MNEMONIC, null) ?: return null
        val privateKey = preferences.getString(KEY_PRIVATE_KEY, null) ?: return null
        val savedAt = preferences.getLong(KEY_SAVED_AT, 0L)

        val words = mnemonic.split(" ").filter { it.isNotBlank() }
        if (words.size != 12) {
            return null
        }

        return StoredWallet(
            snapshot = WalletSnapshot(words, privateKey),
            savedAtMillis = savedAt,
        )
    }

    data class StoredWallet(
        val snapshot: WalletSnapshot,
        val savedAtMillis: Long,
    )

    private companion object {
        const val FILE_NAME = "wallet_secure_store"
        const val KEY_MNEMONIC = "mnemonic_words"
        const val KEY_PRIVATE_KEY = "private_key_hex"
        const val KEY_SAVED_AT = "saved_at"
    }
}
