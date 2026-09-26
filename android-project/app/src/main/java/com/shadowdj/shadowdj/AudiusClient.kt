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

            val encoded =
                BuildConfig.AUDIUS_API_KEY_B64

            if (encoded.isBlank()) {
                return ""
            }

            val bytes =
                Base64.decode(
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

    private fun getAudiusGenre(
        genre: String
    ): String {

        return when (genre.trim()) {

            "Techno" ->
                "Techno"

            "House" ->
                "House"

            "EDM" ->
                "Electronic"

            "Rock" ->
                "Rock"

            "R&B / Hip-Hop" ->
                "R&B"

            "Country" ->
                "Country"

            else ->
                genre
        }
    }

    private fun getTrendingResponse(
        genre: String = "ALL"
    ): String? {

        return try {

            val apiKey =
                getApiKey()

            if (apiKey.isBlank()) {
                return null
            }

            val encodedKey =
                URLEncoder.encode(
                    apiKey,
                    "UTF-8"
                )

            var url =
                "https://api.audius.co/v1/tracks/trending" +
                "?limit=50" +
                "&api_key=" +
                encodedKey

            if (
                genre.isNotBlank() &&
                genre != "ALL"
            ) {

                val audiusGenre =
                    getAudiusGenre(genre)

                val encodedGenre =
                    URLEncoder.encode(
                        audiusGenre,
                        "UTF-8"
                    )

                url +=
                    "&genre=" +
                    encodedGenre
            }

            val connection =
                URL(url)
                    .openConnection() as HttpURLConnection

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                10000

            connection.readTimeout =
                15000

            val code =
                connection.responseCode

            if (code !in 200..299) {
                connection.disconnect()
                return null
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            connection.disconnect()

            response

        } catch (e: Exception) {

            android.util.Log.e(
                "SHADOW_AUDIUS",
                "Trending error: ${e.message}",
                e
            )

            null
        }
    }

    fun testConnection(): Boolean {

        return try {

            val response =
                getTrendingResponse()
                    ?: return false

            val root =
                JSONObject(response)

            root.has("data")

        } catch (_: Exception) {
            false
        }
    }

    fun getOneStreamUrl(): String? {
        return getNextStreamUrl("Techno")
    }

    fun getNextStreamUrl(
        genre: String = "Techno"
    ): String? {

        return try {

            val apiKey =
                getApiKey()

            if (apiKey.isBlank()) {
                return null
            }

            val response =
                getTrendingResponse(genre)
                    ?: return null

            val root =
                JSONObject(response)

            val data =
                root.optJSONArray("data")
                    ?: JSONArray()

            val encodedKey =
                URLEncoder.encode(
                    apiKey,
                    "UTF-8"
                )

            val indexes =
                (0 until data.length())
                    .toMutableList()

            indexes.shuffle()

            for (i in indexes) {

                val track =
                    data.optJSONObject(i)
                        ?: continue

                val id =
                    track.optString("id")

                if (id.isBlank()) {
                    continue
                }

                val streamUrl =
                    "https://api.audius.co/v1/tracks/" +
                    id +
                    "/stream?api_key=" +
                    encodedKey

                if (testStreamUrl(streamUrl)) {
                    return streamUrl
                }
            }

            null

        } catch (e: Exception) {

            android.util.Log.e(
                "SHADOW_AUDIUS",
                "Next track error: ${e.message}",
                e
            )

            null
        }
    }

    private fun testStreamUrl(
        streamUrl: String
    ): Boolean {

        return try {

            val connection =
                URL(streamUrl)
                    .openConnection() as HttpURLConnection

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                10000

            connection.readTimeout =
                10000

            connection.instanceFollowRedirects =
                true

            val code =
                connection.responseCode

            connection.disconnect()

            code in 200..299 ||
                code == HttpURLConnection.HTTP_PARTIAL

        } catch (_: Exception) {
            false
        }
    }

    fun getDiagnostic(): String {

        return try {

            val apiKey =
                getApiKey()

            if (apiKey.isBlank()) {
                return "API KEY EMPTY"
            }

            val response =
                getTrendingResponse()
                    ?: return "TRENDING REQUEST FAILED"

            val root =
                JSONObject(response)

            val data =
                root.optJSONArray("data")
                    ?: return "HTTP 200 • NO DATA"

            if (data.length() == 0) {
                return "HTTP 200 • TRACKS 0"
            }

            var tracksChecked = 0

            for (i in 0 until data.length()) {

                val track =
                    data.optJSONObject(i)
                        ?: continue

                val id =
                    track.optString("id")

                if (id.isBlank()) {
                    continue
                }

                tracksChecked++

                val encodedKey =
                    URLEncoder.encode(
                        apiKey,
                        "UTF-8"
                    )

                val streamUrl =
                    "https://api.audius.co/v1/tracks/" +
                    id +
                    "/stream?api_key=" +
                    encodedKey

                if (testStreamUrl(streamUrl)) {

                    val title =
                        track.optString(
                            "title",
                            "Unknown Track"
                        )

                    return "HTTP 200 • TRACKS ${data.length()} • PLAYABLE FOUND • $title"
                }
            }

            "HTTP 200 • TRACKS ${data.length()} • STREAM TEST FAILED ($tracksChecked CHECKED)"

        } catch (e: Exception) {

            "ERROR • ${e.javaClass.simpleName}: ${e.message ?: "unknown"}"
        }
    }
}
