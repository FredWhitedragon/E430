package com.example.e430.app

import android.content.Context
import coil.imageLoader
import com.example.e430.account.data.AccountRepository
import com.example.e430.account.data.remote.AccountService
import com.example.e430.core.network.CredentialProvider
import com.example.e430.core.network.E621Network
import com.example.e430.core.network.E621Site
import com.example.e430.pools.data.PoolRepository
import com.example.e430.presets.data.PresetRepository
import com.example.e430.pools.data.remote.PoolService
import com.example.e430.posts.data.PostRepository
import com.example.e430.posts.data.remote.PostService
import com.example.e430.posts.detail.data.PostDetailRepository
import com.example.e430.posts.detail.data.MediaFileRepository
import com.example.e430.posts.detail.data.remote.PostDetailService
import com.example.e430.settings.data.SettingsRepository
import com.example.e430.core.storage.EncryptedCredentialStore
import com.example.e430.search.data.TagSuggestionRepository
import com.example.e430.search.data.remote.TagSuggestionService

class AppContainer(context: Context) {
    val imageLoader = context.imageLoader
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
    private val tagSuggestionServices = E621Site.entries.associateWith { site ->
        network.retrofit(site).create(TagSuggestionService::class.java)
    }

    val postRepository = PostRepository(postServices)
    val poolRepository = PoolRepository(poolServices, postRepository)
    val presetRepository = PresetRepository(context.applicationContext)
    val tagSuggestionRepository = TagSuggestionRepository(tagSuggestionServices)
    val settingsRepository = SettingsRepository(context.applicationContext)
    val postDetailRepository = PostDetailRepository(
        postDetailServices,
        context.applicationContext,
        imageLoader,
    )
    val mediaFileRepository = MediaFileRepository(network.mediaClient)
    val accountRepository = AccountRepository(
        services = accountServices,
        credentialProvider = credentialProvider,
        credentialStore = EncryptedCredentialStore(context.applicationContext),
    )
}
