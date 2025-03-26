package edu.ismt.prabin.mealmate

import android.app.Application
import edu.ismt.prabin.mealmate.data.repository.RecipeRepository
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient

class MealMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize SupabaseClient with application context
        SupabaseClient.init(this)
        RecipeRepository.init(this)
    }
} 