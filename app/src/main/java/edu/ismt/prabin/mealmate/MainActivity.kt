package edu.ismt.prabin.mealmate
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import edu.ismt.prabin.mealmate.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import edu.ismt.prabin.mealmate.R
import android.os.Build
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import edu.ismt.prabin.mealmate.util.SplashIdlingResource

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var sharedPreferences: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Initialize SharedPreferences first, before it's used
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        
        // Apply dynamic colors on Android 12+ (if not disabled in settings)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val useDynamicColors = sharedPreferences.getBoolean("use_dynamic_colors", false)
            if (useDynamicColors) {
                DynamicColors.applyToActivityIfAvailable(this)
            }
        }
        
        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)
        
        // Hide bottom nav on auth screens and specific detail screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment, R.id.landingFragment, R.id.loginFragment, 
                R.id.registerFragment, R.id.resetPasswordFragment,
                R.id.recipeDetailFragment, R.id.createRecipeFragment -> {
                    bottomNav.visibility = View.GONE
                }
                else -> {
                    bottomNav.visibility = View.VISIBLE
                    setupScrollListener()
                }
            }
        }
        
        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.updatePadding(bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        
        // Notify IdlingResource when splash screen is complete (for testing)
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
            if (!isDestroyed) {
                SplashIdlingResource.getInstance().setIdleState(true)
            }
        }
    }
    
    private fun setupScrollListener() {
        // Find the current active content in the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
        
        // Get navigation bar reference
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        
        // Clear any existing listeners from RecyclerViews or NestedScrollViews
        currentFragment?.view?.let { fragmentView ->
            // Find RecyclerViews and attach scroll listeners
            val recyclerViewIds = listOf(
                R.id.recipe_recycler_view,
                R.id.recentRecipesRecyclerView,
                R.id.shopping_list_recycler_view
            )
            
            recyclerViewIds.forEach { id ->
                fragmentView.findViewById<RecyclerView>(id)?.let { recyclerView ->
                    recyclerView.clearOnScrollListeners()
                    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            if (dy > 20) {
                                // Scrolling down - hide the bottom navigation
                                bottomNav.animate().translationY(bottomNav.height.toFloat()).setDuration(200).start()
                            } else if (dy < -5) {
                                // Scrolling up - show the bottom navigation
                                bottomNav.animate().translationY(0f).setDuration(200).start()
                            }
                        }
                    })
                }
            }
            
            // Create a reusable scroll listener for NestedScrollViews
            val nestedScrollListener = View.OnScrollChangeListener { _, _, _, oldScrollY, newScrollY ->
                val dy = newScrollY - oldScrollY
                if (dy > 20) {
                    // Scrolling down - hide the bottom navigation
                    bottomNav.animate().translationY(bottomNav.height.toFloat()).setDuration(200).start()
                } else if (dy < -5) {
                    // Scrolling up - show the bottom navigation
                    bottomNav.animate().translationY(0f).setDuration(200).start()
                }
            }
            
            // Find all NestedScrollViews in the layout
            findNestedScrollViews(fragmentView).forEach { scrollView ->
                scrollView.setOnScrollChangeListener(nestedScrollListener)
            }
        }
    }
    
    /**
     * Recursively find all NestedScrollView instances in a view hierarchy
     */
    private fun findNestedScrollViews(view: View): List<NestedScrollView> {
        val results = mutableListOf<NestedScrollView>()
        
        if (view is NestedScrollView) {
            results.add(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                results.addAll(findNestedScrollViews(view.getChildAt(i)))
            }
        }
        
        return results
    }
}