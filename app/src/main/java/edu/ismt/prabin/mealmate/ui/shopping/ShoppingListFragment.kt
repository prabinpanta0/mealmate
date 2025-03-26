package edu.ismt.prabin.mealmate.ui.shopping

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.model.ShoppingListItem
import edu.ismt.prabin.mealmate.ui.common.DeleteConfirmationDialog
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.widget.CheckBox
import android.widget.AdapterView.AdapterContextMenuInfo

/**
 * Fragment for displaying and managing the shopping list.
 */
class ShoppingListFragment : Fragment(), ShoppingListAdapter.ShoppingListListener {

    private val viewModel: ShoppingListViewModel by viewModels()
    private lateinit var adapter: ShoppingListAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var addItemFab: FloatingActionButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    // Selected items for batch operations
    private val selectedItems = mutableSetOf<ShoppingListItem>()
    private var isInSelectionMode = false
    
    private fun enterSelectionMode() {
        if (!isInSelectionMode) {
            isInSelectionMode = true
            adapter.setSelectionMode(true)
            // Update menu items visibility
            requireActivity().invalidateOptionsMenu()
            // Force menu recreation
            setupMenu()
        }
    }
    
    private fun exitSelectionMode() {
        if (isInSelectionMode) {
            isInSelectionMode = false
            selectedItems.clear()
            adapter.setSelectionMode(false)
            // Update menu items visibility
            requireActivity().invalidateOptionsMenu()
            // Force menu recreation
            setupMenu()
        }
    }
    
    private fun toggleItemSelection(item: ShoppingListItem) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
        adapter.notifyDataSetChanged()
        
        if (selectedItems.isEmpty()) {
            exitSelectionMode()
        }
    }
    
    private fun selectAllItems() {
        selectedItems.clear()
        selectedItems.addAll(adapter.getAllItems())
        adapter.notifyDataSetChanged()
        enterSelectionMode()
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
                    // Format the shopping list for SMS
                    viewModel.formatShoppingListForSMS()
                    // Observe the formatted text and send SMS when ready
                    viewModel.smsText.observe(viewLifecycleOwner) { smsText ->
                        if (smsText.isNotEmpty()) {
                            sendSMS(phoneNumber, smsText)
                            // Remove the observer to prevent multiple SMS sends
                            viewModel.smsText.removeObservers(viewLifecycleOwner)
                        }
                    }
                } else {
                    Snackbar.make(requireView(), "Could not find a phone number for this contact", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shopping_list, container, false)
        
        // Initialize UI components
        recyclerView = view.findViewById(R.id.shopping_list_recycler_view)
        emptyView = view.findViewById(R.id.empty_view)
        addItemFab = view.findViewById(R.id.floating_action_button)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        
        // Setup toolbar
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (requireActivity() as androidx.appcompat.app.AppCompatActivity).setSupportActionBar(toolbar)
        
        // Setup SwipeRefreshLayout
        setupSwipeRefresh()
        
        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        
        // Initialize adapter
        adapter = ShoppingListAdapter(emptyList(), this)
        recyclerView.adapter = adapter
        
        // Setup FAB for adding new items
        addItemFab.setOnClickListener {
            showAddItemDialog()
        }
        
        // Setup swipe to delete
        setupSwipeToDelete()
        
        // Observe shopping list items
        viewModel.shoppingListItems.observe(viewLifecycleOwner) { items ->
            updateList(items)
            swipeRefreshLayout.isRefreshing = false
        }
        
        return view
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.secondary,
            R.color.primary_dark
        )
        
        swipeRefreshLayout.setOnRefreshListener {
            refreshShoppingList()
        }
    }
    
    private fun refreshShoppingList() {
        viewModel.loadShoppingList()
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup menu
        setupMenu()
        
        // Observe operation status
        viewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is ShoppingListViewModel.OperationStatus.Error -> {
                    Snackbar.make(view, status.message, Snackbar.LENGTH_LONG).show()
                    swipeRefreshLayout.isRefreshing = false
                }
                is ShoppingListViewModel.OperationStatus.Success -> {
                    if (status.message.contains("deleted")) {
                        Snackbar.make(view, getString(R.string.delete_successful), Snackbar.LENGTH_SHORT).show()
                    } else if (status.message.contains("purchased")) {
                        // No need to show a snackbar for purchase status changes
                        // The UI updates automatically through the LiveData observation
                    }
                    swipeRefreshLayout.isRefreshing = false
                }
                else -> {
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
        
        // Load shopping list
        swipeRefreshLayout.isRefreshing = true
        refreshShoppingList()
    }
    
    /**
     * Check if an item is selected
     */
    fun isItemSelected(item: ShoppingListItem): Boolean {
        return selectedItems.contains(item)
    }
    
    private fun setupMenu() {
        // Remove any existing menu providers
        requireActivity().removeMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        })

        // Add new menu provider
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear() // Clear existing menu items
                menuInflater.inflate(R.menu.menu_shopping_list, menu)
                
                // Setup search functionality
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as? SearchView
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = false
                    override fun onQueryTextChange(newText: String?): Boolean {
                        adapter.filter(newText ?: "")
                        return true
                    }
                })
                
                // Show/hide menu items based on selection mode and selected items count
                menu.findItem(R.id.action_search)?.isVisible = !isInSelectionMode
                menu.findItem(R.id.action_share)?.isVisible = isInSelectionMode && selectedItems.isNotEmpty()
                menu.findItem(R.id.action_select_all)?.isVisible = !isInSelectionMode
                menu.findItem(R.id.action_delete_selected)?.isVisible = isInSelectionMode && selectedItems.isNotEmpty()
                menu.findItem(R.id.action_mark_purchased)?.isVisible = isInSelectionMode && selectedItems.isNotEmpty()
                menu.findItem(R.id.action_cancel_selection)?.isVisible = isInSelectionMode
            }
            
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_share -> {
                        launchContactPicker()
                        true
                    }
                    R.id.action_select_all -> {
                        selectAllItems()
                        true
                    }
                    R.id.action_delete_selected -> {
                        if (selectedItems.isNotEmpty()) {
                            showDeleteSelectedConfirmationDialog()
                        }
                        true
                    }
                    R.id.action_mark_purchased -> {
                        markSelectedItemsAsPurchased()
                        true
                    }
                    R.id.action_cancel_selection -> {
                        exitSelectionMode()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        
        // Register context menu for RecyclerView items
        registerForContextMenu(recyclerView)
    }
    
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v.id == R.id.shopping_list_recycler_view) {
            val activity = requireActivity()
            val menuInflater = activity.menuInflater
            menuInflater.inflate(R.menu.context_menu_shopping_item, menu)
        }
    }
    
    // Add this extension property for MenuItem to support data
    private var MenuItem.data: Any?
        get() = this.actionView?.tag
        set(value) {
            if (this.actionView == null) {
                this.actionView = View(null).apply { tag = value }
            } else {
                this.actionView?.tag = value
            }
        }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = (item.menuInfo as? AdapterContextMenuInfo)?.position ?: -1
        if (position == -1) {
            // Try to get position from custom implementation
            val info = item.data as? Int ?: return false
            val shoppingItem = adapter.getItemAt(info)
            
            return when (item.itemId) {
                R.id.action_edit -> {
                    showEditItemDialog(shoppingItem)
                    true
                }
                R.id.action_delete -> {
                    showDeleteItemConfirmationDialog(shoppingItem)
                    true
                }
                else -> super.onContextItemSelected(item)
            }
        }
        return super.onContextItemSelected(item)
    }
    
    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.getItemAt(position)
                
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // Show delete confirmation dialog
                        showDeleteItemConfirmationDialog(item)
                    }
                    ItemTouchHelper.RIGHT -> {
                        // Show edit dialog
                        showEditItemDialog(item)
                    }
                }
                
                // Reset the swipe
                adapter.notifyItemChanged(position)
            }
        })
        
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
    
    private fun showAddItemDialog() {
        // Show dialog to add a new shopping list item
        AddShoppingItemDialog.newInstance { item ->
            viewModel.addShoppingListItem(item)
        }.show(childFragmentManager, AddShoppingItemDialog.TAG)
    }
    
    private fun showDeleteItemConfirmationDialog(item: ShoppingListItem) {
        DeleteConfirmationDialog.newInstance(
            title = getString(R.string.delete_grocery_item_title),
            message = getString(R.string.delete_grocery_item_message),
            onConfirm = {
                viewModel.deleteShoppingListItem(item.id)
            }
        ).show(childFragmentManager, DeleteConfirmationDialog.TAG)
    }
    
    private fun showDeleteSelectedConfirmationDialog() {
        DeleteConfirmationDialog.newInstance(
            title = getString(R.string.delete_grocery_items_title),
            message = getString(R.string.delete_grocery_items_message),
            onConfirm = {
                deleteSelectedItems()
            }
        ).show(childFragmentManager, DeleteConfirmationDialog.TAG)
    }
    
    private fun deleteSelectedItems() {
        selectedItems.forEach { item ->
            viewModel.deleteShoppingListItem(item.id)
        }
        exitSelectionMode()
    }
    

    
    // ShoppingListListener implementation
    override fun onItemPurchasedChanged(item: ShoppingListItem, isPurchased: Boolean) {
        viewModel.updateItemPurchasedStatus(item.id, isPurchased)
    }

    override fun onItemClick(item: ShoppingListItem) {
        if (isInSelectionMode) {
            toggleItemSelection(item)
            // Show context menu for selected item
            if (selectedItems.size == 1) {
                val activity = requireActivity()
                activity.invalidateOptionsMenu()
            }
        } else {
            // Show edit dialog on click
            showEditItemDialog(item)
        }
    }
    
    override fun onItemLongClick(item: ShoppingListItem): Boolean {
        if (!isInSelectionMode) {
            enterSelectionMode()
        }
        toggleItemSelection(item)
        // Show context menu for selected item
        if (selectedItems.size == 1) {
            val activity = requireActivity()
            activity.invalidateOptionsMenu()
        }
        return true
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
    
    private fun updateList(items: List<ShoppingListItem>) {
        adapter.updateItems(items)
        // Show/hide empty view based on list content
        emptyView.isVisible = items.isEmpty()
        recyclerView.isVisible = items.isNotEmpty()
    }
    
    private fun showEditItemDialog(item: ShoppingListItem) {
        EditShoppingItemDialog.newInstance(item) { updatedItem ->
            viewModel.updateShoppingListItem(updatedItem)
        }.show(childFragmentManager, EditShoppingItemDialog.TAG)
    }
    
    private fun markSelectedItemsAsPurchased() {
        selectedItems.forEach { item ->
            viewModel.updateItemPurchasedStatus(item.id, true)
        }
        exitSelectionMode()
    }
}