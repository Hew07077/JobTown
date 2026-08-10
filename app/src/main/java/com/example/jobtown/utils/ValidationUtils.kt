package com.example.jobtown.utils

/**
 * Centralized input-validation rules shared across the Login, SignUp,
 * CompleteProfile and Profile screens so every form in the app enforces
 * identical constraints and shows identical error copy.
 *
 * Two kinds of helpers are provided:
 *  - filterXxxInput(...)  -> used inside onValueChange to block invalid
 *                            keystrokes as the user types (e.g. letters
 *                            only for name, digits only for phone).
 *  - validateXxx(...)     -> returns null when the value is valid, or a
 *                            user-facing error String otherwise. Called
 *                            on submit (and can also be called live).
 */
object ValidationUtils {

    // ---- Length limits -----------------------------------------------
    const val NAME_MIN_LENGTH = 2
    const val NAME_MAX_LENGTH = 50

    const val EMAIL_MAX_LENGTH = 254

    const val PASSWORD_MIN_LENGTH = 6
    const val PASSWORD_MAX_LENGTH = 64

    const val PHONE_MIN_DIGITS = 7
    const val PHONE_MAX_DIGITS = 15

    const val LOCATION_MAX_LENGTH = 100
    const val SKILLS_MAX_LENGTH = 150
    const val BIO_MAX_LENGTH = 500
    const val URL_MAX_LENGTH = 200

    // ---- Regex ----------------------------------------------------------
    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val NAME_REGEX =
        Regex("^[\\p{L}][\\p{L}'\\-/ ]*$")
    private val PHONE_REGEX =
        Regex("^[+]?[0-9]{$PHONE_MIN_DIGITS,$PHONE_MAX_DIGITS}$")
    private val URL_REGEX =
        Regex("^(https?://)?([\\w-]+\\.)+[\\w-]{2,}(/\\S*)?$", RegexOption.IGNORE_CASE)
    private val PASSWORD_HAS_LETTER = Regex("[A-Za-z]")
    private val PASSWORD_HAS_DIGIT = Regex("[0-9]")

    // ---- Live keystroke filters ------------------------------------------

    /** Letters, spaces, hyphens, apostrophes and '/' only (e.g. "Mary-Jane O'Neil", "Ahmad A/L Ismail"). */
    fun filterNameInput(input: String): String =
        input.filter { it.isLetter() || it == ' ' || it == '\'' || it == '-' || it == '/' }
            .take(NAME_MAX_LENGTH)

    /** Digits only, with an optional leading '+' for country codes. */
    fun filterPhoneInput(input: String): String {
        val allowed = input.filterIndexed { index, c ->
            c.isDigit() || (c == '+' && index == 0)
        }
        return allowed.take(PHONE_MAX_DIGITS + 1) // +1 to allow for the leading '+'
    }

    /** Generic "digits only" filter, capped to [maxLength]. */
    fun filterDigitsOnly(input: String, maxLength: Int = Int.MAX_VALUE): String =
        input.filter { it.isDigit() }.take(maxLength)

    // ---- Validators --------------------------------------------------------

    fun validateFullName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isBlank() -> "Please enter your full name."
            trimmed.length < NAME_MIN_LENGTH -> "Name must be at least $NAME_MIN_LENGTH characters."
            trimmed.length > NAME_MAX_LENGTH -> "Name must be under $NAME_MAX_LENGTH characters."
            !NAME_REGEX.matches(trimmed) -> "Name can only contain letters, spaces, hyphens, apostrophes and '/'."
            else -> null
        }
    }

    /** Company names are allowed any special characters (Sdn Bhd, &, ., etc.) -- only length is enforced. */
    fun validateCompanyName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isBlank() -> "Please enter your company name."
            trimmed.length < NAME_MIN_LENGTH -> "Company name must be at least $NAME_MIN_LENGTH characters."
            trimmed.length > NAME_MAX_LENGTH -> "Company name must be under $NAME_MAX_LENGTH characters."
            else -> null
        }
    }

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isBlank() -> "Please enter your email address."
            trimmed.length > EMAIL_MAX_LENGTH -> "Email address is too long."
            !EMAIL_REGEX.matches(trimmed) -> "Please enter a valid email address."
            else -> null
        }
    }

    /** Login only checks the password isn't empty -- strength rules are enforced at signup. */
    fun validateLoginPassword(password: String): String? =
        if (password.isBlank()) "Please enter your password." else null

    fun validateNewPassword(password: String): String? {
        return when {
            password.isBlank() -> "Please enter a password."
            password.length < PASSWORD_MIN_LENGTH -> "Password must be at least $PASSWORD_MIN_LENGTH characters long."
            password.length > PASSWORD_MAX_LENGTH -> "Password must be under $PASSWORD_MAX_LENGTH characters."
            !PASSWORD_HAS_LETTER.containsMatchIn(password) -> "Password must contain at least one letter."
            !PASSWORD_HAS_DIGIT.containsMatchIn(password) -> "Password must contain at least one number."
            else -> null
        }
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Please confirm your password."
            password != confirmPassword -> "Passwords do not match."
            else -> null
        }
    }

    fun validatePhone(phone: String, required: Boolean = false): String? {
        val trimmed = phone.trim()
        if (trimmed.isBlank()) {
            return if (required) "Please enter your phone number." else null
        }
        val digitCount = trimmed.count { it.isDigit() }
        return when {
            !PHONE_REGEX.matches(trimmed) -> "Phone number can only contain digits (and an optional leading +)."
            digitCount < PHONE_MIN_DIGITS -> "Phone number must have at least $PHONE_MIN_DIGITS digits."
            digitCount > PHONE_MAX_DIGITS -> "Phone number must have at most $PHONE_MAX_DIGITS digits."
            else -> null
        }
    }

    fun validateLocation(location: String, required: Boolean = false): String? {
        val trimmed = location.trim()
        return when {
            trimmed.isBlank() && required -> "Please enter your location."
            trimmed.length > LOCATION_MAX_LENGTH -> "Location must be under $LOCATION_MAX_LENGTH characters."
            else -> null
        }
    }

    fun validateSkills(skills: String): String? =
        if (skills.length > SKILLS_MAX_LENGTH) "Skills must be under $SKILLS_MAX_LENGTH characters." else null

    fun validateBio(bio: String): String? =
        if (bio.length > BIO_MAX_LENGTH) "Bio must be under $BIO_MAX_LENGTH characters." else null

    /** Portfolio URL is optional; only validated when the user actually typed something. */
    fun validatePortfolioUrl(url: String): String? {
        val trimmed = url.trim()
        return when {
            trimmed.isBlank() -> null
            trimmed.length > URL_MAX_LENGTH -> "URL must be under $URL_MAX_LENGTH characters."
            !URL_REGEX.matches(trimmed) -> "Please enter a valid URL (e.g. https://example.com)."
            else -> null
        }
    }
}
