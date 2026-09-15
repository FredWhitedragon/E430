package com.example.e430.pools.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PoolService {
    @GET("pools/{id}.json")
    suspend fun getPool(@Path("id") id: Long): PoolDto

    @GET("pools.json")
    suspend fun getPools(
        @Query("search[order]") order: String = "id_desc",
        @Query("search[name_matches]") nameMatches: String? = null,
        @Query("limit") limit: Int,
        @Query("page") page: Int = 1,
    ): List<PoolDto>
}

@Serializable
data class PoolDto(
    val id: Long,
    val name: String,
    val description: String = "",
    @SerialName("post_ids") val postIds: List<Long> = emptyList(),
    @SerialName("post_count") val postCount: Int = postIds.size,
)
