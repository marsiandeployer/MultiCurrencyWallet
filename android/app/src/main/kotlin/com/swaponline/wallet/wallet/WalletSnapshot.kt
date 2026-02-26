package com.swaponline.wallet.wallet

data class WalletSnapshot(
    val mnemonicWords: List<String>,
    val privateKeyHex: String,
)
