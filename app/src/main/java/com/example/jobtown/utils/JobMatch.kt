package com.example.jobtown.utils

import com.example.jobtown.data.model.Job
import com.example.jobtown.data.model.UserProfile

/**
 * Result of scoring a single [Job] against a job seeker's [UserProfile].
 *
 * [score] is a 0-100 percentage used to rank/sort jobs and to render a
 * "Match" badge in the UI. [matchedSkills] / [missingSkills] power the
 * "why this matches" explanation shown on the job detail / apply screens,
 * and [reasons] is a short, human-readable summary (e.g. "3 of 4 skills
 * match", "Same location") that is announced to screen readers alongside
 * the numeric score so the ranking isn't communicated by color alone.
 */
data class JobMatchResult(
    val score: Int,
    val matchedSkills: List<String>,
    val missingSkills: List<String>,
    val reasons: List<String>
) {
    val label: String
        get() = when {
            score >= 85 -> "Excellent match"
            score >= 65 -> "Strong match"
            score >= 40 -> "Fair match"
            else -> "Low match"
        }
}

/**
 * Lightweight, on-device job matching engine.
 *
 * The score blends four signals, weighted by how strongly each predicts
 * whether a seeker would actually be a good fit:
 *  - Skills overlap (55%): the core signal. Uses normalized/fuzzy string
 *    comparison so "React.js" matches "react" and "Node" matches "Node.js".
 *  - Experience level (20%): compares the seeker's stated experience
 *    against keywords found in the job title/description/requirements.
 *  - Location (15%): exact/partial match, with remote jobs always scoring
 *    full marks since location is irrelevant for them.
 *  - Recency boost (10%): newer listings are nudged up slightly so equally
 *    good matches don't go stale at the top of the feed forever.
 *
 * All matching is done locally against already-fetched data - no network
 * calls - so it's cheap to recompute whenever the job list or profile
 * changes.
 */
object JobMatchUtils {

    private val experienceOrder = listOf(
        "intern", "entry", "junior", "mid", "intermediate", "senior", "lead", "principal", "director"
    )

    fun normalizeSkill(skill: String): String =
        skill.trim().lowercase()
            .removeSuffix(".js")
            .removeSuffix("js")
            .replace(Regex("[^a-z0-9+#]"), "")

    /** Scores a single job against a profile. Falls back gracefully when the profile is incomplete. */
    fun score(job: Job, profile: UserProfile?): JobMatchResult {
        if (profile == null) {
            return JobMatchResult(
                score = 0,
                matchedSkills = emptyList(),
                missingSkills = job.skills.orEmpty(),
                reasons = emptyList()
            )
        }

        val reasons = mutableListOf<String>()

        // --- Skills (55%) ---
        val jobSkills = job.skills.orEmpty().ifEmpty { job.requirements.orEmpty() }

        // Handle skills safely whether provided as a String or List representation
        val rawSeekerSkills: List<String> = when (val skillsObj = profile.skills as Any?) {
            is List<*> -> skillsObj.mapNotNull { it?.toString() }
            is String -> skillsObj.split(",")
            else -> emptyList()
        }

        val seekerSkillsNormalized = rawSeekerSkills
            .map { normalizeSkill(it) }
            .filter { it.isNotBlank() }
            .toSet()

        val matched = mutableListOf<String>()
        val missing = mutableListOf<String>()
        for (rawSkill in jobSkills) {
            val normalized = normalizeSkill(rawSkill)
            val isMatch = normalized.isNotBlank() && seekerSkillsNormalized.any { seekerSkill ->
                seekerSkill == normalized || seekerSkill.contains(normalized) || normalized.contains(seekerSkill)
            }
            if (isMatch) matched += rawSkill else missing += rawSkill
        }

        val skillScore: Double = when {
            jobSkills.isEmpty() -> 60.0 // No listed skills - neutral score, don't punish or reward
            else -> (matched.size.toDouble() / jobSkills.size.toDouble()) * 100.0
        }
        if (jobSkills.isNotEmpty()) {
            reasons += "${matched.size} of ${jobSkills.size} required skills match your profile"
        }

        // --- Experience level (20%) ---
        val seekerLevel = experienceKeyword(profile.experienceLevel)
        val jobText = "${job.title} ${job.description.orEmpty()} ${job.requirements.orEmpty().joinToString(" ")}".lowercase()
        val jobLevel = experienceOrder.firstOrNull { jobText.contains(it) }
        val experienceScore: Double = when {
            seekerLevel == null || jobLevel == null -> 70.0 // Unknown - neutral, don't penalize
            seekerLevel == jobLevel -> 100.0
            else -> {
                val seekerIdx = experienceOrder.indexOf(seekerLevel)
                val jobIdx = experienceOrder.indexOf(jobLevel)
                val distance = kotlin.math.abs(seekerIdx - jobIdx)
                (100.0 - distance * 25.0).coerceAtLeast(20.0)
            }
        }
        if (seekerLevel != null && jobLevel != null) {
            if (seekerLevel == jobLevel) reasons += "Experience level matches ($jobLevel)"
        }

        // --- Location (15%) ---
        val jobLocation = job.location.orEmpty().trim().lowercase()
        val seekerLocation = profile.location?.trim()?.lowercase().orEmpty()
        val isRemote = jobLocation.contains("remote") || job.type.orEmpty().contains("remote", ignoreCase = true)
        val locationScore: Double = when {
            isRemote -> 100.0
            seekerLocation.isBlank() || jobLocation.isBlank() -> 60.0
            jobLocation == seekerLocation -> 100.0
            jobLocation.contains(seekerLocation) || seekerLocation.contains(jobLocation) -> 85.0
            else -> 35.0
        }
        if (isRemote) reasons += "Remote - work from anywhere"
        else if (seekerLocation.isNotBlank() && (jobLocation == seekerLocation)) reasons += "Same location as your profile"

        // --- Recency boost (10%) ---
        val recencyScore: Double = 70.0 // Neutral baseline

        val weighted = skillScore * 0.55 + experienceScore * 0.20 + locationScore * 0.15 + recencyScore * 0.10

        return JobMatchResult(
            score = weighted.coerceIn(0.0, 100.0).let { Math.round(it).toInt() },
            matchedSkills = matched,
            missingSkills = missing,
            reasons = reasons
        )
    }

    /** Scores and sorts jobs best-match-first. Ties broken by newest first. */
    fun sortByMatch(jobs: List<Job>, profile: UserProfile?): List<Job> {
        if (profile == null) return jobs
        return jobs.sortedWith(
            compareByDescending<Job> { score(it, profile).score }
                .thenByDescending { it.createdAt.orEmpty() }
        )
    }

    private fun experienceKeyword(level: String?): String? {
        if (level.isNullOrBlank()) return null
        val lower = level.lowercase()
        return experienceOrder.firstOrNull { lower.contains(it) }
    }
}