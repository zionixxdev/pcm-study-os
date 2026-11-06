package dev.zionixx.pcmstudyos.domain

/** One global-search hit. `route` is the app destination to open. */
data class SearchResult(
    val kind: String,
    val title: String,
    val subtitle: String?,
    val route: String,
)
