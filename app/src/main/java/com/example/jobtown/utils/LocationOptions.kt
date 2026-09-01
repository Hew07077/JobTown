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

    val CITIES_BY_COUNTRY: Map<String, List<String>> = mapOf(
        "Malaysia" to listOf(
            "Kuala Lumpur", "Petaling Jaya", "Shah Alam", "Subang Jaya", "Klang",
            "George Town", "Johor Bahru", "Ipoh", "Melaka", "Seremban",
            "Kota Kinabalu", "Kuching", "Kuantan", "Alor Setar", "Kuala Terengganu",
            "Miri", "Sandakan", "Kangar", "Kota Bharu", "Cyberjaya", "Putrajaya", "Other"
        ),
        "Singapore" to listOf("Singapore", "Other"),
        "Indonesia" to listOf("Jakarta", "Surabaya", "Bandung", "Medan", "Bali", "Yogyakarta", "Other"),
        "Thailand" to listOf("Bangkok", "Chiang Mai", "Phuket", "Pattaya", "Other"),
        "Vietnam" to listOf("Ho Chi Minh City", "Hanoi", "Da Nang", "Other"),
        "Philippines" to listOf("Manila", "Cebu", "Davao", "Quezon City", "Other"),
        "China" to listOf("Beijing", "Shanghai", "Shenzhen", "Guangzhou", "Hangzhou", "Other"),
        "Hong Kong" to listOf("Hong Kong", "Other"),
        "Japan" to listOf("Tokyo", "Osaka", "Kyoto", "Yokohama", "Other"),
        "South Korea" to listOf("Seoul", "Busan", "Incheon", "Other"),
        "India" to listOf("Bengaluru", "Mumbai", "Delhi", "Hyderabad", "Chennai", "Pune", "Other"),
        "Australia" to listOf("Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide", "Other"),
        "United States" to listOf("New York", "San Francisco", "Los Angeles", "Seattle", "Austin", "Chicago", "Other"),
        "United Kingdom" to listOf("London", "Manchester", "Birmingham", "Edinburgh", "Other"),
        "United Arab Emirates" to listOf("Dubai", "Abu Dhabi", "Sharjah", "Other")
    )

    fun citiesFor(country: String): List<String> =
        CITIES_BY_COUNTRY[country].orEmpty()

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
            .map { entry -> parseOneAddress(entry) }
    }

    private fun parseOneAddress(entry: String): Address {
        val parts = entry.split(",", limit = 2).map { it.trim() }.filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> Address(city = parts[0], country = parts[1])
            isKnownCountry(parts.first()) -> Address(city = "", country = parts.first())
            else -> Address(city = parts.first(), country = "")
        }
    }

    fun isKnownCountry(value: String): Boolean =
        COUNTRIES.any { it.equals(value.trim(), ignoreCase = true) }

    /** Joins a primary address plus any additional branch addresses into one storable string. */
    fun buildLocationString(primary: Address, branches: List<Address> = emptyList()): String {
        val all = (listOf(primary) + branches).filter { it.display().isNotBlank() }
        return all.joinToString(ADDRESS_DELIMITER) { it.display() }
    }
}
