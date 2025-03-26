package edu.ismt.prabin.mealmate.data.repository

import android.content.Context
import android.util.Log
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings
import edu.ismt.prabin.mealmate.BuildConfig
import edu.ismt.prabin.mealmate.data.model.User
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Singleton class for handling Supabase authentication and database operations.
 */
object SupabaseClient {
    private lateinit var context: Context
    private lateinit var settings: Settings
    
    // Initialize JSON configuration
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }
    
    fun init(context: Context) {
        this.context = context
        this.settings = SharedPreferencesSettings(context.getSharedPreferences("supabase_auth", Context.MODE_PRIVATE))
        
        // After initializing, apply necessary database migrations
        applyDatabaseMigrations()
    }
    
    private const val TAG = "SupabaseClient"
    
    // Initialize Supabase client lazily
    val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                sessionManager = SettingsSessionManager(settings)
            }
            install(Storage)
            defaultSerializer = KotlinXSerializer(json)
        }
    }
    
    fun getCurrentUserId(): String? = supabase.auth.currentSessionOrNull()?.user?.id
    
    fun getCurrentUserEmail(): String? = supabase.auth.currentSessionOrNull()?.user?.email
    
    fun getCurrentUserName(): String? = supabase.auth.currentSessionOrNull()?.user?.userMetadata?.get("name")?.toString()
    
    /**
     * Apply database migrations to fix issues like RLS policies
     */
    private fun applyDatabaseMigrations() {
        // This will be executed on application startup
        // We'll run the migration in a coroutine to not block the main thread
        kotlinx.coroutines.MainScope().launch {
            try {
                // Check if user is logged in before running migrations
                if (isSignedIn()) {
                    Log.d(TAG, "Applying database migrations...")
                    // Get the SQL content from assets
                    val recipeSecuritySql = context.assets.open("supabase/migrations/fix_recipe_security.sql").bufferedReader().use { it.readText() }
                    val ingredientsSecuritySql = context.assets.open("supabase/migrations/fix_ingredients_security.sql").bufferedReader().use { it.readText() }
                    
                    // Run the SQL migrations in order
                    executeRawSql(recipeSecuritySql)
                    executeRawSql(ingredientsSecuritySql)
                    
                    Log.d(TAG, "Database migrations applied successfully")
                } else {
                    Log.d(TAG, "User not logged in, skipping migrations")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply database migrations: ${e.message}", e)
            }
        }
    }
    
    /**
     * Execute raw SQL on the Supabase database
     * @param sql The SQL to execute
     */
    private suspend fun executeRawSql(sql: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Execute the stored procedure through the REST API
            supabase.postgrest.rpc(
                function = "execute_sql",
                parameters = buildJsonObject {
                    put("query", sql)
                }
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Execute SQL failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Convert Instant to a readable date string
     */
    private fun Instant.toReadableString(): String {
        val date = Date(this.toEpochMilliseconds())
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Sign in with email and password
     * @param email User's email
     * @param password User's password
     * @return Result containing User on success or Exception on failure
     */
    suspend fun signIn(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            // Get user data from the current session
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                val userData = User(
                    id = session.user?.id ?: "",
                    email = session.user?.email ?: "",
                    name = session.user?.userMetadata?.get("name")?.toString() ?: "",
                    createdAt = session.user?.createdAt?.toReadableString() ?: ""
                )
                Result.success(userData)
            } else {
                Result.failure(Exception("Failed to get user session"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Register a new user with email, password, and name
     * @param email User's email
     * @param password User's password
     * @param name User's full name
     * @return Result containing User on success or Exception on failure
     */
    suspend fun signUp(email: String, password: String, name: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Create JSON object for user metadata
            val userMetadata = buildJsonObject {
                put("name", name)
            }
            
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = userMetadata
            }
            
            // Get user data from the current session
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                val userId = session.user?.id ?: ""
                val userEmail = session.user?.email ?: ""
                
                // Create profile for the user
                ProfileRepository.createProfile(userId, userEmail, name)
                
                val userData = User(
                    id = userId,
                    email = userEmail,
                    name = name,
                    createdAt = session.user?.createdAt?.toReadableString() ?: ""
                )
                Result.success(userData)
            } else {
                Result.failure(Exception("Failed to get user session"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send a password reset email
     * @param email User's email
     * @return Result containing Unit on success or Exception on failure
     */
    suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sign out the current user
     * @return Result containing Unit on success or Exception on failure
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if a user is currently signed in
     * @return Boolean indicating if a user is signed in
     */
    suspend fun isSignedIn(): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            Log.e(TAG, "Session check failed", e)
            false
        }
    }
    
    /**
     * Get the current signed-in user
     * @return Result containing User on success or Exception on failure
     */
    suspend fun getCurrentUser(): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                val userData = User(
                    id = session.user?.id ?: "",
                    email = session.user?.email ?: "",
                    name = session.user?.userMetadata?.get("name")?.toString() ?: "",
                    createdAt = session.user?.createdAt?.toReadableString() ?: ""
                )
                Result.success(userData)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get current user failed", e)
            Result.failure(e)
        }
    }


}