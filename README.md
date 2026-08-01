# OfficeRnD QA Assignment

Complete automation project for OfficeRnD Memberships API and Members UI testing.

## Project Structure

```
officernd-qa-assignment/
├── pom.xml
├── README.md
├── Memberships_API_Test_Cases.md          # Detailed test cases document (Assignment 01)
└── src/test/
    ├── resources/
    │   └── config.properties
    └── java/com/officernd/
        ├── api/
        │   ├── BaseApiTest.java             # Base configuration for API tests
        │   └── MembershipsApiTest.java      # Complete API test suite (Assignment 01)
        ├── ui/
        │   ├── BaseUiTest.java              # Base configuration for UI tests
        │   └── MembersPageTest.java         # UI automation test (Assignment 02)
        └── utils/
            └── AuthHelper.java              # Authentication helper
```

---

## Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Chrome Browser** (for UI tests)
- Internet connection (tests run against staging environment)

---

## Installation

1. **Clone or extract the project**

```bash
cd officernd-qa-assignment
```

2. **Install dependencies**

```bash
mvn clean install -DskipTests
```

This downloads all required dependencies: Rest Assured, JUnit 5, Selenium, WebDriverManager, Jackson, and AssertJ.

---

## Running the Tests

### Run All Tests

```bash
mvn clean test
```

### Run Only API Tests (Assignment 01)

```bash
mvn clean test -Dtest=MembershipsApiTest
```

### Run Only UI Tests (Assignment 02)

```bash
mvn clean test -Dtest=MembersPageTest
```

### Run with Custom Properties

```bash
mvn clean test -DbaseUrl=https://identity-staging.officernd.com
```

---

## Configuration

Edit `src/test/resources/config.properties` to change:

| Property | Description | Default |
|----------|-------------|---------|
| `api.base.url` | Identity API base URL | `https://identity-staging.officernd.com` |
| `api.org.slug` | Organization slug | `kremena-qa-assignment-tasks` |
| `ui.base.url` | Staging UI URL | `https://staging.officernd.com` |
| `ui.email` | Login email | `automated@kremena.user` |
| `ui.password` | Login password | `P@ssw0rd` |
| `ui.headless` | Run browser headless | `false` |

---

## Assignment 01: API Testing (Rest Assured)

**File:** `src/test/java/com/officernd/api/MembershipsApiTest.java`

### What is covered:

| Category | Scenarios |
|----------|-----------|
| **GET /memberships** | Valid request, pagination, sorting, filtering, empty list |
| **GET /memberships/{id}** | Valid ID, non-existent ID, invalid ID format, deleted ID |
| **POST /memberships** | Valid creation, missing required fields, invalid plan, invalid member, duplicate, boundary values |
| **PUT /memberships/{id}** | Valid update, update non-existent, invalid data, concurrent update |
| **DELETE /memberships/{id}** | Valid deletion, delete non-existent, delete already deleted, unauthorized |
| **Security** | Invalid token, expired token, missing auth, forbidden access |
| **Contract** | JSON schema validation, content-type, response structure |

### Key Design Decisions:
- **Base Test Class:** Handles authentication, request/response logging, and common configuration
- **Dynamic Test Data:** Each test creates unique data to avoid collisions
- **Cleanup:** Tests clean up created resources in `@AfterEach`
- **Schema Validation:** Validates response structure against expected JSON schema
- **Error Coverage:** Tests 400, 401, 403, 404, 409, 422, 500 status codes

---

## Assignment 02: UI Testing (Selenium)

**File:** `src/test/java/com/officernd/ui/MembersPageTest.java`

### Test Scenario:
1. Navigate to `https://staging.officernd.com/login`
2. Log in with provided credentials
3. Navigate to Members page
4. Apply filter by Name: "zara"
5. Validate exactly 2 results are displayed

### Key Design Decisions:
- **Page Object Model ready:** Locators are separated from test logic
- **Explicit Waits:** Uses `WebDriverWait` for dynamic content
- **WebDriverManager:** Automatically manages ChromeDriver
- **Screenshots:** Captures on failure for debugging
- **Headless Support:** Configurable via properties

---

## Test Cases Document

See `Memberships_API_Test_Cases.md` for the complete manual test plan with:
- Test ID and Description
- Preconditions
- Test Steps
- Expected Results
- Priority (High/Medium/Low)
- Test Type (Positive/Negative/Security)

---

## Notes

- The API tests assume standard REST conventions for the OfficeRnD Memberships API
- Base path for API: `/{org-slug}/memberships`
- Authentication uses OAuth2 Client Credentials flow against the identity endpoint
- UI tests use Chrome by default; modify `BaseUiTest` to use Firefox/Edge if needed
- All tests are independent and can run in any order

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `ChromeDriver` errors | WebDriverManager handles this automatically. Ensure Chrome is installed. |
| `401 Unauthorized` | Check credentials in `config.properties` |
| `Connection refused` | Ensure you have internet access to staging.officernd.com |
| Tests fail intermittently | Increase wait timeouts in `BaseUiTest` or `BaseApiTest` |

---

## Author

QA Automation Assignment - OfficeRnD Memberships API & Members UI
