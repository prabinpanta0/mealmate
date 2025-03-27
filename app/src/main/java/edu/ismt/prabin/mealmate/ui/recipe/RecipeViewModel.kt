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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for handling recipe-related data operations.
 */
class RecipeViewModel : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes
    
    private val _filteredRecipes = MutableLiveData<List<Recipe>>()
    val filteredRecipes: LiveData<List<Recipe>> = _filteredRecipes
    
    private val _currentRecipe = MutableLiveData<Recipe?>()
    val currentRecipe: LiveData<Recipe?> = _currentRecipe
    
    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus
    
    private var searchQuery = ""
    private var selectedCategory: String? = null
    private var selectedTime: Int? = null

    /**
     * Load recipes for a specific user
     */
    fun loadRecipes(userId: String) {
        viewModelScope.launch(Dispatchers.Main + SupervisorJob()) {
            try {
                _operationStatus.value = OperationStatus.Loading

                withContext(Dispatchers.IO) {
                    RecipeRepository.getRecipes(userId)
                }.fold(
                    onSuccess = { recipes ->
                        _recipes.value = recipes
                        _filteredRecipes.value = recipes
                        _operationStatus.value = OperationStatus.Success("Recipes loaded successfully")
                    },
                    onFailure = { error ->
                        _operationStatus.value = OperationStatus.Error(error.message ?: "Failed to load recipes")
                    }
                )
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error(e.message ?: "Failed to load recipes")
            }
        }
    }
    
    /**
     * Load all recipes (not limited to a specific user)
     */
    fun loadAllRecipes() {
        viewModelScope.launch(Dispatchers.Main + SupervisorJob()) {
            try {
                _operationStatus.value = OperationStatus.Loading
                
                withContext(Dispatchers.IO) {
                    RecipeRepository.getAllRecipes()
                }.fold(
                    onSuccess = { recipes ->
                        _recipes.value = recipes
                        _filteredRecipes.value = recipes
                        _operationStatus.value = OperationStatus.Success("All recipes loaded successfully")
                    },
                    onFailure = { error ->
                        android.util.Log.e("RecipeViewModel", "Failed to load all recipes: ${error.message}")
                        _operationStatus.value = OperationStatus.Error(error.message ?: "Failed to load recipes")
                        
                        // Set empty lists as fallback to prevent null issues
                        if (_recipes.value == null) {
                            _recipes.value = emptyList()
                            _filteredRecipes.value = emptyList()
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("RecipeViewModel", "Exception loading all recipes: ${e.message}")
                _operationStatus.value = OperationStatus.Error(e.message ?: "Failed to load recipes")
                
                // Set empty lists as fallback
                if (_recipes.value == null) {
                    _recipes.value = emptyList()
                    _filteredRecipes.value = emptyList()
                }
            }
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
                recipe.prepTime <= selectedTime!!
            } else {
                true
            }
            
            matchesQuery && matchesCategory && matchesTime
        }
        
        _filteredRecipes.postValue(filtered)
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
        viewModelScope.launch(Dispatchers.Main + SupervisorJob()) {
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