package tech.mmarca.openvitals.features.activity.maps

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/** Short-lived local OAuth session, modeled on Mapbox's official CLI. No remote callback server. */
internal class MapboxOAuthClient : AutoCloseable {
    private val listener = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
    private val redirect = "http://127.0.0.1:${listener.localPort}/callback"
    private val state = MapboxOAuthProtocol.randomValue()
    private val verifier = MapboxOAuthProtocol.randomValue()
    private var clientId = ""
    private var clientSecret: String? = null
    var phase: MapboxLoginPhase = MapboxLoginPhase.CONNECT
        private set

    fun prepare(): String {
        val body = JSONObject().put("client_name", "OpenVitals personal maps")
            .put("redirect_uris", JSONArray().put(redirect))
            .put("grant_types", JSONArray().put("authorization_code"))
            .put("response_types", JSONArray().put("code"))
            .put("token_endpoint_auth_method", "none").put("scope", MapboxOAuthProtocol.SCOPE)
        val registration = JSONObject(request(MapboxOAuthProtocol.REGISTER + "?scope=" +
            MapboxOAuthProtocol.encode(MapboxOAuthProtocol.SCOPE), body.toString(), "application/json"))
        val scopes = registration.optString("scope").split(' ').filter { it.isNotBlank() }.toSet()
        if (scopes.isNotEmpty() && MapboxOAuthProtocol.SCOPE !in scopes) throw MapboxLoginFailure()
        clientId = registration.optString("client_id").takeIf { it.isNotBlank() } ?: throw MapboxLoginFailure()
        clientSecret = registration.optString("client_secret").takeIf { it.isNotBlank() }
        return MapboxOAuthProtocol.authorizationUrl(clientId, redirect, state, verifier)
    }

    fun complete(): String {
        val code = awaitCode()
        phase = MapboxLoginPhase.EXCHANGE
        val params = mutableMapOf("grant_type" to "authorization_code", "code" to code,
            "client_id" to clientId, "redirect_uri" to redirect, "code_verifier" to verifier)
        clientSecret?.let { params["client_secret"] = it }
        val exchange = MapboxOAuthProtocol.form(params)
        val credentials = JSONObject(request(MapboxOAuthProtocol.TOKEN, exchange, "application/x-www-form-urlencoded"))
        val oauth = credentials.optString("access_token").takeIf { it.isNotBlank() } ?: throw MapboxLoginFailure()
        phase = MapboxLoginPhase.ACCOUNT
        val username = credentials.optString("username").takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) }
            ?: runCatching { MapboxOAuthProtocol.username(oauth) }.getOrElse {
                val metadata = JSONObject(request("https://api.mapbox.com/tokens/v2?access_token=" + MapboxOAuthProtocol.encode(oauth)))
                metadata.optJSONObject("token")?.optString("u")?.takeIf { name -> name.matches(Regex("[A-Za-z0-9_-]{1,128}")) }
                    ?: throw MapboxLoginFailure()
            }
        phase = MapboxLoginPhase.PUBLIC_TOKEN
        // Restrict the request to the default PUBLIC token; never fetch secret-token metadata.
        val url = "https://api.mapbox.com/tokens/v2/$username?default=true&usage=pk&access_token=" +
            MapboxOAuthProtocol.encode(oauth)
        return MapboxOAuthProtocol.defaultPublicToken(JSONArray(request(url)))
    }

    private fun awaitCode(): String {
        val deadline = System.nanoTime() + 300_000_000_000L
        listener.soTimeout = 1000
        while (!listener.isClosed && System.nanoTime() < deadline) {
            val socket = try { listener.accept() } catch (_: SocketTimeoutException) { continue }
            socket.use {
                it.soTimeout = 2000
                val requestLine = try { readRequestLine(it.getInputStream()) } catch (_: Exception) { "" }
                val parts = requestLine.split(' ')
                val code = if (parts.size == 3 && parts[0] == "GET") {
                    MapboxOAuthProtocol.callbackCode(parts[1], state)
                } else null
                val status = if (code != null) "200 OK" else "400 Bad Request"
                val message = "OpenVitals: return to the app."
                val response = "HTTP/1.1 $status\r\nContent-Type: text/plain; charset=utf-8\r\n" +
                    "Content-Length: ${message.toByteArray().size}\r\nCache-Control: no-store\r\n" +
                    "Content-Security-Policy: default-src 'none'\r\nConnection: close\r\n\r\n$message"
                runCatching { it.getOutputStream().write(response.toByteArray(Charsets.UTF_8)) }
                if (code != null) return code
            }
        }
        throw MapboxLoginFailure()
    }

    private fun readRequestLine(input: java.io.InputStream): String {
        val bytes = java.io.ByteArrayOutputStream()
        repeat(8192) {
            val next = input.read()
            if (next == -1 || next == 10) return bytes.toString("US-ASCII").trimEnd('\r')
            bytes.write(next)
        }
        throw MapboxLoginFailure()
    }

    private fun request(url: String, body: String? = null, contentType: String = "application/json"): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType)
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            if (connection.responseCode !in 200..299) throw MapboxLoginFailure()
            return connection.inputStream.use {
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (output.size() <= 262_144) {
                    val count = it.read(buffer)
                    if (count == -1) break
                    output.write(buffer, 0, count)
                }
                val bytes = output.toByteArray()
                if (bytes.size > 262_144) throw MapboxLoginFailure()
                String(bytes, Charsets.UTF_8)
            }
        } catch (_: Exception) {
            // Network exceptions may include a URL containing the OAuth token: discard all details.
            throw MapboxLoginFailure()
        } finally {
            connection.disconnect()
        }
    }

    override fun close() { clientSecret = null; listener.close() }
}

internal enum class MapboxLoginPhase { CONNECT, EXCHANGE, ACCOUNT, PUBLIC_TOKEN }
