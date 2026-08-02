package com.example.jobtown.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class ProfileViewModel(private val supabase: SupabaseClient) : ViewModel() {

    var phone by mutableStateOf("")
    var location by mutableStateOf("")
    var skillsInput by mutableStateOf("")
    var experienceLevel by mutableStateOf("Junior (1-2 yrs)")
    var portfolioUrl by mutableStateOf("")
    var bio by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isSuccess by mutableStateOf(false)

    fun saveProfile() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val currentUser = supabase.auth.currentUserOrNull()
            if (currentUser == null) {
                errorMessage = "User session expired. Please log in again."
                isLoading = false
                return@launch
            }

            val skillsList = skillsInput
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val profile = UserProfile(
                id = currentUser.id,
                phone = phone.trim(),
                location = location.trim(),
                skills = skillsList,
                experienceLevel = experienceLevel,
                portfolioUrl = portfolioUrl.trim(),
                bio = bio.trim()
            )

            try {
                supabase.from("users").upsert(profile)
                isSuccess = true
            } catch (e: RestException) {
                errorMessage = e.description ?: "Database permission or input error."
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to save profile. Please check your network."
            } finally {
                isLoading = false
            }
        }
    }
}