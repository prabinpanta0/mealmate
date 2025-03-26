package edu.ismt.prabin.mealmate.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.Ingredient
import java.util.UUID

/**
 * Dialog for adding a new ingredient to a recipe.
 */
class AddIngredientDialog : DialogFragment() {
    private lateinit var nameInput: TextInputEditText
    private lateinit var nameLayout: TextInputLayout
    private lateinit var categoryInput: AutoCompleteTextView
    private lateinit var categoryLayout: TextInputLayout
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var addButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private var recipeId: String = ""
    private var onIngredientAdded: ((Ingredient) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Using dialog_add_ingredient layout (we'll create this)
        return inflater.inflate(R.layout.dialog_add_ingredient, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        nameInput = view.findViewById(R.id.ingredient_name_input)
        nameLayout = view.findViewById(R.id.ingredient_name_layout)
        categoryInput = view.findViewById(R.id.category_input)
        categoryLayout = view.findViewById(R.id.category_layout)
        descriptionInput = view.findViewById(R.id.description_input)
        addButton = view.findViewById(R.id.add_button)
        cancelButton = view.findViewById(R.id.cancel_button)

        // Setup category dropdown
        setupCategoryDropdown()

        // Setup buttons
        addButton.setOnClickListener {
            if (validateInputs()) {
                createIngredient()?.let { ingredient ->
                    onIngredientAdded?.invoke(ingredient)
                    dismiss()
                }
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf(
            "Fruit", "Vegetable", "Meat", "Dairy", "Grain", 
            "Spice", "Herb", "Oil", "Sauce", "Other"
        )
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
        categoryInput.setAdapter(adapter)
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate name
        if (nameInput.text.isNullOrBlank()) {
            nameLayout.error = "Name is required"
            isValid = false
        } else {
            nameLayout.error = null
        }

        // Validate category
        if (categoryInput.text.isNullOrBlank()) {
            categoryLayout.error = "Category is required"
            isValid = false
        } else {
            categoryLayout.error = null
        }

        return isValid
    }

    private fun createIngredient(): Ingredient? {
        return try {
            Ingredient(
                id = UUID.randomUUID().toString(),
                recipeId = recipeId,
                name = nameInput.text.toString().trim(),
                category = categoryInput.text.toString().trim(),
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val TAG = "AddIngredientDialog"

        /**
         * Create a new instance of the dialog with a callback for when an ingredient is added.
         */
        fun newInstance(recipeId: String, onIngredientAdded: (Ingredient) -> Unit): AddIngredientDialog {
            return AddIngredientDialog().apply {
                this.recipeId = recipeId
                this.onIngredientAdded = onIngredientAdded
            }
        }
    }
} 