package edu.ismt.prabin.mealmate

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.contrib.RecyclerViewActions
import org.hamcrest.Matchers.allOf
import edu.ismt.prabin.mealmate.ui.shopping.ShoppingListAdapter

@RunWith(AndroidJUnit4::class)
class ShoppingListFeatureTest : BaseTest() {

    @Test
    fun testAddItemToShoppingList() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForSplashScreen()
            
            // Navigate to shopping list
            onView(withId(R.id.btnShoppingList))
                .perform(click())

            // Click add item button
            onView(withId(R.id.floating_action_button))
                .perform(click())

            // Fill in item details
            onView(withId(R.id.item_name_input))
                .perform(typeText("Test Item"), closeSoftKeyboard())
            onView(withId(R.id.quantity_input))
                .perform(typeText("2"), closeSoftKeyboard())
            onView(withId(R.id.unit_input))
                .perform(click())
            onView(withText("pcs"))
                .perform(click())

            // Save item
            onView(withId(R.id.add_button))
                .perform(click())

            // Verify item is in the list
            onView(withId(R.id.shopping_list_recycler_view))
                .check(matches(hasDescendant(withText("Test Item"))))
        }
    }

    @Test
    fun testEditShoppingListItem() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForSplashScreen()
            
            // Navigate to shopping list
            onView(withId(R.id.btnShoppingList))
                .perform(click())

            // Select first item
            onView(withId(R.id.shopping_list_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<ShoppingListAdapter.ShoppingListViewHolder>(0, longClick()))

            // Click edit in context menu
            onView(withText("Edit"))
                .perform(click())

            // Edit item name
            onView(withId(R.id.item_name_input))
                .perform(clearText(), typeText("Updated Item"), closeSoftKeyboard())

            // Save changes
            onView(withId(R.id.save_button))
                .perform(click())

            // Verify updated item name
            onView(withId(R.id.shopping_list_recycler_view))
                .check(matches(hasDescendant(withText("Updated Item"))))
        }
    }

    @Test
    fun testDeleteShoppingListItem() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForSplashScreen()
            
            // Navigate to shopping list
            onView(withId(R.id.btnShoppingList))
                .perform(click())

            // Select first item
            onView(withId(R.id.shopping_list_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<ShoppingListAdapter.ShoppingListViewHolder>(0, longClick()))

            // Click delete in context menu 
            onView(withText("Delete"))
                .perform(click())

            // Confirm deletion
            onView(allOf(withId(android.R.id.button1), withText("Delete")))
                .perform(click())
        }
    }

    @Test
    fun testShareShoppingList() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForSplashScreen()
            
            // Navigate to shopping list
            onView(withId(R.id.btnShoppingList))
                .perform(click())

            // Select multiple items
            onView(withId(R.id.shopping_list_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<ShoppingListAdapter.ShoppingListViewHolder>(0, longClick()))
            onView(withId(R.id.shopping_list_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<ShoppingListAdapter.ShoppingListViewHolder>(1, click()))

            // Click share button in toolbar menu
            onView(withId(R.id.action_share))
                .perform(click())

            // Verify share dialog is displayed
            onView(withText("Share via"))
                .check(matches(isDisplayed()))
        }
    }
}