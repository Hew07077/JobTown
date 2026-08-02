package com.example.jobtown.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://gkcbjvpvpwycsofrlfbc.supabase.co",
        supabaseKey = "sb_publishable_o_HrjcNeiyvRp3PM3TgVcg_Z7oM7qpW"
    ) {
        // Configures the JSON serializer to ignore extra or unknown database/metadata fields
        defaultSerializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
        )

        install(Postgrest)
        install(Auth)     // Needed for user login, signup, and session management
        install(Realtime) // Needed for live chat updates
        install(Storage)  // Needed for PDF resume uploads & user avatars
    }
}