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
import com.google.android.material.textfield.TextInputLayout
import edu.ismt.prabin.mealmate.R
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

/**
 * Fragment for handling user login
 */
class LoginFragment : Fragment() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var loginButton: MaterialButton
    private lateinit var registerButton: MaterialButton
    private lateinit var forgotPasswordText: MaterialTextView
    private lateinit var progressIndicator: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        emailInput = view.findViewById(R.id.email_input)
        passwordInput = view.findViewById(R.id.password_input)
        emailLayout = view.findViewById(R.id.email_layout)
        passwordLayout = view.findViewById(R.id.password_layout)
        loginButton = view.findViewById(R.id.login_button)
        registerButton = view.findViewById(R.id.register_button)
        forgotPasswordText = view.findViewById(R.id.forgot_password_text)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        
        // Set up click listeners
        loginButton.setOnClickListener {
            if (validateInputs()) {
                loginUser()
            }
        }
        
        registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        
        forgotPasswordText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_resetPasswordFragment)
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
        
        // Validate password
        val password = passwordInput.text.toString()
        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            isValid = false
        } else {
            passwordLayout.error = null
        }
        
        return isValid
    }
    
    private fun loginUser() {
        // Show progress indicator and disable buttons
        setLoading(true)
        
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        
        lifecycleScope.launch {
            val result = SupabaseClient.signIn(email, password)
            
            result.fold(
                onSuccess = { user ->
                    // Navigate to home screen
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                },
                onFailure = { exception ->
                    // Show error message
                    Toast.makeText(
                        requireContext(),
                        "Login failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    setLoading(false)
                }
            )
        }
    }
    
    private fun setLoading(isLoading: Boolean) {
        progressIndicator.isVisible = isLoading
        loginButton.isEnabled = !isLoading
        registerButton.isEnabled = !isLoading
        forgotPasswordText.isEnabled = !isLoading
        emailInput.isEnabled = !isLoading
        passwordInput.isEnabled = !isLoading
    }
}