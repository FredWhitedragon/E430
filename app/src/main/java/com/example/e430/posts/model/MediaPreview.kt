package com.example.e430.posts.model

enum class Rating(val code: String) {
    Safe("S"),
    Questionable("Q"),
    Explicit("E"),
}

data class MediaPreview(
    val id: Long,
    val previewUrl: String?,
    val width: Int,
    val height: Int,
    val score: Int,
    val favoriteCount: Int,
    val commentCount: Int,
    val rating: Rating,
    val fileExtension: String,
)

enum class PostFeed {
    Home,
    Latest,
    Popular,
    Favorites,
}

enum class HomeSort {
    Latest,
    Popular,
    Custom,
}
