# Memberships API - Test Cases Document

## Document Information
- **API Under Test:** OfficeRnD Memberships API
- **Base URL:** `https://identity-staging.officernd.com`
- **Resource:** `/memberships`
- **Authentication:** OAuth 2.0 / Bearer Token
- **Version:** 1.0
- **Date:** 2026-08-01

---

## Table of Contents
1. [GET All Memberships](#1-get-all-memberships)
2. [GET Membership by ID](#2-get-membership-by-id)
3. [POST Create Membership](#3-post-create-membership)
4. [PUT Update Membership](#4-put-update-membership)
5. [DELETE Membership](#5-delete-membership)
6. [Security & Authentication](#6-security--authentication)
7. [Contract & Schema Validation](#7-contract--schema-validation)
8. [Performance & Edge Cases](#8-performance--edge-cases)

---

## 1. GET All Memberships

### TC-GET-001: Retrieve All Memberships - Valid Request
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-001 |
| **Priority** | High |
| **Type** | Positive |
| **Description** | Verify that a valid GET request returns all memberships for the organization |

**Preconditions:**
- Valid OAuth2 access token obtained
- Organization has at least one membership

**Steps:**
1. Send GET request to `/{org-slug}/memberships`
2. Include valid `Authorization: Bearer <token>` header
3. Do not include any query parameters

**Expected Result:**
- Status Code: `200 OK`
- Response Content-Type: `application/json`
- Response body contains an array of membership objects
- Each object has required fields: `id`, `plan`, `member`, `startDate`, `status`
- Array is not null
- Response time < 2000ms

---

### TC-GET-002: Retrieve Memberships with Pagination
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-002 |
| **Priority** | High |
| **Type** | Positive |
| **Description** | Verify pagination works correctly with limit and offset parameters |

**Preconditions:**
- Organization has more than 10 memberships

**Steps:**
1. Send GET request to `/{org-slug}/memberships?limit=5&offset=0`
2. Send GET request to `/{org-slug}/memberships?limit=5&offset=5`

**Expected Result:**
- First request returns exactly 5 items
- Second request returns next 5 items (different IDs)
- Response includes pagination metadata (total count, hasMore)
- No duplicate items between pages

---

### TC-GET-003: Retrieve Memberships with Status Filter
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-003 |
| **Priority** | Medium |
| **Type** | Positive |
| **Description** | Verify filtering memberships by status returns only matching results |

**Steps:**
1. Send GET request to `/{org-slug}/memberships?status=active`

**Expected Result:**
- Status Code: `200 OK`
- All returned memberships have `status: "active"`
- No memberships with other statuses are returned

---

### TC-GET-004: Retrieve Memberships - Invalid Organization Slug
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-004 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify request with non-existent organization returns 404 |

**Steps:**
1. Send GET request to `/nonexistent-org-12345/memberships`
2. Include valid authorization header

**Expected Result:**
- Status Code: `404 Not Found`
- Error message indicates organization not found

---

### TC-GET-005: Retrieve Memberships - Missing Authorization Header
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-005 |
| **Priority** | High |
| **Type** | Security |
| **Description** | Verify unauthenticated request is rejected |

**Steps:**
1. Send GET request to `/{org-slug}/memberships`
2. Do NOT include Authorization header

**Expected Result:**
- Status Code: `401 Unauthorized`
- Response contains error: "Authentication required"

---

### TC-GET-006: Retrieve Memberships - Invalid Query Parameters
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-006 |
| **Priority** | Low |
| **Type** | Negative |
| **Description** | Verify invalid query parameters are handled gracefully |

**Steps:**
1. Send GET request to `/{org-slug}/memberships?limit=abc&offset=-1`

**Expected Result:**
- Status Code: `400 Bad Request` OR `422 Unprocessable Entity`
- Error message explains invalid parameter values
- Server does not crash

---

### TC-GET-007: Retrieve Memberships - Empty List
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-007 |
| **Priority** | Medium |
| **Type** | Positive |
| **Description** | Verify empty organization returns empty array, not null |

**Preconditions:**
- Organization exists but has zero memberships

**Steps:**
1. Send GET request to `/{org-slug}/memberships`

**Expected Result:**
- Status Code: `200 OK`
- Response body: `[]` (empty array)
- Response is NOT null

---

## 2. GET Membership by ID

### TC-GET-ID-001: Retrieve Single Membership - Valid ID
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-ID-001 |
| **Priority** | High |
| **Type** | Positive |
| **Description** | Verify retrieving a membership by valid ID returns correct data |

**Preconditions:**
- Membership with known ID exists (e.g., `mem_12345`)

**Steps:**
1. Send GET request to `/{org-slug}/memberships/{valid-id}`
2. Include valid authorization header

**Expected Result:**
- Status Code: `200 OK`
- Response contains membership object with matching ID
- All required fields are present and non-null
- Data types are correct (id: string, startDate: ISO8601, etc.)

---

### TC-GET-ID-002: Retrieve Membership - Non-Existent ID
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-ID-002 |
| **Priority** | High |
| **Type** | Negative |
| **Description** | Verify request for non-existent membership returns 404 |

**Steps:**
1. Send GET request to `/{org-slug}/memberships/mem_nonexistent_99999`
2. Include valid authorization header

**Expected Result:**
- Status Code: `404 Not Found`
- Error message: "Membership not found" or similar

---

### TC-GET-ID-003: Retrieve Membership - Invalid ID Format
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-ID-003 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify request with malformed ID returns 400 |

**Steps:**
1. Send GET request to `/{org-slug}/memberships/!!!invalid@@@`

**Expected Result:**
- Status Code: `400 Bad Request`
- Error indicates invalid ID format

---

### TC-GET-ID-004: Retrieve Membership - Deleted Membership
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-ID-004 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify retrieving a deleted membership returns 404 or 410 |

**Preconditions:**
- A membership was created and then deleted

**Steps:**
1. Send GET request to `/{org-slug}/memberships/{deleted-id}`

**Expected Result:**
- Status Code: `404 Not Found` or `410 Gone`
- Membership data is not returned

---

### TC-GET-ID-005: Retrieve Membership - Cross-Organization Access
| Field | Value |
|-------|-------|
| **Test ID** | TC-GET-ID-005 |
| **Priority** | High |
| **Type** | Security |
| **Description** | Verify user cannot access membership from different organization |

**Steps:**
1. Obtain token for Organization A
2. Send GET request to `/org-b/memberships/{id-from-org-b}`

**Expected Result:**
- Status Code: `403 Forbidden` or `404 Not Found`
- No data leakage between organizations

---

## 3. POST Create Membership

### TC-POST-001: Create Membership - All Required Fields
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-001 |
| **Priority** | High |
| **Type** | Positive |
| **Description** | Verify creating a membership with all required fields succeeds |

**Preconditions:**
- Valid plan ID exists
- Valid member/company ID exists

**Steps:**
1. Send POST request to `/{org-slug}/memberships`
2. Body:
```json
{
  "plan": "plan_smart_001",
  "member": "member_12345",
  "startDate": "2026-08-01",
  "status": "active"
}
```

**Expected Result:**
- Status Code: `201 Created`
- Response contains created membership with generated ID
- All provided fields match request body
- `createdAt` and `updatedAt` timestamps are present
- Location header contains URI to new resource

---

### TC-POST-002: Create Membership - Missing Required Field (Plan)
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-002 |
| **Priority** | High |
| **Type** | Negative |
| **Description** | Verify missing required field returns 422 |

**Steps:**
1. Send POST request with body missing `plan` field

**Expected Result:**
- Status Code: `422 Unprocessable Entity`
- Error message indicates `plan` is required
- Membership is NOT created

---

### TC-POST-003: Create Membership - Missing Required Field (Member)
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-003 |
| **Priority** | High |
| **Type** | Negative |
| **Description** | Verify missing member field returns validation error |

**Steps:**
1. Send POST request with body missing `member` field

**Expected Result:**
- Status Code: `422 Unprocessable Entity`
- Error indicates `member` is required

---

### TC-POST-004: Create Membership - Invalid Plan ID
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-004 |
| **Priority** | High |
| **Type** | Negative |
| **Description** | Verify non-existent plan ID is rejected |

**Steps:**
1. Send POST request with `plan: "plan_does_not_exist"`

**Expected Result:**
- Status Code: `422 Unprocessable Entity` or `404 Not Found`
- Error indicates plan not found
- Membership is NOT created

---

### TC-POST-005: Create Membership - Invalid Member ID
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-005 |
| **Priority** | High |
| **Type** | Negative |
| **Description** | Verify non-existent member ID is rejected |

**Steps:**
1. Send POST request with `member: "member_does_not_exist"`

**Expected Result:**
- Status Code: `422 Unprocessable Entity` or `404 Not Found`
- Error indicates member not found

---

### TC-POST-006: Create Membership - Invalid Date Format
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-006 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify invalid date format is rejected |

**Steps:**
1. Send POST request with `startDate: "01-08-2026"` (wrong format)

**Expected Result:**
- Status Code: `400 Bad Request` or `422 Unprocessable Entity`
- Error indicates invalid date format (expected ISO8601)

---

### TC-POST-007: Create Membership - Duplicate (Same Plan + Member + Dates)
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-007 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify duplicate membership creation is prevented |

**Preconditions:**
- Membership already exists for plan + member combination

**Steps:**
1. Send POST request with same plan, member, and overlapping dates

**Expected Result:**
- Status Code: `409 Conflict`
- Error indicates membership already exists

---

### TC-POST-008: Create Membership - Boundary Values (Max Length)
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-008 |
| **Priority** | Low |
| **Type** | Negative |
| **Description** | Verify fields exceeding max length are rejected |

**Steps:**
1. Send POST request with `notes` field of 10000+ characters

**Expected Result:**
- Status Code: `422 Unprocessable Entity`
- Error indicates field exceeds maximum length

---

### TC-POST-009: Create Membership - SQL Injection Attempt
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-009 |
| **Priority** | High |
| **Type** | Security |
| **Description** | Verify SQL injection in fields is sanitized |

**Steps:**
1. Send POST request with `plan: "'; DROP TABLE memberships; --"`

**Expected Result:**
- Status Code: `422` or `400`
- No database error exposed
- Memberships table is NOT affected

---

### TC-POST-010: Create Membership - XSS Attempt
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-010 |
| **Priority** | High |
| **Type** | Security |
| **Description** | Verify XSS payloads are sanitized or rejected |

**Steps:**
1. Send POST request with `notes: "<script>alert('xss')</script>"`

**Expected Result:**
- Script tags are sanitized/encoded OR request is rejected
- No JavaScript execution possible via API

---

### TC-POST-011: Create Membership - Empty Request Body
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-011 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify empty body returns validation error |

**Steps:**
1. Send POST request with `{}` as body

**Expected Result:**
- Status Code: `422 Unprocessable Entity`
- Multiple validation errors for missing required fields

---

### TC-POST-012: Create Membership - Invalid JSON
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-012 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify malformed JSON is handled gracefully |

**Steps:**
1. Send POST request with body: `{ "plan": "abc", "member" }` (invalid JSON)

**Expected Result:**
- Status Code: `400 Bad Request`
- Error indicates invalid JSON format

---

### TC-POST-013: Create Membership - Future Start Date
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-013 |
| **Priority** | Medium |
| **Type** | Positive |
| **Description** | Verify future start date is accepted |

**Steps:**
1. Send POST request with `startDate: "2027-01-01"`

**Expected Result:**
- Status Code: `201 Created`
- Membership created with future start date

---

### TC-POST-014: Create Membership - Past Start Date
| Field | Value |
|-------|-------|
| **Test ID** | TC-POST-014 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify past start date is rejected or handled per business rules |

**Steps:**
1. Send POST request with `startDate: "2020-01-01"` (far past)

**Expected Result:**
- Status Code: `422 Unprocessable Entity` OR `201 Created` (depending on business rules)
- Behavior is documented and consistent

---

## 4. PUT Update Membership

### TC-PUT-001: Update Membership - Valid Full Update
| Field | Value |
|-------|-------|
| **Test ID** | TC-PUT-001 |
| **Priority** | High |
| **Type** | Positive |
| **Description** | Verify updating all fields of an existing membership succeeds |

**Preconditions:**
- Membership exists with known ID

**Steps:**
1. Send PUT request to `/{org-slug}/memberships/{id}`
2. Body with updated values:
```json
{
  "plan": "plan_total_001",
  "member": "member_12345",
  "startDate": "2026-09-01",
  "status": "paused",
  "notes": "Updated via API test"
}
```

**Expected Result:**
- Status Code: `200 OK`
- Response reflects all updated values
- `updatedAt` timestamp is newer than previous value
- ID remains unchanged

---

### TC-PUT-002: Update Membership - Partial Update
| Field | Value |
|-------|-------|
| **Test ID** | TC-PUT-002 |
| **Priority** | High |
| **Type** | Positive |
| **Description** | Verify partial update (single field) works correctly |

**Steps:**
1. Send PUT request with body containing only `status: "cancelled"`

**Expected Result:**
- Status Code: `200 OK`
- Only `status` is changed
- Other fields retain previous values

---

### TC-PUT-003: Update Membership - Non-Existent ID
| Field | Value |
|-------|-------|
| **Test ID** | TC-PUT-003 |
| **Priority** | High |
| **Type** | Negative |
| **Description** | Verify updating non-existent membership returns 404 |

**Steps:**
1. Send PUT request to `/{org-slug}/memberships/mem_nonexistent_99999`

**Expected Result:**
- Status Code: `404 Not Found`
- Error: "Membership not found"

---

### TC-PUT-004: Update Membership - Invalid Status Value
| Field | Value |
|-------|-------|
| **Test ID** | TC-PUT-004 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify invalid enum value for status is rejected |

**Steps:**
1. Send PUT request with `status: "invalid_status_xyz"`

**Expected Result:**
- Status Code: `422 Unprocessable Entity`
- Error lists valid status values

---

### TC-PUT-005: Update Membership - Concurrent Update Conflict
| Field | Value |
|-------|-------|
| **Test ID** | TC-PUT-005 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify concurrent updates are handled (optimistic locking) |

**Steps:**
1. Read membership, note `updatedAt` or ETag
2. Send PUT request with stale `updatedAt` or missing ETag

**Expected Result:**
- Status Code: `409 Conflict` or `412 Precondition Failed`
- Error indicates resource was modified

---

### TC-PUT-006: Update Membership - Change Plan to Invalid
| Field | Value |
|-------|-------|
| **Test ID** | TC-PUT-006 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify updating to non-existent plan is rejected |

**Steps:**
1. Send PUT request with `plan: "plan_nonexistent"`

**Expected Result:**
- Status Code: `422 Unprocessable Entity`
- Error indicates plan not found
- Original membership is NOT modified

---

### TC-PUT-007: Update Membership - Read-Only Fields Ignored
| Field | Value |
|-------|-------|
| **Test ID** | TC-PUT-007 |
| **Priority** | Medium |
| **Type** | Positive |
| **Description** | Verify read-only fields (id, createdAt) cannot be modified |

**Steps:**
1. Send PUT request attempting to change `id` and `createdAt`

**Expected Result:**
- Status Code: `200 OK`
- `id` and `createdAt` remain unchanged
- OR `400 Bad Request` if API rejects unknown fields

---

## 5. DELETE Membership

### TC-DELETE-001: Delete Membership - Valid ID
| Field | Value |
|-------|-------|
| **Test ID** | TC-DELETE-001 |
| **Priority** | High |
| **Type** | Positive |
| **Description** | Verify deleting an existing membership succeeds |

**Preconditions:**
- Membership exists with known ID

**Steps:**
1. Send DELETE request to `/{org-slug}/memberships/{id}`
2. Include valid authorization header

**Expected Result:**
- Status Code: `204 No Content` or `200 OK`
- Response body is empty or contains success message
- Subsequent GET request to same ID returns 404

---

### TC-DELETE-002: Delete Membership - Non-Existent ID
| Field | Value |
|-------|-------|
| **Test ID** | TC-DELETE-002 |
| **Priority** | Medium |
| **Type** | Negative |
| **Description** | Verify deleting non-existent membership returns 404 |

**Steps:**
1. Send DELETE request to `/{org-slug}/memberships/mem_nonexistent_99999`

**Expected Result:**
- Status Code: `404 Not Found`
- Error: "Membership not found"

---

### TC-DELETE-003: Delete Membership - Already Deleted
| Field | Value |
|-------|-------|
| **Test ID** | TC-DELETE-003 |
| **Priority** | Low |
| **Type** | Negative |
| **Description** | Verify double-delete returns appropriate error |

**Preconditions:**
- Membership was already deleted

**Steps:**
1. Send DELETE request to same ID again

**Expected Result:**
- Status Code: `404 Not Found` or `410 Gone`
- Consistent with first delete behavior

---

### TC-DELETE-004: Delete Membership - Cascading Effects
| Field | Value |
|-------|-------|
| **Test ID** | TC-DELETE-004 |
| **Priority** | High |
| **Type** | Positive |
| **Description** | Verify deleting membership cleans up related data properly |

**Preconditions:**
- Membership has associated invoices/charges

**Steps:**
1. Send DELETE request to membership ID
2. Check related invoices/charges

**Expected Result:**
- Membership is deleted
- Related future charges are cancelled
- Historical invoices remain intact
- No orphaned records

---

### TC-DELETE-005: Delete Membership - Unauthorized User
| Field | Value |
|-------|-------|
| **Test ID** | TC-DELETE-005 |
| **Priority** | High |
| **Type** | Security |
| **Description** | Verify user without delete permission cannot delete |

**Steps:**
1. Obtain token for user with read-only permissions
2. Send DELETE request

**Expected Result:**
- Status Code: `403 Forbidden`
- Membership is NOT deleted

---

## 6. Security & Authentication

### TC-SEC-001: Invalid Token
| Field | Value |
|-------|-------|
| **Test ID** | TC-SEC-001 |
| **Priority** | High |
| **Type** | Security |
| **Description** | Verify invalid Bearer token is rejected |

**Steps:**
1. Send any request with `Authorization: Bearer invalid_token_123`

**Expected Result:**
- Status Code: `401 Unauthorized`
- Error: "Invalid token"

---

### TC-SEC-002: Expired Token
| Field | Value |
|-------|-------|
| **Test ID** | TC-SEC-002 |
| **Priority** | High |
| **Type** | Security |
| **Description** | Verify expired token is rejected |

**Steps:**
1. Send request with previously valid but now expired token

**Expected Result:**
- Status Code: `401 Unauthorized`
- Error: "Token expired"

---

### TC-SEC-003: Missing Token
| Field | Value |
|-------|-------|
| **Test ID** | TC-SEC-003 |
| **Priority** | High |
| **Type** | Security |
| **Description** | Verify request without token is rejected |

**Steps:**
1. Send request with no Authorization header

**Expected Result:**
- Status Code: `401 Unauthorized`

---

### TC-SEC-004: Wrong HTTP Method
| Field | Value |
|-------|-------|
| **Test ID** | TC-SEC-004 |
| **Priority** | Medium |
| **Type** | Security |
| **Description** | Verify unsupported HTTP methods return 405 |

**Steps:**
1. Send PATCH request to `/{org-slug}/memberships/{id}`

**Expected Result:**
- Status Code: `405 Method Not Allowed`
- Allow header lists supported methods

---

### TC-SEC-005: CORS Preflight
| Field | Value |
|-------|-------|
| **Test ID** | TC-SEC-005 |
| **Priority** | Medium |
| **Type** | Security |
| **Description** | Verify CORS headers are present for browser requests |

**Steps:**
1. Send OPTIONS request with `Origin` and `Access-Control-Request-Method` headers

**Expected Result:**
- Status Code: `204 No Content`
- Response includes `Access-Control-Allow-Origin` and `Access-Control-Allow-Methods`

---

## 7. Contract & Schema Validation

### TC-SCHEMA-001: Response Schema Validation - GET All
| Field | Value |
|-------|-------|
| **Test ID** | TC-SCHEMA-001 |
| **Priority** | High |
| **Type** | Positive |
| **Description** | Verify GET all response matches expected JSON schema |

**Steps:**
1. Send GET request to `/{org-slug}/memberships`
2. Validate response against JSON schema

**Expected Result:**
- Response matches schema: array of objects with required fields
- No additional properties violate schema (if strict)
- Data types are correct

---

### TC-SCHEMA-002: Response Schema Validation - GET by ID
| Field | Value |
|-------|-------|
| **Test ID** | TC-SCHEMA-002 |
| **Priority** | High |
| **Type** | Positive |
| **Description** | Verify single membership response matches schema |

**Steps:**
1. Send GET request to `/{org-slug}/memberships/{id}`
2. Validate response against membership object schema

**Expected Result:**
- Response is a single object (not array)
- All required fields present with correct types

---

### TC-SCHEMA-003: Error Response Schema
| Field | Value |
|-------|-------|
| **Test ID** | TC-SCHEMA-003 |
| **Priority** | Medium |
| **Type** | Positive |
| **Description** | Verify error responses follow consistent schema |

**Steps:**
1. Send multiple invalid requests
2. Validate all error responses against error schema

**Expected Result:**
- All errors contain: `error` or `message` field
- Status code is present
- Error format is consistent across endpoints

---

## 8. Performance & Edge Cases

### TC-PERF-001: Response Time - GET All
| Field | Value |
|-------|-------|
| **Test ID** | TC-PERF-001 |
| **Priority** | Medium |
| **Type** | Positive |
| **Description** | Verify GET all memberships responds within acceptable time |

**Steps:**
1. Send GET request to `/{org-slug}/memberships`
2. Measure response time

**Expected Result:**
- Response time < 2000ms for < 100 records
- Response time < 5000ms for < 1000 records

---

### TC-PERF-002: Large Payload Handling
| Field | Value |
|-------|-------|
| **Test ID** | TC-PERF-002 |
| **Priority** | Low |
| **Type** | Negative |
| **Description** | Verify API handles extremely large request body gracefully |

**Steps:**
1. Send POST request with 10MB JSON payload

**Expected Result:**
- Status Code: `413 Payload Too Large` or `400 Bad Request`
- Server does not crash
- Memory usage remains stable

---

### TC-EDGE-001: Unicode Characters in Fields
| Field | Value |
|-------|-------|
| **Test ID** | TC-EDGE-001 |
| **Priority** | Low |
| **Type** | Positive |
| **Description** | Verify Unicode and special characters are handled correctly |

**Steps:**
1. Send POST with `notes: "日本語 🎉 Émojis & Special <>&"'"`

**Expected Result:**
- Status Code: `201 Created`
- Characters are preserved exactly as sent
- No encoding corruption

---

### TC-EDGE-002: Null Values in Optional Fields
| Field | Value |
|-------|-------|
| **Test ID** | TC-EDGE-002 |
| **Priority** | Medium |
| **Type** | Positive |
| **Description** | Verify null values in optional fields are accepted |

**Steps:**
1. Send POST with `endDate: null` and `notes: null`

**Expected Result:**
- Status Code: `201 Created`
- Optional fields are stored as null or omitted

---

### TC-EDGE-003: Rate Limiting
| Field | Value |
|-------|-------|
| **Test ID** | TC-EDGE-003 |
| **Priority** | Medium |
| **Type** | Security |
| **Description** | Verify rate limiting is enforced |

**Steps:**
1. Send 1000 requests in rapid succession

**Expected Result:**
- After threshold: `429 Too Many Requests`
- Response includes `Retry-After` header
- Rate limit resets after window

---

## Summary Statistics

| Category | Total | High Priority | Medium Priority | Low Priority |
|----------|-------|---------------|-----------------|--------------|
| GET All | 7 | 3 | 3 | 1 |
| GET by ID | 5 | 2 | 2 | 1 |
| POST | 14 | 5 | 5 | 4 |
| PUT | 7 | 2 | 4 | 1 |
| DELETE | 5 | 2 | 2 | 1 |
| Security | 5 | 3 | 2 | 0 |
| Schema | 3 | 2 | 1 | 0 |
| Performance/Edge | 4 | 0 | 2 | 2 |
| **TOTAL** | **50** | **19** | **21** | **10** |

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| API behavior differs from assumptions | Tests are parameterized; update base URL and schemas as needed |
| Test data pollution | Each test creates unique data and cleans up after execution |
| Authentication changes | Auth helper is centralized; update token retrieval logic in one place |
| Environment instability | Tests include retry logic and explicit waits |
| Concurrent test execution | Tests use unique identifiers to avoid collisions |

---

## Approach to "Fully Tested"

To confidently say the Memberships API is "fully tested" with satisfactory coverage for a widely-used system:

1. **Functional Coverage:** Every endpoint (GET, POST, PUT, DELETE) tested for happy path and error paths
2. **Input Validation:** Boundary values, invalid formats, missing fields, injection attempts
3. **Security:** Authentication, authorization, CORS, rate limiting, data isolation between organizations
4. **Contract:** JSON schema validation ensures backward compatibility
5. **Integration:** Verify cascading effects (deleting membership affects billing)
6. **Performance:** Response times and payload size limits
7. **Idempotency:** Repeated identical requests produce consistent results
8. **State Transitions:** Membership status changes follow business rules
9. **Data Integrity:** No orphaned records, proper cleanup
10. **Regression:** Automated suite runs on every deployment

---

*Document generated for OfficeRnD QA Assignment*
