package com.example.e430.search.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface TagSuggestionService {
    @GET("tags.json")
    suspend fun getTags(
        @Query("search[name_matches]") nameMatches: String,
        @Query("search[order]") order: String = "count",
        @Query("search[hide_empty]") hideEmpty: Boolean = true,
        @Query("limit") limit: Int = 5,
    ): List<TagSuggestionDto>
}

@Serializable
data class TagSuggestionDto(
    val name: String,
    val category: Int,
    @SerialName("post_count") val postCount: Int,
)
