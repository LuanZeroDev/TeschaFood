package com.tescha.food.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VendorRequestDto(
    val id: String? = null,
    @SerialName("user_id")     val userId: String,
    val status: String = "pendiente",
    @SerialName("doc_url")     val docUrl: String,
    @SerialName("doc_format")  val docFormat: String,
    @SerialName("doc_size_kb") val docSizeKb: Int,
    @SerialName("created_at")  val createdAt: String? = null,
)

@Serializable
data class VendorRequestWithUser(
    val id: String,
    @SerialName("user_id")     val userId: String,
    val status: String,
    @SerialName("doc_url")     val docUrl: String,
    @SerialName("doc_format")  val docFormat: String,
    @SerialName("doc_size_kb") val docSizeKb: Int,
    @SerialName("created_at")  val createdAt: String? = null,
    val users: UserDto? = null,
)
