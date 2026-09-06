package com.example.jobtown.utils

/**
 * Malaysian states/federal territories + the cities within each, plus
 * helpers for building/parsing a "City, State" location string that can
 * optionally hold several branch addresses (used by employers with more
 * than one office). Multiple addresses are stored as a single delimited
 * string so no database schema change is required: each address is
 * "City, State" and addresses are joined with " | ".
 */
object LocationOptions {

    const val ADDRESS_DELIMITER = " | "

    val STATES: List<String> = listOf(
        "Johor", "Kedah", "Kelantan", "Kuala Lumpur", "Labuan", "Malacca",
        "Negeri Sembilan", "Pahang", "Penang", "Perak", "Perlis", "Putrajaya",
        "Sabah", "Sarawak", "Selangor", "Terengganu", "Other"
    )

    val CITIES_BY_STATE: Map<String, List<String>> = mapOf(
        "Johor" to listOf(
            "Johor Bahru", "Muar", "Batu Pahat", "Kluang", "Segamat",
            "Pontian", "Kulai", "Skudai", "Other"
        ),
        "Kedah" to listOf(
            "Alor Setar", "Sungai Petani", "Kulim", "Langkawi", "Jitra", "Other"
        ),
        "Kelantan" to listOf(
            "Kota Bharu", "Pasir Mas", "Tanah Merah", "Machang", "Other"
        ),
        "Kuala Lumpur" to listOf("Kuala Lumpur", "Other"),
        "Labuan" to listOf("Labuan", "Other"),
        "Malacca" to listOf("Melaka", "Alor Gajah", "Jasin", "Other"),
        "Negeri Sembilan" to listOf(
            "Seremban", "Port Dickson", "Nilai", "Bahau", "Other"
        ),
        "Pahang" to listOf(
            "Kuantan", "Temerloh", "Bentong", "Cameron Highlands", "Raub", "Other"
        ),
        "Penang" to listOf(
            "George Town", "Bayan Lepas", "Butterworth", "Bukit Mertajam", "Other"
        ),
        "Perak" to listOf(
            "Ipoh", "Taiping", "Teluk Intan", "Sitiawan", "Kampar", "Other"
        ),
        "Perlis" to listOf("Kangar", "Arau", "Other"),
        "Putrajaya" to listOf("Putrajaya", "Other"),
        "Sabah" to listOf(
            "Kota Kinabalu", "Sandakan", "Tawau", "Lahad Datu", "Other"
        ),
        "Sarawak" to listOf(
            "Kuching", "Miri", "Sibu", "Bintulu", "Other"
        ),
        "Selangor" to listOf(
            "Shah Alam", "Petaling Jaya", "Subang Jaya", "Klang", "Cyberjaya",
            "Kajang", "Ampang", "Puchong", "Rawang", "Other"
        ),
        "Terengganu" to listOf(
            "Kuala Terengganu", "Kemaman", "Dungun", "Other"
        )
    )

    fun citiesFor(state: String): List<String> =
        CITIES_BY_STATE[state].orEmpty()

    /**
     * Alphabetical, but keeps a trailing "Other" pinned at the end rather
     * than sorted in with the real place names -- it's a fallback option,
     * not an actual place. Used wherever these lists are shown as a
     * dropdown, so the menu is always sorted regardless of the order the
     * list happens to be declared in above.
     */
    fun sortedForDisplay(options: List<String>): List<String> {
        val (real, other) = options.partition { !it.equals("Other", ignoreCase = true) }
        return real.sorted() + other
    }

    data class Address(val city: String, val state: String) {
        fun display(): String = when {
            city.isBlank() && state.isBlank() -> ""
            city.isBlank() -> state
            state.isBlank() -> city
            else -> "$city, $state"
        }
    }

    /** Splits a combined "City, State | City, State" string into individual addresses. */
    fun parseAddresses(combined: String): List<Address> {
        if (combined.isBlank()) return emptyList()
        return combined.split(ADDRESS_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { entry -> parseOneAddress(entry) }
    }

    private fun parseOneAddress(entry: String): Address {
        val parts = entry.split(",", limit = 2).map { it.trim() }.filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> Address(city = parts[0], state = parts[1])
            isKnownState(parts.first()) -> Address(city = "", state = parts.first())
            else -> Address(city = parts.first(), state = "")
        }
    }

    fun isKnownState(value: String): Boolean =
        STATES.any { it.equals(value.trim(), ignoreCase = true) }

    /** Joins a primary address plus any additional branch addresses into one storable string. */
    fun buildLocationString(primary: Address, branches: List<Address> = emptyList()): String {
        val all = (listOf(primary) + branches).filter { it.display().isNotBlank() }
        return all.joinToString(ADDRESS_DELIMITER) { it.display() }
    }
}