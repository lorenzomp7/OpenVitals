package tech.mmarca.openvitals.features.activity.maps

import java.net.URI
import java.net.URLDecoder
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class MapboxOAuthProtocolTest {
    @Test fun pkceMatchesTheRfc7636Vector() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            MapboxOAuthProtocol.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
        assertNotEquals(MapboxOAuthProtocol.randomValue(), MapboxOAuthProtocol.randomValue())
    }

    @Test fun authorizationUsesPkceAndOnlyTokenReadScope() {
        val url = URI(MapboxOAuthProtocol.authorizationUrl("client", "http://127.0.0.1:1234/callback", "state", "verifier"))
        val query = URLDecoder.decode(url.rawQuery, "UTF-8")
        assertEquals("api.mapbox.com", url.host)
        assertTrue(query.contains("scope=tokens:read"))
        assertTrue(query.contains("code_challenge_method=S256"))
        assertFalse(query.contains("code_verifier"))
    }

    @Test fun callbackRejectsCsrfDuplicateParametersAndOtherPaths() {
        for (target in listOf("/callback?code=x&state=wrong", "/other?code=x&state=ok",
            "/callback?code=x&state=ok&state=ok", "/callback?code=x&code=y&state=ok",
            "https://evil.example/callback?code=x&state=ok", "/callback?code=x&state=ok#fragment",
            "/callback?code=x&state=%ZZ")) {
            assertNull(target, MapboxOAuthProtocol.callbackCode(target, "ok"))
        }
        assertEquals("one+two", MapboxOAuthProtocol.callbackCode("/callback?code=one%2Btwo&state=ok", "ok"))
    }

    @Test(expected = MapboxLoginFailure::class)
    fun denialFailsWithoutReflectingProviderDetails() {
        MapboxOAuthProtocol.callbackCode("/callback?error=access_denied&state=ok", "ok")
    }

    @Test fun selectsOnlyAnUnrestrictedDefaultPublicMapToken() {
        val valid = """{"usage":"pk","default":true,"scopes":["styles:read","styles:tiles","fonts:read"],"token":"pk.fixture.signature"}"""
        assertEquals("pk.fixture.signature", MapboxOAuthProtocol.defaultPublicToken(JSONArray("[$valid]")))
        for (bad in listOf(valid.replace("\"pk\"", "\"sk\""), valid.replace("true", "false"),
            valid.replace("styles:tiles", "other:read"), valid.replace("pk.fixture", "sk.fixture"),
            valid.replace("\"default\":true", "\"allowedUrls\":[\"https://example.com\"],\"default\":true"))) {
            assertThrows(MapboxLoginFailure::class.java) { MapboxOAuthProtocol.defaultPublicToken(JSONArray("[$bad]")) }
        }
    }

    @Test fun usernameCannotChangeTheApiPath() {
        val payload = java.util.Base64.getUrlEncoder().withoutPadding()
        fun token(user: String) = "header." + payload.encodeToString("{\"u\":\"$user\"}".toByteArray()) + ".signature"
        assertEquals("example-user", MapboxOAuthProtocol.username(token("example-user")))
        assertThrows(MapboxLoginFailure::class.java) { MapboxOAuthProtocol.username(token("../other")) }
    }
}
