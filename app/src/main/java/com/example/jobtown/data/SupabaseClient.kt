@file:OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)

package com.example.jobtown.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

/**
 * supabase-kt 2.0 puts the API key on Authorization when there is no session.
 * A publishable key is not a JWT, so PostgREST rejects it. Keep Authorization
 * only when the value is a real user access token so RLS sees `authenticated`
 * and `auth.uid()`.
 */
private fun looksLikeJwt(token: String): Boolean {
    val trimmed = token.trim()
    return trimmed.startsWith("eyJ") && trimmed.count { it == '.' } == 2
}

private val jwtOnlyAuthorization = createClientPlugin("JwtOnlyAuthorization") {
    on(Send) { request ->
        val tokens = request.headers.getAll(HttpHeaders.Authorization)
            .orEmpty()
            .map { it.removePrefix("Bearer").trim() }
            .filter { it.isNotEmpty() }
        val jwt = tokens.firstOrNull { looksLikeJwt(it) }
        request.headers.remove(HttpHeaders.Authorization)
        if (jwt != null) {
            request.headers.append(HttpHeaders.Authorization, "Bearer $jwt")
        }
        proceed(request)
    }
}

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://neyyuqwgqlvcdwabzmut.supabase.co",
        supabaseKey = "sb_publishable_TSybP_WPy2E4PrAXA9CBBg_V2D8XAKr"
    ) {
        // Configures the JSON serializer to ignore extra or unknown database/metadata fields
        defaultSerializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
        )

        httpConfig {
            install(jwtOnlyAuthorization)
        }

        install(Postgrest)
        install(Auth)     // Needed for user login, signup, and session management
        install(Realtime) // Needed for live chat updates
        install(Storage)  // Needed for PDF resume uploads & user avatars
    }
}
