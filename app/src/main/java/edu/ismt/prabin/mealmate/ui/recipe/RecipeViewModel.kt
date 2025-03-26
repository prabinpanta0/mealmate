package edu.ismt.prabin.mealmate.ui.recipe

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ismt.prabin.mealmate.data.model.Ingredient
import edu.ismt.prabin.mealmate.data.model.Recipe
import edu.ismt.prabin.mealmate.data.repository.RecipeRepository
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

/**
 * ViewModel for handling recipe-related data operations.
 */
class RecipeViewModel : ViewModel() {
    
    // LiveData for recipes list
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes
    
    // LiveData for filtered recipes list
    private val _filteredRecipes = MutableLiveData<List<Recipe>>()
    val filteredRecipes: LiveData<List<Recipe>> = _filteredRecipes
    
    // LiveData for current recipe
    private val _currentRecipe = MutableLiveData<Recipe?>()
    val currentRecipe: LiveData<Recipe?> = _currentRecipe
    
    // LiveData for operation status
    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus
    
    // Search query
    private var searchQuery = ""
    
    // Selected category filter
    private var selectedCategory: String? = null
    
    // Selected time filter (in minutes)
    private var selectedTime: Int? = null
    
    /**
     * Load recipes for a specific user
     */
    fun loadRecipes(userId: String) {
        viewModelScope.launch {
            RecipeRepository.getRecipes(userId).fold(
                onSuccess = { recipeList ->
                    _recipes.value = recipeList
                    applyFilters()
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error(error.message ?: "Failed to load recipes")
                }
            )
        }
    }
    
    /**
     * Apply search query and filters to the recipes list
     */
    private fun applyFilters() {
        val allRecipes = _recipes.value ?: emptyList()
        
        val filtered = allRecipes.filter { recipe ->
            // Apply search query filter
            val matchesQuery = if (searchQuery.isNotEmpty()) {
                recipe.title.contains(searchQuery, ignoreCase = true) ||
                recipe.description?.contains(searchQuery, ignoreCase = true) == true ||
                recipe.ingredients.any { it.name.contains(searchQuery, ignoreCase = true) }
            } else {
                true
            }
            
            // Apply category filter
            val matchesCategory = if (selectedCategory != null) {
                recipe.foodType.equals(selectedCategory, ignoreCase = true)
            } else {
                true
            }
            
            // Apply time filter
            val matchesTime = if (selectedTime != null) {
                val timeLimit = selectedTime ?: return@filter false
                when (timeLimit) {
                    Int.MAX_VALUE -> recipe.prepTime > 60
                    else -> recipe.prepTime <= timeLimit
                }
            } else {
                true
            }
            
            // Recipe must match all applied filters
            matchesQuery && matchesCategory && matchesTime
        }
        
        _filteredRecipes.value = filtered
    }
    
    /**
     * Set search query and apply filters
     */
    fun setSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }
    
    /**
     * Set category filter and apply filters
     */
    fun setCategoryFilter(category: String?) {
        selectedCategory = category
        applyFilters()
    }
    
    /**
     * Set time filter and apply filters
     */
    fun setTimeFilter(minutes: Int?) {
        selectedTime = minutes
        applyFilters()
    }
    
    /**
     * Clear all filters
     */
    fun clearFilters() {
        searchQuery = ""
        selectedCategory = null
        selectedTime = null
        applyFilters()
    }
    
    /**
     * Load a specific recipe by ID
     */
    fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            RecipeRepository.getRecipeById(recipeId).fold(
                onSuccess = { recipe ->
                    _currentRecipe.value = recipe
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error(error.message ?: "Failed to load recipe")
                }
            )
        }
    }
    
    /**
     * Create a new recipe
     */
    fun createRecipe(recipe: Recipe, imageUri: Uri? = null) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            RecipeRepository.createRecipe(recipe, imageUri).fold(
                onSuccess = { recipeId ->
                    _operationStatus.value = OperationStatus.Success("Recipe created successfully")
                    // Reload recipes to include the new one
                    loadRecipes(recipe.userId)
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error(error.message ?: "Failed to create recipe")
                }
            )
        }
    }
    
    /**
     * Update an existing recipe
     */
    fun updateRecipe(recipe: Recipe, imageUri: Uri? = null) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            RecipeRepository.updateRecipe(recipe, imageUri).fold(
                onSuccess = {
                    _operationStatus.value = OperationStatus.Success("Recipe updated successfully")
                    // Reload the current recipe to reflect changes
                    loadRecipe(recipe.id)
                    // Also reload the recipes list
                    loadRecipes(recipe.userId)
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error(error.message ?: "Failed to update recipe")
                }
            )
        }
    }
    
    /**
     * Delete a recipe
     */
    fun deleteRecipe(recipeId: String, userId: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            RecipeRepository.deleteRecipe(recipeId).fold(
                onSuccess = {
                    _operationStatus.value = OperationStatus.Success("Recipe deleted successfully")
                    // Reload recipes to reflect the deletion
                    loadRecipes(userId)
                },
                onFailure = { error ->
                    _operationStatus.value = OperationStatus.Error(error.message ?: "Failed to delete recipe")
                }
            )
        }
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
     * Add a new ingredient to the current recipe
     */
    fun addIngredientToRecipe(ingredient: Ingredient) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            
            // Get current recipe
            val currentRecipe = _currentRecipe.value ?: return@launch
            
            try {
                // Insert the ingredient into the database
                SupabaseClient.supabase.postgrest[RecipeRepository.INGREDIENTS_TABLE].insert(ingredient)
                
                // Create a new list with the added ingredient
                val updatedIngredients = currentRecipe.ingredients.toMutableList().apply {
                    add(ingredient)
                }
                
                // Update the local recipe with the new ingredient list
                val updatedRecipe = currentRecipe.copy(ingredients = updatedIngredients)
                _currentRecipe.value = updatedRecipe
                
                _operationStatus.value = OperationStatus.Success("Ingredient added successfully")
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error(e.message ?: "Failed to add ingredient")
            }
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