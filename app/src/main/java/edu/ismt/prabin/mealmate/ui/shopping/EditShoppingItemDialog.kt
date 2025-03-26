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

/**
 * Dialog for editing an existing shopping list item.
 */
class EditShoppingItemDialog : DialogFragment() {
    private lateinit var nameInput: TextInputEditText
    private lateinit var quantityInput: TextInputEditText
    private lateinit var unitInput: AutoCompleteTextView
    private lateinit var nameLayout: TextInputLayout
    private lateinit var quantityLayout: TextInputLayout
    private lateinit var unitLayout: TextInputLayout
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private var onItemUpdated: ((ShoppingListItem) -> Unit)? = null
    private lateinit var item: ShoppingListItem

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_edit_shopping_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        nameInput = view.findViewById(R.id.name_input)
        quantityInput = view.findViewById(R.id.quantity_input)
        unitInput = view.findViewById(R.id.unit_input)
        nameLayout = view.findViewById(R.id.name_layout)
        quantityLayout = view.findViewById(R.id.quantity_layout)
        unitLayout = view.findViewById(R.id.unit_layout)
        saveButton = view.findViewById(R.id.save_button)
        cancelButton = view.findViewById(R.id.cancel_button)

        // Setup unit dropdown
        setupUnitDropdown()
        
        // Fill in existing item data
        populateFields()

        // Setup buttons
        saveButton.setOnClickListener {
            if (validateInputs()) {
                updateShoppingListItem()?.let { updatedItem ->
                    onItemUpdated?.invoke(updatedItem)
                    dismiss()
                }
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun populateFields() {
        nameInput.setText(item.name)
        quantityInput.setText(item.quantity.toString())
        unitInput.setText(item.unit)
    }

    private fun setupUnitDropdown() {
        val units = arrayOf("pcs", "g", "kg", "ml", "l", "tbsp", "tsp", "cup")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, units)
        unitInput.setAdapter(adapter)
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

        // Validate quantity
        if (quantityInput.text.isNullOrBlank()) {
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
        if (unitInput.text.isNullOrBlank()) {
            unitLayout.error = "Unit is required"
            isValid = false
        } else {
            unitLayout.error = null
        }

        return isValid
    }

    private fun updateShoppingListItem(): ShoppingListItem? {
        return try {
            if (!this::nameInput.isInitialized || !this::quantityInput.isInitialized || !this::unitInput.isInitialized) {
                return null
            }
            
            item.copy(
                name = nameInput.text.toString().trim(),
                quantity = quantityInput.text.toString().toDouble(),
                unit = unitInput.text.toString().trim()
                // Keep existing isPurchased status
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val TAG = "EditShoppingItemDialog"

        /**
         * Create a new instance of the dialog with a callback for when an item is updated.
         */
        fun newInstance(item: ShoppingListItem, onItemUpdated: (ShoppingListItem) -> Unit): EditShoppingItemDialog {
            return EditShoppingItemDialog().apply {
                this.item = item
                this.onItemUpdated = onItemUpdated
            }
        }
    }
}