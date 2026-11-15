package com.example.starter.feature.comments.domain

enum class PetLabel { DOG, CAT }

data class Comment(
    val id: Int,
    /** null = top-level comment. */
    val parentId: Int?,
    val author: String,
    val text: String,
    val label: PetLabel,
    val score: Int
)

enum class Preference(val excluded: PetLabel?) {
    DOG_PERSON(excluded = PetLabel.CAT),
    CAT_PERSON(excluded = PetLabel.DOG),
    SHOW_ALL(excluded = null)
}
