package com.example.e430.app

import android.content.Context
import com.example.e430.account.data.AccountRepository
import com.example.e430.account.data.remote.AccountService
import com.example.e430.core.network.CredentialProvider
import com.example.e430.core.network.E621Network
import com.example.e430.core.network.E621Site
import com.example.e430.pools.data.PoolRepository
import com.example.e430.pools.data.remote.PoolService
import com.example.e430.posts.data.PostRepository
import com.example.e430.posts.data.remote.PostService
import com.example.e430.posts.detail.data.PostDetailRepository
import com.example.e430.posts.detail.data.MediaFileRepository
import com.example.e430.posts.detail.data.remote.PostDetailService
import com.example.e430.settings.data.SettingsRepository
import com.example.e430.core.storage.EncryptedCredentialStore

class AppContainer(context: Context) {
    private val credentialProvider = CredentialProvider()
    private val network = E621Network(credentialProvider)
    private val postServices = E621Site.entries.associateWith { site ->
        network.retrofit(site).create(PostService::class.java)
    }
    private val poolServices = E621Site.entries.associateWith { site ->
        network.retrofit(site).create(PoolService::class.java)
    }
    private val accountServices = E621Site.entries.associateWith { site ->
        network.retrofit(site).create(AccountService::class.java)
    }
    private val postDetailServices = E621Site.entries.associateWith { site ->
        network.retrofit(site).create(PostDetailService::class.java)
    }

    val postRepository = PostRepository(postServices)
    val poolRepository = PoolRepository(poolServices, postRepository)
    val settingsRepository = SettingsRepository(context.applicationContext)
    val postDetailRepository = PostDetailRepository(postDetailServices)
    val mediaFileRepository = MediaFileRepository(network.mediaClient)
    val accountRepository = AccountRepository(
        services = accountServices,
        credentialProvider = credentialProvider,
        credentialStore = EncryptedCredentialStore(context.applicationContext),
    )
}
