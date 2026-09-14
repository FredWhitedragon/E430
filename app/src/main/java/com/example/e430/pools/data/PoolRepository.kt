package com.example.e430.pools.data

import com.example.e430.core.network.E621Site
import com.example.e430.pools.data.remote.PoolService
import com.example.e430.pools.model.PoolPreview
import com.example.e430.posts.data.PostRepository

class PoolRepository(
    private val services: Map<E621Site, PoolService>,
    private val postRepository: PostRepository,
) {
    suspend fun getPools(site: E621Site, query: String, page: Int): PoolPage {
        val pools = requireNotNull(services[site]).getPools(
            nameMatches = query.trim().ifEmpty { null },
            limit = PAGE_SIZE,
            page = page,
        )
        val coverIds = pools.mapNotNull { it.postIds.firstOrNull() }
        val covers = postRepository.getPostsByIds(site, coverIds)
        val items = pools.mapNotNull { pool ->
            val cover = pool.postIds.firstOrNull()?.let(covers::get)
            if (cover?.previewUrl == null) return@mapNotNull null
            PoolPreview(
                id = pool.id,
                name = pool.name,
                postCount = pool.postCount,
                coverUrl = cover?.previewUrl,
                coverWidth = cover?.width ?: 1,
                coverHeight = cover?.height ?: 1,
            )
        }
        return PoolPage(items = items, endReached = pools.size < PAGE_SIZE)
    }

    companion object {
        const val PAGE_SIZE = 24
    }
}

data class PoolPage(
    val items: List<PoolPreview>,
    val endReached: Boolean,
)
