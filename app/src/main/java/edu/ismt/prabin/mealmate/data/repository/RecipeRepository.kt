package edu.ismt.prabin.mealmate.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import edu.ismt.prabin.mealmate.data.model.Ingredient
import edu.ismt.prabin.mealmate.data.model.Recipe
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Repository class for handling recipe and ingredient CRUD operations with Supabase.
 */
object RecipeRepository {
    private const val TAG = "RecipeRepository"
    const val RECIPES_TABLE = "recipes"
    const val INGREDIENTS_TABLE = "ingredients"
    private const val RECIPE_IMAGES_BUCKET = "recipes"
    
    private lateinit var context: Context
    
    fun init(context: Context) {
        this.context = context
    }
    
    /**
     * Create a new recipe with ingredients
     * @param recipe The recipe to create
     * @param imageUri Local URI of the image to upload (optional)
     * @return Result containing the created recipe ID on success or Exception on failure
     */
    suspend fun createRecipe(recipe: Recipe, imageUri: Uri? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if user has a profile
            val userId = SupabaseClient.getCurrentUserId() ?: throw Exception("User not logged in")
            val hasProfile = ProfileRepository.profileExists(userId).getOrNull() ?: false
            
            // Create profile if it doesn't exist
            if (!hasProfile) {
                val email = SupabaseClient.getCurrentUserEmail() ?: throw Exception("User email not found")
                val name = SupabaseClient.getCurrentUserName() ?: throw Exception("User name not found")
                ProfileRepository.createProfile(userId, email, name).getOrThrow()
            }
            
            // Generate a new ID for the recipe
            val recipeId = UUID.randomUUID().toString()
            
            // Upload image if provided
            val imageUrl = if (imageUri != null) {
                uploadRecipeImage(recipeId, imageUri).getOrNull() ?: ""
            } else {
                ""
            }
            
            // Create recipe object with ID and image URL
            val recipeToInsert = recipe.copy(
                id = recipeId,
                imageUrl = imageUrl,
                createdAt = System.currentTimeMillis()
            )
            
            // Insert recipe into database
            SupabaseClient.supabase.postgrest[RECIPES_TABLE].insert(recipeToInsert)
            
            // Insert ingredients with recipe ID
            try {
                recipe.ingredients.forEach { ingredient ->
                    val ingredientToInsert = ingredient.copy(
                        id = UUID.randomUUID().toString(),
                        recipeId = recipeId
                    )
                    try {
                        SupabaseClient.supabase.postgrest[INGREDIENTS_TABLE].insert(ingredientToInsert)
                    } catch (e: Exception) {
                        // Log the error but continue with other ingredients
                        Log.w(TAG, "Failed to insert ingredient ${ingredient.name}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting ingredients", e)
                // Continue with recipe creation even if some ingredients fail
            }
            
            Result.success(recipeId)
        } catch (e: Exception) {
            Log.e(TAG, "Create recipe failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all recipes for the current user
     * @param userId ID of the user whose recipes to fetch
     * @return Result containing a list of recipes on success or Exception on failure
     */
    suspend fun getRecipes(userId: String): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        try {
            // Get recipes for the user
            val recipes = SupabaseClient.supabase.postgrest[RECIPES_TABLE]
                .select() {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Recipe>()
            
            // For each recipe, get its ingredients
            val recipesWithIngredients = recipes.map { recipe ->
                val ingredients = SupabaseClient.supabase.postgrest[INGREDIENTS_TABLE]
                    .select() {
                        filter {
                            eq("recipe_id", recipe.id)
                        }
                    }
                    .decodeList<Ingredient>()
                
                recipe.copy(ingredients = ingredients)
            }
            
            Result.success(recipesWithIngredients)
        } catch (e: Exception) {
            Log.e(TAG, "Get recipes failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all recipes from all users
     * @return Result containing a list of all recipes on success or Exception on failure
     */
    suspend fun getAllRecipes(): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        try {
            // Get all recipes
            val recipes = SupabaseClient.supabase.postgrest[RECIPES_TABLE]
                .select()
                .decodeList<Recipe>()
            
            // For each recipe, get its ingredients
            val recipesWithIngredients = recipes.map { recipe ->
                val ingredients = SupabaseClient.supabase.postgrest[INGREDIENTS_TABLE]
                    .select() {
                        filter {
                            eq("recipe_id", recipe.id)
                        }
                    }
                    .decodeList<Ingredient>()
                
                recipe.copy(ingredients = ingredients)
            }
            
            Result.success(recipesWithIngredients)
        } catch (e: Exception) {
            Log.e(TAG, "Get all recipes failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get a recipe by ID
     * @param recipeId ID of the recipe to fetch
     * @return Result containing the recipe on success or Exception on failure
     */
    suspend fun getRecipeById(recipeId: String): Result<Recipe> = withContext(Dispatchers.IO) {
        try {
            // Get recipe by ID
            val recipe = SupabaseClient.supabase.postgrest[RECIPES_TABLE]
                .select() {
                    filter {
                        eq("id", recipeId)
                    }
                }
                .decodeSingle<Recipe>()
            
            // Get ingredients for the recipe
            val ingredients = SupabaseClient.supabase.postgrest[INGREDIENTS_TABLE]
                .select() {
                    filter {
                        eq("recipe_id", recipeId)
                    }
                }
                .decodeList<Ingredient>()
            
            Result.success(recipe.copy(ingredients = ingredients))
        } catch (e: Exception) {
            Log.e(TAG, "Get recipe by ID failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing recipe
     * @param recipe The recipe to update
     * @param imageUri Local URI of the new image to upload (optional)
     * @return Result containing Unit on success or Exception on failure
     */
    suspend fun updateRecipe(recipe: Recipe, imageUri: Uri? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Upload new image if provided
            val imageUrl = if (imageUri != null) {
                uploadRecipeImage(recipe.id, imageUri).getOrNull() ?: recipe.imageUrl
            } else {
                recipe.imageUrl
            }
            
            // Update recipe with new image URL
            val recipeToUpdate = recipe.copy(imageUrl = imageUrl)
            
            // Update recipe in database
            SupabaseClient.supabase.postgrest[RECIPES_TABLE]
                .update(recipeToUpdate) {
                    filter {
                        eq("id", recipe.id)
                    }
                }
            
            // Delete existing ingredients
            SupabaseClient.supabase.postgrest[INGREDIENTS_TABLE]
                .delete {
                    filter {
                        eq("recipe_id", recipe.id)
                    }
                }
            
            // Insert new ingredients
            try {
                recipe.ingredients.forEach { ingredient ->
                    val ingredientToInsert = ingredient.copy(
                        id = UUID.randomUUID().toString(),
                        recipeId = recipe.id
                    )
                    try {
                        SupabaseClient.supabase.postgrest[INGREDIENTS_TABLE].insert(ingredientToInsert)
                    } catch (e: Exception) {
                        // Log the error but continue with other ingredients
                        Log.w(TAG, "Failed to insert ingredient ${ingredient.name}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting ingredients", e)
                // Continue with recipe update even if some ingredients fail
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update recipe failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a recipe and its ingredients
     * @param recipeId ID of the recipe to delete
     * @return Result containing Unit on success or Exception on failure
     */
    suspend fun deleteRecipe(recipeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete recipe from database
            SupabaseClient.supabase.postgrest[RECIPES_TABLE]
                .delete {
                    filter {
                        eq("id", recipeId)
                    }
                }
            
            // Delete ingredients for the recipe
            SupabaseClient.supabase.postgrest[INGREDIENTS_TABLE]
                .delete {
                    filter {
                        eq("recipe_id", recipeId)
                    }
                }
            
            // Delete recipe image if it exists
            try {
                SupabaseClient.supabase.storage[RECIPE_IMAGES_BUCKET].delete("$recipeId.jpg")
            } catch (e: Exception) {
                // Ignore if image doesn't exist
                Log.w(TAG, "Failed to delete recipe image", e)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete recipe failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload a recipe image to Supabase storage
     * @param recipeId ID of the recipe
     * @param imageUri Local URI of the image to upload
     * @return Result containing the image URL on success or Exception on failure
     */
    private suspend fun uploadRecipeImage(recipeId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "$recipeId.jpg"
            
            // Read image data using ContentResolver
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Failed to open image file"))
            
            val imageData = inputStream.readBytes()
            inputStream.close()
            
            // Upload image to Supabase storage
            SupabaseClient.supabase.storage[RECIPE_IMAGES_BUCKET].upload(
                path = fileName,
                data = imageData
            ) {
                upsert = true
            }
            
            // Get public URL of the uploaded image
            val imageUrl = SupabaseClient.supabase.storage[RECIPE_IMAGES_BUCKET].publicUrl(fileName)
            Result.success(imageUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Upload recipe image failed", e)
            Result.failure(e)
        }
    }
}