package com.officernd.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI Automation Test for OfficeRnD Members Page.
 *
 * Test Scenario:
 * 1. Navigate to staging.officernd.com/login
 * 2. Log in with provided credentials
 * 3. Navigate to Members page
 * 4. Apply filter by Name: "zara"
 * 5. Validate exactly 2 results are displayed
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("ui")
@Tag("critical")
public class MembersPageTest extends BaseUiTest {

    private static final String LOGIN_PATH = "/login";
    private static final String MEMBERS_PATH = "/admin/kremena-qa-assignment-tasks/operations/members";

    // Locators - These are based on common OfficeRnD UI patterns.
    // Adjust selectors if the actual DOM structure differs.
    private static final By EMAIL_INPUT = By.cssSelector("input[type='email'], input[name='email'], #email");
    private static final By PASSWORD_INPUT = By.cssSelector("input[type='password'], input[name='password'], #password");
    private static final By LOGIN_BUTTON = By.cssSelector("button[type='submit'], .btn-login, button:contains('Log in')");
    private static final By FILTER_NAME_INPUT = By.cssSelector("input[placeholder*='Name' i], input[name*='name' i], .filter-name input");
    private static final By APPLY_FILTER_BUTTON = By.cssSelector("button:contains('Apply'), .apply-filters, .btn-filter");
    private static final By GRID_ROWS = By.cssSelector(".ag-row, .data-grid-row, tbody tr, .member-row");
    private static final By LOADING_INDICATOR = By.cssSelector(".loading, .spinner, .ag-loading");
    private static final By USER_MENU = By.cssSelector(".user-menu, .avatar, .profile-dropdown");

    @Test
    @Order(1)
    @DisplayName("Assignment 02: Filter Members by Name 'zara' and Validate 2 Results")
    void filterMembersByNameZara_ExpectTwoResults() {
        // Step 1: Navigate to login page
        navigateToLoginPage();

        // Step 2: Log in with credentials
        performLogin(getEmail(), getPassword());

        // Step 3: Navigate to Members page
        navigateToMembersPage();

        // Step 4: Apply filter by Name "zara"
        applyNameFilter("zara");

        // Step 5: Validate exactly 2 results are displayed
        int resultCount = getGridResultCount();
        assertThat(resultCount)
            .as("Expected exactly 2 members with name containing 'zara', but found %d", resultCount)
            .isEqualTo(2);
    }

    // ==================== STEP METHODS ====================

    /**
     * Step 1: Navigate to the login page.
     */
    private void navigateToLoginPage() {
        driver.get(getBaseUrl() + LOGIN_PATH);

        // Wait for login form to be present
        wait.until(ExpectedConditions.presenceOfElementLocated(EMAIL_INPUT));

        // Verify we're on the login page
        String currentUrl = driver.getCurrentUrl();
        assertThat(currentUrl)
            .as("Should be on login page")
            .contains("login");
    }

    /**
     * Step 2: Perform login with email and password.
     */
    private void performLogin(String email, String password) {
        // Enter email
        WebElement emailField = driver.findElement(EMAIL_INPUT);
        emailField.clear();
        emailField.sendKeys(email);

        // Enter password
        WebElement passwordField = driver.findElement(PASSWORD_INPUT);
        passwordField.clear();
        passwordField.sendKeys(password);

        // Click login button
        WebElement loginBtn = driver.findElement(LOGIN_BUTTON);
        loginBtn.click();

        // Wait for navigation to dashboard/members page
        // Using multiple conditions for robustness
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/admin/"),
            ExpectedConditions.presenceOfElementLocated(USER_MENU),
            ExpectedConditions.urlContains("/members")
        ));

        // Small buffer for any post-login redirects
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Step 3: Navigate directly to the Members page.
     */
    private void navigateToMembersPage() {
        driver.get(getBaseUrl() + MEMBERS_PATH);

        // Wait for the page to load - look for grid or filter elements
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(FILTER_NAME_INPUT),
            ExpectedConditions.presenceOfElementLocated(GRID_ROWS),
            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".members-page, [data-testid='members-page']"))
        ));

        // Wait for any loading indicators to disappear
        waitForLoadingToComplete();
    }

    /**
     * Step 4: Apply the "zara" filter to the Name field.
     */
    private void applyNameFilter(String filterValue) {
        // Find and interact with the name filter input
        WebElement nameFilter = wait.until(
            ExpectedConditions.elementToBeClickable(FILTER_NAME_INPUT)
        );

        nameFilter.clear();
        nameFilter.sendKeys(filterValue);

        // Try to find and click apply filter button, or trigger search via Enter
        try {
            List<WebElement> applyButtons = driver.findElements(APPLY_FILTER_BUTTON);
            if (!applyButtons.isEmpty() && applyButtons.get(0).isDisplayed()) {
                applyButtons.get(0).click();
            } else {
                // Fallback: press Enter to trigger filter
                nameFilter.submit();
            }
        } catch (Exception e) {
            // If no apply button, the filter might auto-apply
            nameFilter.submit();
        }

        // Wait for grid to update after filtering
        waitForLoadingToComplete();

        // Additional wait for grid to stabilize
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Step 5: Count the number of results displayed in the grid.
     */
    private int getGridResultCount() {
        // Try multiple selector strategies for robustness
        List<WebElement> rows = driver.findElements(GRID_ROWS);

        // Filter out header rows or empty rows if necessary
        int count = (int) rows.stream()
            .filter(row -> row.isDisplayed())
            .filter(row -> !row.getAttribute("class").contains("header"))
            .filter(row -> !row.getAttribute("class").contains("empty"))
            .filter(row -> row.findElements(By.cssSelector("td, .ag-cell")).size() > 0)
            .count();

        // If no rows found with primary selector, try alternative
        if (count == 0) {
            List<WebElement> altRows = driver.findElements(By.cssSelector(
                ".members-grid tbody tr, .react-grid-row, [role='row']:not([role='rowheader'])"
            ));
            count = (int) altRows.stream()
                .filter(WebElement::isDisplayed)
                .count();
        }

        return count;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Waits for loading indicators to disappear.
     */
    private void waitForLoadingToComplete() {
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADING_INDICATOR));
        } catch (Exception e) {
            // Loading indicator may not exist - that's fine
        }
    }
}
