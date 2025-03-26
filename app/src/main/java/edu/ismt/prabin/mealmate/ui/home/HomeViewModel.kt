package edu.ismt.prabin.mealmate.ui.home

import androidx.lifecycle.ViewModel
import edu.ismt.prabin.mealmate.data.model.Recipe
import java.util.Calendar

class HomeViewModel : ViewModel() {
    // Dashboard statistics
    val dashboardStats = listOf(
        StatCard("Weekly Recipes", "15", "recipe1"),
        StatCard("Active Plans", "3", ""),
        StatCard("Grocery Items", "23", "")
    )
    
    // Shopping list items
    val shoppingListItems = listOf(
        GroceryItem("Tomatoes", 5),
        GroceryItem("Chicken", 2),
        GroceryItem("Rice", 1)
    )

    private fun getCurrentMealTime(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 6..11 -> "breakfast"
            in 12..15 -> "lunch"
            in 16..20 -> "dinner"
            else -> "snack"
        }
    }

    fun getSuggestedRecipes(allRecipes: List<Recipe>): List<Recipe> {
        val currentMeal = getCurrentMealTime()
        return allRecipes
            .filter { it.foodType.equals(currentMeal, ignoreCase = true) }
            .shuffled()
            .take(5)
    }
}

// Data classes for UI
data class StatCard(val title: String, val value: String, val recipeId: String = "")
data class GroceryItem(val name: String, val quantity: Int) 