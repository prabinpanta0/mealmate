package edu.ismt.prabin.mealmate.util

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

class SplashIdlingResource : IdlingResource {
    private val isIdle = AtomicBoolean(false)
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String = this.javaClass.name

    override fun isIdleNow(): Boolean = isIdle.get()

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        resourceCallback = callback
    }

    fun setIdleState(isIdleNow: Boolean) {
        isIdle.set(isIdleNow)
        if (isIdleNow) {
            resourceCallback?.onTransitionToIdle()
        }
    }

    companion object {
        private var instance: SplashIdlingResource? = null

        @Synchronized
        fun getInstance(): SplashIdlingResource {
            if (instance == null) {
                instance = SplashIdlingResource()
            }
            return instance!!
        }
    }
}