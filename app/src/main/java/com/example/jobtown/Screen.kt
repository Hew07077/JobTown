package com.example.jobtown

sealed class Screen(val route: String) {
    object Startup : Screen("startup")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object CompleteProfile : Screen("complete_profile")
    object Home : Screen("home")
    object Applied : Screen("applied")
    object Schedule : Screen("schedule")
    object Chat : Screen("chat")
    object Profile : Screen("profile")
    object PostJob : Screen("post_job")
}