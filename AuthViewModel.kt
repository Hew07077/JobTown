package com.example.jobtown.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

sealed class LoginNavigationState {
    object Idle : LoginNavigationState()
    object NavigateToHome : LoginNavigationState()
    object NavigateToCompleteProfile : LoginNavigationState()
}

class AuthViewModel(private val supabase: SupabaseClient) : ViewModel() {

    var emailInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
    var navigationState by mutableStateOf<LoginNavigationState>(LoginNavigationState.Idle)

    fun login() {
        if (emailInput.isBlank() || passwordInput.isBlank()) {
            errorMessage = "Please fill in all fields."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // 1. Perform login via Supabase Auth
                supabase.auth.signInWith(Email) {
                    email = emailInput
                    password = passwordInput
                }

                // 2. Set navigation state on success
                navigationState = LoginNavigationState.NavigateToHome

            } catch (e: Exception) {
                e.printStackTrace()
                // 3. Display a clean message to the user
                errorMessage = "Invalid email or password. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    fun resetNavigationState() {
        navigationState = LoginNavigationState.Idle
    }
}