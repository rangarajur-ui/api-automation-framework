package tests.report;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import org.testng.Assert;
import org.testng.ITestResult;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import pojo.response.OrderItemsTotal;
import pojo.response.PaymentData;
import pojo.response.PaymentStatusData;
import utils.TokenMasker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes the same business facts to Jenkins console and Extent.
 * PASS/FAIL lines are only emitted from real TestNG assertions.
 */
public final class TestReporter {

    private static final String BANNER = "==================================================";
    private static final ThreadLocal<ExtentTest> EXTENT = new ThreadLocal<>();
    private static final ThreadLocal<TestInfo> INFO = new ThreadLocal<>();
    private static final ThreadLocal<List<String[]>> DATA_ROWS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<Map<String, String>> CONTEXT = ThreadLocal.withInitial(LinkedHashMap::new);
    private static final ThreadLocal<Boolean> ASSERTION_HEADER = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> RESULT_LOGGED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> FAILURE_LOGGED = ThreadLocal.withInitial(() -> false);

    private TestReporter() {
    }

    static void attach(ExtentTest test, TestInfo info) {
        EXTENT.set(test);
        INFO.set(info);
        DATA_ROWS.get().clear();
        CONTEXT.get().clear();
        ASSERTION_HEADER.set(false);
        RESULT_LOGGED.set(false);
        FAILURE_LOGGED.set(false);

        console(BANNER);
        console("TEST: " + info.title());
        console(BANNER);
        console("");

        if (test != null) {
            test.info(info.purpose());
            test.info("API / business flow : " + info.flow());
        }
    }

    static void finish(ITestResult result) {
        flushData();
        if (result.getStatus() == ITestResult.SUCCESS && !Boolean.TRUE.equals(RESULT_LOGGED.get())) {
            TestInfo info = INFO.get();
            if (info != null) {
                result(info.passResult());
            }
        }
        console("");
        console(BANNER);
        console("");
        EXTENT.remove();
        INFO.remove();
        DATA_ROWS.remove();
        CONTEXT.remove();
        ASSERTION_HEADER.remove();
        RESULT_LOGGED.remove();
        FAILURE_LOGGED.remove();
    }

    public static void section(String name) {
        flushData();
        console("");
        console(name);
        console("");
        ExtentTest test = EXTENT.get();
        if (test != null) {
            test.info(MarkupHelper.createLabel(name, ExtentColor.BLUE));
        }
    }

    public static void data(String label, Object value) {
        String display = sanitize(label, value);
        console(pad(label) + " : " + display);
        DATA_ROWS.get().add(new String[]{label, display});
        if (isIdentifier(label)) {
            CONTEXT.get().put(label, display);
        }
    }

    public static void money(String label, double value) {
        data(label, formatMoney(value));
    }

    public static void logAccount(int accountId, String accountName) {
        data("Account ID", accountId);
        data("Account Name", accountName);
    }

    public static void logCartSummary(CartResponse cart) {
        logCartSummary(cart, null);
    }

    public static void logCartSummary(CartResponse cart, Integer httpStatus) {
        if (httpStatus != null) {
            data("Cart Status", httpStatus);
        }
        if (cart == null || cart.getData() == null) {
            return;
        }
        if (cart.getData().getOrderItems() != null) {
            data("Items Added", cart.getData().getOrderItems().size());
        }
        OrderItemsTotal totals = cart.getData().getOrderItemsTotal();
        if (totals != null) {
            money("Cart Total", totals.getTotalAmount());
            money("Tax", totals.getOriginalTaxTotal());
            money("Net Payable", totals.getNetPayableAmount());
        }
        if (cart.getData().getPaymentStatus() != null) {
            data("Payment Status", cart.getData().getPaymentStatus());
        }
        if (cart.getData().getPaymentProviderName() != null) {
            data("Payment Provider", cart.getData().getPaymentProviderName());
        }
    }

    public static void logPaymentSummary(PaymentData payment) {
        section("PAYMENT");
        if (payment == null) {
            return;
        }
        if (payment.getPgName() != null) {
            data("Provider", payment.getPgName());
        }
        data("Payment Success", payment.isSuccess());
        if (payment.getOrderId() != null) {
            data("Order ID", payment.getOrderId());
        }
    }

    public static void logPaymentSummary(String provider, boolean success, String paymentStatus) {
        section("PAYMENT");
        data("Provider", provider);
        data("Payment Success", success);
        data("Payment Status", paymentStatus);
    }

    public static void logOrderSummary(PaymentStatusData order) {
        section("ORDER");
        if (order == null) {
            return;
        }
        if (order.getOrderId() != null) {
            data("Order ID", order.getOrderId());
        }
        if (order.getOrderNumber() != null) {
            data("Order Number", order.getOrderNumber());
        }
        if (order.getOrderStatus() != null) {
            data("Order Status", order.getOrderStatus());
        }
    }

    public static void logOrderItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        section("ORDER ITEMS");
        int index = 1;
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"#", "Item", "Quantity", "Line Total"});
        for (OrderItem item : items) {
            String name = displayItemName(item.getName());
            String quantity = formatQuantity(item.getQuantity());
            String lineTotal = formatMoney(item.getNetPrice());
            console(index + ". " + name);
            console(pad("   Quantity") + " : " + quantity);
            console(pad("   Line Total") + " : " + lineTotal);
            rows.add(new String[]{String.valueOf(index), name, quantity, lineTotal});
            index++;
        }
        ExtentTest test = EXTENT.get();
        if (test != null) {
            test.info(MarkupHelper.createTable(rows.toArray(new String[0][])));
        }
    }

    public static void logTotals(OrderItemsTotal totals) {
        if (totals == null) {
            return;
        }
        section("TOTALS");
        money("Subtotal/Net", totals.getTotalAmount());
        money("Tax", totals.getOriginalTaxTotal());
        money("Net Payable", totals.getNetPayableAmount());
    }

    public static void logNegative(
            String scenario,
            String inputCondition,
            int expectedStatus,
            int actualStatus,
            String expectedResult,
            String actualResult
    ) {
        data("Scenario", scenario);
        data("Input", inputCondition);
        data("Expected Status", expectedStatus);
        data("Actual Status", actualStatus);
        data("Expected Result", expectedResult);
        data("Actual Result", actualResult);
    }

    public static void assertEquals(String name, Object actual, Object expected) {
        try {
            Assert.assertEquals(actual, expected, name);
            logAssertion(name, expected, actual, true, null);
        } catch (AssertionError error) {
            logAssertion(name, expected, actual, false, error.getMessage());
            throw error;
        }
    }

    public static void assertEquals(String name, double actual, double expected, double delta) {
        try {
            Assert.assertEquals(actual, expected, delta, name);
            logAssertion(name, formatMoney(expected), formatMoney(actual), true, null);
        } catch (AssertionError error) {
            logAssertion(name, formatMoney(expected), formatMoney(actual), false, error.getMessage());
            throw error;
        }
    }

    public static void assertEqualsRaw(String name, double actual, double expected, double delta) {
        try {
            Assert.assertEquals(actual, expected, delta, name);
            logAssertion(name, formatQuantity(expected), formatQuantity(actual), true, null);
        } catch (AssertionError error) {
            logAssertion(name, formatQuantity(expected), formatQuantity(actual), false, error.getMessage());
            throw error;
        }
    }

    public static void assertTrue(String name, boolean condition) {
        try {
            Assert.assertTrue(condition, name);
            logAssertion(name, true, true, true, null);
        } catch (AssertionError error) {
            logAssertion(name, true, false, false, error.getMessage());
            throw error;
        }
    }

    public static void assertFalse(String name, boolean condition) {
        try {
            Assert.assertFalse(condition, name);
            logAssertion(name, false, false, true, null);
        } catch (AssertionError error) {
            logAssertion(name, false, true, false, error.getMessage());
            throw error;
        }
    }

    public static void assertNotNull(String name, Object actual) {
        try {
            Assert.assertNotNull(actual, name);
            logAssertion(name, "present", "present", true, null);
        } catch (AssertionError error) {
            logAssertion(name, "present", "missing", false, error.getMessage());
            throw error;
        }
    }

    public static void assertNotBlank(String name, String actual) {
        boolean present = actual != null && !actual.isBlank();
        String shown = !present ? "missing" : (looksLikeSecret(actual) ? "present" : actual);
        try {
            Assert.assertNotNull(actual, name);
            Assert.assertFalse(actual.isBlank(), name);
            logAssertion(name, "present", shown, true, null);
        } catch (AssertionError error) {
            logAssertion(name, "present", shown, false, error.getMessage());
            throw error;
        }
    }

    public static void assertContains(String name, String actual, String expectedFragment) {
        String safeActual = actual == null ? "" : actual;
        try {
            Assert.assertTrue(safeActual.contains(expectedFragment), name);
            logAssertion(name, expectedFragment, safeActual, true, null);
        } catch (AssertionError error) {
            logAssertion(name, expectedFragment, safeActual, false, error.getMessage());
            throw error;
        }
    }

    public static void result(String message) {
        flushData();
        RESULT_LOGGED.set(true);
        console("");
        console("Result           : " + message);
        ExtentTest test = EXTENT.get();
        if (test != null) {
            test.info(MarkupHelper.createLabel("RESULT", ExtentColor.GREEN));
            test.info(escape(message));
        }
    }

    static void recordListenerFailure(ITestResult result) {
        if (Boolean.TRUE.equals(FAILURE_LOGGED.get())) {
            return;
        }
        Throwable error = result.getThrowable();
        String expected = "";
        String actual = "";
        String reason = error == null ? "Test failed" : firstLine(error.getMessage());
        if (error != null && error.getMessage() != null) {
            String message = error.getMessage();
            expected = extractBetween(message, "expected [", "]");
            actual = extractBetween(message, "but found [", "]");
        }
        logFailure(
                result.getMethod().getMethodName(),
                expected.isBlank() ? "see failure" : expected,
                actual.isBlank() ? "see failure" : actual,
                reason
        );
    }

    public static void logFailure(String assertionName, Object expected, Object actual, String reason) {
        flushData();
        FAILURE_LOGGED.set(true);
        console("");
        console("[FAIL] " + assertionName);
        console("");
        console("Expected : " + display(expected));
        console("Actual   : " + display(actual));
        printContext();
        console("");
        console("Failure:");
        console(reason == null ? "Assertion failed" : reason);

        ExtentTest test = EXTENT.get();
        if (test != null) {
            StringBuilder html = new StringBuilder();
            html.append("<b>[FAIL] ").append(escape(assertionName)).append("</b><br/>");
            html.append("Expected : ").append(escape(display(expected))).append("<br/>");
            html.append("Actual   : ").append(escape(display(actual))).append("<br/>");
            for (Map.Entry<String, String> entry : CONTEXT.get().entrySet()) {
                html.append(escape(entry.getKey())).append(" : ")
                        .append(escape(entry.getValue())).append("<br/>");
            }
            html.append("<br/>Failure:<br/>").append(escape(reason == null ? "Assertion failed" : reason));
            test.fail(html.toString());
        }
    }

    private static void logAssertion(
            String name,
            Object expected,
            Object actual,
            boolean pass,
            String failureReason
    ) {
        flushData();
        if (!Boolean.TRUE.equals(ASSERTION_HEADER.get())) {
            console("");
            console("Assertions:");
            ASSERTION_HEADER.set(true);
            ExtentTest test = EXTENT.get();
            if (test != null) {
                test.info(MarkupHelper.createLabel("ASSERTIONS", ExtentColor.BLUE));
            }
        }

        String expectedText = display(expected);
        String actualText = display(actual);
        if (pass) {
            console("[PASS] " + name);
            console("       Expected : " + expectedText);
            console("       Actual   : " + actualText);
            ExtentTest test = EXTENT.get();
            if (test != null) {
                test.pass(escape(name)
                        + "<br/>Expected : " + escape(expectedText)
                        + "<br/>Actual   : " + escape(actualText)
                        + "<br/>Result   : PASS");
            }
            return;
        }

        logFailure(name, expectedText, actualText, shortReason(name, expectedText, actualText, failureReason));
    }

    private static String shortReason(String name, String expected, String actual, String failureReason) {
        if (failureReason != null && failureReason.toLowerCase(Locale.ROOT).contains("status")) {
            return name + " failed.";
        }
        return name + " failed. Expected " + expected + " but found " + actual + ".";
    }

    private static void flushData() {
        List<String[]> rows = DATA_ROWS.get();
        if (rows.isEmpty()) {
            return;
        }
        ExtentTest test = EXTENT.get();
        if (test != null) {
            String[][] table = new String[rows.size() + 1][2];
            table[0] = new String[]{"Field", "Value"};
            for (int i = 0; i < rows.size(); i++) {
                table[i + 1] = rows.get(i);
            }
            test.info(MarkupHelper.createLabel("BUSINESS DATA", ExtentColor.BLUE));
            test.info(MarkupHelper.createTable(table));
        }
        rows.clear();
    }

    private static void printContext() {
        for (Map.Entry<String, String> entry : CONTEXT.get().entrySet()) {
            console(pad(entry.getKey()) + " : " + entry.getValue());
        }
    }

    private static void console(String line) {
        System.out.println(line);
    }

    private static String pad(String label) {
        String trimmed = label == null ? "" : label;
        if (trimmed.length() >= 16) {
            return trimmed;
        }
        return String.format("%-16s", trimmed);
    }

    private static String display(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double || value instanceof Float) {
            return formatMoney(((Number) value).doubleValue());
        }
        String text = String.valueOf(value);
        if (looksLikeSecret(text)) {
            return "present";
        }
        return sanitize("value", value);
    }

    private static boolean looksLikeSecret(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String value = text.trim();
        if (looksLikeCardNumber(value)) {
            return true;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("secure.telr.com")
                || lower.contains("session-token")
                || lower.contains("ct=")
                || lower.contains("authorization")
                || value.matches("(?i)[a-f0-9]{24,}");
    }

    public static String formatMoney(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    public static String formatQuantity(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    public static String maskedMobile(String mobile) {
        return TokenMasker.mask(mobile);
    }

    public static String displayItemName(String name) {
        if (name == null || name.isBlank()) {
            return "Item";
        }
        return name.replaceFirst("\\s+\\d+(?:\\.\\d+)?\\s+qty$", "");
    }

    private static String sanitize(String label, Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (looksLikeCardNumber(text)) {
            return "****";
        }
        if (isSensitiveLabel(label) && !isSafeFlag(text)) {
            return TokenMasker.mask(text);
        }
        return text;
    }

    private static boolean isSensitiveLabel(String label) {
        String key = label == null ? "" : label.toLowerCase(Locale.ROOT);
        return key.contains("token")
                || key.contains("authorization")
                || key.contains("password")
                || key.contains("secret")
                || key.contains("card number")
                || key.contains("cvv")
                || key.contains("expiry")
                || key.contains("payment url")
                || key.equals("ct")
                || key.contains("session-token");
    }

    private static boolean isSafeFlag(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        return value.equals("present")
                || value.equals("missing")
                || value.equals("true")
                || value.equals("false")
                || value.equals("yes")
                || value.equals("no");
    }

    private static boolean looksLikeCardNumber(String text) {
        String digits = text.replaceAll("\\s", "");
        return digits.matches("\\d{13,19}");
    }

    private static boolean isIdentifier(String label) {
        String key = label == null ? "" : label.toLowerCase(Locale.ROOT);
        return key.contains("order id")
                || key.contains("order number")
                || key.contains("account id")
                || key.contains("account name");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String firstLine(String message) {
        if (message == null || message.isBlank()) {
            return "Assertion failed";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    private static String extractBetween(String message, String start, String end) {
        int from = message.indexOf(start);
        if (from < 0) {
            return "";
        }
        from += start.length();
        int to = message.indexOf(end, from);
        if (to < 0) {
            return "";
        }
        return message.substring(from, to);
    }

}
