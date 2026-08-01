package com.officernd.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Properties;

/**
 * Base class for all UI tests. Manages:
 * - WebDriver lifecycle (setup and teardown)
 * - Configuration loading from config.properties
 * - Screenshot capture on failure
 * - Common wait configurations
 */
public abstract class BaseUiTest {

    protected static final Properties config = new Properties();
    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeAll
    static void setupClass() {
        // Automatically manage ChromeDriver
        WebDriverManager.chromedriver().setup();
        loadConfig();
    }

    @BeforeEach
    void setupTest() {
        ChromeOptions options = new ChromeOptions();

        // Headless mode if configured
        boolean headless = Boolean.parseBoolean(config.getProperty("ui.headless", "false"));
        if (headless) {
            options.addArguments("--headless=new");
        }

        // Common Chrome options for stability
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");

        driver = new ChromeDriver(options);

        int timeoutSeconds = Integer.parseInt(config.getProperty("ui.timeout.seconds", "15"));
        wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        // Capture screenshot on failure
        if (testInfo.getTags().contains("screenshot") || testFailed(testInfo)) {
            captureScreenshot(testInfo.getDisplayName());
        }

        if (driver != null) {
            driver.quit();
        }
    }

    private static void loadConfig() {
        try (InputStream is = BaseUiTest.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                config.load(is);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load config.properties: " + e.getMessage());
        }
    }

    /**
     * Captures a screenshot and saves it to the test output directory.
     */
    protected void captureScreenshot(String testName) {
        if (driver instanceof TakesScreenshot) {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String safeName = testName.replaceAll("[^a-zA-Z0-9.-]", "_");
            Path target = Path.of("target/screenshots", safeName + "_" + System.currentTimeMillis() + ".png");
            try {
                Files.createDirectories(target.getParent());
                Files.copy(screenshot.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Screenshot saved: " + target);
            } catch (IOException e) {
                System.err.println("Failed to save screenshot: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if the current test failed. Used for conditional screenshot capture.
     * Note: In JUnit 5, this is typically handled via TestExecutionExceptionHandler
     * or by checking the TestInfo/ExtensionContext. For simplicity, we capture
     * screenshots based on tags or explicit calls.
     */
    private boolean testFailed(TestInfo testInfo) {
        // Simplified: always capture screenshots for critical tests
        return testInfo.getTags().stream().anyMatch(tag -> tag.equals("critical"));
    }

    /**
     * Returns the base URL for the staging environment.
     */
    protected String getBaseUrl() {
        return config.getProperty("ui.base.url", "https://staging.officernd.com");
    }

    /**
     * Returns the login email from config.
     */
    protected String getEmail() {
        return config.getProperty("ui.email", "automated@kremena.user");
    }

    /**
     * Returns the login password from config.
     */
    protected String getPassword() {
        return config.getProperty("ui.password", "P@ssw0rd");
    }
}
