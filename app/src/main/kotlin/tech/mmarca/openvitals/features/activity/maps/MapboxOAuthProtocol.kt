package tech.mmarca.openvitals.features.activity.maps

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/** Pure protocol helpers. Never format credentials into exceptions or log messages. */
internal object MapboxOAuthProtocol {
    const val SCOPE = "tokens:read"
    const val REGISTER = "https://api.mapbox.com/oauth/register"
    const val AUTHORIZE = "https://api.mapbox.com/oauth/2.1/authorize"
    const val TOKEN = "https://api.mapbox.com/oauth/2.1/token"

    fun randomValue(): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })

    fun challenge(verifier: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    fun form(values: Map<String, String>): String = values.entries.joinToString("&") { (k, v) ->
        "${encode(k)}=${encode(v)}"
    }

    fun authorizationUrl(client: String, redirect: String, state: String, verifier: String): String =
        AUTHORIZE + "?" + form(mapOf("client_id" to client, "redirect_uri" to redirect,
            "response_type" to "code", "scope" to SCOPE, "state" to state,
            "code_challenge" to challenge(verifier), "code_challenge_method" to "S256"))

    fun callbackCode(target: String, expectedState: String): String? {
        val uri = runCatching { URI(target) }.getOrNull() ?: return null
        if (uri.isAbsolute || uri.rawAuthority != null || uri.path != "/callback" || uri.fragment != null) return null
        val pairs = runCatching {
            uri.rawQuery.orEmpty().split("&").map { entry ->
                val pair = entry.split("=", limit = 2)
                URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
            }
        }.getOrNull() ?: return null
        if (pairs.map { it.first }.distinct().size != pairs.size) return null
        val params = pairs.toMap()
        if (params["state"] != expectedState) return null
        if (params.containsKey("error")) throw MapboxLoginFailure()
        return params["code"]?.takeIf { it.isNotBlank() && it.length <= 4096 }
    }

    fun username(oauthToken: String): String {
        val payload = oauthToken.split('.').getOrNull(1) ?: throw MapboxLoginFailure()
        val name = JSONObject(String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)).optString("u")
        if (!name.matches(Regex("[A-Za-z0-9_-]{1,128}"))) throw MapboxLoginFailure()
        return name
    }

    fun defaultPublicToken(response: JSONArray): String {
        for (index in 0 until response.length()) {
            val item = response.getJSONObject(index)
            val scopes = item.optJSONArray("scopes") ?: continue
            val allowed = item.optJSONArray("allowedUrls")
            val names = (0 until scopes.length()).map { scopes.getString(it) }.toSet()
            val token = item.optString("token")
            if (item.optString("usage", "pk") == "pk" && item.optBoolean("default", true) &&
                (allowed == null || allowed.length() == 0) &&
                names.containsAll(setOf("styles:read", "fonts:read")) && isPublicToken(token)
            ) return token
        }
        throw MapboxLoginFailure()
    }

    fun isPublicToken(value: String): Boolean = value.matches(Regex("pk\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"))
}

internal class MapboxLoginFailure : Exception("Mapbox sign-in could not be completed")
