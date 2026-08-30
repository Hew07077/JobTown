package com.example.jobtown

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Home : Screen("home")
    /** Job seeker: applications they submitted. */
    object Applied : Screen("applied")
    /** Employer: jobs they posted and incoming applications. */
    object ManageJobs : Screen("manage_jobs")
    object Schedule : Screen("schedule")
    object ScheduleDetail : Screen("schedule_detail/{scheduleId}") {
        fun createRoute(scheduleId: String) = "schedule_detail/$scheduleId"
    }
    object Chat : Screen("chat")

    object ApplicationDetail : Screen("application_detail/{applicationId}") {
        fun createRoute(applicationId: String) = "application_detail/$applicationId"
    }

    object ChatDetail : Screen("chat_detail/{chatRoomId}?company={company}&title={title}&initialQuestion={initialQuestion}") {
        fun createRoute(
            chatRoomId: String,
            company: String = "",
            title: String = "",
            initialQuestion: String = ""
        ): String {
            val encodedCompany = URLEncoder.encode(company, StandardCharsets.UTF_8.toString())
            val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            val encodedQuestion = URLEncoder.encode(initialQuestion, StandardCharsets.UTF_8.toString())
            return "chat_detail/$chatRoomId?company=$encodedCompany&title=$encodedTitle&initialQuestion=$encodedQuestion"
        }
    }
}