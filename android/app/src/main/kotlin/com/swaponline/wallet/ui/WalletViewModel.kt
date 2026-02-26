package com.swaponline.wallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.swaponline.wallet.wallet.WalletGenerator
import com.swaponline.wallet.wallet.WalletStorage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = WalletStorage(application)

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        loadSavedWallet()
    }

    fun createAndSaveWallet() {
        runCatching {
            WalletGenerator.generateWallet().also(storage::save)
        }.onSuccess { snapshot ->
            _uiState.value = _uiState.value.copy(
                mnemonicWords = snapshot.mnemonicWords,
                privateKeyHex = snapshot.privateKeyHex,
                importPhrase = snapshot.mnemonicWords.joinToString(" "),
                savedAt = formatTimestamp(System.currentTimeMillis()),
                error = null,
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                error = error.message ?: "Failed to generate wallet",
            )
        }
    }

    fun updateImportPhrase(value: String) {
        _uiState.value = _uiState.value.copy(importPhrase = value)
    }

    fun importAndSaveWallet() {
        runCatching {
            WalletGenerator.importWallet(_uiState.value.importPhrase).also(storage::save)
        }.onSuccess { snapshot ->
            _uiState.value = _uiState.value.copy(
                mnemonicWords = snapshot.mnemonicWords,
                privateKeyHex = snapshot.privateKeyHex,
                importPhrase = snapshot.mnemonicWords.joinToString(" "),
                savedAt = formatTimestamp(System.currentTimeMillis()),
                error = null,
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                error = error.message ?: "Failed to import wallet",
            )
        }
    }

    fun loadSavedWallet() {
        val saved = storage.load() ?: return
        _uiState.value = _uiState.value.copy(
            mnemonicWords = saved.snapshot.mnemonicWords,
            privateKeyHex = saved.snapshot.privateKeyHex,
            importPhrase = saved.snapshot.mnemonicWords.joinToString(" "),
            savedAt = formatTimestamp(saved.savedAtMillis),
            error = null,
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        val localDateTime = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }
}

data class WalletUiState(
    val mnemonicWords: List<String> = emptyList(),
    val privateKeyHex: String = "",
    val importPhrase: String = "",
    val savedAt: String? = null,
    val error: String? = null,
)
