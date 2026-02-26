package com.swaponline.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swaponline.wallet.ui.theme.McwTheme

@Composable
fun WalletApp(walletViewModel: WalletViewModel = viewModel()) {
    val uiState by walletViewModel.uiState.collectAsStateWithLifecycle()

    McwTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            WalletScreen(
                state = uiState,
                onCreateWallet = walletViewModel::createAndSaveWallet,
                onLoadWallet = walletViewModel::loadSavedWallet,
            )
        }
    }
}

@Composable
private fun WalletScreen(
    state: WalletUiState,
    onCreateWallet: () -> Unit,
    onLoadWallet: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "MultiCurrencyWallet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Android MVP: create key pair, save 12-word seed phrase. Exchange is intentionally disabled.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCreateWallet) {
                Text("Create wallet")
            }
            OutlinedButton(onClick = onLoadWallet) {
                Text("Load saved")
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Private key",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (state.privateKeyHex.isBlank()) {
                    Text("No wallet generated yet")
                } else {
                    Text(
                        text = state.privateKeyHex,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Text(
                    text = "Seed phrase (12 words)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (state.mnemonicWords.isEmpty()) {
                    Text("Tap Create wallet to generate and store phrase")
                } else {
                    MnemonicGrid(words = state.mnemonicWords)
                }

                state.savedAt?.let { savedAt ->
                    Text(
                        text = "Saved: $savedAt",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                state.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE32950),
                    )
                }
            }
        }
    }
}

@Composable
private fun MnemonicGrid(words: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        words.chunked(3).forEachIndexed { rowIndex, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEachIndexed { columnIndex, word ->
                    val wordIndex = rowIndex * 3 + columnIndex + 1
                    WordCell(index = wordIndex, word = word)
                }
            }
        }
    }
}

@Composable
private fun WordCell(index: Int, word: String) {
    Column(
        modifier = Modifier
            .size(width = 100.dp, height = 56.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = word,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
