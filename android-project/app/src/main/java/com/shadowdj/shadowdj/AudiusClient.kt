package com.shadowdj.shadowdj

import android.util.Base64
import org.json.JSONArray
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

    fun getOneStreamUrl(): String? {

        return try {

            val apiKey = getApiKey()

            if (apiKey.isBlank()) {
                return null
            }

            val encodedKey =
                URLEncoder.encode(
                    apiKey,
                    "UTF-8"
                )

            val url =
                "https://api.audius.co/v1/tracks/trending" +
                "?limit=20" +
                "&api_key=" +
                encodedKey

            val connection =
                URL(url)
                    .openConnection()
                    as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 15000

            val code = connection.responseCode

            if (code !in 200..299) {
                connection.disconnect()
                return null
            }

            val response =
                connection
                    .inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            connection.disconnect()

            val root =
                JSONObject(response)

            val data =
                root.optJSONArray(
                    "data"
                ) ?: JSONArray()

            for (i in 0 until data.length()) {

                val track =
                    data.optJSONObject(i)
                        ?: continue

                val id =
                    track.optString("id")

                val streamableValue =
    track.opt("isStreamable")

val streamable =
    when (streamableValue) {
        is Boolean -> streamableValue
        is String -> streamableValue.equals(
            "true",
            ignoreCase = true
        )
        else -> false
    }
                    

                if (
                    id.isBlank() ||
                    !streamable
                ) {
                    continue
                }

                return "https://api.audius.co/v1/tracks/" +
                    id +
                    "/stream?api_key=" +
                    encodedKey
            }

                        null

        } catch (e: Exception) {

            android.util.Log.e(
                "SHADOW_AUDIUS",
                "Audius error: ${e.message}",
                e
            )

            null
        }
  
   fun getDiagnostic(): String {

    return try {

        val apiKey = getApiKey()

        if (apiKey.isBlank()) {
            return "API KEY EMPTY"
        }

        val encodedKey =
            URLEncoder.encode(
                apiKey,
                "UTF-8"
            )

        val url =
            "https://api.audius.co/v1/tracks/trending" +
            "?limit=20" +
            "&api_key=" +
            encodedKey

        val connection =
            URL(url)
                .openConnection()
                as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 15000

        val code =
            connection.responseCode

        if (code !in 200..299) {
            connection.disconnect()
            return "HTTP ERROR $code"
        }

        val response =
            connection
                .inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }

        connection.disconnect()

        val root =
            JSONObject(response)

        val data =
            root.optJSONArray("data")
                ?: return "HTTP $code • NO DATA"

        if (data.length() == 0) {
            return "HTTP $code • TRACKS 0"
        }

        for (i in 0 until data.length()) {

            val track =
                data.optJSONObject(i)
                    ?: continue

            val id =
                track.optString("id")

            val streamableValue =
                track.opt("isStreamable")

            val streamable =
                when (streamableValue) {
                    is Boolean -> streamableValue
                    is String ->
                        streamableValue.equals(
                            "true",
                            ignoreCase = true
                        )
                    else -> false
                }

            if (
                id.isNotBlank() &&
                streamable
            ) {
                return "HTTP $code • TRACKS ${data.length()} • PLAYABLE FOUND"
            }
        }

        return "HTTP $code • TRACKS ${data.length()} • NO PLAYABLE TRACKS"

    } catch (e: Exception) {

        return "ERROR • ${e.javaClass.simpleName}: ${e.message ?: "unknown"}"
    }
   }
    }
}
