package com.example.jobtown.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.model.ProfileEntry
import com.example.jobtown.data.model.User
import com.example.jobtown.data.model.UserRole
import com.example.jobtown.data.repository.AvatarHistoryItem
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.utils.ValidationUtils
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    var user by mutableStateOf<User?>(null)
        private set

    private var boundUserId: String? = null

    var isEditing by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var saveErrorMessage by mutableStateOf("")

    var editName by mutableStateOf("")
    var editPhone by mutableStateOf("")
    var editLocation by mutableStateOf("")
    var editIndustry by mutableStateOf("")
    var editCompanySize by mutableStateOf("")
    var editExperienceLevel by mutableStateOf("")
    var editSkills by mutableStateOf("")
    var editBio by mutableStateOf("")
    var editPortfolioUrl by mutableStateOf("")
    var editWebsiteUrl by mutableStateOf("")
    var editTagline by mutableStateOf("")
    var editPerks by mutableStateOf<List<String>>(emptyList())

    var nameError by mutableStateOf<String?>(null)
    var phoneError by mutableStateOf<String?>(null)
    var locationError by mutableStateOf<String?>(null)
    var urlError by mutableStateOf<String?>(null)

    var selectedTab by mutableStateOf("Overview")
    var addEntryDialogFor by mutableStateOf<String?>(null)

    val experienceEntries: List<ProfileEntry>
        get() = user?.experienceEntries.orEmpty()
    val educationEntries: List<ProfileEntry>
        get() = user?.educationEntries.orEmpty()
    val certificationEntries: List<ProfileEntry>
        get() = user?.certificationEntries.orEmpty()

    var isUploadingAvatar by mutableStateOf(false)
        private set
    var avatarError by mutableStateOf<String?>(null)
    var showAvatarManager by mutableStateOf(false)
    var isLoadingAvatarHistory by mutableStateOf(false)
        private set
    var avatarHistory by mutableStateOf<List<AvatarHistoryItem>>(emptyList())
        private set

    var isUploadingResume by mutableStateOf(false)
        private set
    var resumeError by mutableStateOf<String?>(null)
    var showResumeDialog by mutableStateOf(false)

    var isUploadingCertificate by mutableStateOf(false)
        private set
    var certificateError by mutableStateOf<String?>(null)

    val isEmployer: Boolean
        get() = user?.role == UserRole.EMPLOYER

    fun bind(incoming: User?) {
        if (incoming == null) return
        val shouldReload = boundUserId != incoming.id
        val existing = user
        user = if (!shouldReload && existing != null) {
            incoming.copy(
                experienceEntries = incoming.experienceEntries.ifEmpty { existing.experienceEntries },
                educationEntries = incoming.educationEntries.ifEmpty { existing.educationEntries },
                certificationEntries = incoming.certificationEntries.ifEmpty { existing.certificationEntries }
            )
        } else {
            incoming
        }
        if (shouldReload) {
            boundUserId = incoming.id
            refreshUser()
        }
    }

    fun consumeAvatarError() {
        avatarError = null
    }

    fun consumeResumeError() {
        resumeError = null
    }

    fun consumeCertificateError() {
        certificateError = null
    }

    fun startEditing() {
        val current = user ?: return
        editName = if (isEmployer) current.companyName else current.name
        editPhone = current.phone
        editLocation = current.location
        editIndustry = current.industry
        editCompanySize = current.companySize
        editExperienceLevel = current.experienceLevel.ifBlank { ProfileOptions.EXPERIENCE_LEVELS.first() }
        editSkills = current.skills
        editBio = current.bio
        editPortfolioUrl = current.portfolioUrl
        editWebsiteUrl = current.websiteUrl
        editTagline = current.tagline
        editPerks = current.perks
        nameError = null
        phoneError = null
        locationError = null
        urlError = null
        saveErrorMessage = ""
        isEditing = true
    }

    fun cancelEditing() {
        isEditing = false
        saveErrorMessage = ""
    }

    fun togglePerk(perk: String) {
        editPerks = if (editPerks.contains(perk)) editPerks - perk else editPerks + perk
    }

    fun saveProfile(onUpdated: (User) -> Unit) {
        val current = user
        if (current == null) {
            saveErrorMessage = "User information is missing."
            return
        }

        val nameValidation = if (isEmployer) {
            ValidationUtils.validateCompanyName(editName)
        } else {
            ValidationUtils.validateFullName(editName)
        }
        val phoneValidation = ValidationUtils.validatePhone(editPhone, required = false)
        val locationValidation = ValidationUtils.validateLocation(editLocation, required = false)
        val urlValue = if (isEmployer) editWebsiteUrl else editPortfolioUrl
        val urlValidation = ValidationUtils.validatePortfolioUrl(urlValue)

        nameError = nameValidation
        phoneError = phoneValidation
        locationError = locationValidation
        urlError = urlValidation

        if (nameValidation != null || phoneValidation != null || locationValidation != null || urlValidation != null) {
            saveErrorMessage = "Please fix the highlighted fields before saving."
            return
        }

        persistUser(
            current.copy(
                name = if (isEmployer) current.name else editName.trim(),
                companyName = if (isEmployer) editName.trim() else current.companyName,
                phone = editPhone.trim(),
                location = editLocation.trim(),
                industry = if (isEmployer) editIndustry.trim() else current.industry,
                companySize = if (isEmployer) editCompanySize.trim() else current.companySize,
                experienceLevel = if (isEmployer) current.experienceLevel else editExperienceLevel.trim(),
                skills = if (isEmployer) current.skills else editSkills.trim(),
                bio = editBio.trim(),
                portfolioUrl = if (isEmployer) current.portfolioUrl else editPortfolioUrl.trim(),
                websiteUrl = if (isEmployer) editWebsiteUrl.trim() else current.websiteUrl,
                tagline = if (isEmployer) editTagline.trim() else current.tagline,
                perks = if (isEmployer) editPerks else current.perks
            ),
            savingProfile = true,
            onUpdated = { updated ->
                onUpdated(updated)
                isEditing = false
            }
        )
    }

    fun uploadAvatar(bytes: ByteArray, extension: String, onUpdated: (User) -> Unit) {
        val current = user
        val userId = current?.id
        if (current == null || userId.isNullOrBlank()) {
            avatarError = "User information is missing."
            return
        }
        viewModelScope.launch {
            isUploadingAvatar = true
            try {
                val uploadedUrl = UserRepository.uploadAvatar(userId, bytes, extension)
                if (uploadedUrl == null) {
                    avatarError = "Failed to upload photo. Please try again."
                    return@launch
                }
                persistUser(
                    current.copy(avatarUrl = "$uploadedUrl?t=${System.currentTimeMillis()}"),
                    onUpdated = onUpdated
                )
            } catch (e: Exception) {
                avatarError = e.message ?: "An unexpected error occurred."
            } finally {
                isUploadingAvatar = false
            }
        }
    }

    fun loadAvatarHistory() {
        val userId = user?.id
        if (userId.isNullOrBlank()) return
        viewModelScope.launch {
            isLoadingAvatarHistory = true
            avatarHistory = UserRepository.listAvatarHistory(userId)
            isLoadingAvatarHistory = false
        }
    }

    fun selectAvatarFromHistory(item: AvatarHistoryItem, onUpdated: (User) -> Unit) {
        val current = user ?: return
        persistUser(
            current.copy(avatarUrl = "${item.url}?t=${System.currentTimeMillis()}"),
            onUpdated = {
                onUpdated(it)
                showAvatarManager = false
            }
        )
    }

    fun deleteAvatarFromHistory(item: AvatarHistoryItem, onUpdated: (User) -> Unit) {
        viewModelScope.launch {
            val deleted = UserRepository.deleteAvatar(item.path)
            if (!deleted) {
                avatarError = "Couldn't delete photo. Please try again."
                return@launch
            }
            avatarHistory = avatarHistory.filterNot { it.path == item.path }
            val current = user
            if (current != null && current.avatarUrl.substringBefore("?") == item.url) {
                persistUser(current.copy(avatarUrl = ""), onUpdated = onUpdated)
            }
        }
    }

    fun uploadResume(bytes: ByteArray, onUpdated: (User) -> Unit) {
        val current = user
        val userId = current?.id
        if (current == null || userId.isNullOrBlank()) {
            resumeError = "User information is missing."
            return
        }
        viewModelScope.launch {
            isUploadingResume = true
            try {
                val uploadedUrl = UserRepository.uploadResume(userId, bytes)
                if (uploadedUrl == null) {
                    resumeError = "Failed to upload resume. Please try again."
                    return@launch
                }
                persistUser(
                    current.copy(resumeUrl = "$uploadedUrl?t=${System.currentTimeMillis()}"),
                    onUpdated = onUpdated
                )
            } catch (e: Exception) {
                resumeError = e.message ?: "An unexpected error occurred."
            } finally {
                isUploadingResume = false
            }
        }
    }

    fun removeResume(onUpdated: (User) -> Unit) {
        val current = user ?: return
        persistUser(
            current.copy(resumeUrl = ""),
            onUpdated = {
                onUpdated(it)
                showResumeDialog = false
            }
        )
    }

    fun uploadCertificate(bytes: ByteArray, extension: String, onDone: (String?) -> Unit) {
        val userId = user?.id
        if (userId.isNullOrBlank()) {
            certificateError = "User information is missing."
            onDone(null)
            return
        }
        viewModelScope.launch {
            isUploadingCertificate = true
            val url = UserRepository.uploadCertificate(userId, bytes, extension)
            isUploadingCertificate = false
            if (url == null) {
                certificateError = "Failed to upload certificate. Please try again."
            }
            onDone(url)
        }
    }

    fun addEntry(category: String, entry: ProfileEntry) {
        val current = user ?: return
        val updated = when (category) {
            "Experience" -> current.copy(experienceEntries = current.experienceEntries + entry)
            "Education" -> current.copy(educationEntries = current.educationEntries + entry)
            "Certification" -> current.copy(certificationEntries = current.certificationEntries + entry)
            else -> current
        }
        addEntryDialogFor = null
        persistUser(updated, failMessage = "Couldn't save profile entries. Please try again.")
    }

    fun removeEntry(category: String, entry: ProfileEntry) {
        val current = user ?: return
        val updated = when (category) {
            "Experience" -> current.copy(experienceEntries = current.experienceEntries.filterNot { it.id == entry.id })
            "Education" -> current.copy(educationEntries = current.educationEntries.filterNot { it.id == entry.id })
            "Certification" -> current.copy(certificationEntries = current.certificationEntries.filterNot { it.id == entry.id })
            else -> current
        }
        persistUser(updated, failMessage = "Couldn't save profile entries. Please try again.")
    }

    private fun refreshUser() {
        val userId = user?.id ?: return
        if (isEmployer) return
        viewModelScope.launch {
            val fresh = UserRepository.fetchUserById(userId) ?: return@launch
            user = fresh
        }
    }

    private fun persistUser(
        updated: User,
        savingProfile: Boolean = false,
        failMessage: String? = null,
        onUpdated: ((User) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (savingProfile) {
                isSaving = true
                saveErrorMessage = ""
            }
            try {
                val isSaved = UserRepository.updateUserInSupabase(updated)
                if (isSaved) {
                    user = updated
                    onUpdated?.invoke(updated)
                } else {
                    val message = failMessage ?: "Failed to save profile. Please try again."
                    if (savingProfile) saveErrorMessage = message else certificateError = message
                }
            } catch (e: Exception) {
                val message = e.message ?: "An unexpected error occurred."
                if (savingProfile) saveErrorMessage = message else certificateError = message
            } finally {
                if (savingProfile) isSaving = false
            }
        }
    }
}
