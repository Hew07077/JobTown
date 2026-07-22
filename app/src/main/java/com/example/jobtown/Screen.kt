package com.example.jobtown

sealed class Screen(val route: String) {
    object Startup : Screen("startup")
    object Login : Screen("login")
    object SignUp : Screen("sign_up")
    object CompleteProfile : Screen("complete_profile")
    object Home : Screen("home")
    object PostJob : Screen("post_job")
    object Chat : Screen("chat")
    object Applied : Screen("applied")
    object Schedule : Screen("schedule")
    object Profile : Screen("profile")
}