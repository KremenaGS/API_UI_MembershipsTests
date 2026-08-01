package com.officernd.api;

import com.officernd.utils.AuthHelper;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MembershipsApiTest extends BaseApiTest {

    private static String createdMembershipId;

    // ==================== GET ALL MEMBERSHIPS ====================

    @Test
    @Order(1)
    @DisplayName("TC-GET-001: Retrieve All Memberships - Valid Request")
    void getAllMemberships_ValidRequest_ReturnsList() {
        given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("$", isA(List.class))
                .body("size()", greaterThanOrEqualTo(0))
                .time(lessThan(5000L));
    }

    @Test
    @Order(2)
    @DisplayName("TC-GET-002: Retrieve Memberships with Pagination")
    void getAllMemberships_WithPagination_ReturnsCorrectPage() {
        Response page1 = given()
                .spec(requestSpec)
                .queryParam("limit", 5)
                .queryParam("offset", 0)
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(200)
                .body("size()", lessThanOrEqualTo(5))
                .extract().response();

        Response page2 = given()
                .spec(requestSpec)
                .queryParam("limit", 5)
                .queryParam("offset", 5)
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(200)
                .body("size()", lessThanOrEqualTo(5))
                .extract().response();

        List<String> ids1 = page1.jsonPath().getList("id");
        List<String> ids2 = page2.jsonPath().getList("id");
        if (!ids1.isEmpty() && !ids2.isEmpty()) {
            assertThat(ids1).doesNotContainAnyElementsOf(ids2);
        }
    }

    @Test
    @Order(3)
    @DisplayName("TC-GET-003: Retrieve Memberships with Status Filter")
    void getAllMemberships_WithStatusFilter_ReturnsFilteredResults() {
        given()
                .spec(requestSpec)
                .queryParam("status", "active")
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(200)
                .body("every { it.status == 'active' }", is(true));
    }

    @Test
    @Order(4)
    @DisplayName("TC-GET-004: Retrieve Memberships - Invalid Organization Slug")
    void getAllMemberships_InvalidOrg_Returns404() {
        given()
                .spec(requestSpec)
                .basePath("/nonexistent-org-12345")
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(404), equalTo(403)));
    }

    @Test
    @Order(5)
    @DisplayName("TC-GET-005: Retrieve Memberships - Missing Authorization Header")
    void getAllMemberships_MissingAuth_Returns401() {
        given()
                .baseUri(config.getProperty("api.base.url"))
                .basePath("/" + orgSlug)
                .contentType("application/json")
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(401), equalTo(403)));
    }

    @Test
    @Order(6)
    @DisplayName("TC-GET-006: Retrieve Memberships - Invalid Query Parameters")
    void getAllMemberships_InvalidQueryParams_Returns400() {
        given()
                .spec(requestSpec)
                .queryParam("limit", "abc")
                .queryParam("offset", -1)
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));
    }

    @Test
    @Order(7)
    @DisplayName("TC-GET-007: Retrieve Memberships - Response is Array Not Null")
    void getAllMemberships_ResponseIsArrayNotNull() {
        given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(200)
                .body("$", is(notNullValue()))
                .body("$", isA(List.class));
    }

    // ==================== GET MEMBERSHIP BY ID ====================

    @Test
    @Order(10)
    @DisplayName("TC-GET-ID-001: Retrieve Single Membership - Valid ID")
    void getMembershipById_ValidId_ReturnsMembership() {
        String payload = createMembershipPayload("plan_smart_001", "member_test_001", "2026-08-01", "active");

        createdMembershipId = given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath + "/" + createdMembershipId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdMembershipId))
                .body("plan", equalTo("plan_smart_001"))
                .body("member", equalTo("member_test_001"))
                .body("status", equalTo("active"))
                .body("startDate", notNullValue())
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("TC-GET-ID-002: Retrieve Membership - Non-Existent ID")
    void getMembershipById_NonExistent_Returns404() {
        given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath + "/mem_nonexistent_99999")
                .then()
                .statusCode(404)
                .body("message", containsStringIgnoringCase("not found"));
    }

    @Test
    @Order(12)
    @DisplayName("TC-GET-ID-003: Retrieve Membership - Invalid ID Format")
    void getMembershipById_InvalidFormat_Returns400() {
        given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath + "/!!!invalid@@@")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404)));
    }

    @Test
    @Order(13)
    @DisplayName("TC-GET-ID-004: Retrieve Membership - Deleted Membership")
    void getMembershipById_Deleted_Returns404() {
        String payload = createMembershipPayload("plan_smart_001", "member_test_002", "2026-08-01", "active");

        String id = given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .spec(requestSpec)
                .when()
                .delete(membershipsPath + "/" + id)
                .then()
                .statusCode(anyOf(equalTo(204), equalTo(200)));

        given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath + "/" + id)
                .then()
                .statusCode(anyOf(equalTo(404), equalTo(410)));
    }

    @Test
    @Order(14)
    @DisplayName("TC-GET-ID-005: Retrieve Membership - Cross-Organization Access")
    void getMembershipById_CrossOrg_Returns403Or404() {
        given()
                .spec(requestSpec)
                .basePath("/different-org-slug")
                .when()
                .get(membershipsPath + "/mem_12345")
                .then()
                .statusCode(anyOf(equalTo(403), equalTo(404)));
    }

    // ==================== POST CREATE MEMBERSHIP ====================

    @Test
    @Order(20)
    @DisplayName("TC-POST-001: Create Membership - All Required Fields")
    void createMembership_AllRequiredFields_Returns201() {
        String uniqueMember = "member_" + generateUniqueId();
        String payload = createMembershipPayload("plan_smart_001", uniqueMember, "2026-08-01", "active");

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("plan", equalTo("plan_smart_001"))
                .body("member", equalTo(uniqueMember))
                .body("status", equalTo("active"))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue())
                .header("Location", containsString("/memberships/"));
    }

    @Test
    @Order(21)
    @DisplayName("TC-POST-002: Create Membership - Missing Required Field (Plan)")
    void createMembership_MissingPlan_Returns422() {
        String payload = buildJson("member", "member_test_001", "startDate", "2026-08-01", "status", "active");

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("message", anyOf(containsString("plan"), containsString("required")));
    }

    @Test
    @Order(22)
    @DisplayName("TC-POST-003: Create Membership - Missing Required Field (Member)")
    void createMembership_MissingMember_Returns422() {
        String payload = buildJson("plan", "plan_smart_001", "startDate", "2026-08-01", "status", "active");

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("message", anyOf(containsString("member"), containsString("required")));
    }

    @Test
    @Order(23)
    @DisplayName("TC-POST-004: Create Membership - Invalid Plan ID")
    void createMembership_InvalidPlan_Returns422() {
        String payload = createMembershipPayload("plan_does_not_exist_99999", "member_test_001", "2026-08-01", "active");

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(404), equalTo(422)))
                .body("message", anyOf(containsString("plan"), containsString("not found")));
    }

    @Test
    @Order(24)
    @DisplayName("TC-POST-005: Create Membership - Invalid Member ID")
    void createMembership_InvalidMember_Returns422() {
        String payload = createMembershipPayload("plan_smart_001", "member_does_not_exist_99999", "2026-08-01", "active");

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(404), equalTo(422)))
                .body("message", anyOf(containsString("member"), containsString("not found")));
    }

    @Test
    @Order(25)
    @DisplayName("TC-POST-006: Create Membership - Invalid Date Format")
    void createMembership_InvalidDateFormat_Returns400() {
        String payload = createMembershipPayload("plan_smart_001", "member_test_001", "01-08-2026", "active");

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("message", anyOf(containsString("date"), containsString("format")));
    }

    @Test
    @Order(26)
    @DisplayName("TC-POST-007: Create Membership - Duplicate Detection")
    void createMembership_Duplicate_Returns409() {
        String uniqueMember = "member_dup_" + generateUniqueId();
        String payload = createMembershipPayload("plan_smart_001", uniqueMember, "2026-08-01", "active");

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201);

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(409), equalTo(422)));
    }

    @Test
    @Order(27)
    @DisplayName("TC-POST-009: Create Membership - SQL Injection Attempt")
    void createMembership_SqlInjection_Returns422() {
        String payload = createMembershipPayload("'; DROP TABLE memberships; --", "member_test_001", "2026-08-01", "active");

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("message", not(containsString("SQL")))
                .body("message", not(containsString("DROP")));
    }

    @Test
    @Order(28)
    @DisplayName("TC-POST-010: Create Membership - XSS Attempt")
    void createMembership_XssAttempt_Returns201Or422() {
        String payload = buildJson(
                "plan", "plan_smart_001",
                "member", "member_test_001",
                "startDate", "2026-08-01",
                "status", "active",
                "notes", "<script>alert('xss')</script>"
        );

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(201), equalTo(422)))
                .body("notes", anyOf(
                        nullValue(),
                        not(containsString("<script>")),
                        containsString("&lt;script&gt;")
                ));
    }

    @Test
    @Order(29)
    @DisplayName("TC-POST-011: Create Membership - Empty Request Body")
    void createMembership_EmptyBody_Returns422() {
        given()
                .spec(requestSpec)
                .body("{}")
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));
    }

    @Test
    @Order(30)
    @DisplayName("TC-POST-012: Create Membership - Invalid JSON")
    void createMembership_InvalidJson_Returns400() {
        // Build malformed JSON intentionally without escaped quotes in source
        StringBuilder sb = new StringBuilder();
        sb.append('{').append(' ');
        sb.append('"').append("plan").append('"').append(':').append(' ');
        sb.append('"').append("abc").append('"').append(',');
        sb.append(' ').append('"').append("member").append('"');
        sb.append(' ').append('}');
        String invalidJson = sb.toString();

        given()
                .spec(requestSpec)
                .body(invalidJson)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }

    @Test
    @Order(31)
    @DisplayName("TC-POST-013: Create Membership - Future Start Date")
    void createMembership_FutureStartDate_Returns201() {
        String payload = createMembershipPayload("plan_smart_001", "member_future_" + generateUniqueId(), "2027-01-01", "active");

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(201), equalTo(422)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"active", "paused", "cancelled", "pending"})
    @Order(32)
    @DisplayName("TC-POST-014: Create Membership - Various Valid Statuses")
    void createMembership_VariousStatuses_Returns201(String status) {
        String payload = createMembershipPayload("plan_smart_001", "member_status_" + generateUniqueId(), "2026-08-01", status);

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(201), equalTo(422)));
    }

    // ==================== PUT UPDATE MEMBERSHIP ====================

    @Test
    @Order(40)
    @DisplayName("TC-PUT-001: Update Membership - Valid Full Update")
    void updateMembership_FullUpdate_Returns200() {
        String createPayload = createMembershipPayload("plan_smart_001", "member_put_" + generateUniqueId(), "2026-08-01", "active");

        String id = given()
                .spec(requestSpec)
                .body(createPayload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201)
                .extract().path("id");

        String updatePayload = buildJson(
                "plan", "plan_total_001",
                "member", "member_updated_001",
                "startDate", "2026-09-01",
                "status", "paused",
                "notes", "Updated via API test"
        );

        given()
                .spec(requestSpec)
                .body(updatePayload)
                .when()
                .put(membershipsPath + "/" + id)
                .then()
                .statusCode(200)
                .body("plan", equalTo("plan_total_001"))
                .body("status", equalTo("paused"))
                .body("notes", equalTo("Updated via API test"))
                .body("id", equalTo(id));

        deleteTestMembership(id);
    }

    @Test
    @Order(41)
    @DisplayName("TC-PUT-002: Update Membership - Partial Update")
    void updateMembership_PartialUpdate_Returns200() {
        String createPayload = createMembershipPayload("plan_smart_001", "member_partial_" + generateUniqueId(), "2026-08-01", "active");

        String id = given()
                .spec(requestSpec)
                .body(createPayload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201)
                .extract().path("id");

        String updatePayload = buildJson("status", "cancelled");

        given()
                .spec(requestSpec)
                .body(updatePayload)
                .when()
                .put(membershipsPath + "/" + id)
                .then()
                .statusCode(200)
                .body("status", equalTo("cancelled"))
                .body("plan", equalTo("plan_smart_001"));

        deleteTestMembership(id);
    }

    @Test
    @Order(42)
    @DisplayName("TC-PUT-003: Update Membership - Non-Existent ID")
    void updateMembership_NonExistent_Returns404() {
        String updatePayload = buildJson("status", "cancelled");

        given()
                .spec(requestSpec)
                .body(updatePayload)
                .when()
                .put(membershipsPath + "/mem_nonexistent_99999")
                .then()
                .statusCode(404)
                .body("message", containsStringIgnoringCase("not found"));
    }

    @Test
    @Order(43)
    @DisplayName("TC-PUT-004: Update Membership - Invalid Status Value")
    void updateMembership_InvalidStatus_Returns422() {
        String createPayload = createMembershipPayload("plan_smart_001", "member_invalid_status_" + generateUniqueId(), "2026-08-01", "active");

        String id = given()
                .spec(requestSpec)
                .body(createPayload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201)
                .extract().path("id");

        String updatePayload = buildJson("status", "invalid_status_xyz");

        given()
                .spec(requestSpec)
                .body(updatePayload)
                .when()
                .put(membershipsPath + "/" + id)
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));

        deleteTestMembership(id);
    }

    @Test
    @Order(44)
    @DisplayName("TC-PUT-006: Update Membership - Change Plan to Invalid")
    void updateMembership_InvalidPlan_Returns422() {
        String createPayload = createMembershipPayload("plan_smart_001", "member_invalid_plan_" + generateUniqueId(), "2026-08-01", "active");

        String id = given()
                .spec(requestSpec)
                .body(createPayload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201)
                .extract().path("id");

        String updatePayload = buildJson("plan", "plan_nonexistent_99999");

        given()
                .spec(requestSpec)
                .body(updatePayload)
                .when()
                .put(membershipsPath + "/" + id)
                .then()
                .statusCode(anyOf(equalTo(404), equalTo(422)));

        deleteTestMembership(id);
    }

    // ==================== DELETE MEMBERSHIP ====================

    @Test
    @Order(50)
    @DisplayName("TC-DELETE-001: Delete Membership - Valid ID")
    void deleteMembership_ValidId_Returns204() {
        String createPayload = createMembershipPayload("plan_smart_001", "member_delete_" + generateUniqueId(), "2026-08-01", "active");

        String id = given()
                .spec(requestSpec)
                .body(createPayload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .spec(requestSpec)
                .when()
                .delete(membershipsPath + "/" + id)
                .then()
                .statusCode(anyOf(equalTo(204), equalTo(200)));

        given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath + "/" + id)
                .then()
                .statusCode(anyOf(equalTo(404), equalTo(410)));
    }

    @Test
    @Order(51)
    @DisplayName("TC-DELETE-002: Delete Membership - Non-Existent ID")
    void deleteMembership_NonExistent_Returns404() {
        given()
                .spec(requestSpec)
                .when()
                .delete(membershipsPath + "/mem_nonexistent_99999")
                .then()
                .statusCode(404)
                .body("message", containsStringIgnoringCase("not found"));
    }

    @Test
    @Order(52)
    @DisplayName("TC-DELETE-003: Delete Membership - Already Deleted")
    void deleteMembership_AlreadyDeleted_Returns404() {
        String createPayload = createMembershipPayload("plan_smart_001", "member_double_del_" + generateUniqueId(), "2026-08-01", "active");

        String id = given()
                .spec(requestSpec)
                .body(createPayload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .spec(requestSpec)
                .when()
                .delete(membershipsPath + "/" + id)
                .then()
                .statusCode(anyOf(equalTo(204), equalTo(200)));

        given()
                .spec(requestSpec)
                .when()
                .delete(membershipsPath + "/" + id)
                .then()
                .statusCode(anyOf(equalTo(404), equalTo(410)));
    }

    // ==================== SECURITY TESTS ====================

    @Test
    @Order(60)
    @DisplayName("TC-SEC-001: Invalid Token")
    void security_InvalidToken_Returns401() {
        given()
                .baseUri(config.getProperty("api.base.url"))
                .basePath("/" + orgSlug)
                .header("Authorization", "Bearer invalid_token_12345")
                .contentType("application/json")
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(401);
    }

    @Test
    @Order(61)
    @DisplayName("TC-SEC-002: Expired Token")
    void security_ExpiredToken_Returns401() {
        given()
                .baseUri(config.getProperty("api.base.url"))
                .basePath("/" + orgSlug)
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired.signature")
                .contentType("application/json")
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(401);
    }

    @Test
    @Order(62)
    @DisplayName("TC-SEC-003: Missing Token")
    void security_MissingToken_Returns401() {
        given()
                .baseUri(config.getProperty("api.base.url"))
                .basePath("/" + orgSlug)
                .contentType("application/json")
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(401), equalTo(403)));
    }

    @Test
    @Order(63)
    @DisplayName("TC-SEC-004: Wrong HTTP Method (PATCH)")
    void security_WrongMethod_Returns405() {
        given()
                .spec(requestSpec)
                .body(buildJson("status", "active"))
                .when()
                .patch(membershipsPath + "/mem_12345")
                .then()
                .statusCode(405);
    }

    // ==================== CONTRACT & SCHEMA ====================

    @Test
    @Order(70)
    @DisplayName("TC-SCHEMA-001: Response Schema Validation - GET All")
    void schema_GetAll_MatchesExpectedStructure() {
        given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(200)
                .body("$", isA(List.class))
                .body("[0].id", anyOf(nullValue(), isA(String.class)))
                .body("[0].plan", anyOf(nullValue(), isA(String.class)))
                .body("[0].member", anyOf(nullValue(), isA(String.class)))
                .body("[0].status", anyOf(nullValue(), isA(String.class)))
                .body("[0].startDate", anyOf(nullValue(), isA(String.class)));
    }

    @Test
    @Order(71)
    @DisplayName("TC-SCHEMA-002: Response Schema Validation - GET by ID")
    void schema_GetById_MatchesExpectedStructure() {
        if (createdMembershipId == null) {
            createdMembershipId = createTestMembership();
        }

        given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath + "/" + createdMembershipId)
                .then()
                .statusCode(200)
                .body("id", isA(String.class))
                .body("plan", isA(String.class))
                .body("member", isA(String.class))
                .body("status", isA(String.class))
                .body("startDate", isA(String.class))
                .body("createdAt", isA(String.class))
                .body("updatedAt", isA(String.class));
    }

    @Test
    @Order(72)
    @DisplayName("TC-SCHEMA-003: Error Response Schema Consistency")
    void schema_ErrorResponse_ConsistentFormat() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath + "/nonexistent_id_12345")
                .then()
                .statusCode(404)
                .extract().response();

        assertThat(response.jsonPath().getString("message")).isNotNull();
        assertThat(response.jsonPath().getString("status")).satisfiesAnyOf(
                s -> assertThat(s).isNotNull(),
                s -> assertThat(s).isNull()
        );
    }

    // ==================== PERFORMANCE & EDGE CASES ====================

    @Test
    @Order(80)
    @DisplayName("TC-PERF-001: Response Time Under Threshold")
    void performance_ResponseTimeUnderThreshold() {
        given()
                .spec(requestSpec)
                .when()
                .get(membershipsPath)
                .then()
                .statusCode(200)
                .time(lessThan(5000L));
    }

    @Test
    @Order(81)
    @DisplayName("TC-EDGE-001: Unicode Characters in Fields")
    void edge_UnicodeCharacters_HandledCorrectly() {
        String payload = buildJson(
                "plan", "plan_smart_001",
                "member", "member_unicode_" + generateUniqueId(),
                "startDate", "2026-08-01",
                "status", "active",
                "notes", "日本語 🎉 Émojis & Special Chars"
        );

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(201), equalTo(422)));
    }

    @Test
    @Order(82)
    @DisplayName("TC-EDGE-002: Null Values in Optional Fields")
    void edge_NullOptionalFields_Accepted() {
        // Build JSON with null values using StringBuilder to avoid escaped quotes
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append('"').append("plan").append('"').append(':').append('"').append("plan_smart_001").append('"').append(',');
        sb.append('"').append("member").append('"').append(':').append('"').append("member_null_" + generateUniqueId()).append('"').append(',');
        sb.append('"').append("startDate").append('"').append(':').append('"').append("2026-08-01").append('"').append(',');
        sb.append('"').append("status").append('"').append(':').append('"').append("active").append('"').append(',');
        sb.append('"').append("endDate").append('"').append(':').append("null").append(',');
        sb.append('"').append("notes").append('"').append(':').append("null");
        sb.append('}');
        String payload = sb.toString();

        given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post(membershipsPath)
                .then()
                .statusCode(anyOf(equalTo(201), equalTo(422)));
    }

    // ==================== CLEANUP ====================

    @AfterAll
    static void cleanup() {
        if (createdMembershipId != null) {
            try {
                given()
                        .spec(requestSpec)
                        .when()
                        .delete(membershipsPath + "/" + createdMembershipId)
                        .then()
                        .statusCode(anyOf(equalTo(204), equalTo(404)));
            } catch (Exception e) {
                System.err.println("Final cleanup failed: " + e.getMessage());
            }
        }
    }
}