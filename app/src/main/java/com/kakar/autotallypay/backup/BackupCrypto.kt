package com.kakar.autotallypay.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-based encrypted backup: AES-256-GCM with a PBKDF2-HmacSHA256
 * derived key (120,000 iterations). Fully offline; the user holds the
 * password and the file. Format: "ATB1" + salt(16) + iv(12) + ciphertext.
 * Pure JVM crypto — no Android dependency.
 */
object BackupCrypto {

    private const val MAGIC = "ATB1"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128

    fun isBackupFile(data: ByteArray): Boolean =
        data.size > MAGIC.length &&
            data.copyOfRange(0, MAGIC.length).toString(Charsets.UTF_8) == MAGIC

    fun encrypt(password: String, plaintext: ByteArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
        val ct = cipher.doFinal(plaintext)
        return MAGIC.toByteArray(Charsets.UTF_8) + salt + iv + ct
    }

    /** Throws on a wrong password or a corrupt file (AEAD tag mismatch). */
    fun decrypt(password: String, data: ByteArray): ByteArray {
        require(isBackupFile(data)) { "Not an AutoTally backup file" }
        val salt = data.copyOfRange(4, 4 + SALT_LEN)
        val iv = data.copyOfRange(4 + SALT_LEN, 4 + SALT_LEN + IV_LEN)
        val ct = data.copyOfRange(4 + SALT_LEN + IV_LEN, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
