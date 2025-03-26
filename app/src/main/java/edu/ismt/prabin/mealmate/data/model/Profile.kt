package edu.ismt.prabin.mealmate.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Profile(
    val id: String,
    val email: String,
    @SerialName("created_at")
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at")
    @Serializable(with = TimestampSerializer::class)
    val updatedAt: Long = System.currentTimeMillis(),
    val name: String
) 