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
 * Fragment for handling user registration
 */
class RegisterFragment : Fragment() {

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var nameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var registerButton: MaterialButton
    private lateinit var loginButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        nameInput = view.findViewById(R.id.name_input)
        emailInput = view.findViewById(R.id.email_input)
        passwordInput = view.findViewById(R.id.password_input)
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input)
        nameLayout = view.findViewById(R.id.name_layout)
        emailLayout = view.findViewById(R.id.email_layout)
        passwordLayout = view.findViewById(R.id.password_layout)
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout)
        registerButton = view.findViewById(R.id.register_button)
        loginButton = view.findViewById(R.id.login_button)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        
        // Set up click listeners
        registerButton.setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }
        
        loginButton.setOnClickListener {
            // Navigate back to login screen
            findNavController().navigateUp()
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Validate name
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            nameLayout.error = "Name is required"
            isValid = false
        } else {
            nameLayout.error = null
        }
        
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
        
        // Validate password
        val password = passwordInput.text.toString()
        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            passwordLayout.error = "Password must be at least 8 characters"
            isValid = false
        } else if (!password.matches(Regex(".*[A-Z].*"))) {
            passwordLayout.error = "Password must contain at least one uppercase letter"
            isValid = false
        } else if (!password.matches(Regex(".*[a-z].*"))) {
            passwordLayout.error = "Password must contain at least one lowercase letter"
            isValid = false
        } else if (!password.matches(Regex(".*\\d.*"))) {
            passwordLayout.error = "Password must contain at least one number"
            isValid = false
        } else {
            passwordLayout.error = null
        }
        
        // Validate confirm password
        val confirmPassword = confirmPasswordInput.text.toString()
        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = "Please confirm your password"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordLayout.error = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordLayout.error = null
        }
        
        return isValid
    }
    
    private fun registerUser() {
        // Show progress indicator and disable buttons
        setLoading(true)
        
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        
        lifecycleScope.launch {
            val result = SupabaseClient.signUp(email, password, name)
            
            result.fold(
                onSuccess = { user ->
                    // Show success message
                    Toast.makeText(
                        requireContext(),
                        "Registration successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Navigate to home screen
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                },
                onFailure = { exception ->
                    // Show error message
                    Toast.makeText(
                        requireContext(),
                        "Registration failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    setLoading(false)
                }
            )
        }
    }
    
    private fun setLoading(isLoading: Boolean) {
        progressIndicator.isVisible = isLoading
        registerButton.isEnabled = !isLoading
        loginButton.isEnabled = !isLoading
        nameInput.isEnabled = !isLoading
        emailInput.isEnabled = !isLoading
        passwordInput.isEnabled = !isLoading
        confirmPasswordInput.isEnabled = !isLoading
    }
}