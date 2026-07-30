package com.example.jobtown.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://gkcbjvpvpwycsofrlfbc.supabase.co/rest/v1/",
        supabaseKey = "sb_secret_buyIFRTLsRXJgnQOtHYC_A_2ZHh6ofC"
    ) {
        install(Postgrest)
        install(Realtime)
    }
}