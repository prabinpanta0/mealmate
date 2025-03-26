package edu.ismt.prabin.mealmate.util

/**
 * Utility class for test-related helper functions
 */
object TestUtils {
    /**
     * Pauses the current thread for the specified duration
     * @param millis Time to wait in milliseconds
     */
    fun waitFor(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}