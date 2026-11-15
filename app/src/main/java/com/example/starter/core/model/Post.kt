package com.example.starter.core.model

/** Shared domain model (would live in :core:model). Pure Kotlin: no Android / Retrofit / Room annotations. */
data class Post(val id: Int, val userId: Int, val title: String, val body: String)
