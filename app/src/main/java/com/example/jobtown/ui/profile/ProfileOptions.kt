package com.example.jobtown.ui.profile

import java.util.Calendar

object ProfileOptions {
    val EXPERIENCE_LEVELS = listOf(
        "Student / Entry",
        "Junior (1-2 yrs)",
        "Mid-Level (3-5 yrs)",
        "Senior (5+ yrs)"
    )

    val EMPLOYMENT_TYPES = listOf(
        "Full-time",
        "Part-time",
        "Contract",
        "Internship"
    )

    val EDUCATION_LEVELS = listOf(
        "SPM / O-Level",
        "Diploma",
        "Bachelor's Degree",
        "Master's Degree",
        "PhD",
        "Professional Certificate",
        "Other"
    )

    val CERTIFICATE_ISSUERS = listOf(
        "Google",
        "Microsoft",
        "Amazon Web Services",
        "Meta",
        "IBM",
        "CompTIA",
        "Coursera",
        "LinkedIn Learning",
        "Other"
    )

    val INDUSTRIES = listOf(
        "Technology / IT",
        "Finance / Banking",
        "Healthcare",
        "Retail / E-commerce",
        "Manufacturing",
        "Education",
        "Hospitality / F&B",
        "Construction / Real Estate",
        "Logistics / Transportation",
        "Media / Marketing",
        "Other"
    )

    val COMPANY_SIZES = listOf(
        "1-10 employees",
        "11-50 employees",
        "51-200 employees",
        "201-500 employees",
        "501-1000 employees",
        "1000+ employees"
    )

    val PERKS = listOf(
        "Remote Friendly",
        "Health Insurance",
        "Flexible Hours",
        "401(k) Matching",
        "Learning Stipend",
        "Paid Time Off",
        "Parental Leave",
        "Performance Bonus"
    )

    val YEARS: List<String> = buildList {
        add("Present")
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        for (year in thisYear downTo 1990) add(year.toString())
    }
}
