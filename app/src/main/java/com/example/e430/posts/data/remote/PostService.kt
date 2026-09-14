package com.example.e430.posts.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface PostService {
    @GET("posts.json")
    suspend fun getPosts(
        @Query("tags") tags: String,
        @Query("limit") limit: Int,
        @Query("page") page: Int = 1,
        @Query("v2") v2: Boolean = true,
        @Query("mode") mode: String = "thumbnail",
    ): List<PostThumbnailDto>

    @GET("popular.json")
    suspend fun getPopular(
        @Query("scale") scale: String = "month",
        @Query("v2") v2: Boolean = true,
        @Query("mode") mode: String = "thumbnail",
    ): List<PostThumbnailDto>

    @GET("favorites.json")
    suspend fun getFavorites(
        @Query("limit") limit: Int,
        @Query("page") page: Int = 1,
        @Query("v2") v2: Boolean = true,
        @Query("mode") mode: String = "thumbnail",
    ): List<PostThumbnailDto>
}

@Serializable
data class PostThumbnailDto(
    val id: Long,
    @SerialName("preview_url") val previewUrl: String? = null,
    @SerialName("preview_webp") val previewWebp: String? = null,
    @SerialName("preview_width") val previewWidth: Int = 1,
    @SerialName("preview_height") val previewHeight: Int = 1,
    val score: Int = 0,
    @SerialName("fav_count") val favoriteCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    val rating: String = "s",
    val tags: String = "",
    @SerialName("uploader_id") val uploaderId: Long = 0,
    val flags: String = "",
)
