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
import edu.ismt.prabin.mealmate.ui.recipe.RecipeAdapter

@RunWith(AndroidJUnit4::class)
class RecipeFeatureTest {

    @Test
    fun testCreateRecipe() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // First, click the "Let's Cook" button to navigate past the landing screen
            onView(withId(R.id.continue_button))
                .perform(click())
            
            // Navigate to Create Recipe using bottom navigation
            onView(withId(R.id.createRecipeFragment))
                .perform(click())

            // Fill in recipe details
            onView(withId(R.id.title_input))
                .perform(typeText("Test Recipe"), closeSoftKeyboard())
            
            onView(withId(R.id.description_input))
                .perform(typeText("Test Description for a delicious recipe that everyone will enjoy!"), closeSoftKeyboard())
            
            // Input category
            onView(withId(R.id.category_input))
                .perform(click())
            // Select first item from dropdown
            onView(withText("Main Course"))
                .perform(click())
            
            // Input cooking time
            onView(withId(R.id.cooking_time_input))
                .perform(typeText("30"), closeSoftKeyboard())
            
            // Input servings
            onView(withId(R.id.servings_input))
                .perform(typeText("4"), closeSoftKeyboard())
                
            // Add several ingredients
            addIngredient("Salt")
            addIngredient("Pepper")
            addIngredient("Olive Oil")
            addIngredient("Garlic")
            
            // Add detailed instructions
            onView(withId(R.id.instructions_input))
                .perform(typeText("1. Preheat the oven to 350°F.\n2. Mix all ingredients in a bowl.\n3. Cook for 20 minutes.\n4. Let it cool before serving."), closeSoftKeyboard())

            // Scroll to and save recipe
            onView(withId(R.id.save_button))
                .perform(scrollTo(), click())

            // Verify navigation back to recipe list
            onView(withId(R.id.recipe_recycler_view))
                .check(matches(isDisplayed()))
        }
    }

    private fun addIngredient(name: String) {
        onView(withId(R.id.add_ingredient_input))
            .perform(clearText(), typeText(name), closeSoftKeyboard())
        onView(withId(R.id.add_ingredient_layout))
            .perform(click())
    }

    @Test
    fun testEditRecipe() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // First, click the "Let's Cook" button to navigate past the landing screen
            onView(withId(R.id.continue_button))
                .perform(click())
                
            // Navigate to recipe list
            onView(withId(R.id.recipeListFragment))
                .perform(click())
                
            // Select first recipe
            onView(withId(R.id.recipe_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecipeAdapter.RecipeViewHolder>(0, click()))

            // Click edit button in the toolbar menu
            onView(withId(R.id.action_edit_recipe))
                .perform(click())

            // Edit recipe title
            onView(withId(R.id.title_input))
                .perform(clearText(), typeText("Updated Recipe"), closeSoftKeyboard())
                
            // Edit description
            onView(withId(R.id.description_input))
                .perform(clearText(), typeText("This is an updated description with new details"), closeSoftKeyboard())
                
            // Edit cooking time
            onView(withId(R.id.cooking_time_input))
                .perform(clearText(), typeText("45"), closeSoftKeyboard())
                
            // Edit servings
            onView(withId(R.id.servings_input))
                .perform(clearText(), typeText("6"), closeSoftKeyboard())
                
            // Edit instructions
            onView(withId(R.id.instructions_input))
                .perform(clearText(), typeText("1. Updated step one\n2. Updated step two\n3. Updated step three"), closeSoftKeyboard())

            // Scroll to and save changes
            onView(withId(R.id.save_button))
                .perform(scrollTo(), click())

            // Verify updated title is displayed
            onView(withId(R.id.recipe_title))
                .check(matches(withText("Updated Recipe")))
        }
    }

    @Test
    fun testDeleteRecipe() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // First, click the "Let's Cook" button to navigate past the landing screen
            onView(withId(R.id.continue_button))
                .perform(click())
                
            // Navigate to recipe list
            onView(withId(R.id.recipeListFragment))
                .perform(click())
                
            // Select first recipe
            onView(withId(R.id.recipe_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecipeAdapter.RecipeViewHolder>(0, click()))

            // Click delete button in the toolbar menu
            onView(withId(R.id.action_delete_recipe))
                .perform(click())

            // Confirm deletion
            onView(allOf(withId(android.R.id.button1), withText("Delete")))
                .perform(click())

            // Verify navigation back to recipe list
            onView(withId(R.id.recipe_recycler_view))
                .check(matches(isDisplayed()))
        }
    }
}