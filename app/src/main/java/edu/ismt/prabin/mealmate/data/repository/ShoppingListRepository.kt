package edu.ismt.prabin.mealmate.data.repository

import android.util.Log
import edu.ismt.prabin.mealmate.data.model.Ingredient
import edu.ismt.prabin.mealmate.data.model.Recipe
import edu.ismt.prabin.mealmate.data.model.ShoppingListItem
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository class for handling shopping list operations with Supabase.
 */
object ShoppingListRepository {
    private const val TAG = "ShoppingListRepository"
    private const val SHOPPING_LIST_TABLE = "shopping_list"
    
    /**
     * Get all shopping list items for the current user
     * @return Result containing list of shopping list items on success or Exception on failure
     */
    suspend fun getShoppingList(): Result<List<ShoppingListItem>> {
        return try {
            val userId = SupabaseClient.getCurrentUserId() ?: throw Exception("User not authenticated")
            val items = SupabaseClient.supabase.postgrest[SHOPPING_LIST_TABLE]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<ShoppingListItem>()
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting shopping list: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Add a single item to the shopping list
     * @param item The shopping list item to add
     * @return Result containing the created item ID on success or Exception on failure
     */
    suspend fun addShoppingListItem(item: ShoppingListItem): Result<Unit> {
        return try {
            val userId = SupabaseClient.getCurrentUserId() ?: throw Exception("User not authenticated")
            
            // Ensure the user ID is set
            val itemWithUserId = if (item.userId.isBlank()) {
                item.copy(userId = userId)
            } else {
                item
            }
            
            SupabaseClient.supabase.postgrest[SHOPPING_LIST_TABLE]
                .insert(itemWithUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding shopping list item: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing shopping list item
     * @param item The updated shopping list item
     * @return Result containing success or failure
     */
    suspend fun updateShoppingListItem(item: ShoppingListItem): Result<Unit> {
        return try {
            SupabaseClient.supabase.postgrest[SHOPPING_LIST_TABLE]
                .update({
                    set("name", item.name)
                    set("quantity", item.quantity)
                    set("unit", item.unit)
                    set("is_purchased", item.isPurchased)
                }) {
                    filter { eq("id", item.id) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating shopping list item: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Generate shopping list items from a recipe and add them to the shopping list
     * @param recipe The recipe to generate shopping list items from
     * @return Result containing success or failure
     */
    suspend fun addRecipeToShoppingList(recipe: Recipe): Result<Unit> {
        return try {
            val userId = SupabaseClient.getCurrentUserId() ?: throw Exception("User not authenticated")
            val items = recipe.ingredients.map { ingredient ->
                ShoppingListItem(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    ingredientId = ingredient.id,
                    name = ingredient.name,
                    quantity = 1.0, // Default quantity
                    unit = "", // Default empty unit
                    recipeId = recipe.id,
                    recipeName = recipe.title
                )
            }
            
            SupabaseClient.supabase.postgrest[SHOPPING_LIST_TABLE]
                .insert(items)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding recipe to shopping list: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update the purchased status of a shopping list item
     * @param itemId The ID of the item to update
     * @param isPurchased The new purchased status
     * @return Result containing success or failure
     */
    suspend fun updateItemPurchasedStatus(itemId: String, isPurchased: Boolean): Result<Unit> {
        return try {
            SupabaseClient.supabase.postgrest[SHOPPING_LIST_TABLE]
                .update({
                    set("is_purchased", isPurchased)
                }) {
                    filter { eq("id", itemId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating item purchased status: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Delete a shopping list item
     * @param itemId The ID of the item to delete
     * @return Result containing success or failure
     */
    suspend fun deleteShoppingListItem(itemId: String): Result<Unit> {
        return try {
            SupabaseClient.supabase.postgrest[SHOPPING_LIST_TABLE]
                .delete {
                    filter { eq("id", itemId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting shopping list item: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Format shopping list as text for SMS sharing
     * @param items List of shopping list items to format
     * @return Formatted text for SMS
     */
    fun formatShoppingListForSMS(items: List<ShoppingListItem>): String {
        val groupedItems = items.groupBy { it.recipeName }
        
        return buildString {
            append("Shopping List:\n\n")
            
            groupedItems.forEach { (recipeName, recipeItems) ->
                if (recipeName.isNotEmpty()) {
                    append("For $recipeName:\n")
                }
                
                recipeItems.forEach { item ->
                    val quantityText = if (item.quantity > 0) "${item.quantity} ${item.unit} " else ""
                    append("- $quantityText${item.name}\n")
                }
                
                append("\n")
            }
        }
    }
}