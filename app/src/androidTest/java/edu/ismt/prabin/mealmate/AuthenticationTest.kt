package edu.ismt.prabin.mealmate

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationTest : BaseTest() {

    @Test
    fun testLoginFlow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Wait for splash screen to finish
            waitForSplashScreen()
            
            // First click the "Let's Cook" button on landing screen
            onView(withId(R.id.continue_button))
                .perform(click())

            // Check if login form is displayed
            onView(withId(R.id.email_input))
                .check(matches(isDisplayed()))
            onView(withId(R.id.password_input))
                .check(matches(isDisplayed()))

            // Input credentials
            onView(withId(R.id.email_input))
                .perform(typeText("test@example.com"), closeSoftKeyboard())
            onView(withId(R.id.password_input))
                .perform(typeText("Aa123456@"), closeSoftKeyboard())

            // Click login button
            onView(withId(R.id.login_button))
                .perform(click())

            // Verify navigation to home screen
            onView(withId(R.id.homeFragment))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testSignupFlow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Wait for splash screen to finish
            waitForSplashScreen()
            
            // First click the "Let's Cook" button on landing screen
            onView(withId(R.id.continue_button))
                .perform(click())

            // Now on login screen, click register button
            onView(withId(R.id.register_button))
                .perform(click())

            // Check if signup form is displayed
            onView(withId(R.id.name_input))
                .check(matches(isDisplayed()))
            onView(withId(R.id.email_input))
                .check(matches(isDisplayed()))
            onView(withId(R.id.password_input))
                .check(matches(isDisplayed()))
        }
    }
}