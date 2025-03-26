package edu.ismt.prabin.mealmate.data.repository

import android.util.Log
import edu.ismt.prabin.mealmate.data.model.Profile
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProfileRepository {
    private const val TAG = "ProfileRepository"
    private const val PROFILES_TABLE = "profiles"

    /**
     * Create a new profile for the user
     * @param userId The ID of the user
     * @param email The email of the user
     * @return Result containing Unit on success or Exception on failure
     */
    suspend fun createProfile(userId: String, email: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = Profile(
                id = userId,
                name = name,
                email = email
            )

            // Insert profile into database
            SupabaseClient.supabase.postgrest[PROFILES_TABLE].insert(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Create profile failed", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a profile exists for the user
     * @param userId The ID of the user
     * @return Result containing Boolean indicating if profile exists
     */
    suspend fun profileExists(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val profiles = SupabaseClient.supabase.postgrest[PROFILES_TABLE]
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<Profile>()
            
            Result.success(profiles.isNotEmpty())
        } catch (e: Exception) {
            Log.e(TAG, "Check profile exists failed", e)
            Result.failure(e)
        }
    }
} 