package com.example.e430.search.data

import com.example.e430.core.network.E621Site
import com.example.e430.search.data.remote.TagSuggestionService
import com.example.e430.search.model.TagSuggestion

class TagSuggestionRepository(
    private val services: Map<E621Site, TagSuggestionService>,
) {
    suspend fun suggest(site: E621Site, prefix: String): List<TagSuggestion> =
        requireNotNull(services[site])
            .getTags(nameMatches = "${prefix.lowercase()}*")
            .take(SUGGESTION_LIMIT)
            .map { tag ->
                TagSuggestion(
                    name = tag.name,
                    category = tag.category,
                    postCount = tag.postCount,
                )
            }

    private companion object {
        const val SUGGESTION_LIMIT = 5
    }
}
