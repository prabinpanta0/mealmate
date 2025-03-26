package edu.ismt.prabin.mealmate

import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import edu.ismt.prabin.mealmate.util.SplashIdlingResource
import org.junit.After
import org.junit.Before
import androidx.test.rule.ActivityTestRule
import androidx.test.espresso.base.DefaultFailureHandler
import org.junit.Rule

open class BaseTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, true, false)
    
    private val splashIdlingResource = SplashIdlingResource.getInstance()

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(splashIdlingResource)
        // Launch activity
        activityRule.launchActivity(null)
        // Wait for splash screen
        waitForSplashScreen()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(splashIdlingResource)
        activityRule.finishActivity()
    }

    protected fun waitForSplashScreen() {
        // Initial wait for splash screen to appear and fragment to attach
        Thread.sleep(1000)
        // Signal that splash screen is complete
        splashIdlingResource.setIdleState(true)
    }
}