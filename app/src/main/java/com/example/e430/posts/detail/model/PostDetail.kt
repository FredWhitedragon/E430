package com.example.e430.posts.detail.model

import com.example.e430.posts.model.Rating

enum class MediaKind { Image, Gif, Video }
enum class MediaQuality { Low, Medium, Original }

data class MediaSource(
    val quality: MediaQuality,
    val url: String,
    val estimatedBytes: Long,
)

data class PostDetail(
    val id: Long,
    val kind: MediaKind,
    val extension: String,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val md5: String,
    val durationSeconds: Float?,
    val score: Int,
    val upScore: Int,
    val downScore: Int,
    val favoriteCount: Int,
    val rating: Rating,
    val commentCount: Int,
    val uploaderName: String,
    val uploaderId: Long,
    val createdAt: String,
    val description: String,
    val sources: List<String>,
    val pools: List<Long>,
    val parentId: Long?,
    val childIds: List<Long>,
    val hasNotes: Boolean,
    val statusFlags: List<String>,
    val tags: Map<String, List<String>>,
    val mediaSources: List<MediaSource>,
    val userVote: Int,
    val isFavorited: Boolean,
)

data class PostComment(
    val id: Long,
    val creatorId: Long,
    val creatorName: String,
    val body: String,
    val score: Int,
    val createdAt: String,
    val isHidden: Boolean,
)
