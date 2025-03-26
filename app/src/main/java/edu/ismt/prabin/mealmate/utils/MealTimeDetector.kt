package edu.ismt.prabin.mealmate.utils

import java.util.Calendar
import java.util.Date

object MealTimeDetector {
    fun getMealType(date: Date = Calendar.getInstance().time): String {
        val calendar = Calendar.getInstance().apply {
            time = date
        }
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> "breakfast"
            in 11..14 -> "lunch"
            in 15..16 -> "snack"  // Afternoon snack time
            in 17..20 -> "dinner"
            else -> "snack"  // Late night/early morning snack
        }
    }
}