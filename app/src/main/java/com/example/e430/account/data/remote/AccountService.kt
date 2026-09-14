package com.example.e430.account.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface AccountService {
    @GET("users/me.json")
    suspend fun getCurrentUser(): CurrentUserDto

    @FormUrlEncoded
    @PATCH("users/{id}.json")
    suspend fun updateBlacklist(
        @Path("id") id: Long,
        @Field("user[blacklisted_tags]") blacklistedTags: String,
    )

    @GET("posts/{id}.json")
    suspend fun getAvatarPost(
        @Path("id") id: Long,
        @Query("v2") v2: Boolean = true,
        @Query("mode") mode: String = "thumbnail",
    ): AvatarPostDto
}

@Serializable
data class CurrentUserDto(
    val id: Long = 0,
    val name: String = "",
    @SerialName("level_string") val level: String = "",
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("post_upload_count") val uploadCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("avatar_id") val avatarId: Long? = null,
    @SerialName("blacklisted_tags") val blacklistedTags: String = "",
)

@Serializable
data class AvatarPostDto(
    @SerialName("preview_webp") val previewWebp: String? = null,
    @SerialName("preview_url") val previewUrl: String? = null,
)
