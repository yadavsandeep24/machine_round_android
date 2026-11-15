package com.example.starter.feature.search.domain

/** A stock asset (image/illustration) returned by search. Pure Kotlin. */
data class Asset(val id: Int, val title: String, val author: String, val tags: List<String>)

data class AssetPage(val items: List<Asset>, val endReached: Boolean)
