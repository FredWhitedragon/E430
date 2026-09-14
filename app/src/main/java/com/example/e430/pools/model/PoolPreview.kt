package com.example.e430.pools.model

data class PoolPreview(
    val id: Long,
    val name: String,
    val postCount: Int,
    val coverUrl: String?,
    val coverWidth: Int,
    val coverHeight: Int,
)
