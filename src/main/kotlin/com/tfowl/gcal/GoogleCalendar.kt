package com.tfowl.gcal

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AbstractPromptReceiver
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.apache.v2.ApacheHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.services.calendar.Calendar
import java.io.Reader

data class GoogleApiServiceConfig(
    val secretsProvider: () -> Reader,
    val applicationName: String,
    val scopes: List<String>,
    val headless: Boolean = false,
    val dataStoreFactory: DataStoreFactory,
    val httpTransport: HttpTransport = ApacheHttpTransport(),
    val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance(),
)

object GoogleCalendar {
    private fun getCredentials(
        config: GoogleApiServiceConfig,
    ): Credential {
        val input = config.secretsProvider()
        val secrets = GoogleClientSecrets.load(config.jsonFactory, input)

        val flow = GoogleAuthorizationCodeFlow.Builder(config.httpTransport, config.jsonFactory, secrets, config.scopes)
            .setDataStoreFactory(config.dataStoreFactory).setAccessType("offline").build()

        val receiver = if (config.headless) {

            object : AbstractPromptReceiver() {
                override fun getRedirectUri(): String {
                    return "redirect_uri=urn:ietf:wg:oauth:2.0:oob"
                }
            }
        } else {
            LocalServerReceiver.Builder().setPort(8888).build()
        }

        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    fun create(
        config: GoogleApiServiceConfig,
    ): Calendar {
        val credentials = getCredentials(config)

        return Calendar.Builder(config.httpTransport, config.jsonFactory, credentials)
            .setApplicationName(config.applicationName).build()
    }
}
