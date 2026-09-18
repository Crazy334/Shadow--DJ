package com.shadowdj.shadowdj

import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AudiusClient {

    private fun getApiKey(): String {
        return try {
            val encoded = BuildConfig.AUDIUS_API_KEY_B64

            if (encoded.isBlank()) {
                return ""
            }

            val bytes = Base64.decode(
                encoded,
                Base64.DEFAULT
            )

            String(
                bytes,
                Charsets.UTF_8
            )

        } catch (_: Exception) {
            ""
        }
    }

    fun testConnection(): Boolean {

        return try {

            val apiKey = getApiKey()

            if (apiKey.isBlank()) {
                return false
            }

            val url =
                "https://api.audius.co/v1/tracks/trending" +
                "?limit=1" +
                "&api_key=" +
                URLEncoder.encode(
                    apiKey,
                    "UTF-8"
                )

            val connection =
                URL(url)
                    .openConnection()
                    as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val code = connection.responseCode

            connection.disconnect()

            code in 200..299

        } catch (_: Exception) {

            false
        }
    }
}
