package edu.ismt.prabin.mealmate.ui.recipe

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.Ingredient
import edu.ismt.prabin.mealmate.data.model.Recipe
import edu.ismt.prabin.mealmate.data.model.ShoppingListItem
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import edu.ismt.prabin.mealmate.databinding.FragmentRecipeDetailsBinding
import edu.ismt.prabin.mealmate.ui.common.DeleteConfirmationDialog
import edu.ismt.prabin.mealmate.ui.shopping.ShoppingListViewModel
import edu.ismt.prabin.mealmate.utils.ShakeDetector
import java.util.UUID

/**
 * Fragment for displaying detailed recipe information.
 */
class RecipeDetailFragment : Fragment() {

    private lateinit var viewModel: RecipeViewModel
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private val args: RecipeDetailFragmentArgs by navArgs()
    
    private val ingredientsAdapter = IngredientsAdapter()
    
    // Flag to track if current user is the recipe creator
    private var isRecipeCreator = false
    
    private var _binding: FragmentRecipeDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    private var accelerometer: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize sensor manager and shake detector
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeDetector = ShakeDetector()

        // Check if accelerometer is available
        if (accelerometer == null) {
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                "Shake-to-share feature not available - no accelerometer found",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        
        shakeDetector.setOnShakeListener(object : ShakeDetector.OnShakeListener {
            override fun onShake() {
                viewModel.currentRecipe.value?.let { recipe ->
                    // Launch contact picker when phone is shaken
                    launchContactPicker()
                    // Show a hint about the shake feature
                    Snackbar.make(
                        requireView(),
                        getString(R.string.shake_to_share_recipe),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup toolbar navigation
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Configure toolbar with menu item click listener - no need to manually inflate the menu
        // as it will be inflated automatically by the AppCompatActivity
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            handleMenuItemClick(menuItem)
        }
        
        // Setup ingredients RecyclerView
        binding.ingredientsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ingredientsAdapter
        }
        
        // Setup add ingredient button
        binding.addIngredientButton.setOnClickListener {
            showAddIngredientDialog()
        }
        
        // Initialize ViewModels
        viewModel = ViewModelProvider(this)[RecipeViewModel::class.java]
        shoppingListViewModel = ViewModelProvider(requireActivity())[ShoppingListViewModel::class.java]
        
        // Load recipe details
        viewModel.loadRecipe(args.recipeId)
        
        // Observe current recipe
        viewModel.currentRecipe.observe(viewLifecycleOwner) { recipe ->
            recipe?.let { 
                displayRecipe(it)
                
                // Check if current user is the recipe creator
                val currentUserId = SupabaseClient.getCurrentUserId()
                isRecipeCreator = recipe.userId == currentUserId
                
                // Update menu items visibility based on creator status
                updateMenuItemsVisibility()
                
                // Show add ingredient button only if user is the recipe creator
                binding.addIngredientButton.visibility = if (isRecipeCreator) View.VISIBLE else View.GONE
            }
        }
        
        // Observe operation status
        viewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is RecipeViewModel.OperationStatus.Error -> {
                    Snackbar.make(view, status.message, Snackbar.LENGTH_LONG).show()
                }
                is RecipeViewModel.OperationStatus.Success -> {
                    if (status.message.contains("deleted")) {
                        Snackbar.make(view, getString(R.string.delete_successful), Snackbar.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else if (status.message.contains("ingredient added")) {
                        Snackbar.make(view, getString(R.string.ingredient_added), Snackbar.LENGTH_SHORT).show()
                    }
                }
                else -> {}
            }
        }
        
        // Observe shopping list operation status
        shoppingListViewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is ShoppingListViewModel.OperationStatus.Success -> {
                    if (status.message.contains("added")) {
                        Snackbar.make(view, getString(R.string.added_to_shopping_list), Snackbar.LENGTH_SHORT).show()
                    }
                }
                is ShoppingListViewModel.OperationStatus.Error -> {
                    Snackbar.make(view, status.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Only register the shake detector if accelerometer is available
        accelerometer?.let { sensor ->
            sensorManager.registerListener(shakeDetector, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        // Unregister the shake detector
        sensorManager.unregisterListener(shakeDetector)
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun displayRecipe(recipe: Recipe) {
        with(binding) {
            // Update UI with recipe data
            recipeTitle.text = recipe.title
            recipeDescription.text = recipe.description ?: ""
            cookingTimeChip.text = getString(R.string.prep_time, recipe.prepTime)
            recipeTypeChip.text = recipe.foodType
            servingsChip.text = getString(R.string.servings, recipe.servings ?: 1)
            
            // Set difficulty chip
            difficultyChip.text = when (recipe.prepTime) {
                in 0..15 -> getString(R.string.difficulty_easy)
                in 16..30 -> getString(R.string.difficulty_medium)
                else -> getString(R.string.difficulty_hard)
            }
            
            instructionsList.text = recipe.instructions
            
            // Fix image loading issues
            try {
                // Log the image URL for debugging
                println("Loading recipe image URL: ${recipe.imageUrl}")
                
                if (recipe.imageUrl.isNotEmpty()) {
                    // Clear any existing foreground drawable that might be blocking the image
                    recipeImage.foreground = null
                    
                    Glide.with(requireContext())
                        .load(recipe.imageUrl)
                        .placeholder(R.drawable.placeholder_recipe)
                        .error(R.drawable.placeholder_recipe)
                        .centerCrop()
                        .into(recipeImage)
                } else {
                    // No image URL available, show placeholder
                    recipeImage.setImageResource(R.drawable.placeholder_recipe)
                }
            } catch (e: Exception) {
                // Handle any exceptions during image loading
                println("Error loading image: ${e.message}")
                recipeImage.setImageResource(R.drawable.placeholder_recipe)
            }
            
            // Setup ingredients adapter with swipe functionality
            ingredientsAdapter.setIngredients(recipe.ingredients)
            setupIngredientsSwipe(recipe)
        }
    }
    
    private fun setupIngredientsSwipe(recipe: Recipe) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val ingredient = recipe.ingredients[position]
                
                // Reset the swipe
                ingredientsAdapter.notifyItemChanged(position)
                
                // Show quantity input dialog
                showQuantityInputDialog(ingredient, recipe.title)
            }
            
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // Draw the swipe background
                val itemView = viewHolder.itemView
                val swipeBackground = layoutInflater.inflate(
                    R.layout.item_ingredient_swipe_actions,
                    recyclerView,
                    false
                )
                
                if (dX < 0) { // Swiping left
                    swipeBackground.measure(
                        View.MeasureSpec.makeMeasureSpec(itemView.width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(itemView.height, View.MeasureSpec.EXACTLY)
                    )
                    swipeBackground.layout(0, 0, itemView.width, itemView.height)
                    
                    val saveCount = c.save()
                    c.clipRect(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    c.translate(
                        itemView.right.toFloat() + dX,
                        itemView.top.toFloat()
                    )
                    swipeBackground.draw(c)
                    c.restoreToCount(saveCount)
                }
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.ingredientsList)
    }
    
    /**
     * Show dialog to input quantity for adding to shopping list
     */
    private fun showQuantityInputDialog(ingredient: Ingredient, recipeName: String) {
        // Create dialog view
        val dialogView = layoutInflater.inflate(R.layout.dialog_quantity_input, null)
        
        // Get references to views
        val ingredientNameText = dialogView.findViewById<TextView>(R.id.ingredient_name_text)
        val quantityInput = dialogView.findViewById<TextInputEditText>(R.id.quantity_input)
        val unitInput = dialogView.findViewById<AutoCompleteTextView>(R.id.unit_input)
        
        // Set ingredient name
        ingredientNameText.text = ingredient.name
        
        // Setup unit dropdown
        val units = resources.getStringArray(R.array.measurement_units)
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        unitInput.setAdapter(unitAdapter)
        
        // Set default unit if available
        unitInput.setText(units[0], false)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_to_shopping_list))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                // Get input values
                val quantity = try {
                    quantityInput.text.toString().toDoubleOrNull() ?: 1.0
                } catch (e: Exception) {
                    1.0
                }
                
                val unit = unitInput.text.toString().ifEmpty { units[0] }
                
                // Create shopping list item
                val shoppingItem = ShoppingListItem(
                    id = UUID.randomUUID().toString(),
                    userId = SupabaseClient.getCurrentUserId() ?: "",
                    ingredientId = ingredient.id,
                    name = ingredient.name,
                    quantity = quantity,
                    unit = unit,
                    recipeId = args.recipeId,
                    recipeName = recipeName
                )
                
                // Add to shopping list
                shoppingListViewModel.addShoppingListItem(shoppingItem)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    // Contact picker result launcher
    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { contactUri ->
                // Get the phone number from the selected contact
                val phoneNumber = getPhoneNumberFromContact(contactUri)
                if (phoneNumber.isNotEmpty()) {
                    // Get the current recipe and format it for SMS
                    viewModel.currentRecipe.value?.let { recipe ->
                        val smsText = viewModel.formatRecipeForSMS(recipe)
                        sendSMS(phoneNumber, smsText)
                    }
                } else {
                    Snackbar.make(requireView(), "Could not find a phone number for this contact", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupMenu() {
        // The old menu provider implementation is removed since we're using toolbar directly
    }
    
    /**
     * Updates the visibility of menu items based on whether the current user is the recipe creator
     */
    private fun updateMenuItemsVisibility() {
        binding.toolbar.menu?.let { menu ->
            menu.findItem(R.id.action_overflow)?.isVisible = true
            // Hide edit/delete menu items based on creator status
            menu.findItem(R.id.action_overflow)?.subMenu?.let { subMenu ->
                subMenu.findItem(R.id.action_edit_recipe)?.isVisible = isRecipeCreator
                subMenu.findItem(R.id.action_delete_recipe)?.isVisible = isRecipeCreator
            }
        }
    }
    
    /**
     * Handles menu item clicks
     */
    private fun handleMenuItemClick(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_edit_recipe -> {
                // Navigate to edit recipe screen
                val action = RecipeDetailFragmentDirections
                    .actionRecipeDetailFragmentToCreateRecipeFragment(args.recipeId)
                findNavController().navigate(action)
                true
            }
            R.id.action_delete_recipe -> {
                // Show confirmation dialog before deleting
                showDeleteConfirmationDialog()
                true
            }
            R.id.action_share_sms -> {
                // Launch contact picker to share recipe via SMS
                launchContactPicker()
                true
            }
            R.id.action_share_email -> {
                // Share recipe via email
                shareViaEmail()
                true
            }
            R.id.action_share_social -> {
                // Share recipe via social media
                shareViaSocial()
                true
            }
            R.id.action_add_to_shopping_list -> {
                // Add recipe ingredients to shopping list with quantity dialog
                viewModel.currentRecipe.value?.let { recipe ->
                    showAddToShoppingListDialog(recipe)
                }
                true
            }
            else -> false
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        // Only show delete confirmation if user is the recipe creator
        if (!isRecipeCreator) {
            Snackbar.make(requireView(), getString(R.string.cannot_delete_others_recipe), Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // Use the reusable DeleteConfirmationDialog
        DeleteConfirmationDialog.newInstance(
            title = getString(R.string.delete_recipe),
            message = getString(R.string.delete_recipe_confirmation),
            onConfirm = {
                val currentUserId = SupabaseClient.getCurrentUserId() ?: ""
                viewModel.deleteRecipe(args.recipeId, currentUserId)
            }
        ).show(childFragmentManager, DeleteConfirmationDialog.TAG)
    }
    
    /**
     * Share recipe via email
     */
    private fun shareViaEmail() {
        viewModel.currentRecipe.value?.let { recipe ->
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"  // Use email mime type
                putExtra(Intent.EXTRA_SUBJECT, "Recipe: ${recipe.title}")
                putExtra(Intent.EXTRA_TEXT, viewModel.formatRecipeForSMS(recipe))
            }
            
            try {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.share_via_email)))
            } catch (e: Exception) {
                Snackbar.make(requireView(), getString(R.string.no_email_app_found), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Share recipe via social media or other apps
     */
    private fun shareViaSocial() {
        viewModel.currentRecipe.value?.let { recipe ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Recipe: ${recipe.title}")
                putExtra(Intent.EXTRA_TEXT, viewModel.formatRecipeForSMS(recipe))
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share_recipe))
            startActivity(chooserIntent)
        }
    }
    
    /**
     * Adapter for displaying ingredients in a list.
     */
    inner class IngredientsAdapter :
        RecyclerView.Adapter<IngredientsAdapter.IngredientViewHolder>() {
        
        private var ingredients: List<Ingredient> = emptyList()

        fun setIngredients(newIngredients: List<Ingredient>) {
            ingredients = newIngredients
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ingredient, parent, false)
            return IngredientViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
            holder.bind(ingredients[position])
        }
        
        override fun getItemCount(): Int = ingredients.size
        
        inner class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ingredientName: TextView = itemView.findViewById(R.id.ingredient_name)
            
            fun bind(ingredient: Ingredient) {
                ingredientName.text = ingredient.name
            }
        }
    }
    
    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }
    
    private fun getPhoneNumberFromContact(contactUri: Uri): String {
        var phoneNumber = ""
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        
        requireContext().contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            }
        }
        
        return phoneNumber
    }
    
    private fun sendSMS(phoneNumber: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
        }
        startActivity(intent)
    }
    
    private fun showAddToShoppingListDialog(recipe: Recipe) {
        val ingredients = recipe.ingredients
        var currentIndex = 0

        fun showIngredientDialog(ingredient: Ingredient) {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_quantity_input, null)

            val ingredientNameText = dialogView.findViewById<TextView>(R.id.ingredient_name_text)
            val quantityInput = dialogView.findViewById<TextInputEditText>(R.id.quantity_input)
            val unitInput = dialogView.findViewById<AutoCompleteTextView>(R.id.unit_input)

            // Setup unit dropdown
            val units = resources.getStringArray(R.array.measurement_units)
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, units)
            unitInput.setAdapter(adapter)

            // Pre-fill the name
            ingredientNameText.text = ingredient.name

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.specify_quantity))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.add)) { _, _ ->
                    try {
                        val quantity = quantityInput.text.toString().toDoubleOrNull() ?: 1.0
                        val unit = unitInput.text.toString()

                        val shoppingItem = ShoppingListItem(
                            id = UUID.randomUUID().toString(),
                            userId = SupabaseClient.getCurrentUserId() ?: "",
                            ingredientId = ingredient.id,
                            name = ingredient.name,  // Using ingredient name directly
                            quantity = quantity,
                            unit = unit,
                            recipeId = recipe.id,
                            recipeName = recipe.title
                        )

                        shoppingListViewModel.addShoppingListItem(shoppingItem)
                        
                        // Move to next ingredient or show completion message
                        currentIndex++
                        if (currentIndex < ingredients.size) {
                            showIngredientDialog(ingredients[currentIndex])
                        } else {
                            Snackbar.make(requireView(), getString(R.string.added_to_shopping_list), Snackbar.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Snackbar.make(requireView(), getString(R.string.invalid_quantity), Snackbar.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(getString(R.string.skip)) { _, _ ->
                    // Skip this ingredient and move to next
                    currentIndex++
                    if (currentIndex < ingredients.size) {
                        showIngredientDialog(ingredients[currentIndex])
                    } else {
                        Snackbar.make(requireView(), getString(R.string.added_to_shopping_list), Snackbar.LENGTH_SHORT).show()
                    }
                }
                .setNeutralButton(getString(R.string.cancel)) { _, _ ->
                    // Cancel the entire operation
                }
                .show()
        }

        // Start with the first ingredient
        if (ingredients.isNotEmpty()) {
            showIngredientDialog(ingredients[0])
        }
    }
    
    /**
     * Shows dialog to add a new ingredient to the recipe
     */
    private fun showAddIngredientDialog() {
        // Only show add ingredient dialog if user is the recipe creator
        if (!isRecipeCreator) {
            Snackbar.make(requireView(), getString(R.string.cannot_edit_others_recipe), Snackbar.LENGTH_SHORT).show()
            return
        }
        
        AddIngredientDialog.newInstance(args.recipeId) { newIngredient ->
            // Add ingredient to the recipe
            viewModel.addIngredientToRecipe(newIngredient)
        }.show(childFragmentManager, AddIngredientDialog.TAG)
    }
}