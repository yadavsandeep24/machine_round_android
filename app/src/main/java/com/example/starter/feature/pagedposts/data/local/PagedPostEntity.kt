package com.example.starter.feature.pagedposts.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Own table so the Paging 3 sample is independent of the manual-pagination feature. */
@Entity(tableName = "paged_posts")
data class PagedPostEntity(@PrimaryKey val id: Int, val userId: Int, val title: String, val body: String)

/** Remembers which page RemoteMediator should fetch next. One row per list (label). */
@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val label: String,
    /** null = end of pagination reached. */
    val nextPage: Int?
)
