import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import java.time.Duration;
import java.util.List;

public class SeleniumTest {

    public static WebDriver driver;

    @BeforeTest
    public void setup() {
        System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/src/test/resources/chromedriver.exe");
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get("https://app.scieline.com/ChemTest");
    }

    @Test
    public void teststeps() throws InterruptedException {
        // Wait for the menu toggle to be clickable and click it
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Enter username and password using form1 with input fields txtUser and txtPassword
        WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//form[@id='form1']//input[@name='txtUser']")));
        WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//form[@id='form1']//input[@name='txtPassword']")));

        usernameField.sendKeys("Chan Min");
        passwordField.sendKeys("1234");

        // Submit the first form
        WebElement firstForm = driver.findElement(By.id("form1"));
        firstForm.submit();
        // Wait for the next page to load by ensuring a unique element is visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='secondTable_Caption' and text()='Experiment']")));

        // Click on the element with the text "2510-01-001-0005"
        WebElement targetElement = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//td[text()='2510-01-001-0005']")));
        Actions actions = new Actions(driver);
        actions.doubleClick(targetElement).perform();

        // Wait for the new element to appear and click on it
        WebElement newElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='ui-id-16']")));
        newElement.click();

        // Validate and update the table
        validateAndUpdateTable();
    }

    void validateAndUpdateTable() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Selectors and values for the input fields
        String[] selectors = {
                "#plannedCompositions_col_WW_P_row_227123", // Compound A (using CSS Selector)
                "/html/body/div[4]/table/tbody/tr[2]/td/table/tbody/tr[1]/td/div/div[2]/div[3]/div/div/div/div[2]/div/div[2]/div/div[3]/div[5]/div/div/table/tbody/tr[2]/td[4]/input", // Compound B
                "/html/body/div[4]/table/tbody/tr[2]/td/table/tbody/tr[1]/td/div/div[2]/div[3]/div/div/div/div[2]/div/div[2]/div/div[3]/div[5]/div/div/table/tbody/tr[3]/td[4]/input", // Material F
                "/html/body/div[4]/table/tbody/tr[2]/td/table/tbody/tr[1]/td/div/div[2]/div[3]/div/div/div/div[2]/div/div[2]/div/div[3]/div[5]/div/div/table/tbody/tr[4]/td[4]/input"  // Recipe A
        };

        String[] values = {"25.00", "15.00", "30.00", "30.00"};

        boolean allValuesUpdated = true;
        double totalWwP = 0;

        for (int i = 0; i < selectors.length; i++) {
            try {
                WebElement inputField;

                // Use CSS Selector if available, else use XPath
                if (selectors[i].startsWith("#")) {
                    inputField = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selectors[i])));
                } else {
                    inputField = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(selectors[i])));
                }

                js.executeScript("arguments[0].scrollIntoView(true);", inputField);

                // Clear existing value and set new value using JavaScript to bypass events
                js.executeScript("arguments[0].value = '';", inputField);
                js.executeScript("arguments[0].value = arguments[1];", inputField, values[i]);

                // Manually trigger change event to update calculations
                js.executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", inputField);

                // Re-validate the value after input
                String actualValue = inputField.getAttribute("value").trim();
                if (!actualValue.equals(values[i])) {
                    allValuesUpdated = false;
                    System.err.println("Mismatch in row " + (i + 1) + ": Expected - " + values[i] + " | Actual - " + actualValue);
                } else {
                    // Add to total for validation
                    totalWwP += Double.parseDouble(actualValue);
                }
            } catch (Exception e) {
                allValuesUpdated = false;
                System.err.println("Error updating row " + (i + 1) + ": " + e.getMessage());
            }
        }

        // If all values are updated correctly and total w/w% is 100, click on Composition Details
        if (allValuesUpdated && totalWwP == 100.0) {
            System.out.println("All values are updated correctly and total w/w% is 100. Clicking on Composition Details.");

            // Wait for 2 seconds to ensure UI is fully updated
            Thread.sleep(2000);

            // Locate the Composition Details button using the XPath
            WebElement compositionDetailsButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                    "//*[@id='plannedCompositions_dataTableStructButtons']/button[4]"
            )));

            // Scroll into view and click using JavaScript for better reliability
            js.executeScript("arguments[0].scrollIntoView(true);", compositionDetailsButton);
            js.executeScript("arguments[0].click();", compositionDetailsButton);

            // Wait for Composition Details table to load
            Thread.sleep(4000); // Slightly longer wait for table rendering

            // Switch to the iframe containing the Composition Details table
            driver.switchTo().frame("formIframeId");

            // Wait for the Material F value to be visible
            WebElement materialFValue = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
                    "#compositionDetails > tbody > tr:nth-child(3) > td:nth-child(2)"
            )));

            js.executeScript("arguments[0].scrollIntoView(true);", materialFValue);

            String actualMaterialFValue = materialFValue.getText().trim();
            double expectedMaterialFValueNum = 35.0;

            try {
                // Convert the actual value to double to ignore trailing zeros
                double actualMaterialFValueNum = Double.parseDouble(actualMaterialFValue);

                // Use a small delta to compare doubles
                if (Math.abs(actualMaterialFValueNum - expectedMaterialFValueNum) < 0.001) {
                    System.out.println("Test Success: Material F is 35 w/w as expected.");
                } else {
                    System.err.println("Test Failed: Material F value is " + actualMaterialFValue + " instead of 35.");
                    Assert.fail("Material F value is incorrect.");
                }
            } catch (NumberFormatException e) {
                System.err.println("Test Failed: Unable to parse Material F value. Received: " + actualMaterialFValue);
                Assert.fail("Material F value is not a valid number.");
            }

            // Switch back to the main content after finishing in the iframe
            driver.switchTo().defaultContent();

        } else {
            System.err.println("Total w/w% is not 100 or values did not update correctly. Button not clicked.");
            Assert.fail("Validation failed: Total w/w% is not 100 or values did not update correctly.");
        }
    }


}
