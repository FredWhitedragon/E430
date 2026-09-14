package com.example.e430.posts.detail.data

import com.example.e430.core.network.E621Site
import com.example.e430.posts.detail.data.remote.CommentDto
import com.example.e430.posts.detail.data.remote.PostDetailDto
import com.example.e430.posts.detail.data.remote.PostDetailService
import com.example.e430.posts.detail.model.MediaKind
import com.example.e430.posts.detail.model.MediaQuality
import com.example.e430.posts.detail.model.MediaSource
import com.example.e430.posts.detail.model.PostComment
import com.example.e430.posts.detail.model.PostDetail
import com.example.e430.posts.model.Rating

class PostDetailRepository(private val services: Map<E621Site, PostDetailService>) {
    suspend fun getPost(site: E621Site, id: Long): PostDetail = service(site).getPost(id).post.toModel()
    suspend fun getComments(site: E621Site, postId: Long) = service(site).getComments(postId = postId).map(CommentDto::toModel)
    suspend fun createComment(site: E621Site, postId: Long, body: String) = service(site).createComment(postId, body).toModel()
    suspend fun updateComment(site: E621Site, id: Long, body: String) = service(site).updateComment(id, body)
    suspend fun hideComment(site: E621Site, id: Long) = service(site).hideComment(id).toModel()
    suspend fun vote(site: E621Site, id: Long, score: Int) = service(site).vote(id, score)
    suspend fun removeVote(site: E621Site, id: Long) = service(site).removeVote(id)
    suspend fun addFavorite(site: E621Site, id: Long) = service(site).addFavorite(id)
    suspend fun removeFavorite(site: E621Site, id: Long) = service(site).removeFavorite(id)
    private fun service(site: E621Site) = requireNotNull(services[site])
}

private fun PostDetailDto.toModel(): PostDetail {
    val kind = when (file.ext.lowercase()) {
        "webm", "mp4" -> MediaKind.Video
        "gif" -> MediaKind.Gif
        else -> MediaKind.Image
    }
    val original = file.url?.let { MediaSource(MediaQuality.Original, it, file.size) }
    val mediumAlternate = sample.alternates.samples["720p"]
        ?: sample.alternates.samples["480p"]
        ?: sample.alternates.variants["mp4"]
    val mediumUrl = mediumAlternate?.url?.takeIf(String::isNotBlank) ?: sample.url
    val mediumSize = mediumAlternate?.size?.takeIf { it > 0 }
        ?: estimateSize(file.size, file.width, file.height, sample.width, sample.height)
    val lowSize = estimateSize(file.size, file.width, file.height, preview.width, preview.height)
    val mediaSources = listOfNotNull(
        preview.url?.let { MediaSource(MediaQuality.Low, it, lowSize) },
        mediumUrl?.let { MediaSource(MediaQuality.Medium, it, mediumSize) },
        original,
    ).distinctBy { it.url }
    return PostDetail(
        id = id,
        kind = kind,
        extension = file.ext,
        width = file.width,
        height = file.height,
        fileSize = file.size,
        md5 = file.md5,
        durationSeconds = duration,
        score = score.total,
        upScore = score.up,
        downScore = score.down,
        favoriteCount = favoriteCount,
        rating = when (rating.lowercase()) { "q" -> Rating.Questionable; "e" -> Rating.Explicit; else -> Rating.Safe },
        commentCount = commentCount,
        uploaderName = uploaderName,
        uploaderId = uploaderId,
        createdAt = createdAt,
        description = description,
        sources = sources,
        pools = pools,
        parentId = relationships.parentId,
        childIds = relationships.children,
        hasNotes = hasNotes,
        statusFlags = buildList {
            if (flags.pending) add("pending")
            if (flags.flagged) add("flagged")
            if (flags.noteLocked) add("note_locked")
            if (flags.statusLocked) add("status_locked")
            if (flags.ratingLocked) add("rating_locked")
            if (flags.deleted) add("deleted")
        },
        tags = linkedMapOf(
            "artist" to tags.artist, "copyright" to tags.copyright, "character" to tags.character,
            "species" to tags.species, "general" to tags.general, "meta" to tags.meta,
            "lore" to tags.lore, "contributor" to tags.contributor, "invalid" to tags.invalid,
        ).filterValues(List<String>::isNotEmpty),
        mediaSources = mediaSources,
        userVote = vote,
        isFavorited = isFavorited,
    )
}

private fun estimateSize(original: Long, originalWidth: Int, originalHeight: Int, width: Int?, height: Int?): Long {
    if (original <= 0 || originalWidth <= 0 || originalHeight <= 0 || width == null || height == null) return 0
    return (original * (width.toDouble() * height) / (originalWidth.toDouble() * originalHeight)).toLong().coerceAtLeast(1)
}

private fun CommentDto.toModel() = PostComment(id, creatorId, creatorName, body, score, createdAt, isHidden)
