package edu.ismt.prabin.mealmate.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.Recipe
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import kotlinx.coroutines.launch

/**
 * Fragment for displaying a grid of recipe cards.
 */
class RecipeListFragment : Fragment() {

    private lateinit var viewModel: RecipeViewModel
    private lateinit var adapter: RecipeAdapter
    
    // UI components
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var searchInput: TextInputEditText
    private lateinit var filterChipGroup: com.google.android.material.chip.ChipGroup
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var emptyStateView: View
    private lateinit var loadingIndicator: View
    private lateinit var chipAll: Chip
    private lateinit var addRecipeButton: com.google.android.material.button.MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recipe_list, container, false)
        
        // Initialize UI components
        recyclerView = view.findViewById(R.id.recipe_recycler_view)
        searchInput = view.findViewById(R.id.search_input)
        filterChipGroup = view.findViewById(R.id.filter_chip_group)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        emptyStateView = view.findViewById(R.id.empty_state)
        loadingIndicator = view.findViewById(R.id.loading_indicator)
        chipAll = view.findViewById(R.id.chip_all)
        addRecipeButton = view.findViewById(R.id.add_recipe_button)
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[RecipeViewModel::class.java]
        
        // Setup RecyclerView with GridLayout
        setupRecyclerView()
        
        // Setup swipe refresh
        setupSwipeRefresh()
        
        // Setup search functionality
        setupSearch()
        
        // Setup filter chips
        setupFilterChips()
        
        // Setup add recipe button
        setupAddRecipeButton()
        
        // Observe recipes from ViewModel
        observeRecipes()
        
        // Load recipes on start
        loadRecipes()
    }
    
    private fun setupRecyclerView() {
        // Setup LinearLayoutManager for RecyclerView (1 item per row)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        
        // Initialize adapter with empty list and click listener
        adapter = RecipeAdapter(emptyList()) { recipe ->
            navigateToRecipeDetail(recipe)
        }
        recyclerView.adapter = adapter
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
    
    private fun setupSearch() {
        // Setup live search query listener
        searchInput.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
        
        // Handle search action from keyboard
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                searchInput.clearFocus()
                true
            } else {
                false
            }
        }
    }
    
    private fun setupFilterChips() {
        // Setup chip group listener
        filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_all -> viewModel.setCategoryFilter(null)
                R.id.chip_category_breakfast -> viewModel.setCategoryFilter("breakfast")
                R.id.chip_category_lunch -> viewModel.setCategoryFilter("lunch")
                R.id.chip_category_dinner -> viewModel.setCategoryFilter("dinner")
                R.id.chip_category_snack -> viewModel.setCategoryFilter("snack")
                null -> viewModel.setCategoryFilter(null)
            }
        }
        
        // Set "All" as the default selected chip
        chipAll.isChecked = true
    }
    
    private fun setupAddRecipeButton() {
        // Set click listener for add recipe button
        addRecipeButton.setOnClickListener {
            navigateToCreateRecipe()
        }
    }
    
    private fun observeRecipes() {
        // Observe filtered recipes
        viewModel.filteredRecipes.observe(viewLifecycleOwner) { recipes ->
            updateUI(recipes)
        }
        
        // Observe operation status
        viewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is RecipeViewModel.OperationStatus.Error -> {
                    // Show error message
                    Snackbar.make(requireView(), status.message, Snackbar.LENGTH_LONG).show()
                    
                    // Hide loading indicators
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.isVisible = false
                }
                is RecipeViewModel.OperationStatus.Loading -> {
                    // Show loading indicator if we don't have recipes yet
                    if (adapter.itemCount == 0) {
                        loadingIndicator.isVisible = true
                    }
                }
                is RecipeViewModel.OperationStatus.Success -> {
                    // Hide loading indicators
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.isVisible = false
                }
            }
        }
    }
    
    private fun updateUI(recipes: List<Recipe>) {
        // Update adapter
        adapter.updateRecipes(recipes)
        
        // Hide loading indicators
        swipeRefreshLayout.isRefreshing = false
        loadingIndicator.isVisible = false
        
        // Show/hide empty state
        emptyStateView.isVisible = recipes.isEmpty()
    }
    
    private fun loadRecipes() {
        // Show loading indicator
        loadingIndicator.isVisible = true
        
        // Get current user ID and load recipes
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.getCurrentUser().getOrNull()
                val currentUserId = currentUser?.id
                
                if (currentUserId != null) {
                    viewModel.loadRecipes(currentUserId)
                } else {
                    // Show error if user is not logged in
                    Snackbar.make(requireView(), getString(R.string.not_logged_in), Snackbar.LENGTH_LONG).show()
                    loadingIndicator.isVisible = false
                    emptyStateView.isVisible = true
                }
            } catch (e: Exception) {
                // Handle any exceptions
                Snackbar.make(requireView(), getString(R.string.error_loading_recipes), Snackbar.LENGTH_LONG).show()
                loadingIndicator.isVisible = false
                emptyStateView.isVisible = true
            }
        }
    }
    
    private fun navigateToRecipeDetail(recipe: Recipe) {
        val bundle = Bundle().apply {
            putString("recipeId", recipe.id)
        }
        findNavController().navigate(R.id.action_recipeList_to_recipeDetail, bundle)
    }
    
    private fun navigateToCreateRecipe() {
        findNavController().navigate(R.id.action_recipeListFragment_to_createRecipeFragment)
    }
    
    /**
     * Calculate the number of columns based on screen width following Material 3 guidelines
     */
    private fun calculateSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        
        // Get the minimum width for a recipe card
        val minCardWidthDp = 180 // Material 3 recommended minimum width for cards
        val minCardWidthPx = (minCardWidthDp * displayMetrics.density).toInt()
        
        // Calculate how many columns can fit
        val spanCount = maxOf(1, screenWidthPx / minCardWidthPx)
        return minOf(spanCount, 2) // Cap at 2 columns for better readability
    }
}