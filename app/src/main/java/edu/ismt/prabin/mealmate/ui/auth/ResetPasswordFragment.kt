package edu.ismt.prabin.mealmate.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import kotlinx.coroutines.launch

/**
 * Fragment for handling password reset
 */
class ResetPasswordFragment : Fragment() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var resetButton: MaterialButton
    private lateinit var backButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reset_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        emailInput = view.findViewById(R.id.email_input)
        emailLayout = view.findViewById(R.id.email_layout)
        resetButton = view.findViewById(R.id.reset_button)
        backButton = view.findViewById(R.id.back_button)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        
        // Set up click listeners
        resetButton.setOnClickListener {
            if (validateInputs()) {
                resetPassword()
            }
        }
        
        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_resetPasswordFragment_to_loginFragment)
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Validate email
        val email = emailInput.text.toString().trim()
        if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Invalid email format"
            isValid = false
        } else {
            emailLayout.error = null
        }
        
        return isValid
    }
    
    private fun resetPassword() {
        // Show progress indicator and disable buttons
        setLoading(true)
        
        val email = emailInput.text.toString().trim()
        
        lifecycleScope.launch {
            val result = SupabaseClient.resetPassword(email)
            
            result.fold(
                onSuccess = {
                    // Show success message
                    Toast.makeText(
                        requireContext(),
                        "Password reset instructions sent to your email",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Navigate back to login
                    findNavController().navigate(R.id.action_resetPasswordFragment_to_loginFragment)
                },
                onFailure = { exception ->
                    // Show error message
                    Toast.makeText(
                        requireContext(),
                        "Password reset failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    setLoading(false)
                }
            )
        }
    }
    
    private fun setLoading(isLoading: Boolean) {
        progressIndicator.isVisible = isLoading
        resetButton.isEnabled = !isLoading
        backButton.isEnabled = !isLoading
        emailInput.isEnabled = !isLoading
    }
} 