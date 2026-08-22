package com.example.jobtown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.navigation.AppNavGraph
import com.example.jobtown.ui.theme.JobTownTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JobTownTheme {
                // Pass the client from your SupabaseClient singleton object
                AppNavGraph(supabaseClient = SupabaseClient.client)
            }
        }
    }
}