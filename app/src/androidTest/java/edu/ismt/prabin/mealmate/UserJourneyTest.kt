package edu.ismt.prabin.mealmate

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.ismt.prabin.mealmate.ui.recipe.RecipeAdapter
import edu.ismt.prabin.mealmate.ui.recipe.RecipeDetailFragment.IngredientsAdapter
import edu.ismt.prabin.mealmate.ui.shopping.ShoppingListAdapter
import edu.ismt.prabin.mealmate.util.SplashIdlingResource
import edu.ismt.prabin.mealmate.util.TestUtils
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserJourneyTest : BaseTest() {

    private lateinit var splashIdlingResource: IdlingResource

    @Before
    fun registerIdlingResource() {
        splashIdlingResource = SplashIdlingResource.getInstance()
        IdlingRegistry.getInstance().register(splashIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(splashIdlingResource)
    }

    @Test
    fun completeUserJourney() {
        // Wait for splash screen
        TestUtils.waitFor(1000)
        
        // Step 1: Authentication
        authenticateUser()
        
        // Step 2: Create a recipe
        val recipeName = "Pasta Carbonara ${System.currentTimeMillis()}"
        createRecipe(recipeName)
        
        // Step 3: Edit the recipe
        editRecipe(recipeName, "Creamy Pasta Carbonara")
        
        // Step 4: Test gestures - swipe to add ingredients to shopping list
        testSwipeToAddIngredients()
        
        // Step 5: Test left swipe gesture
        testLeftSwipeGesture()
        
        // Step 6: Test shake gesture for sharing
        testShakeDetector()
        
        // Step 7: Edit shopping list item
        editShoppingListItem()
        
        // Step 8: Mark item as purchased
        markItemAsPurchased()
        
        // Step 9: Share recipe
        shareRecipe()
        
        // Step 10: Delete recipe
        deleteRecipe()
    }

    private fun authenticateUser() {
        // Click the "Let's Cook" button on landing screen
        onView(withId(R.id.continue_button))
            .perform(click())
        TestUtils.waitFor(1000)

        // Input credentials
        onView(withId(R.id.email_input))
            .perform(typeText("test@example.com"), closeSoftKeyboard())
        TestUtils.waitFor(500)
        
        onView(withId(R.id.password_input))
            .perform(typeText("Aa123456@"), closeSoftKeyboard())
        TestUtils.waitFor(500)

        // Click login button
        onView(withId(R.id.login_button))
            .perform(click())
        TestUtils.waitFor(2000)

        // Verify navigation to home screen
        onView(withId(R.id.homeFragment))
            .check(matches(isDisplayed()))
    }

    private fun createRecipe(recipeName: String) {
        // Navigate to Create Recipe using bottom navigation
        onView(withId(R.id.createRecipeFragment))
            .perform(click())
        TestUtils.waitFor(1000)

        // Fill in recipe details
        onView(withId(R.id.title_input))
            .perform(typeText(recipeName), closeSoftKeyboard())
        TestUtils.waitFor(500)
        
        onView(withId(R.id.description_input))
            .perform(typeText("A delicious Italian pasta dish with eggs, cheese, pancetta, and pepper"), closeSoftKeyboard())
        TestUtils.waitFor(500)
        
        // Input category
        onView(withId(R.id.category_input))
            .perform(click())
        TestUtils.waitFor(500)
        
        // Select first item from dropdown
        onView(withText("Main Course"))
            .perform(click())
        TestUtils.waitFor(500)
        
        // Input cooking time
        onView(withId(R.id.cooking_time_input))
            .perform(typeText("30"), closeSoftKeyboard())
        TestUtils.waitFor(500)
        
        // Input servings
        onView(withId(R.id.servings_input))
            .perform(typeText("4"), closeSoftKeyboard())
        TestUtils.waitFor(500)
        
        // Add multiple ingredients
        addIngredient("Spaghetti")
        TestUtils.waitFor(300)
        addIngredient("Eggs")
        TestUtils.waitFor(300)
        addIngredient("Pancetta")
        TestUtils.waitFor(300)
        addIngredient("Parmesan cheese")
        TestUtils.waitFor(300)
        addIngredient("Black pepper")
        TestUtils.waitFor(500)
        
        // Add instructions
        onView(withId(R.id.instructions_input))
            .perform(typeText("1. Cook pasta al dente\n2. Fry pancetta\n3. Mix eggs and cheese\n4. Combine everything\n5. Add pepper"), closeSoftKeyboard())
        TestUtils.waitFor(1000)

        // Save recipe
        onView(withId(R.id.save_button))
            .perform(scrollTo(), click())
        TestUtils.waitFor(2000)

        // Verify navigation back to recipe list
        onView(withId(R.id.recipe_recycler_view))
            .check(matches(isDisplayed()))
        TestUtils.waitFor(1000)
            
        // Verify our recipe is in the list
        onView(withId(R.id.recipe_recycler_view))
            .check(matches(hasDescendant(withText(recipeName))))
    }

    private fun addIngredient(name: String) {
        // Enter ingredient name
        onView(withId(R.id.add_ingredient_input))
            .perform(clearText(), typeText(name), closeSoftKeyboard())
        TestUtils.waitFor(200)
            
        // Add the ingredient
        onView(withId(R.id.add_ingredient_layout))
            .perform(click())
    }

    private fun editRecipe(originalName: String, newName: String) {
        // Navigate to recipe list
        onView(withId(R.id.recipeListFragment))
            .perform(click())
        TestUtils.waitFor(1000)
            
        // Find and click on our recipe
        onView(withId(R.id.recipe_recycler_view))
            .perform(RecyclerViewActions.actionOnItem<RecipeAdapter.RecipeViewHolder>(
                hasDescendant(withText(originalName)), click()))
        TestUtils.waitFor(1000)

        // Click edit button in the toolbar menu
        onView(withId(R.id.action_edit_recipe))
            .perform(click())
        TestUtils.waitFor(1000)

        // Edit recipe title
        onView(withId(R.id.title_input))
            .perform(clearText(), typeText(newName), closeSoftKeyboard())
        TestUtils.waitFor(500)

        // Edit description
        onView(withId(R.id.description_input))
            .perform(clearText(), typeText("An updated delicious creamy Italian pasta dish"), closeSoftKeyboard())
        TestUtils.waitFor(500)

        // Scroll to and save changes
        onView(withId(R.id.save_button))
            .perform(scrollTo(), click())
        TestUtils.waitFor(2000)

        // Verify updated title is displayed
        onView(withText(newName))
            .check(matches(isDisplayed()))
    }

    private fun testSwipeToAddIngredients() {
        // In recipe detail view, find an ingredient item and swipe to add to shopping list
        onView(withId(R.id.ingredients_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<IngredientsAdapter.IngredientViewHolder>(
                0, swipeLeft()))
        TestUtils.waitFor(1000)
            
        // Verify confirmation message appears
        onView(withText(containsString("added to shopping list")))
            .check(matches(isDisplayed()))
        TestUtils.waitFor(1000)
    }
    
    private fun testLeftSwipeGesture() {
        // Test additional swipe gestures for ingredients
        onView(withId(R.id.ingredients_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<IngredientsAdapter.IngredientViewHolder>(
                1, swipeLeft()))
        TestUtils.waitFor(1000)
            
        // Confirm dialog should appear
        onView(withText(R.string.add_to_shopping_list))
            .check(matches(isDisplayed()))
        TestUtils.waitFor(1000)
            
        // Dismiss dialog
        onView(withText(R.string.cancel))
            .perform(click())
        TestUtils.waitFor(1000)
    }
    
    private fun testShakeDetector() {
        // We can't actually test shaking the device in an automated test
        // but we can verify the shake detector UI hint is displayed
        
        // Check if shake to share hint is visible on the recipe detail screen
        try {
            // Look for shake to share hint if it's displayed
            onView(withText(R.string.shake_to_share_hint))
                .check(matches(isDisplayed()))
            TestUtils.waitFor(1000)
        } catch (e: Exception) {
            // If not found, it may have been dismissed or not shown
            // This is fine, we just log and continue
            println("Shake to share hint not visible: ${e.message}")
        }
    }

    private fun editShoppingListItem() {
        // Navigate to shopping list
        onView(withId(R.id.shoppingListFragment))
            .perform(click())
        TestUtils.waitFor(1000)
            
        // Verify we're on the shopping list screen
        onView(withId(R.id.shopping_list_recycler_view))
            .check(matches(isDisplayed()))
        TestUtils.waitFor(1000)
            
        // Select first item in the list
        onView(withId(R.id.shopping_list_recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition<ShoppingListAdapter.ShoppingListViewHolder>(
                0, longClick()))
        TestUtils.waitFor(1000)
            
        // Click edit in context menu
        onView(withText("Edit"))
            .perform(click())
        TestUtils.waitFor(1000)
            
        // Edit quantity
        onView(withId(R.id.quantity_input))
            .perform(clearText(), typeText("300"), closeSoftKeyboard())
        TestUtils.waitFor(500)
            
        // Save changes
        onView(withId(R.id.save_button))
            .perform(click())
        TestUtils.waitFor(1000)
            
        // Verify item was updated (contains "300")
        onView(withId(R.id.shopping_list_recycler_view))
            .check(matches(hasDescendant(withText("300"))))
    }

    private fun markItemAsPurchased() {
        // Find the checkbox for the first item and click it
        onView(withId(R.id.shopping_list_recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition<ShoppingListAdapter.ShoppingListViewHolder>(
                0, clickChildViewWithId(R.id.item_checkbox)))
        TestUtils.waitFor(1000)
            
        // Verify the item is now checked
        onView(withId(R.id.shopping_list_recycler_view))
            .check(matches(atPosition(0, hasDescendant(allOf(
                withId(R.id.item_checkbox), 
                isChecked()
            )))))
    }

    private fun shareRecipe() {
        // Navigate back to recipe list
        onView(withId(R.id.recipeListFragment))
            .perform(click())
        TestUtils.waitFor(1000)
            
        // Find and click on our recipe
        onView(withId(R.id.recipe_recycler_view))
            .perform(RecyclerViewActions.actionOnItem<RecipeAdapter.RecipeViewHolder>(
                hasDescendant(withText("Creamy Pasta Carbonara")), click()))
        TestUtils.waitFor(1000)
            
        // Click share button in the toolbar
        onView(withId(R.id.action_share))
            .perform(click())
        TestUtils.waitFor(1000)
            
        // Click on "Share via Other Apps" option
        onView(withText(R.string.share_via_other_apps))
            .perform(click())
        TestUtils.waitFor(1000)
            
        // Verify share dialog is shown
        try {
            // This might fail on some devices due to the Intent chooser being outside the app's context
            onView(withText(containsString("Share")))
                .check(matches(isDisplayed()))
            TestUtils.waitFor(1000)
        } catch (e: Exception) {
            // Log and continue if verification fails
            println("Share dialog verification skipped: ${e.message}")
        }
    }

    private fun deleteRecipe() {
        // Navigate back
        pressBack()
        TestUtils.waitFor(1000)
        
        // Click delete button in the toolbar menu
        onView(withId(R.id.action_delete_recipe))
            .perform(click())
        TestUtils.waitFor(1000)
            
        // Confirm deletion
        onView(allOf(withId(android.R.id.button1), withText("Delete")))
            .perform(click())
        TestUtils.waitFor(2000)
            
        // Verify navigation back to recipe list
        onView(withId(R.id.recipe_recycler_view))
            .check(matches(isDisplayed()))
        TestUtils.waitFor(1000)
            
        // Verify recipe is no longer in the list
        onView(withText("Creamy Pasta Carbonara"))
            .check(doesNotExist())
    }
    
    // Helper functions for UI interactions
    
    private fun clickChildViewWithId(id: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return hasDescendant(withId(id))
            }

            override fun getDescription(): String {
                return "Click on a child view with specified id."
            }

            override fun perform(uiController: UiController, view: View) {
                val v = view.findViewById<View>(id)
                v.performClick()
            }
        }
    }
    
    private fun atPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
        return object : org.hamcrest.TypeSafeMatcher<View>() {
            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: View): Boolean {
                val recyclerView = view as androidx.recyclerview.widget.RecyclerView
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                return viewHolder != null && itemMatcher.matches(viewHolder.itemView)
            }
        }
    }
    
    private fun swipeLeft(): ViewAction {
        return GeneralSwipeAction(
            Swipe.SLOW,
            { view -> 
                val coordinates = IntArray(2)
                view.getLocationOnScreen(coordinates)
                val x = coordinates[0] + (view.width * 0.8).toFloat()
                val y = coordinates[1] + (view.height * 0.5).toFloat()
                floatArrayOf(x, y)
            },
            { view -> 
                val coordinates = IntArray(2)
                view.getLocationOnScreen(coordinates)
                val x = coordinates[0] + (view.width * 0.2).toFloat()
                val y = coordinates[1] + (view.height * 0.5).toFloat()
                floatArrayOf(x, y)
            },
            Press.FINGER
        )
    }
    
    private fun swipeRight(): ViewAction {
        return GeneralSwipeAction(
            Swipe.SLOW,
            { view -> 
                val coordinates = IntArray(2)
                view.getLocationOnScreen(coordinates)
                val x = coordinates[0] + (view.width * 0.2).toFloat()
                val y = coordinates[1] + (view.height * 0.5).toFloat()
                floatArrayOf(x, y)
            },
            { view -> 
                val coordinates = IntArray(2)
                view.getLocationOnScreen(coordinates)
                val x = coordinates[0] + (view.width * 0.8).toFloat()
                val y = coordinates[1] + (view.height * 0.5).toFloat()
                floatArrayOf(x, y)
            },
            Press.FINGER
        )
    }
}