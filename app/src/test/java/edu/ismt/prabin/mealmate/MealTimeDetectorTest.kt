package edu.ismt.prabin.mealmate

import edu.ismt.prabin.mealmate.utils.MealTimeDetector
import org.junit.Test
import org.junit.Assert.*
import java.util.*

class MealTimeDetectorTest {
    @Test
    fun `test breakfast time detection`() {
        // Test breakfast time (6 AM - 11 AM)
        val breakfastTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
        }
        assertEquals("breakfast", MealTimeDetector.getMealType(breakfastTime.time))
    }

    @Test
    fun `test lunch time detection`() {
        // Test lunch time (11 AM - 3 PM)
        val lunchTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 0)
        }
        assertEquals("lunch", MealTimeDetector.getMealType(lunchTime.time))
    }

    @Test
    fun `test dinner time detection`() {
        // Test dinner time (3 PM - 10 PM)
        val dinnerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
        }
        assertEquals("dinner", MealTimeDetector.getMealType(dinnerTime.time))
    }

    @Test
    fun `test late night snack time detection`() {
        // Test late night (10 PM - 6 AM)
        val lateNightTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
        }
        assertEquals("snack", MealTimeDetector.getMealType(lateNightTime.time))
    }
}