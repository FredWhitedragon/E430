package com.example.e430.posts.detail.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PostDetailService {
    @GET("posts/{id}.json")
    suspend fun getPost(@Path("id") id: Long): PostDetailResponseDto

    @POST("posts/{id}/votes.json")
    suspend fun vote(@Path("id") id: Long, @Query("score") score: Int): VoteResponseDto

    @DELETE("posts/{id}/votes.json")
    suspend fun removeVote(@Path("id") id: Long)

    @FormUrlEncoded
    @POST("favorites.json")
    suspend fun addFavorite(@Field("post_id") postId: Long): FavoriteResponseDto

    @DELETE("favorites/{id}.json")
    suspend fun removeFavorite(@Path("id") postId: Long): FavoriteResponseDto

    @GET("comments.json")
    suspend fun getComments(
        @Query("group_by") groupBy: String = "comment",
        @Query("search[post_id]") postId: Long,
        @Query("search[order]") order: String = "id_asc",
        @Query("limit") limit: Int = 100,
    ): List<CommentDto>

    @FormUrlEncoded
    @POST("comments.json")
    suspend fun createComment(
        @Field("comment[post_id]") postId: Long,
        @Field("comment[body]") body: String,
    ): CommentDto

    @FormUrlEncoded
    @PATCH("comments/{id}.json")
    suspend fun updateComment(
        @Path("id") id: Long,
        @Field("comment[body]") body: String,
    )

    @POST("comments/{id}/hide.json")
    suspend fun hideComment(@Path("id") id: Long): CommentDto
}

@Serializable data class PostDetailResponseDto(val post: PostDetailDto)
@Serializable data class PostDetailDto(
    val id: Long,
    val file: FileDto,
    val preview: PreviewDto,
    val sample: SampleDto,
    val score: ScoreDto,
    val tags: TagsDto,
    val rating: String,
    @SerialName("fav_count") val favoriteCount: Int,
    val sources: List<String> = emptyList(),
    val pools: List<Long> = emptyList(),
    @SerialName("uploader_id") val uploaderId: Long = 0,
    @SerialName("uploader_name") val uploaderName: String = "",
    val description: String = "",
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("is_favorited") val isFavorited: Boolean = false,
    val vote: Int = 0,
    val duration: Float? = null,
    @SerialName("has_notes") val hasNotes: Boolean = false,
    val relationships: RelationshipsDto = RelationshipsDto(),
    val flags: FlagsDto = FlagsDto(),
    @SerialName("created_at") val createdAt: String = "",
)
@Serializable data class FileDto(
    val width: Int = 1,
    val height: Int = 1,
    val ext: String = "",
    val size: Long = 0,
    val url: String? = null,
    val md5: String = "",
)
@Serializable data class PreviewDto(val width: Int = 1, val height: Int = 1, val url: String? = null)
@Serializable data class SampleDto(
    val width: Int? = null,
    val height: Int? = null,
    val url: String? = null,
    val alternates: SampleAlternatesDto = SampleAlternatesDto(),
)
@Serializable data class SampleAlternatesDto(
    val original: AlternateDto? = null,
    val variants: Map<String, AlternateDto> = emptyMap(),
    val samples: Map<String, AlternateDto> = emptyMap(),
)
@Serializable data class AlternateDto(
    val size: Long = 0,
    val width: Int = 1,
    val height: Int = 1,
    val url: String = "",
)
@Serializable data class ScoreDto(val up: Int = 0, val down: Int = 0, val total: Int = 0)
@Serializable data class RelationshipsDto(
    @SerialName("parent_id") val parentId: Long? = null,
    val children: List<Long> = emptyList(),
)
@Serializable data class FlagsDto(
    val pending: Boolean = false,
    val flagged: Boolean = false,
    @SerialName("note_locked") val noteLocked: Boolean = false,
    @SerialName("status_locked") val statusLocked: Boolean = false,
    @SerialName("rating_locked") val ratingLocked: Boolean = false,
    val deleted: Boolean = false,
)
@Serializable data class TagsDto(
    val artist: List<String> = emptyList(),
    val copyright: List<String> = emptyList(),
    val character: List<String> = emptyList(),
    val species: List<String> = emptyList(),
    val general: List<String> = emptyList(),
    val meta: List<String> = emptyList(),
    val lore: List<String> = emptyList(),
    val contributor: List<String> = emptyList(),
    val invalid: List<String> = emptyList(),
)
@Serializable data class VoteResponseDto(
    val score: Int,
    val up: Int,
    val down: Int,
    @SerialName("our_score") val ourScore: Int,
)
@Serializable data class FavoriteResponseDto(
    @SerialName("post_id") val postId: Long,
    @SerialName("favorite_count") val favoriteCount: Int,
)
@Serializable data class CommentDto(
    val id: Long,
    @SerialName("creator_id") val creatorId: Long = 0,
    @SerialName("creator_name") val creatorName: String = "",
    val body: String = "",
    val score: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("is_hidden") val isHidden: Boolean = false,
)
