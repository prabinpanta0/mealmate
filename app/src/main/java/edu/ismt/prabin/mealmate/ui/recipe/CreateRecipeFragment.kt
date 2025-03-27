package edu.ismt.prabin.mealmate.ui.recipe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.Ingredient
import edu.ismt.prabin.mealmate.data.model.Recipe
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import java.util.*

/**
 * Fragment for creating or editing a recipe.
 */
class CreateRecipeFragment : Fragment() {

    private lateinit var viewModel: RecipeViewModel
    private val args by navArgs<CreateRecipeFragmentArgs>()
    
    // UI components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var recipeImage: ImageView
    private lateinit var addImageFab: FloatingActionButton
    private lateinit var titleInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var categoryInput: AutoCompleteTextView
    private lateinit var cookingTimeInput: TextInputEditText
    private lateinit var servingsInput: TextInputEditText
    private lateinit var ingredientsChipGroup: ChipGroup
    private lateinit var addIngredientInput: TextInputEditText
    private lateinit var addIngredientLayout: TextInputLayout
    private lateinit var instructionsInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator
    
    // State variables
    private var selectedImageUri: Uri? = null
    private var isEditMode = false
    private var currentRecipe: Recipe? = null
    private var hasEditPermission = true
    
    // Image picker launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                displaySelectedImage(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_recipe, container, false)
        
        // Initialize UI components
        toolbar = view.findViewById(R.id.toolbar)
        recipeImage = view.findViewById(R.id.recipe_image)
        addImageFab = view.findViewById(R.id.add_image_fab)
        titleInput = view.findViewById(R.id.title_input)
        descriptionInput = view.findViewById(R.id.description_input)
        categoryInput = view.findViewById(R.id.category_input)
        cookingTimeInput = view.findViewById(R.id.cooking_time_input)
        servingsInput = view.findViewById(R.id.servings_input)
        ingredientsChipGroup = view.findViewById(R.id.ingredients_chip_group)
        addIngredientInput = view.findViewById(R.id.add_ingredient_input)
        addIngredientLayout = view.findViewById(R.id.add_ingredient_layout)
        instructionsInput = view.findViewById(R.id.instructions_input)
        saveButton = view.findViewById(R.id.save_button)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        
        // Setup click listeners
        addImageFab.setOnClickListener { openImagePicker() }
        addIngredientLayout.setEndIconOnClickListener { addIngredient() }
        saveButton.setOnClickListener { saveRecipe() }

        // Setup category dropdown
        val categories = resources.getStringArray(R.array.recipe_categories)
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        categoryInput.setAdapter(categoryAdapter)
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[RecipeViewModel::class.java]
        
        // Check if we're in edit mode
        isEditMode = args.recipeId.isNotEmpty()
        
        // Set toolbar title based on mode
        toolbar.title = if (isEditMode) getString(R.string.edit_recipe) else getString(R.string.create_recipe_title)
        
        // Setup toolbar navigation
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Observe operation status
        viewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is RecipeViewModel.OperationStatus.Loading -> {
                    // Show loading indicator
                    saveButton.isEnabled = false
                    progressIndicator.visibility = View.VISIBLE
                }
                is RecipeViewModel.OperationStatus.Success -> {
                    // Hide loading indicator
                    progressIndicator.visibility = View.GONE
                    // Show success message and navigate back
                    Snackbar.make(view, status.message, Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is RecipeViewModel.OperationStatus.Error -> {
                    // Hide loading indicator
                    progressIndicator.visibility = View.GONE
                    // Show error message
                    Snackbar.make(view, status.message, Snackbar.LENGTH_LONG).show()
                    saveButton.isEnabled = true
                }
                else -> {}
            }
        }
        
        // If in edit mode, load the recipe
        if (isEditMode) {
            viewModel.loadRecipe(args.recipeId)
            viewModel.currentRecipe.observe(viewLifecycleOwner) { recipe ->
                recipe?.let {
                    currentRecipe = it
                    
                    // Check if current user is the recipe creator
                    val currentUserId = SupabaseClient.getCurrentUserId()
                    hasEditPermission = recipe.userId == currentUserId
                    
                    if (hasEditPermission) {
                        // Populate form with recipe data
                        populateFormWithRecipe(it)
                    } else {
                        // Show error message and navigate back
                        Snackbar.make(view, getString(R.string.cannot_edit_others_recipe), Snackbar.LENGTH_LONG).show()
                        // Disable all input fields
                        disableAllInputs()
                        // Navigate back after a short delay
                        view.postDelayed({ findNavController().navigateUp() }, 2000)
                    }
                }
            }
        } else {
            // Add initial ingredient field
            addIngredientField()
        }
    }
    
    /**
     * Disable all input fields when user doesn't have edit permission
     */
    private fun disableAllInputs() {
        titleInput.isEnabled = false
        descriptionInput.isEnabled = false
        categoryInput.isEnabled = false
        cookingTimeInput.isEnabled = false
        servingsInput.isEnabled = false
        addIngredientInput.isEnabled = false
        addIngredientLayout.isEnabled = false
        instructionsInput.isEnabled = false
        saveButton.isEnabled = false
        addImageFab.isEnabled = false
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
    private fun displaySelectedImage(uri: Uri) {
        Glide.with(requireContext())
            .load(uri)
            .centerCrop()
            .into(recipeImage)
    }
    
    private fun addIngredient() {
        val ingredientText = addIngredientInput.text.toString().trim()
        if (ingredientText.isEmpty()) {
            return
        }
        
        // Create a new chip for the ingredient
        val chip = Chip(requireContext()).apply {
            text = ingredientText
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                ingredientsChipGroup.removeView(this)
            }
        }
        
        // Add the chip to the chip group
        ingredientsChipGroup.addView(chip)
        
        // Clear the input field
        addIngredientInput.text?.clear()
    }
    
    private fun populateFormWithRecipe(recipe: Recipe) {
        titleInput.setText(recipe.title)
        descriptionInput.setText(recipe.description ?: "")
        categoryInput.setText(recipe.foodType)
        cookingTimeInput.setText(recipe.prepTime.toString())
        servingsInput.setText(recipe.servings?.toString() ?: "1")
        instructionsInput.setText(recipe.instructions)
        
        // Load image if available
        if (recipe.imageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(recipe.imageUrl)
                .centerCrop()
                .into(recipeImage)
        }
        
        // Clear existing ingredients and add the ones from the recipe
        ingredientsChipGroup.removeAllViews()
        recipe.ingredients.forEach { ingredient ->
            val chip = Chip(requireContext()).apply {
                text = ingredient.name
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    ingredientsChipGroup.removeView(this)
                }
            }
            ingredientsChipGroup.addView(chip)
        }
    }
    
    private fun addIngredientField() {
        // This is just a placeholder for adding an initial empty ingredient field
        // In this implementation, we're using a single input field with a chip group
    }
    
    private fun saveRecipe() {
        // Check if user has permission to edit
        if (isEditMode && !hasEditPermission) {
            Snackbar.make(requireView(), getString(R.string.cannot_edit_others_recipe), Snackbar.LENGTH_LONG).show()
            return
        }
        
        // Validate inputs
        val title = titleInput.text.toString().trim()
        if (title.isEmpty()) {
            titleInput.error = "Title is required"
            return
        }
        
        val description = descriptionInput.text.toString().trim()
        val foodType = categoryInput.text.toString().trim()
        if (foodType.isEmpty()) {
            categoryInput.error = "Category is required"
            return
        }
        
        val prepTimeStr = cookingTimeInput.text.toString().trim()
        if (prepTimeStr.isEmpty()) {
            cookingTimeInput.error = "Cooking time is required"
            return
        }
        val prepTime = prepTimeStr.toIntOrNull() ?: 0
        
        val servingsStr = servingsInput.text.toString().trim()
        val servings = servingsStr.toIntOrNull() ?: 1
        
        val instructions = instructionsInput.text.toString().trim()
        if (instructions.isEmpty()) {
            instructionsInput.error = "Instructions are required"
            return
        }
        
        // Get ingredients from chip group
        val ingredients = mutableListOf<Ingredient>()
        for (i in 0 until ingredientsChipGroup.childCount) {
            val chip = ingredientsChipGroup.getChildAt(i) as? Chip ?: continue
            val name = chip.text.toString()
            
            ingredients.add(
                Ingredient(
                    id = UUID.randomUUID().toString(),
                    recipeId = currentRecipe?.id ?: "",
                    name = name,
                    category = foodType
                )
            )
        }
        
        val currentUserId = SupabaseClient.getCurrentUserId() ?: ""
        val recipe = Recipe(
            id = currentRecipe?.id ?: "",
            userId = currentUserId,
            title = title,
            description = description,
            instructions = instructions,
            prepTime = prepTime,
            servings = servings,
            imageUrl = currentRecipe?.imageUrl ?: "",
            foodType = foodType,
            createdAt = currentRecipe?.createdAt ?: System.currentTimeMillis(),
            ingredients = ingredients
        )
        
        // Show loading indicator
        progressIndicator.visibility = View.VISIBLE
        saveButton.isEnabled = false
        
        // Save recipe with image if one was selected
        if (isEditMode) {
            viewModel.updateRecipe(recipe, selectedImageUri)
        } else {
            viewModel.createRecipe(recipe, selectedImageUri)
        }
    }
}