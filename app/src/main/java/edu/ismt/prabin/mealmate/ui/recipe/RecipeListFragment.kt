package edu.ismt.prabin.mealmate.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.search.SearchBar
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.Recipe
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import kotlinx.coroutines.launch

/**
 * Fragment for displaying a grid of recipe cards.
 */
class RecipeListFragment : Fragment() {

    private lateinit var viewModel: RecipeViewModel
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var searchResultAdapter: RecipeAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchResultRecyclerView: RecyclerView
    private lateinit var searchBar: SearchBar
    private lateinit var searchView: com.google.android.material.search.SearchView
    private lateinit var filterChipGroup: com.google.android.material.chip.ChipGroup
    private lateinit var chipClearFilters: Chip
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchSwipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recipe_list, container, false)
        
        // Initialize UI components
        recyclerView = view.findViewById(R.id.recipe_recycler_view)
        searchResultRecyclerView = view.findViewById(R.id.search_result_recycler_view)
        searchBar = view.findViewById(R.id.search_bar)
        searchView = view.findViewById(R.id.search_view)
        filterChipGroup = view.findViewById(R.id.filter_chip_group)
        chipClearFilters = view.findViewById(R.id.chip_clear_filters)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        searchSwipeRefreshLayout = view.findViewById(R.id.searchSwipeRefreshLayout)
        
        // Setup swipe refresh layouts
        setupSwipeRefresh()
        
        // Setup main RecyclerView with single column layout
        val mainLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = mainLayoutManager
        
        // Setup search result RecyclerView with single column layout
        val searchLayoutManager = LinearLayoutManager(context)
        searchResultRecyclerView.layoutManager = searchLayoutManager
        
        // Initialize adapters
        recipeAdapter = RecipeAdapter(emptyList()) { recipe ->
            navigateToRecipeDetail(recipe)
        }
        recyclerView.adapter = recipeAdapter
        
        searchResultAdapter = RecipeAdapter(emptyList()) { recipe ->
            searchView.hide()
            navigateToRecipeDetail(recipe)
        }
        searchResultRecyclerView.adapter = searchResultAdapter
        
        return view
    }

    private fun setupSwipeRefresh() {
        // Configure main swipe refresh layout
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.secondary,
            R.color.primary_dark
        )
        
        swipeRefreshLayout.setOnRefreshListener {
            refreshRecipes()
        }
        
        // Configure search swipe refresh layout
        searchSwipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.secondary,
            R.color.primary_dark
        )
        
        searchSwipeRefreshLayout.setOnRefreshListener {
            refreshRecipes()
        }
    }
    
    private fun refreshRecipes() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentUser = SupabaseClient.getCurrentUser().getOrNull()
            val currentUserId = currentUser?.id ?: ""
            if (currentUserId.isNotEmpty()) {
                viewModel.loadRecipes(currentUserId)
            }
            
            // Hide both refresh indicators when done
            swipeRefreshLayout.isRefreshing = false
            searchSwipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[RecipeViewModel::class.java]
        
        // Setup search functionality
        setupSearch()
        
        // Setup filter chips
        setupFilterChips()
        
        // Observe recipes
        viewModel.filteredRecipes.observe(viewLifecycleOwner) { recipes ->
            recipeAdapter.updateRecipes(recipes)
            searchResultAdapter.updateRecipes(recipes)
            
            // Show empty state if no recipes
            if (recipes.isEmpty()) {
                // You can add an empty state view here if needed
            }
            
            // Hide refresh indicators
            swipeRefreshLayout.isRefreshing = false
            searchSwipeRefreshLayout.isRefreshing = false
        }
        
        // Load recipes for current user
        swipeRefreshLayout.isRefreshing = true
        refreshRecipes()
    }
    
    private fun setupSearch() {
        // Setup search view
        searchView.addTransitionListener { _, previousState, newState ->
            if (newState == com.google.android.material.search.SearchView.TransitionState.SHOWING) {
                // When search view is showing, update the search results adapter with all recipes
                viewModel.recipes.value?.let { searchResultAdapter.updateRecipes(it) }
            }
        }
        
        // Setup live search query listener
        searchView.editText.setOnEditorActionListener(null) // Remove the old listener
        searchView.editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })

        // Observe filtered recipes for search results
        viewModel.filteredRecipes.observe(viewLifecycleOwner) { recipes ->
            searchResultAdapter.updateRecipes(recipes)
            recipeAdapter.updateRecipes(recipes)
        }
    }
    
    private fun setupFilterChips() {
        // Setup category and time filter chips
        filterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_category_breakfast -> viewModel.setCategoryFilter("breakfast")
                R.id.chip_category_lunch -> viewModel.setCategoryFilter("lunch")
                R.id.chip_category_dinner -> viewModel.setCategoryFilter("dinner")
                R.id.chip_category_snack -> viewModel.setCategoryFilter("snack")
                R.id.chip_time_15 -> viewModel.setTimeFilter(15)
                R.id.chip_time_30 -> viewModel.setTimeFilter(30)
                R.id.chip_time_60 -> viewModel.setTimeFilter(60)
                null -> {
                    viewModel.setCategoryFilter(null)
                    viewModel.setTimeFilter(null)
                }
            }
        }

        // Setup clear filters chip
        chipClearFilters.setOnClickListener {
            filterChipGroup.clearCheck()
            viewModel.clearFilters()
        }
    }
    
    private fun navigateToRecipeDetail(recipe: Recipe) {
        // Use resource ID directly instead of Directions class
        val bundle = Bundle().apply {
            putString("recipeId", recipe.id)
        }
        findNavController().navigate(R.id.action_recipeList_to_recipeDetail, bundle)
    }

    /**
     * Calculate the number of columns based on screen width following Material 3 guidelines
     */
    private fun calculateSpanCount(): Int {
        // Get the screen width in pixels
        val displayMetrics = resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        
        // Get the minimum width for a recipe card (300dp converted to px)
        val minCardWidthDp = 180 // Material 3 recommended minimum width for cards
        val minCardWidthPx = (minCardWidthDp * displayMetrics.density).toInt()
        
        // Calculate how many columns can fit
        val spanCount = maxOf(1, screenWidthPx / minCardWidthPx)
        return minOf(spanCount, 2) // Cap at 2 columns for better readability
    }
}