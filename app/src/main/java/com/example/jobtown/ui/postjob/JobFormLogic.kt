package com.example.jobtown.ui.postjob

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

val JobTypeOptions = listOf("Full-time", "Part-time", "Contract", "Internship", "Freelance")
val MinSalaryOptions = (1000..20000 step 1000).map { "%,d".format(it) }
val MaxSalaryOptions = (2000..30000 step 1000).map { "%,d".format(it) } + listOf("30,000+")

class JobFormFields(
    title: String = "",
    company: String = "",
    location: String = "",
    minSalary: String = "",
    maxSalary: String = "",
    type: String = "Full-time",
    description: String = "",
    requirements: String = "",
    skills: String = "",
    isFeatured: Boolean = false,
    isOkuFriendly: Boolean = false,
    useCustomLocation: Boolean = true
) {
    var title by mutableStateOf(title)
    var company by mutableStateOf(company)
    var location by mutableStateOf(location)
    var minSalary by mutableStateOf(minSalary)
    var maxSalary by mutableStateOf(maxSalary)
    var type by mutableStateOf(type)
    var description by mutableStateOf(description)
    var requirements by mutableStateOf(requirements)
    var skills by mutableStateOf(skills)
    var isFeatured by mutableStateOf(isFeatured)
    var isOkuFriendly by mutableStateOf(isOkuFriendly)
    var useCustomLocation by mutableStateOf(useCustomLocation)
    var errorMessage by mutableStateOf("")

    fun formattedSalary(blankFallback: String = "Negotiable"): String = when {
        minSalary.isNotBlank() && maxSalary.isNotBlank() -> "$$minSalary - $$maxSalary / month"
        minSalary.isNotBlank() -> "From $$minSalary / month"
        maxSalary.isNotBlank() -> "Up to $$maxSalary / month"
        else -> blankFallback
    }

    fun requirementsList(): List<String> = splitCommaList(requirements)
    fun skillsList(): List<String> = splitCommaList(skills)

    fun validate(requireSalary: Boolean): String? {
        if (title.isBlank() || company.isBlank() || location.isBlank() || description.isBlank()) {
            return "Please fill in all required fields marked with *"
        }
        if (requireSalary && (minSalary.isBlank() || maxSalary.isBlank())) {
            return "Please select both Min and Max salary range."
        }
        val minVal = parseSalaryValue(minSalary)
        val maxVal = parseSalaryValue(maxSalary)
        if (minSalary.isNotBlank() && maxSalary.isNotBlank() && maxSalary != "30,000+" && minVal > maxVal) {
            return "Minimum salary cannot be greater than Maximum salary."
        }
        return null
    }
}//

fun parseSalaryValue(valueStr: String): Int {
    return valueStr.replace(",", "").replace("+", "").replace("$", "").trim().toIntOrNull() ?: 0
}

fun parseSalaryBounds(salary: String): Pair<String, String> {
    val amounts = Regex("""\$([0-9,]+)""").findAll(salary).map { it.groupValues[1] }.toList()
    val min = amounts.getOrNull(0).orEmpty()
    val max = if (salary.contains("30,000+")) "30,000+" else amounts.getOrNull(1).orEmpty()
    return min to max
}

fun splitCommaList(value: String): List<String> =
    value.split(",").map { it.trim() }.filter { it.isNotEmpty() }

@Composable
fun rememberJobFormFields(
    title: String = "",
    company: String = "",
    location: String = "",
    minSalary: String = "",
    maxSalary: String = "",
    type: String = "Full-time",
    description: String = "",
    requirements: String = "",
    skills: String = "",
    isFeatured: Boolean = false,
    isOkuFriendly: Boolean = false,
    useCustomLocation: Boolean = true
): JobFormFields = remember(
    title, company, location, minSalary, maxSalary, type,
    description, requirements, skills, isFeatured, isOkuFriendly, useCustomLocation
) {
    JobFormFields(
        title = title,
        company = company,
        location = location,
        minSalary = minSalary,
        maxSalary = maxSalary,
        type = type,
        description = description,
        requirements = requirements,
        skills = skills,
        isFeatured = isFeatured,
        isOkuFriendly = isOkuFriendly,
        useCustomLocation = useCustomLocation
    )
}
