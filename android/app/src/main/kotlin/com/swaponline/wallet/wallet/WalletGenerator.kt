package com.swaponline.wallet.wallet

import java.security.SecureRandom
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode

object WalletGenerator {
    fun generateWallet(): WalletSnapshot {
        val entropy = ByteArray(16)
        SecureRandom().nextBytes(entropy)

        val mnemonicWords = MnemonicCode.INSTANCE.toMnemonic(entropy)
        val seed = MnemonicCode.toSeed(mnemonicWords, "")
        val masterKey = HDKeyDerivation.createMasterPrivateKey(seed)
        val privateKeyHex = masterKey.privKey.toString(16).padStart(64, 0)

        return WalletSnapshot(
            mnemonicWords = mnemonicWords,
            privateKeyHex = privateKeyHex,
        )
    }
}
