package de.dingeldangbang.agentmobile.security

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypted endpoint/token storage backed directly by Android Keystore. */
data class CloudCredentials(val endpoint: String, val token: String)

object EndpointNormalizer {
    fun normalize(raw: String): String {
        val value = raw.trim().trimEnd('/')
        require(value.isNotBlank()) { "Endpoint darf nicht leer sein." }
        val uri = Uri.parse(value)
        require(uri.scheme.equals("https", ignoreCase = true)) {
            "Nur HTTPS-Endpunkte sind erlaubt."
        }
        require(!uri.host.isNullOrBlank()) { "Der Endpoint muss einen Host enthalten." }
        return "$value/"
    }
}

class CredentialStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun save(endpoint: String, token: String) {
        val normalized = EndpointNormalizer.normalize(endpoint)
        require(token.trim().isNotEmpty()) { "Token darf nicht leer sein." }
        val plaintext = "$normalized\n${token.trim()}".toByteArray(StandardCharsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext)
        val packed = ByteArray(cipher.iv.size + ciphertext.size)
        cipher.iv.copyInto(packed)
        ciphertext.copyInto(packed, cipher.iv.size)
        prefs.edit().putString(KEY_BLOB, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun load(): CloudCredentials? {
        val encoded = prefs.getString(KEY_BLOB, null) ?: return null
        return runCatching {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            require(packed.size > IV_BYTES)
            val iv = packed.copyOfRange(0, IV_BYTES)
            val ciphertext = packed.copyOfRange(IV_BYTES, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            val values = cipher.doFinal(ciphertext).toString(StandardCharsets.UTF_8).split('\n', limit = 2)
            require(values.size == 2)
            CloudCredentials(values[0], values[1])
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().remove(KEY_BLOB).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES = "agent_mobile_secure_preferences"
        const val KEY_BLOB = "cloud_credentials"
        const val KEY_ALIAS = "agent_mobile_cloud_credentials"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
