package edu.ismt.prabin.mealmate.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.ShoppingListItem
import java.util.UUID

/**
 * Dialog for adding a new shopping list item.
 */
class AddShoppingItemDialog : DialogFragment() {
    private var _binding: View? = null
    private val binding get() = _binding!!
    
    private lateinit var nameInput: TextInputEditText
    private lateinit var quantityInput: TextInputEditText
    private lateinit var unitInput: AutoCompleteTextView
    private lateinit var nameLayout: TextInputLayout
    private lateinit var quantityLayout: TextInputLayout
    private lateinit var unitLayout: TextInputLayout
    private lateinit var addButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private var onItemAdded: ((ShoppingListItem) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflater.inflate(R.layout.dialog_add_shopping_item, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initializeViews(view)
        
        // Setup unit dropdown
        setupUnitDropdown()

        // Setup buttons
        setupButtonListeners()
    }
    
    private fun initializeViews(view: View) {
        try {
            // Use the correct IDs from the layout file
            nameInput = view.findViewById(R.id.item_name_input)
            quantityInput = view.findViewById(R.id.quantity_input)
            unitInput = view.findViewById(R.id.unit_input)
            nameLayout = view.findViewById(R.id.item_name_layout)
            quantityLayout = view.findViewById(R.id.quantity_layout)
            unitLayout = view.findViewById(R.id.unit_layout)
            addButton = view.findViewById(R.id.add_button)
            cancelButton = view.findViewById(R.id.cancel_button)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupButtonListeners() {
        addButton.setOnClickListener {
            if (this::nameInput.isInitialized && validateInputs()) {
                createShoppingListItem()?.let { item ->
                    onItemAdded?.invoke(item)
                    dismiss()
                }
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupUnitDropdown() {
        if (this::unitInput.isInitialized) {
            val units = arrayOf("pcs", "g", "kg", "ml", "l", "tbsp", "tsp", "cup")
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, units)
            unitInput.setAdapter(adapter)
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate name
        if (!this::nameInput.isInitialized || nameInput.text.isNullOrBlank()) {
            nameLayout.error = "Name is required"
            isValid = false
        } else {
            nameLayout.error = null
        }

        // Validate quantity
        if (!this::quantityInput.isInitialized || quantityInput.text.isNullOrBlank()) {
            quantityLayout.error = "Quantity is required"
            isValid = false
        } else {
            try {
                quantityInput.text.toString().toDouble()
                quantityLayout.error = null
            } catch (e: NumberFormatException) {
                quantityLayout.error = "Invalid quantity"
                isValid = false
            }
        }

        // Validate unit
        if (!this::unitInput.isInitialized || unitInput.text.isNullOrBlank()) {
            unitLayout.error = "Unit is required"
            isValid = false
        } else {
            unitLayout.error = null
        }

        return isValid
    }

    private fun createShoppingListItem(): ShoppingListItem? {
        return try {
            if (!this::nameInput.isInitialized || !this::quantityInput.isInitialized || !this::unitInput.isInitialized) {
                return null
            }
            
            ShoppingListItem(
                id = UUID.randomUUID().toString(),
                userId = "", // This will be set in the repository
                name = nameInput.text.toString().trim(),
                quantity = quantityInput.text.toString().toDouble(),
                unit = unitInput.text.toString().trim(),
                ingredientId = "", // Empty for manual items
                recipeId = "", // Empty for manual items
                recipeName = "" // Empty for manual items
            )
        } catch (e: Exception) {
            null
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddShoppingItemDialog"

        /**
         * Create a new instance of the dialog with a callback for when an item is added.
         */
        fun newInstance(onItemAdded: (ShoppingListItem) -> Unit): AddShoppingItemDialog {
            return AddShoppingItemDialog().apply {
                this.onItemAdded = onItemAdded
            }
        }
    }
}