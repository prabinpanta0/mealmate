package edu.ismt.prabin.mealmate

import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import android.content.Context

class SupabaseClientTest {
    @Mock
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        SupabaseClient.init(mockContext)
    }

    @Test
    fun `test isSignedIn returns false when not logged in`() = runBlocking {
        val result = SupabaseClient.isSignedIn()
        assertFalse(result)
    }

    @Test
    fun `test getCurrentUserId returns null when not logged in`() {
        val userId = SupabaseClient.getCurrentUserId()
        assertNull(userId)
    }

    @Test
    fun `test getCurrentUserEmail returns null when not logged in`() {
        val userEmail = SupabaseClient.getCurrentUserEmail()
        assertNull(userEmail)
    }
}