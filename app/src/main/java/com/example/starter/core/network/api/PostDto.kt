package com.example.starter.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostDto(
    @SerialName("id") val id: Int,
    @SerialName("userId") val userId: Int = 0,
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = ""
)
