package com.example.jobtown.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://YOUR_PROJECT_ID.supabase.co", // Replace with your URL
        supabaseKey = "YOUR_ANON_KEY"                        // Replace with your Anon Key
    ) {
        install(Postgrest)
    }
}
