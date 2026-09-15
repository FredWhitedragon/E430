package com.example.e430.posts.data

import com.example.e430.core.network.E621Site
import com.example.e430.posts.data.remote.PostService
import com.example.e430.posts.data.remote.PostThumbnailDto
import com.example.e430.posts.model.HomeSort
import com.example.e430.posts.model.MediaPreview
import com.example.e430.posts.model.PostFeed
import com.example.e430.posts.model.Rating

class PostRepository(
    private val services: Map<E621Site, PostService>,
) {
    suspend fun getPosts(
        site: E621Site,
        feed: PostFeed,
        homeSort: HomeSort,
        query: String,
        page: Int,
        blacklist: String = "",
    ): PostPage {
        val service = requireNotNull(services[site])
        val matcher = PostBlacklist(blacklist)
        if (feed == PostFeed.Popular) {
            val items = service.getPopular()
            return PostPage(
                items = items.filterNot(matcher::blocks).map(PostThumbnailDto::toModel).filter { it.previewUrl != null },
                endReached = true,
            )
        }
        if (feed == PostFeed.Favorites) {
            val response = service.getFavorites(limit = PAGE_SIZE, page = page)
            return PostPage(
                items = response.filterNot(matcher::blocks).map(PostThumbnailDto::toModel).filter { it.previewUrl != null },
                endReached = response.size < PAGE_SIZE,
            )
        }
        val tags = buildFeedTags(feed, homeSort, query)
        val response = service.getPosts(tags = tags, limit = PAGE_SIZE, page = page)
        return PostPage(
            items = response.filterNot(matcher::blocks).map(PostThumbnailDto::toModel).filter { it.previewUrl != null },
            endReached = response.size < PAGE_SIZE,
        )
    }

    suspend fun getPostsByIds(site: E621Site, ids: List<Long>): Map<Long, MediaPreview> {
        if (ids.isEmpty()) return emptyMap()
        return requireNotNull(services[site])
            .getPosts(
                tags = "id:${ids.distinct().joinToString(",")}",
                limit = ids.distinct().size.coerceAtMost(320),
            )
            .filter { it.previewWebp != null || it.previewUrl != null }
            .associate { it.id to it.toModel() }
    }

    internal fun buildTags(query: String, order: String?): String {
        if (order == null) return query.trim()
        val userTags = query
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && !it.startsWith("order:", ignoreCase = true) }
        return (userTags + order).joinToString(" ")
    }

    internal fun buildFeedTags(feed: PostFeed, homeSort: HomeSort, query: String): String {
        val order = when (feed) {
            PostFeed.Home -> when (homeSort) {
                HomeSort.Latest -> "order:id_desc"
                HomeSort.Popular -> "order:score"
                HomeSort.Custom -> null
            }
            PostFeed.Latest -> "order:id_desc"
            PostFeed.Popular -> error("Popular uses the dedicated endpoint")
            PostFeed.Favorites -> error("Favorites uses the dedicated endpoint")
        }
        return buildTags(if (feed == PostFeed.Home) query else "", order)
    }

    companion object {
        const val PAGE_SIZE = 40
    }
}

data class PostPage(
    val items: List<MediaPreview>,
    val endReached: Boolean,
)

private fun PostThumbnailDto.toModel() = MediaPreview(
    id = id,
    previewUrl = previewWebp ?: previewUrl,
    width = previewWidth.coerceAtLeast(1),
    height = previewHeight.coerceAtLeast(1),
    score = score,
    favoriteCount = favoriteCount,
    commentCount = commentCount,
    rating = when (rating.lowercase()) {
        "q" -> Rating.Questionable
        "e" -> Rating.Explicit
        else -> Rating.Safe
    },
    fileExtension = fileExtension,
)
