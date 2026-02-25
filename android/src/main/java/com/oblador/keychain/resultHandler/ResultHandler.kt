package com.oblador.keychain.resultHandler

import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult
import java.security.Key
import javax.crypto.Cipher

/** Handler that allows injecting some actions during decrypt operations. */
interface ResultHandler {
  /** Ask user for interaction, often its unlock of keystore by biometric data providing. */
  fun askAccessPermissions(context: CryptoContext)

  /** Handle decryption result or error. */
  fun onDecrypt(decryptionResult: DecryptionResult?, error: Throwable?)

  /** Handle encryption result or error. */
  fun onEncrypt(encryptionResult: EncryptionResult?, error: Throwable?)

  /** Property to get reference to encryption results. */
  val encryptionResult: EncryptionResult?

  /** Property to get reference to decryption results. */
  val decryptionResult: DecryptionResult?

  /** Property to get reference to captured error. */
  val error: Throwable?

  /** Block thread and wait for any result of execution. */
  fun waitResult()
}

/**
 * Context for crypto operations that require user interaction.
 *
 * @param alias The key alias in the keystore
 * @param key The cryptographic key
 * @param password The password bytes to encrypt/decrypt
 * @param username The username bytes to encrypt/decrypt
 * @param operation The type of crypto operation (ENCRYPT or DECRYPT)
 * @param cipher Optional pre-initialized Cipher for CryptoObject binding. When provided,
 *               this cipher will be used with BiometricPrompt.CryptoObject to atomically
 *               bind the biometric authentication to the crypto operation, preventing
 *               "User not authenticated" errors caused by timing issues.
 */
data class CryptoContext(
  val alias: String,
  val key: Key,
  val password: ByteArray,
  val username: ByteArray,
  val operation: CryptoOperation,
  val cipher: Cipher? = null
)

enum class CryptoOperation {
  ENCRYPT,
  DECRYPT
}
