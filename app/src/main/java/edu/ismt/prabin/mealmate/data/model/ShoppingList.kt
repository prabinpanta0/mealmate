package edu.ismt.prabin.mealmate.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.datetime.Instant

@Serializable
data class ShoppingList(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("list_name")
    val listName: String = "",
    val completed: Boolean = false,
    @SerialName("created_at")
    val createdAt: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    @SerialName("created_by")
    val createdBy: String = "",
    @SerialName("items")
    val items: List<ShoppingListItem> = emptyList()
) 