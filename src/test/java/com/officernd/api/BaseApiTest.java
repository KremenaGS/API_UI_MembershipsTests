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

public abstract class BaseApiTest {

    protected static final Properties config = new Properties();
    protected static RequestSpecification requestSpec;
    protected static String orgSlug;
    protected static String membershipsPath;

    @BeforeAll
    static void globalSetup() {
        loadConfig();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void setupAuth() {
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
        try (InputStream is = BaseApiTest.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) config.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
        orgSlug = config.getProperty("api.org.slug", "kremena-qa-assignment-tasks");
        membershipsPath = "/memberships";
    }

    protected String generateUniqueId() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String buildJson(String... keyValues) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append('"').append(keyValues[i]).append('"');
            sb.append(':');
            sb.append('"').append(keyValues[i + 1]).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    protected String createMembershipPayload(String plan, String member, String startDate, String status) {
        return buildJson("plan", plan, "member", member, "startDate", startDate, "status", status);
    }

    protected String createTestMembership() {
        String payload = createMembershipPayload("plan_smart_001", "member_test_001", "2026-08-01", "active");
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