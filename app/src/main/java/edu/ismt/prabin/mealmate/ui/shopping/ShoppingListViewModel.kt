package edu.ismt.prabin.mealmate.ui.shopping

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ismt.prabin.mealmate.data.model.Recipe
import edu.ismt.prabin.mealmate.data.model.ShoppingListItem
import edu.ismt.prabin.mealmate.data.repository.ShoppingListRepository
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import kotlinx.coroutines.launch

/**
 * ViewModel for handling shopping list-related data operations.
 */
class ShoppingListViewModel : ViewModel() {
    
    // LiveData for shopping list items
    private val _shoppingListItems = MutableLiveData<List<ShoppingListItem>>()
    val shoppingListItems: LiveData<List<ShoppingListItem>> = _shoppingListItems
    
    // LiveData for operation status
    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus
    
    // LiveData for formatted SMS text
    private val _smsText = MutableLiveData<String>()
    val smsText: LiveData<String> = _smsText
    
    /**
     * Load shopping list items for the current user
     */
    fun loadShoppingList() {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            ShoppingListRepository.getShoppingList().fold(
                onSuccess = { items ->
                    _shoppingListItems.value = items
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error(error.message ?: "Failed to load shopping list")
                }
            )
        }
    }
    
    /**
     * Update the purchased status of a shopping list item
     */
    fun updateItemPurchasedStatus(itemId: String, isPurchased: Boolean) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            ShoppingListRepository.updateItemPurchasedStatus(itemId, isPurchased).fold(
                onSuccess = {
                    _operationStatus.value = OperationStatus.Success("Item ${if (isPurchased) "purchased" else "unpurchased"}")
                    // Update the local list to reflect the change immediately
                    val currentList = _shoppingListItems.value.orEmpty().toMutableList()
                    val index = currentList.indexOfFirst { it.id == itemId }
                    if (index != -1) {
                        currentList[index] = currentList[index].copy(isPurchased = isPurchased)
                        _shoppingListItems.value = currentList
                    }
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error("Failed to update purchase status: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Add a recipe's ingredients to the shopping list
     */
    fun addRecipeToShoppingList(recipe: Recipe) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            ShoppingListRepository.addRecipeToShoppingList(recipe).fold(
                onSuccess = {
                    _operationStatus.value = OperationStatus.Success("Recipe added to shopping list")
                    loadShoppingList() // Reload the shopping list
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error(error.message ?: "Failed to add recipe to shopping list")
                }
            )
        }
    }
    
    /**
     * Add a single item to the shopping list
     */
    fun addShoppingListItem(item: ShoppingListItem) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            ShoppingListRepository.addShoppingListItem(item).fold(
                onSuccess = {
                    _operationStatus.value = OperationStatus.Success("Item added successfully")
                    loadShoppingList() // Reload the shopping list
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error("Failed to add item: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Update an existing shopping list item
     */
    fun updateShoppingListItem(item: ShoppingListItem) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            ShoppingListRepository.updateShoppingListItem(item).fold(
                onSuccess = {
                    _operationStatus.value = OperationStatus.Success("Item updated successfully")
                    loadShoppingList() // Reload the shopping list
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error("Failed to update item: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Delete a shopping list item
     */
    fun deleteShoppingListItem(itemId: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            ShoppingListRepository.deleteShoppingListItem(itemId).fold(
                onSuccess = {
                    _operationStatus.value = OperationStatus.Success("Item deleted successfully")
                    loadShoppingList() // Reload the shopping list
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error("Failed to delete item: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Format shopping list as text for SMS sharing
     */
    fun formatShoppingListForSMS() {
        val items = _shoppingListItems.value.orEmpty()
        val smsText = ShoppingListRepository.formatShoppingListForSMS(items)
        _smsText.value = smsText
    }
    
    /**
     * Format recipe as text for SMS sharing
     */
    fun formatRecipeForSMS(recipe: Recipe): String {
        return buildString {
            append("${recipe.title}\n\n")
            
            append("Preparation Time: ${recipe.prepTime} minutes\n")
            append("Food Type: ${recipe.foodType}\n\n")
            
            append("Ingredients:\n")
            recipe.ingredients.forEach { ingredient ->
                append("- ${ingredient.name} (${ingredient.category})\n")
            }
            
            append("\nInstructions:\n${recipe.instructions}\n")
        }
    }
    
    /**
     * Sealed class representing the status of operations
     */
    sealed class OperationStatus {
        object Loading : OperationStatus()
        data class Success(val message: String) : OperationStatus()
        data class Error(val message: String) : OperationStatus()
    }
}