package edu.ismt.prabin.mealmate.ui.shopping

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.ShoppingListItem

/**
 * Adapter for displaying shopping list items in a RecyclerView.
 */
class ShoppingListAdapter(
    private var items: List<ShoppingListItem>,
    private val listener: ShoppingListListener
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingListViewHolder>() {

    private var filteredItems: List<ShoppingListItem> = items
    private var isSelectionMode = false

    interface ShoppingListListener {
        fun onItemClick(item: ShoppingListItem)
        fun onItemLongClick(item: ShoppingListItem): Boolean
        fun onItemPurchasedChanged(item: ShoppingListItem, isPurchased: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ShoppingListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingListViewHolder, position: Int) {
        val item = filteredItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = filteredItems.size

    fun getItemAt(position: Int): ShoppingListItem = filteredItems[position]

    fun getAllItems(): List<ShoppingListItem> = items

    fun updateItems(newItems: List<ShoppingListItem>) {
        val oldItems = items
        items = newItems
        filter("")
        
        // Calculate the differences and notify specific changes
        val removedItems = oldItems.filter { oldItem -> !newItems.contains(oldItem) }
        val addedItems = newItems.filter { newItem -> !oldItems.contains(newItem) }
        
        removedItems.forEach { oldItem ->
            val oldIndex = oldItems.indexOf(oldItem)
            notifyItemRemoved(oldIndex)
        }
        
        addedItems.forEach { newItem ->
            val newIndex = newItems.indexOf(newItem)
            notifyItemInserted(newIndex)
        }
        
        // For modified items, notify changes
        newItems.forEachIndexed { index, newItem ->
            val oldIndex = oldItems.indexOf(newItem)
            if (oldIndex != -1 && oldIndex != index) {
                notifyItemMoved(oldIndex, index)
            }
        }
    }
    
    fun setSelectionMode(enabled: Boolean) {
        if (isSelectionMode != enabled) {
            isSelectionMode = enabled
            notifyItemRangeChanged(0, itemCount)
        }
    }
    
    fun filter(query: String) {
        filteredItems = if (query.isEmpty()) {
            items
        } else {
            items.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.recipeName?.contains(query, ignoreCase = true) == true
            }
        }
        notifyDataSetChanged()
    }

    inner class ShoppingListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemName: TextView = itemView.findViewById(R.id.item_name)
        private val itemQuantity: TextView = itemView.findViewById(R.id.item_quantity)
        private val itemRecipe: TextView = itemView.findViewById(R.id.item_recipe)
        private val itemContainer: View = itemView.findViewById(R.id.item_container)
        private val itemCheckbox: CheckBox = itemView.findViewById(R.id.item_checkbox)
        private val purchasedStatus: TextView = itemView.findViewById(R.id.purchased_status)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(filteredItems[position])
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    return@setOnLongClickListener listener.onItemLongClick(filteredItems[position])
                }
                false
            }

            itemCheckbox.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemPurchasedChanged(filteredItems[position], isChecked)
                }
            }
        }

        fun bind(item: ShoppingListItem) {
            itemName.text = item.name
            itemQuantity.text = "${item.quantity} ${item.unit}"
            
            // Show recipe name if available
            if (item.recipeName.isNotEmpty()) {
                itemRecipe.visibility = View.VISIBLE
                itemRecipe.text = "From: ${item.recipeName}"
            } else {
                itemRecipe.visibility = View.GONE
            }

            // Set checkbox state without triggering the listener
            itemCheckbox.setOnCheckedChangeListener(null)
            itemCheckbox.isChecked = item.isPurchased
            itemCheckbox.setOnCheckedChangeListener { _, isChecked ->
                listener.onItemPurchasedChanged(item, isChecked)
            }

            // Show/hide purchased status text
            purchasedStatus.visibility = if (item.isPurchased) View.VISIBLE else View.GONE

            // Handle purchased state (strikethrough and background)
            if (item.isPurchased) {
                itemName.paintFlags = itemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                itemQuantity.paintFlags = itemQuantity.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                itemContainer.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.purchased_item_background))
            } else {
                itemName.paintFlags = itemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                itemQuantity.paintFlags = itemQuantity.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                itemContainer.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent))
            }

            // Handle selection mode visibility
            if (isSelectionMode) {
                val isSelected = (listener as? ShoppingListFragment)?.isItemSelected(item) == true
                itemContainer.setBackgroundColor(
                    if (isSelected) {
                        ContextCompat.getColor(itemView.context, R.color.selection_highlight)
                    } else {
                        if (item.isPurchased) 
                            ContextCompat.getColor(itemView.context, R.color.purchased_item_background)
                        else 
                            ContextCompat.getColor(itemView.context, android.R.color.transparent)
                    }
                )
                itemCheckbox.visibility = View.GONE
            } else {
                itemCheckbox.visibility = View.VISIBLE
            }
        }
    }
}