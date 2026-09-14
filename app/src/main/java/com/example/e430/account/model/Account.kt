package com.example.e430.account.model

import com.example.e430.core.network.E621Site

data class Account(
    val id: Long,
    val username: String,
    val site: E621Site,
    val level: String,
    val favoriteCount: Int,
    val uploadCount: Int,
    val createdAt: String,
    val avatarUrl: String?,
    val blacklistedTags: String,
)
