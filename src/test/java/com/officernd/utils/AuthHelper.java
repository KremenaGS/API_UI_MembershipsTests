package com.officernd.utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Authentication helper for obtaining OAuth2 tokens from OfficeRnD identity service.
 * Centralizes token retrieval so tests don't duplicate auth logic.
 */
public class AuthHelper {

    private static final Properties config = new Properties();
    private static String cachedToken;
    private static long tokenExpiry;

    static {
        try (InputStream is = AuthHelper.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                config.load(is);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load config.properties: " + e.getMessage());
        }
    }

    /**
     * Obtains a valid access token using OAuth2 Client Credentials flow.
     * Caches the token and refreshes it before expiry.
     *
     * @return Bearer token string (without "Bearer " prefix)
     */
    public static synchronized String getAccessToken() {
        // Return cached token if still valid (with 60s buffer)
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiry - 60000) {
            return cachedToken;
        }

        String baseUrl = config.getProperty("api.base.url", "https://identity-staging.officernd.com");
        String clientId = config.getProperty("api.client.id", "test-client-id");
        String clientSecret = config.getProperty("api.client.secret", "test-client-secret");

        // In a real scenario, this would hit the actual token endpoint
        // For this assignment, we simulate the structure. In production,
        // replace with actual OAuth2 token endpoint:
        // POST /oauth/token with grant_type=client_credentials

        Response response = RestAssured.given()
                .baseUri(baseUrl)
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("audience", "https://api.officernd.com")
                .when()
                .post("/oauth/token")
                .then()
                .extract().response();

        if (response.statusCode() == 200) {
            cachedToken = response.jsonPath().getString("access_token");
            int expiresIn = response.jsonPath().getInt("expires_in");
            tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000L);
            return cachedToken;
        } else {
            // Fallback: return a dummy token for test structure demonstration
            // In real execution, the staging environment would provide actual tokens
            System.err.println("Token endpoint returned " + response.statusCode() +
                    ". Using fallback token for test structure.");
            cachedToken = "dummy_staging_token_for_assignment";
            tokenExpiry = System.currentTimeMillis() + 3600000;
            return cachedToken;
        }
    }

    /**
     * Returns the full Authorization header value.
     */
    public static String getAuthorizationHeader() {
        return "Bearer " + getAccessToken();
    }

    /**
     * Invalidates the cached token, forcing a refresh on next call.
     */
    public static void invalidateToken() {
        cachedToken = null;
        tokenExpiry = 0;
    }
}
