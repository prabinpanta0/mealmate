package edu.ismt.prabin.mealmate.ui.onboarding

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import edu.ismt.prabin.mealmate.R

class LandingFragment : Fragment() {
    private val TAG = "LandingFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_landing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the get started button
        view.findViewById<MaterialButton>(R.id.continue_button)?.setOnClickListener {
            // Navigate to the login screen instead of directly to home
            Log.d(TAG, "Get started clicked, navigating to login screen")
            findNavController().navigate(R.id.action_landing_to_login)
        }
    }
}