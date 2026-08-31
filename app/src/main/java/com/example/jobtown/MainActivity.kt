package com.example.jobtown

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.navigation.AppNavGraph
import com.example.jobtown.ui.theme.JobTownTheme
import io.github.jan.supabase.gotrue.handleDeeplinks

class MainActivity : ComponentActivity() {

    // Whatever deep link (currently just the "Forgot Password" email link,
    // jobtown://reset-password -- see AndroidManifest.xml's intent-filter)
    // the Activity was opened or re-opened with. Backed by Compose state
    // (not a plain var) so re-assigning it from onNewIntent() below actually
    // triggers recomposition and reaches AppNavGraph.
    private var pendingDeepLinkUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lets the Supabase SDK pull the auth tokens out of the launching
        // intent's URI -- required for the password-reset link to actually
        // establish a recovery session. Must happen before setContent().
        SupabaseClient.client.handleDeeplinks(intent)
        pendingDeepLinkUri = intent.data

        setContent {
            JobTownTheme {
                // Pass the client from your SupabaseClient singleton object
                AppNavGraph(
                    supabaseClient = SupabaseClient.client,
                    pendingDeepLinkUri = pendingDeepLinkUri,
                    onDeepLinkHandled = { pendingDeepLinkUri = null }
                )
            }
        }
    }

    // AndroidManifest.xml sets launchMode="singleTask" on this Activity, so
    // if the app is already running (foreground or background) when the
    // reset-password link is tapped, Android reuses this same Activity
    // instance and delivers the new intent here instead of restarting it.
    // Without this override, tapping the link while the app was already
    // open would silently do nothing.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SupabaseClient.client.handleDeeplinks(intent)
        pendingDeepLinkUri = intent.data
    }
}