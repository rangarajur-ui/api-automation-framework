package tests;

import api.CartApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import pojo.response.QrMenuResponse;
import tests.report.TestReporter;
import tests.support.QrTestHelper;

import java.util.List;
import java.util.Map;

/**
 * Core business checks on the happy-flow menu and cart.
 */
public class SanityTests {

    @Test(groups = {"sanity", "regression"},
            description = "Validates that the QR code belongs to the expected account and channel.")
    public void qrMenuReturnsExpectedAccount() {
        Response menuResponse = QrTestHelper.menuResponse();
        QrMenuResponse menu = menuResponse.as(QrMenuResponse.class);
        int accountId = menu.getData().getAccount().getId();
        String accountName = menu.getData().getAccount().getName();

        TestReporter.logAccount(accountId, accountName);

        TestReporter.assertEquals("Account ID", accountId, TestDataReader.getExpectedAccountId());
        TestReporter.assertEquals("Account name", accountName, TestDataReader.getExpectedAccountName());
        TestReporter.result("Account and channel validated.");
    }

    @Test(groups = {"sanity", "regression"},
            description = "Validates that both baseline products are added and the cart waits for Telr payment.")
    public void baselineCartHasBothItemsAndPendingTelr() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();
        Response cartHttpResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);

        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());
        TestReporter.logOrderItems(cart.getData().getOrderItems());

        TestReporter.assertEquals("Expected item count", cart.getData().getOrderItems().size(), 2);

        OrderItem first = cart.getData().getOrderItems().get(0);
        OrderItem second = cart.getData().getOrderItems().get(1);

        TestReporter.assertEquals("First item ID", String.valueOf(first.getItemObjectId()), TestDataReader.getItem1Id());
        TestReporter.assertEqualsRaw(
                "First item quantity",
                first.getQuantity(),
                TestDataReader.getItem1Quantity(),
                0.0
        );
        TestReporter.assertEquals("Second item ID", String.valueOf(second.getItemObjectId()), TestDataReader.getItem2Id());
        TestReporter.assertEqualsRaw(
                "Second item quantity",
                second.getQuantity(),
                TestDataReader.getItem2Quantity(),
                0.0
        );
        TestReporter.assertEquals(
                "Payment status",
                cart.getData().getPaymentStatus(),
                TestDataReader.getExpectedPaymentStatus()
        );
        TestReporter.assertEquals(
                "Payment provider",
                cart.getData().getPaymentProviderName(),
                TestDataReader.getExpectedPaymentProvider()
        );
        TestReporter.assertEquals(
                "Tax",
                cart.getData().getOrderItemsTotal().getOriginalTaxTotal(),
                TestDataReader.getExpectedTax(),
                0.01
        );
        TestReporter.assertEquals(
                "Net payable",
                cart.getData().getOrderItemsTotal().getNetPayableAmount(),
                TestDataReader.getExpectedNetPayable(),
                0.01
        );
        TestReporter.result("Cart created and pending Telr payment.");
    }

    @Test(groups = {"sanity", "regression"},
            description = "Validates that the menu exposes the expected customizable product.")
    public void menuWithOptionsIncludesCustomizableItem() {
        Response menuResponse = QrTestHelper.menuWithOptionsResponse();
        int customItemId = Integer.parseInt(TestDataReader.getCustomItemId());

        List<Map<String, Object>> items = menuResponse.jsonPath().getList("data.categories.items.flatten()");
        if (items == null || items.isEmpty()) {
            items = menuResponse.jsonPath().getList("data.categories.collectMany { it.items }");
        }

        Map<String, Object> customItem = null;
        if (items != null) {
            for (Map<String, Object> item : items) {
                if (item != null && customItemId == ((Number) item.get("id")).intValue()) {
                    customItem = item;
                    break;
                }
            }
        }

        Object name = customItem == null ? null : customItem.get("name");
        Object customizable = customItem == null ? null : customItem.get("is_customizable");

        TestReporter.data("HTTP Status", menuResponse.statusCode());
        TestReporter.data("Product ID", customItemId);
        if (name != null) {
            TestReporter.data("Product Name", name);
        }
        TestReporter.data("Customizable", customizable);

        TestReporter.assertEquals("HTTP status validation", menuResponse.statusCode(), 200);
        TestReporter.assertNotNull("Menu returns category items", items);
        TestReporter.assertNotNull("Customizable product is on the menu", customItem);
        TestReporter.assertEquals("Product is customizable", customizable, true);
        TestReporter.result("Customizable product is available on the menu.");
    }
}
