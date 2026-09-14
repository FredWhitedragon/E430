package com.example.e430.core.storage

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.example.e430.core.network.ApiCredentials
import com.example.e430.core.network.E621Site
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedCredentialStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun read(): ApiCredentials? {
        val encrypted = preferences.getString(ENCRYPTED_VALUE, null) ?: return null
        val initializationVector = preferences.getString(INITIALIZATION_VECTOR, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH, Base64.decode(initializationVector, Base64.NO_WRAP)),
            )
            val plaintext = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
            json.decodeFromString<StoredCredentials>(plaintext.decodeToString()).toApiCredentials()
        }.getOrElse {
            resetAfterReadFailure()
            null
        }
    }

    @SuppressLint("ApplySharedPref")
    fun write(credentials: ApiCredentials) {
        val plaintext = json.encodeToString(StoredCredentials.from(credentials)).encodeToByteArray()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plaintext)
        preferences.edit(commit = true) {
            putString(ENCRYPTED_VALUE, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            putString(
                INITIALIZATION_VECTOR,
                Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            )
        }
    }

    @SuppressLint("ApplySharedPref")
    fun clear() {
        preferences.edit(commit = true) { clear() }
    }

    @SuppressLint("ApplySharedPref")
    private fun resetAfterReadFailure() {
        preferences.edit(commit = true) { clear() }
        runCatching {
            KeyStore.getInstance(ANDROID_KEY_STORE).apply {
                load(null)
                deleteEntry(KEY_ALIAS)
            }
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    @Serializable
    private data class StoredCredentials(
        val site: String,
        val username: String,
        val apiKey: String,
    ) {
        fun toApiCredentials() = ApiCredentials(
            site = E621Site.entries.firstOrNull { it.name == site } ?: E621Site.E621,
            username = username,
            apiKey = apiKey,
        )

        companion object {
            fun from(credentials: ApiCredentials) = StoredCredentials(
                site = credentials.site.name,
                username = credentials.username,
                apiKey = credentials.apiKey,
            )
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "encrypted_credentials"
        const val ENCRYPTED_VALUE = "encrypted_value"
        const val INITIALIZATION_VECTOR = "initialization_vector"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "e430_account_credentials"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH = 128
    }
}
