package ui;

import config.TelrCardDetails;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Opens Telr Hosted Payment Page, then waits until Paytm redirects back with ct.
 * Card values come from TelrCardDetails, not from the API framework test data.
 */
public class TelrHostedPage {

    private static final Duration PAGE_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration FIELD_TIMEOUT = Duration.ofSeconds(20);

    public String completePaymentAndReturnPaytmUrl(String paymentUrl) {
        ChromeOptions options = new ChromeOptions();
        if (TelrCardDetails.isHeadless()) {
            options.addArguments("--headless=new");
        }

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get(paymentUrl);
            WebDriverWait wait = new WebDriverWait(driver, PAGE_TIMEOUT);

            if (TelrCardDetails.isConfigured()) {
                fillCardAndSubmit(driver);
            } else {
                System.out.println(
                        "Telr hosted payment is waiting for a test card to be entered in the browser."
                );
            }

            wait.until(webDriver -> {
                String currentUrl = webDriver.getCurrentUrl();
                return currentUrl.contains("paytmcheckout.com") && currentUrl.contains("ct=");
            });

            return driver.getCurrentUrl();
        } finally {
            driver.quit();
        }
    }

    private void fillCardAndSubmit(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, FIELD_TIMEOUT);

        WebElement cardNumber = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("#creditCardNumber, input[name='CCNo']")
        ));
        cardNumber.clear();
        cardNumber.sendKeys(TelrCardDetails.getCardNumber());

        setExpiry(driver, TelrCardDetails.getExpiryMonth(), TelrCardDetails.getExpiryYear());

        WebElement cvv = firstDisplayed(
                driver,
                By.cssSelector("#cardCVV, input[name='cc-csc'], #cvv, input[name='CCCVV']")
        );
        if (cvv == null) {
            throw missingField("CVV", driver);
        }
        cvv.clear();
        cvv.sendKeys(TelrCardDetails.getCvv());

        WebElement payButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#cardpaybutton_v2, #payButton, button[type='submit']")
        ));
        payButton.click();
    }

    private void setExpiry(WebDriver driver, String month, String year) {
        WebElement expiry = firstDisplayed(
                driver,
                By.cssSelector("#creditCardExp, input[name='CCExpiresValid']")
        );
        if (expiry != null) {
            expiry.clear();
            expiry.sendKeys(combinedExpiry(month, year));
            return;
        }

        WebElement monthSelect = firstDisplayed(
                driver,
                By.cssSelector("select[name='CCMonth'], select[name='ivp_exm'], #expiryMonth")
        );
        if (monthSelect != null) {
            new Select(monthSelect).selectByValue(month);
            WebElement yearSelect = firstDisplayed(
                    driver,
                    By.cssSelector("select[name='CCYear'], select[name='ivp_exy'], #expiryYear")
            );
            if (yearSelect == null) {
                throw missingField("expiry year", driver);
            }
            selectYear(yearSelect, year);
            return;
        }

        throw missingField("expiry", driver);
    }

    private String combinedExpiry(String month, String year) {
        String twoDigitYear = year.length() == 4 ? year.substring(2) : year;
        return month + "/" + twoDigitYear;
    }

    private void selectYear(WebElement yearSelect, String year) {
        Select select = new Select(yearSelect);
        try {
            select.selectByValue(year);
        } catch (Exception ignored) {
            select.selectByValue("20" + year);
        }
    }

    private WebElement firstDisplayed(WebDriver driver, By locator) {
        List<WebElement> found = driver.findElements(locator);
        for (WebElement element : found) {
            if (element.isDisplayed()) {
                return element;
            }
        }
        return found.isEmpty() ? null : found.get(0);
    }

    private IllegalStateException missingField(String fieldName, WebDriver driver) {
        StringBuilder names = new StringBuilder();
        for (WebElement element : driver.findElements(By.cssSelector("input, select, button"))) {
            String id = element.getAttribute("id");
            String name = element.getAttribute("name");
            String type = element.getAttribute("type");
            names.append(" [")
                    .append(element.getTagName())
                    .append(" id=").append(id)
                    .append(" name=").append(name)
                    .append(" type=").append(type)
                    .append("]");
        }
        return new IllegalStateException(
                "Telr hosted page: could not find " + fieldName + ". Visible fields:" + names
        );
    }
}
