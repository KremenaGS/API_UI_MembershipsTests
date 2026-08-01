package com.officernd.api;

import com.officernd.utils.AuthHelper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

/**
 * Base class for all API tests. Configures Rest Assured with:
 * - Base URI and path
 * - Authentication header
 * - Request/response logging
 * - Common headers (Content-Type, Accept)
 */
public abstract class BaseApiTest {

    protected static final Properties config = new Properties();
    protected static RequestSpecification requestSpec;

    // Base paths
    protected static String orgSlug;
    protected static String membershipsPath;

    @BeforeAll
    static void globalSetup() {
        loadConfig();
        configureRestAssured();
    }

    @BeforeEach
    void setupAuth() {
        // Refresh auth header before each test to ensure valid token
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(config.getProperty("api.base.url", "https://identity-staging.officernd.com"))
                .setBasePath("/" + orgSlug)
                .addHeader("Authorization", AuthHelper.getAuthorizationHeader())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();
    }

    private static void loadConfig() {
        try (InputStream is = BaseApiTest.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                config.load(is);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }

        orgSlug = config.getProperty("api.org.slug", "kremena-qa-assignment-tasks");
        membershipsPath = "/memberships";
    }

    private static void configureRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Generates a unique identifier for test data to avoid collisions.
     */
    protected String generateUniqueId() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generates a unique membership payload for creation tests.
     */
    protected String createMembershipPayload(String plan, String member, String startDate, String status) {
        return String.format(
            "{"plan":"%s","member":"%s","startDate":"%s","status":"%s"}",
            plan, member, startDate, status
        );
    }

    /**
     * Creates a membership and returns its ID. Used for setup in other tests.
     */
    protected String createTestMembership() {
        String payload = createMembershipPayload(
                "plan_smart_001",
                "member_test_001",
                "2026-08-01",
                "active"
        );

        return io.restassured.RestAssured.given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    /**
     * Cleans up a membership by ID. Should be called in @AfterEach.
     * Accepts 204 (deleted) or 404 (already gone) as success.
     */
    protected void deleteTestMembership(String membershipId) {
        if (membershipId == null) return;
        try {
            int status = io.restassured.RestAssured.given()
                    .spec(requestSpec)
                    .when()
                    .delete(membershipsPath + "/" + membershipId)
                    .getStatusCode();
            if (status != 204 && status != 404) {
                System.err.println("Cleanup returned unexpected status " + status + " for membership " + membershipId);
            }
        } catch (Exception e) {
            System.err.println("Cleanup failed for membership " + membershipId + ": " + e.getMessage());
        }
    }
}
