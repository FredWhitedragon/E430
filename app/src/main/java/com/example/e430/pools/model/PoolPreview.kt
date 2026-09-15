package com.example.e430.pools.model

data class PoolPreview(
    val id: Long,
    val name: String,
    val postCount: Int,
    val coverUrl: String?,
    val coverWidth: Int,
    val coverHeight: Int,
)

data class PoolDetail(
    val id: Long,
    val name: String,
    val description: String,
    val postIds: List<Long>,
)
