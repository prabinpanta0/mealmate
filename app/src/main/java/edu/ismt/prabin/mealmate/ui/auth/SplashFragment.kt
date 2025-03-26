package edu.ismt.prabin.mealmate.ui.auth
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.progressindicator.CircularProgressIndicator
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import edu.ismt.prabin.mealmate.util.SplashIdlingResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen fragment that displays the app logo and checks authentication status
 * before navigating to the appropriate screen.
 */
class SplashFragment : Fragment() {
    private val TAG = "SplashFragment"
    private lateinit var splashProgress: CircularProgressIndicator
    private lateinit var splashLogo: ImageView
    private lateinit var splashTitle: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        splashProgress = view.findViewById(R.id.splash_progress)
        splashLogo = view.findViewById(R.id.splash_logo)
        splashTitle = view.findViewById(R.id.splash_title)
        
        // Ensure progress indicator is visible and animating
        splashProgress.isIndeterminate = true
        splashProgress.visibility = View.VISIBLE
        
        // Animate the logo with a pulse effect
        animateLogo()
        
        // Animate the title
        animateTitle()
        
        // Signal for testing that the splash screen has started
        SplashIdlingResource.getInstance().setIdleState(false)
        
        // Delay for 2.5 seconds to show the splash screen
        lifecycleScope.launch {
            delay(2500) // 2.5 seconds delay
            checkAuthStatus()
        }
    }
    
    private fun animateLogo() {
        // Create a pulsing and slight rotation animation
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 1.1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 1.1f)
        
        ObjectAnimator.ofPropertyValuesHolder(splashLogo, scaleX, scaleY).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
    
    private fun animateTitle() {
        // Fade in the title
        splashTitle.alpha = 0f
        splashTitle.animate()
            .alpha(1f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    
    private suspend fun checkAuthStatus() {
        try {
            // Check if user is already signed in
            val isSignedIn = SupabaseClient.isSignedIn()
            Log.d(TAG, "User signed in status: $isSignedIn")
            
            if (isSignedIn) {
                // Double-check by trying to get the current user
                val currentUserResult = SupabaseClient.getCurrentUser()
                if (currentUserResult.isSuccess && currentUserResult.getOrNull() != null) {
                    // User is definitely signed in, navigate to home
                    Log.d(TAG, "User is signed in, navigating to home")
                    SplashIdlingResource.getInstance().setIdleState(true)
                    findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                } else {
                    // Session exists but user data can't be retrieved, go to login
                    Log.d(TAG, "Session exists but user data unavailable, navigating to login")
                    SplashIdlingResource.getInstance().setIdleState(true)
                    findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                }
            } else {
                // User is not signed in, navigate to landing screen
                Log.d(TAG, "User is not signed in, navigating to landing screen")
                SplashIdlingResource.getInstance().setIdleState(true)
                findNavController().navigate(R.id.action_splash_to_landing)
            }
        } catch (e: Exception) {
            // Handle any exceptions by navigating to landing screen
            Log.e(TAG, "Error checking auth status: ${e.message}")
            SplashIdlingResource.getInstance().setIdleState(true)
            findNavController().navigate(R.id.action_splash_to_landing)
        }
    }
}