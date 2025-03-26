package edu.ismt.prabin.mealmate.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Data class representing an item in the shopping list.
 */
@Serializable
data class ShoppingListItem(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("ingredient_id")
    val ingredientId: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    @SerialName("recipe_id")
    val recipeId: String = "",
    @SerialName("recipe_name")
    val recipeName: String = "",
    @SerialName("is_purchased")
    val isPurchased: Boolean = false
)