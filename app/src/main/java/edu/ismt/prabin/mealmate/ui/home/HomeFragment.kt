package edu.ismt.prabin.mealmate.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.repository.RecipeRepository
import edu.ismt.prabin.mealmate.ui.adapters.RecipePagerAdapter
import edu.ismt.prabin.mealmate.ui.adapters.RecentRecipeAdapter
import edu.ismt.prabin.mealmate.utils.MealTimeDetector
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
    
    private lateinit var suggestedRecipesPager: ViewPager2
    private lateinit var recentRecipesRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    // Adapters
    private lateinit var suggestedAdapter: RecipePagerAdapter
    private lateinit var recentAdapter: RecentRecipeAdapter
    
    // Auto-scrolling
    private var autoScrollTimer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isAutoScrolling = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        suggestedRecipesPager = view.findViewById(R.id.suggested_recipes_pager)
        recentRecipesRecyclerView = view.findViewById(R.id.recentRecipesRecyclerView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        // Setup swipe refresh
        setupSwipeRefresh()

        // Setup quick action buttons
        setupQuickActions(view)

        // Initialize adapters
        suggestedAdapter = RecipePagerAdapter(emptyList()) { recipe ->
            navigateToRecipeDetail(recipe.id)
        }
        
        recentAdapter = RecentRecipeAdapter(emptyList()) { recipe ->
            navigateToRecipeDetail(recipe.id)
        }

        // Setup suggested recipes pager
        setupSuggestedRecipes()

        // Setup recent recipes
        setupRecentRecipes()
        
        // Load recipes
        loadRecipes()
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.secondary,
            R.color.primary_dark
        )
        
        swipeRefreshLayout.setOnRefreshListener {
            loadRecipes()
        }
    }

    private fun setupQuickActions(view: View) {
        view.findViewById<MaterialButton>(R.id.btnCreateRecipe).setOnClickListener {
            findNavController().navigate(R.id.createRecipeFragment)
        }

        view.findViewById<MaterialButton>(R.id.btnShoppingList).setOnClickListener {
            findNavController().navigate(R.id.shoppingListFragment)
        }

        view.findViewById<MaterialButton>(R.id.btnProfile).setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun setupSuggestedRecipes() {
        // Set up ViewPager with adapter
        suggestedRecipesPager.adapter = suggestedAdapter
        
        // Add page transformer for nice animation
        suggestedRecipesPager.setPageTransformer { page, position ->
            val absPosition = Math.abs(position)
            page.apply {
                scaleY = 0.85f + (1f - absPosition) * 0.15f
                scaleX = 0.85f + (1f - absPosition) * 0.15f
                alpha = 0.5f + (1f - absPosition) * 0.5f
            }
        }
        
        // Setup auto-scrolling
        startAutoScrolling()
        
        // Stop scrolling when user touches the ViewPager
        suggestedRecipesPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> stopAutoScrolling()
                    ViewPager2.SCROLL_STATE_IDLE -> startAutoScrolling()
                }
            }
        })
    }

    private fun setupRecentRecipes() {
        recentRecipesRecyclerView.layoutManager = GridLayoutManager(context, 2)
        recentRecipesRecyclerView.adapter = recentAdapter
    }
    
    private fun loadRecipes() {
        if (!isAdded) return  // Don't proceed if fragment is not added to activity
        
        swipeRefreshLayout.isRefreshing = true
        
        // Use lifecycleScope but check if view still exists before accessing it
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentUserId = edu.ismt.prabin.mealmate.data.repository.SupabaseClient.getCurrentUserId()
                if (currentUserId == null) {
                    view?.let { safeView ->
                        Snackbar.make(safeView, "Not logged in", Snackbar.LENGTH_SHORT).show()
                    }
                    swipeRefreshLayout.isRefreshing = false
                    return@launch
                }

                // Get current meal type based on time of day
                val currentMealType = MealTimeDetector.getMealType()
                
                // Get all recipes for suggested section
                val allRecipesResult = RecipeRepository.getAllRecipes()
                
                // Get user recipes for recent section
                val userRecipesResult = RecipeRepository.getRecipes(currentUserId)
                
                // Ensure view still exists before proceeding
                if (!isAdded || view == null) {
                    return@launch
                }
                
                allRecipesResult.fold(
                    onSuccess = { allRecipes ->
                        if (!isAdded) return@fold // Check again before processing
                        
                        // First, prioritize current meal type recipes
                        val currentMealRecipes = allRecipes
                            .filter { it.foodType.equals(currentMealType, ignoreCase = true) }
                        
                        val suggestedRecipes = if (currentMealRecipes.size >= 5) {
                            currentMealRecipes.shuffled().take(5)
                        } else {
                            val result = currentMealRecipes.toMutableList()
                            val otherRecipes = allRecipes
                                .filter { !it.foodType.equals(currentMealType, ignoreCase = true) }
                                .shuffled()
                                .take(5 - result.size)
                            result.addAll(otherRecipes)
                            result.shuffled()
                        }
                        
                        // Update UI safely
                        view?.let { safeView ->
                            suggestedAdapter.updateRecipes(suggestedRecipes)
                            restartAutoScrolling()
                        }
                    },
                    onFailure = { error ->
                        view?.let { safeView ->
                            Snackbar.make(safeView, 
                                "Failed to load suggested recipes: ${error.message}", 
                                Snackbar.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // Handle user's recent recipes
                userRecipesResult.fold(
                    onSuccess = { userRecipes ->
                        if (!isAdded) return@fold
                        val recentRecipes = userRecipes
                            .sortedByDescending { it.createdAt }
                            .take(6)
                        recentAdapter.updateRecipes(recentRecipes)
                    },
                    onFailure = { error ->
                        view?.let { safeView ->
                            Snackbar.make(safeView,
                                "Failed to load recent recipes: ${error.message}",
                                Snackbar.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                if (isAdded) {
                    view?.let { safeView ->
                        Snackbar.make(safeView, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } finally {
                if (isAdded) {
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }
    
    private fun navigateToRecipeDetail(recipeId: String) {
        val bundle = Bundle().apply {
            putString("recipeId", recipeId)
        }
        findNavController().navigate(R.id.action_homeFragment_to_recipeDetailFragment, bundle)
    }
    
    private fun startAutoScrolling() {
        if (isAutoScrolling || suggestedAdapter.itemCount <= 1) return
        
        isAutoScrolling = true
        autoScrollTimer = Timer()
        autoScrollTimer?.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    if (suggestedAdapter.itemCount > 1) {
                        val nextItem = (suggestedRecipesPager.currentItem + 1) % suggestedAdapter.itemCount
                        suggestedRecipesPager.setCurrentItem(nextItem, true)
                    }
                }
            }
        }, 3000, 3000) // Change page every 3 seconds
    }
    
    private fun stopAutoScrolling() {
        autoScrollTimer?.cancel()
        autoScrollTimer = null
        isAutoScrolling = false
    }
    
    private fun restartAutoScrolling() {
        stopAutoScrolling()
        startAutoScrolling()
    }
    
    override fun onPause() {
        super.onPause()
        stopAutoScrolling()
    }
    
    override fun onResume() {
        super.onResume()
        startAutoScrolling()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScrolling()
    }
}