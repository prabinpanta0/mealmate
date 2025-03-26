package edu.ismt.prabin.mealmate.data.model

/**
 * Data class representing a user in the application.
 * This will be used for authentication and user profile management.
 */
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val createdAt: String = ""
)