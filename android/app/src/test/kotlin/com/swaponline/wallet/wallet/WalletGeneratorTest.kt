package com.swaponline.wallet.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletGeneratorTest {
    @Test
    fun generateWalletReturns12WordsAndPrivateKey() {
        val wallet = WalletGenerator.generateWallet()

        assertEquals(12, wallet.mnemonicWords.size)
        assertTrue(wallet.privateKeyHex.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun importWalletProducesStablePrivateKey() {
        val phrase = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

        val walletA = WalletGenerator.importWallet(phrase)
        val walletB = WalletGenerator.importWallet(phrase.uppercase())

        assertEquals(walletA.privateKeyHex, walletB.privateKeyHex)
        assertEquals(12, walletA.mnemonicWords.size)
    }

    @Test
    fun importWalletRejectsWrongWordCount() {
        assertThrows(IllegalArgumentException::class.java) {
            WalletGenerator.importWallet("abandon abandon abandon")
        }
    }
}
