package tests;

import api.CartApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import tests.report.TestReporter;
import tests.support.QrTestHelper;

/**
 * Add a customization / add-on using the live nested-table format.
 * Does not change the 10667 + 10668 baseline cart.
 */
public class CustomizationItemTests {

    @Test(groups = {"customization", "regression"},
            description = "Validates that a size option updates the product name and cart total.")
    public void addSizeCustomizationUpdatesNameAndTotal() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.guestCart();
        cartRequest.addCustomizedItem(
                TestDataReader.getCustomItemId(),
                TestDataReader.getCustomItemPrice(),
                TestDataReader.getCustomItemQuantity(),
                TestDataReader.getCustomOptionId(),
                TestDataReader.getCustomOptionPrice(),
                TestDataReader.getCustomOptionMappingId()
        );

        Response cartHttpResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);
        OrderItem item = cart.getData().getOrderItems().get(0);

        TestReporter.data("Product ID", TestDataReader.getCustomItemId());
        TestReporter.data("Product Name", item.getName());
        TestReporter.data("Quantity", TestReporter.formatQuantity(item.getQuantity()));
        TestReporter.money("Line Total", item.getNetPrice());
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());

        TestReporter.assertEquals("HTTP status validation", cartHttpResponse.statusCode(), 200);
        TestReporter.assertContains(
                "Item name includes selected size",
                item.getName(),
                TestDataReader.getCustomExpectedNameContains()
        );
        TestReporter.assertEquals(
                "Cart total",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getCustomExpectedTotal(),
                0.01
        );
        TestReporter.assertEquals(
                "Line total",
                item.getNetPrice(),
                TestDataReader.getCustomExpectedTotal(),
                0.01
        );
        TestReporter.result("Size customization applied and total validated.");
    }

    @Test(groups = {"customization", "regression"},
            description = "Validates that an add-on increases the cart total.")
    public void addAddonCustomizationIncreasesCartTotal() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.guestCart();
        cartRequest.addCustomizedItem(
                TestDataReader.getCustomAddonItemId(),
                TestDataReader.getCustomAddonItemPrice(),
                TestDataReader.getCustomAddonItemQuantity(),
                TestDataReader.getCustomAddonOptionId(),
                TestDataReader.getCustomAddonOptionPrice(),
                TestDataReader.getCustomAddonOptionMappingId()
        );

        Response cartHttpResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);
        OrderItem item = cart.getData().getOrderItems().get(0);

        TestReporter.data("Product ID", TestDataReader.getCustomAddonItemId());
        TestReporter.data("Product Name", item.getName());
        TestReporter.data("Quantity", TestReporter.formatQuantity(item.getQuantity()));
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());

        TestReporter.assertEquals("HTTP status validation", cartHttpResponse.statusCode(), 200);
        TestReporter.assertEquals(
                "Cart total",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getCustomAddonExpectedTotal(),
                0.01
        );
        TestReporter.result("Add-on applied and total validated.");
    }
}
