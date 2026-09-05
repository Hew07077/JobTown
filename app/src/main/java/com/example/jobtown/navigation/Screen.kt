package com.example.jobtown

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    /** Jobseeker: applications they submitted. */
    object Applied : Screen("applied")
    /** Employer: jobs they posted and incoming applications. */
    object ManageJobs : Screen("manage_jobs")
    object Schedule : Screen("schedule")
    object ScheduleDetail : Screen("schedule_detail/{scheduleId}") {
        fun createRoute(scheduleId: String) = "schedule_detail/$scheduleId"
    }
    object Chat : Screen("chat")
    object Notifications : Screen("notifications")

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
            // Uri.encode() (not URLEncoder.encode()) -- this must match Uri.decode()
            // on the receiving end in NavGraph.kt. URLEncoder encodes spaces as '+',
            // but Uri.decode() only understands %XX percent-encoding and leaves a
            // literal '+' alone, so names with spaces showed up as "ming+en" instead
            // of "ming en" after decoding.
            val encodedCompany = Uri.encode(company)
            val encodedTitle = Uri.encode(title)
            val encodedQuestion = Uri.encode(initialQuestion)
            return "chat_detail/$chatRoomId?company=$encodedCompany&title=$encodedTitle&initialQuestion=$encodedQuestion"
        }
    }
}