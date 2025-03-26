package edu.ismt.prabin.mealmate.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import edu.ismt.prabin.mealmate.R

import kotlinx.coroutines.launch
import edu.ismt.prabin.mealmate.data.model.Profile
import edu.ismt.prabin.mealmate.data.repository.ProfileRepository
import edu.ismt.prabin.mealmate.data.repository.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.request.SelectRequestBuilder

class Filter {
    companion object {
        fun eq(column: String, value: Any): String {
            return "$column = '$value'"
        }
    }
}

class ProfileFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels()
    
    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var darkModeSwitch: MaterialSwitch
    private lateinit var logoutButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image)
        profileName = view.findViewById(R.id.profile_name)
        profileEmail = view.findViewById(R.id.profile_email)
        darkModeSwitch = view.findViewById(R.id.dark_mode_switch)
        logoutButton = view.findViewById(R.id.logout_button)

        // Setup UI
        setupUserInfo()
        setupDarkModeSwitch()
        setupLogoutButton()
    }

    private fun setupUserInfo() {
        lifecycleScope.launch {
            val userResult = edu.ismt.prabin.mealmate.data.repository.SupabaseClient.getCurrentUser()
            userResult.onSuccess { user ->
                user?.let { currentUser ->
                    // Get profile information
                    ProfileRepository.profileExists(currentUser.id).onSuccess { exists ->
                        if (exists) {
                            // Fetch profile from Supabase and display name
                            SupabaseClient.supabase.postgrest["profiles"]
                                .select {
                                    filter {
                                        eq("id", currentUser.id)
                                    }
                                }
                                .decodeSingle<Profile>()
                                .let { profile ->
                                    profileName.text = profile.name
                                    profileEmail.text = currentUser.email ?: "Not signed in"
                                }
                        } else {
                            profileName.text = "Guest"
                            profileEmail.text = currentUser.email ?: "Not signed in"
                        }
                    }
                } ?: run {
                    profileName.text = "Guest"
                    profileEmail.text = "Not signed in"
                }
            }
        }
    }

    private fun setupDarkModeSwitch() {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        darkModeSwitch.isChecked = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        // Delay the initial state to prevent race condition during inflation
        darkModeSwitch.post {
            viewModel.darkModeEnabled = darkModeSwitch.isChecked
            darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleTheme(isChecked)
            }
        }
    }

    private fun setupLogoutButton() {
        logoutButton.setOnClickListener {
            lifecycleScope.launch {
                edu.ismt.prabin.mealmate.data.repository.SupabaseClient.signOut()
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }
}

class ProfileViewModel : ViewModel() {
    var darkModeEnabled = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

    fun toggleTheme(enabled: Boolean) {
        if (darkModeEnabled != enabled) {
            darkModeEnabled = enabled
            viewModelScope.launch {
                AppCompatDelegate.setDefaultNightMode(
                    if (enabled) AppCompatDelegate.MODE_NIGHT_YES 
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }
}
