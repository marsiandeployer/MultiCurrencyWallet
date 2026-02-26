package com.swaponline.wallet.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletGeneratorTest {
    @Test
    fun generateWalletReturns12WordsAndPrivateKey() {
        val wallet = WalletGenerator.generateWallet()

        assertEquals(12, wallet.mnemonicWords.size)
        assertTrue(wallet.privateKeyHex.matches(Regex("^[0-9a-f]{64}$")))
    }
}
