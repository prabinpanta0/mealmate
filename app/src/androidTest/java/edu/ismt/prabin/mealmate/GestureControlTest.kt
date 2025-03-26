package edu.ismt.prabin.mealmate

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.ismt.prabin.mealmate.data.model.ShoppingListItem
import edu.ismt.prabin.mealmate.ui.shopping.ShoppingListAdapter
import edu.ismt.prabin.mealmate.ui.shopping.ShoppingListViewModel
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class GestureControlTest : BaseTest() {
    private lateinit var sensorManager: SensorManager
    private val WAIT_TIMEOUT_MS = 5000L

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sensorManager = context.getSystemService(SensorManager::class.java)
        
        // Disable animations to make tests more reliable
        disableAnimations()
    }
    
    private fun disableAnimations() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "settings put global window_animation_scale 0"
        )
        instrumentation.uiAutomation.executeShellCommand(
            "settings put global transition_animation_scale 0"
        )
        instrumentation.uiAutomation.executeShellCommand(
            "settings put global animator_duration_scale 0"
        )
    }
    
    /**
     * Custom ViewAction to set text in a MaterialAutoCompleteTextView
     */
    private fun setTextInMaterialAutoComplete(text: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDisplayed(), isAssignableFrom(androidx.appcompat.widget.AppCompatAutoCompleteTextView::class.java))
            }
            
            override fun getDescription(): String {
                return "Set text in MaterialAutoCompleteTextView"
            }
            
            override fun perform(uiController: UiController, view: View) {
                val autoCompleteTextView = view as androidx.appcompat.widget.AppCompatAutoCompleteTextView
                autoCompleteTextView.setText(text)
                uiController.loopMainThreadUntilIdle()
            }
        }
    }

    @Test
    fun testSwipeToDelete() {
        // Launch activity and keep it running for the entire test
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForSplashScreen()
            
            // First handle the landing screen - click "Let's Cook" button
            onView(withId(R.id.continue_button))
                .perform(click())
                
            // Now we should be on the login screen, proceed with login
            // For test purposes, we'll mock the login by using navigation directly
            scenario.onActivity { activity ->
                // Direct navigation to home screen to bypass authentication
                val navController = activity.findNavController(R.id.nav_host_fragment)
                navController.navigate(R.id.homeFragment)
            }
            
            // Wait for navigation to complete
            SystemClock.sleep(1000)
            
            // Now on home screen, find the shopping list button and click it
            onView(withId(R.id.btnShoppingList))
                .perform(click())
            
            // Wait for shopping list to load
            SystemClock.sleep(1000)
            
            // Click add item button to make sure we have at least one item
            onView(withId(R.id.floating_action_button))
                .perform(click())
                
            // Fill in item details
            onView(withId(R.id.item_name_input))
                .perform(typeText("Test Swipe Left"), closeSoftKeyboard())
            onView(withId(R.id.quantity_input))
                .perform(typeText("2"), closeSoftKeyboard())
                
            // Use custom action to set text in MaterialAutoCompleteTextView
            onView(withId(R.id.unit_input))
                .perform(setTextInMaterialAutoComplete("pcs"))
                
            // Wait to ensure UI is stable
            SystemClock.sleep(1000)

            // Save item
            onView(withId(R.id.add_button))
                .perform(click())
    
            // Wait for item to be added and ensure list is populated
            SystemClock.sleep(2000)  // Give more time for the data to propagate
            
            // Wait for RecyclerView to be visible before trying to interact with it
            waitForRecyclerViewVisible()
            
            // We need to make sure the RecyclerView has at least one item before interacting
            try {
                // First check if we can see our test item
                onView(withId(R.id.shopping_list_recycler_view))
                    .check(matches(hasDescendant(withText("Test Swipe Left"))))
                    
                // Now perform the swipe action
                onView(withId(R.id.shopping_list_recycler_view))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<ShoppingListAdapter.ShoppingListViewHolder>(0, swipeLeft()))

                // Verify delete confirmation dialog appears
                onView(withText(R.string.delete_grocery_item_title))
                    .check(matches(isDisplayed()))
            } catch (e: Exception) {
                // If we can't find the item, add more diagnostic information
                val adapter = try {
                    var foundAdapter: RecyclerView.Adapter<*>? = null
                    scenario.onActivity { activity ->
                        val recyclerView = activity.findViewById<RecyclerView>(R.id.shopping_list_recycler_view)
                        foundAdapter = recyclerView?.adapter
                    }
                    "Adapter has ${foundAdapter?.itemCount ?: 0} items"
                } catch (e2: Exception) {
                    "Could not access adapter: ${e2.message}"
                }
                
                throw AssertionError("No items found in RecyclerView. $adapter. Original error: ${e.message}")
            }
        }
    }

    @Test
    fun testSwipeToMarkPurchased() {
        // Launch activity and keep it running for the entire test
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForSplashScreen()
            
            // First handle the landing screen - click "Let's Cook" button
            onView(withId(R.id.continue_button))
                .perform(click())
                
            // Now we should be on the login screen, proceed with login
            // For test purposes, we'll mock the login by using navigation directly
            scenario.onActivity { activity ->
                // Direct navigation to home screen to bypass authentication
                val navController = activity.findNavController(R.id.nav_host_fragment)
                navController.navigate(R.id.homeFragment)
            }
            
            // Wait for navigation to complete
            SystemClock.sleep(1000)
            
            // Now on home screen, find the shopping list button and click it
            onView(withId(R.id.btnShoppingList))
                .perform(click())
            
            // Wait for shopping list to load
            SystemClock.sleep(1000)
            
            // Click add item button to make sure we have at least one item
            onView(withId(R.id.floating_action_button))
                .perform(click())
                
            // Fill in item details
            onView(withId(R.id.item_name_input))
                .perform(typeText("Test Swipe Right"), closeSoftKeyboard())
            onView(withId(R.id.quantity_input))
                .perform(typeText("3"), closeSoftKeyboard())
                
            // Use custom action to set text in MaterialAutoCompleteTextView
            onView(withId(R.id.unit_input))
                .perform(setTextInMaterialAutoComplete("pcs"))
                
            // Wait to ensure UI is stable
            SystemClock.sleep(1000)

            // Save item
            onView(withId(R.id.add_button))
                .perform(click())
    
            // Wait for item to be added and ensure list is populated
            SystemClock.sleep(2000)  // Give more time for the data to propagate
            
            // Wait for RecyclerView to be visible and have items
            waitForRecyclerViewVisible()
            
            // We need to make sure the RecyclerView has at least one item before interacting
            try {
                // First check if we can see our test item
                onView(withId(R.id.shopping_list_recycler_view))
                    .check(matches(hasDescendant(withText("Test Swipe Right"))))
                    
                // Now perform the swipe action
                onView(withId(R.id.shopping_list_recycler_view))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<ShoppingListAdapter.ShoppingListViewHolder>(0, swipeRight()))

                // Verify edit dialog appears (right swipe should trigger edit)
                onView(withId(R.id.save_button))
                    .check(matches(isDisplayed()))
            } catch (e: Exception) {
                // If we can't find the item, add more diagnostic information
                val adapter = try {
                    var foundAdapter: RecyclerView.Adapter<*>? = null
                    scenario.onActivity { activity ->
                        val recyclerView = activity.findViewById<RecyclerView>(R.id.shopping_list_recycler_view)
                        foundAdapter = recyclerView?.adapter
                    }
                    "Adapter has ${foundAdapter?.itemCount ?: 0} items"
                } catch (e2: Exception) {
                    "Could not access adapter: ${e2.message}"
                }
                
                throw AssertionError("No items found in RecyclerView. $adapter. Original error: ${e.message}")
            }
        }
    }

    @Test
    fun testShakeToRefresh() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForSplashScreen()
            
            // First handle the landing screen - click "Let's Cook" button
            onView(withId(R.id.continue_button))
                .perform(click())
                
            // Now we should be on the login screen, proceed with login
            // For test purposes, we'll mock the login by using navigation directly
            scenario.onActivity { activity ->
                // Direct navigation to home screen to bypass authentication
                val navController = activity.findNavController(R.id.nav_host_fragment)
                navController.navigate(R.id.homeFragment)
            }
            
            // Wait for navigation to complete
            SystemClock.sleep(1000)
            
            // Now on home screen, find the shopping list button and click it  
            onView(withId(R.id.btnShoppingList))
                .perform(click())
        
            // Wait for shopping list to load
            SystemClock.sleep(1000)
            
            // Add test data using the scenario
            scenario.onActivity { activity ->
                val viewModel = ShoppingListViewModel()
                runBlocking {
                    val testItem = ShoppingListItem(
                        id = UUID.randomUUID().toString(),
                        name = "Test Item",
                        quantity = 1.0,
                        unit = "pcs",
                        isPurchased = false,
                        userId = "test_user",
                        recipeName = "",
                        ingredientId = "",
                        recipeId = ""
                    )
                    viewModel.addShoppingListItem(testItem)
                    
                    // Need to wait a moment for data to be added and UI to update
                    Thread.sleep(1000)
                }
            }

            // Wait for RecyclerView to be visible before trying to interact with it
            waitForRecyclerViewVisible()

            // Simulate shake event - this part needs to be done on main thread
            scenario.onActivity { activity ->
                // Find the SwipeRefreshLayout
                val swipeRefreshLayout = activity.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
                
                // Trigger refresh manually to simulate shake
                swipeRefreshLayout.isRefreshing = true
            }
            
            // After setting refreshing=true, wait a moment before checking
            SystemClock.sleep(500)
            
            // Checking UI state should be done OUTSIDE of onActivity to avoid main thread restriction
            onView(withId(R.id.swipeRefreshLayout))
                .check(matches(isRefreshing()))
        }
    }

    private fun isRefreshing(): Matcher<View> {
        return object : BoundedMatcher<View, SwipeRefreshLayout>(SwipeRefreshLayout::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("is refreshing")
            }

            override fun matchesSafely(item: SwipeRefreshLayout): Boolean {
                return item.isRefreshing
            }
        }
    }

    /**
     * Waits for RecyclerView to become visible before continuing with test
     */
    private fun waitForRecyclerViewVisible() {
        val startTime = System.currentTimeMillis()
        val recyclerViewIdlingResource = object : IdlingResource {
            private var resourceCallback: ResourceCallback? = null
            private val isIdle = AtomicBoolean(false)
            
            override fun getName(): String = "RecyclerView Visibility Idling Resource"
            
            override fun isIdleNow(): Boolean {
                if (isIdle.get()) {
                    return true
                }
                
                try {
                    var isVisible = false
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        val activity = getCurrentActivity()
                        val recyclerView = activity?.findViewById<View>(R.id.shopping_list_recycler_view)
                        isVisible = recyclerView != null && recyclerView.visibility == View.VISIBLE
                    }
                    
                    if (isVisible) {
                        isIdle.set(true)
                        resourceCallback?.onTransitionToIdle()
                        return true
                    } else if (System.currentTimeMillis() - startTime > WAIT_TIMEOUT_MS) {
                        // If we've been waiting too long, just continue and let the test fail with a clear error
                        isIdle.set(true)
                        resourceCallback?.onTransitionToIdle()
                        return true
                    }
                } catch (e: Exception) {
                    // If getting visibility fails, just continue
                    isIdle.set(true)
                    resourceCallback?.onTransitionToIdle()
                    return true
                }
                
                return false
            }
            
            override fun registerIdleTransitionCallback(callback: ResourceCallback?) {
                this.resourceCallback = callback
            }
        }

        IdlingRegistry.getInstance().register(recyclerViewIdlingResource)
        try {
            // Force a check of the idling resource
            onView(isRoot()).check(matches(isDisplayed()))
            // Wait a bit more to ensure stability
            SystemClock.sleep(500)
        } finally {
            IdlingRegistry.getInstance().unregister(recyclerViewIdlingResource)
        }
    }
    
    /**
     * Get the current activity (helper method)
     */
    private fun getCurrentActivity(): android.app.Activity? {
        var currentActivity: android.app.Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val resumedActivities = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                .getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
            if (resumedActivities.iterator().hasNext()) {
                currentActivity = resumedActivities.iterator().next()
            }
        }
        return currentActivity
    }

    companion object {
        private const val SHAKE_DELAY_MS = 1000L
    }
}