package com.example.e430.account.data

import com.example.e430.account.data.remote.AccountService
import com.example.e430.account.data.remote.CurrentUserDto
import com.example.e430.account.model.Account
import com.example.e430.core.network.ApiCredentials
import com.example.e430.core.network.CredentialProvider
import com.example.e430.core.network.E621Site
import com.example.e430.core.storage.EncryptedCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountRepository(
    private val services: Map<E621Site, AccountService>,
    private val credentialProvider: CredentialProvider,
    private val credentialStore: EncryptedCredentialStore,
) {
    suspend fun restore(): Account? {
        val credentials = withContext(Dispatchers.IO) { credentialStore.read() } ?: return null
        credentialProvider.set(credentials)
        return runCatching { loadAccount(credentials) }
            .onFailure { credentialProvider.clear() }
            .getOrThrow()
    }

    suspend fun login(site: E621Site, username: String, apiKey: String): Account {
        val credentials = ApiCredentials(
            site = site,
            username = username.trim(),
            apiKey = apiKey.trim(),
        )
        credentialProvider.set(credentials)
        return try {
            val account = loadAccount(credentials)
            withContext(Dispatchers.IO) { credentialStore.write(credentials) }
            account
        } catch (error: Exception) {
            credentialProvider.clear()
            throw error
        }
    }

    suspend fun logout() {
        credentialProvider.clear()
        withContext(Dispatchers.IO) { credentialStore.clear() }
    }

    suspend fun updateBlacklist(account: Account, blacklistedTags: String): Account {
        requireNotNull(services[account.site]).updateBlacklist(account.id, blacklistedTags)
        return account.copy(blacklistedTags = blacklistedTags)
    }

    private suspend fun loadAccount(credentials: ApiCredentials): Account {
        val service = requireNotNull(services[credentials.site])
        val user = service.getCurrentUser()
        check(
            user.id > 0 && user.name.equals(credentials.username, ignoreCase = true),
        ) { "Authenticated user did not match the supplied username" }
        val avatarUrl = user.avatarId?.let { avatarId ->
            runCatching { service.getAvatarPost(avatarId) }
                .getOrNull()
                ?.let { it.previewWebp ?: it.previewUrl }
        }
        return user.toModel(credentials.site, avatarUrl)
    }
}

private fun CurrentUserDto.toModel(site: E621Site, avatarUrl: String?) = Account(
    id = id,
    username = name,
    site = site,
    level = level,
    favoriteCount = favoriteCount,
    uploadCount = uploadCount,
    createdAt = createdAt,
    avatarUrl = avatarUrl,
    blacklistedTags = blacklistedTags,
)
