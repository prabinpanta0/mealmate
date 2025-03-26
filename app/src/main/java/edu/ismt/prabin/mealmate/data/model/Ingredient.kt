package edu.ismt.prabin.mealmate.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Data class representing an ingredient in a recipe.
 */
@Serializable
data class Ingredient(
    val id: String = "",
    @SerialName("recipe_id")
    val recipeId: String = "",
    val name: String = "",
    val category: String = "",
    @SerialName("created_at")
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = System.currentTimeMillis()
)