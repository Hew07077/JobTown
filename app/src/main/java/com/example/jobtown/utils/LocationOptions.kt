package com.example.jobtown.utils

/**
 * Shared country list + helpers for building/parsing a "City, Country" location string
 * that can optionally hold several branch addresses (used by employers with more than
 * one office). Multiple addresses are stored as a single delimited string so no database
 * schema change is required: each address is "City, Country" and addresses are joined
 * with " | ".
 */
object LocationOptions {

    const val ADDRESS_DELIMITER = " | "

    val COUNTRIES: List<String> = listOf(
        "Malaysia", "Singapore", "Indonesia", "Thailand", "Vietnam", "Philippines",
        "Brunei", "Cambodia", "Laos", "Myanmar",
        "China", "Hong Kong", "Macau", "Taiwan", "Japan", "South Korea",
        "India", "Pakistan", "Bangladesh", "Sri Lanka", "Nepal",
        "Australia", "New Zealand",
        "United States", "Canada", "Mexico", "Brazil", "Argentina",
        "United Kingdom", "Ireland", "France", "Germany", "Netherlands",
        "Belgium", "Switzerland", "Austria", "Spain", "Portugal", "Italy",
        "Sweden", "Norway", "Denmark", "Finland", "Poland",
        "United Arab Emirates", "Saudi Arabia", "Qatar", "Kuwait", "Bahrain", "Oman",
        "South Africa", "Egypt", "Nigeria", "Kenya",
        "Other"
    )

    data class Address(val city: String, val country: String) {
        fun display(): String = when {
            city.isBlank() && country.isBlank() -> ""
            city.isBlank() -> country
            country.isBlank() -> city
            else -> "$city, $country"
        }
    }

    /** Splits a combined "City, Country | City, Country" string into individual addresses. */
    fun parseAddresses(combined: String): List<Address> {
        if (combined.isBlank()) return emptyList()
        return combined.split(ADDRESS_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { entry ->
                val parts = entry.split(",", limit = 2).map { it.trim() }
                if (parts.size == 2) Address(city = parts[0], country = parts[1])
                else Address(city = entry, country = "")
            }
    }

    /** Joins a primary address plus any additional branch addresses into one storable string. */
    fun buildLocationString(primary: Address, branches: List<Address> = emptyList()): String {
        val all = (listOf(primary) + branches).filter { it.display().isNotBlank() }
        return all.joinToString(ADDRESS_DELIMITER) { it.display() }
    }
}
