package tests;

import api.CartApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import tests.report.TestReporter;
import tests.support.CheckoutFlow;
import tests.support.CheckoutResult;
import tests.support.CheckoutStats;
import tests.support.QrTestHelper;

/**
 * Customization must appear on the paid order, not only on the cart.
 */
public class CustomizationItemTests {

    @Test(groups = {"customization", "regression", "checkout"},
            description = "Validates that a size option is ordered and appears on the accepted order.")
    public void shouldCreateOrderWithSizeCustomization() {
        String token = QrTestHelper.freshSessionToken();
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

        TestReporter.section("BUSINESS FLOW");
        TestReporter.data("Flow", "Product → Size customization → Cart → Payment → Order");
        TestReporter.section("PRODUCT");
        TestReporter.data("Product ID", TestDataReader.getCustomItemId());
        TestReporter.data("Product Name", TestReporter.displayItemName(item.getName()));
        TestReporter.data("Customization", TestDataReader.getCustomExpectedNameContains());
        TestReporter.data("Quantity", TestReporter.formatQuantity(item.getQuantity()));
        TestReporter.money("Line Total", item.getNetPrice());
        TestReporter.section("CART");
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());

        TestReporter.assertEquals("Product added", cartHttpResponse.statusCode(), 200);
        TestReporter.assertContains(
                "Size customization applied",
                item.getName(),
                TestDataReader.getCustomExpectedNameContains()
        );
        TestReporter.assertEquals(
                "Cart total validated",
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

        CheckoutResult checkout = CheckoutFlow.payAndConfirm(token, QrTestHelper.paymentFromCart(cartRequest));
        OrderItem paid = checkout.findItemContaining(TestDataReader.getCustomExpectedNameContains());
        TestReporter.assertNotNull("Customization persisted in final order", paid);
        TestReporter.assertContains(
                "Final order includes selected size",
                paid == null ? "" : paid.getName(),
                TestDataReader.getCustomExpectedNameContains()
        );
        TestReporter.assertEquals(
                "Final line total",
                paid == null ? 0 : paid.getNetPrice(),
                TestDataReader.getCustomExpectedTotal(),
                0.01
        );
        pojo.response.oms.OmsOrderItem omsItem =
                checkout.findOmsItemContaining(TestDataReader.getCustomExpectedNameContains());
        TestReporter.assertNotNull("OMS customized item present", omsItem);
        TestReporter.assertContains(
                "OMS size customization",
                omsItem == null ? "" : omsItem.getName(),
                TestDataReader.getCustomExpectedNameContains()
        );
        CheckoutStats.customizationOrder();
        TestReporter.logOrderItems(checkout.confirmation().getData().getOrderItems());
        TestReporter.logTotals(checkout.confirmation().getData().getOrderItemsTotal());
        TestReporter.result("Customized product successfully ordered.");
    }

    @Test(groups = {"customization", "regression", "checkout"},
            description = "Validates that an add-on is ordered and the paid total includes it.")
    public void shouldCreateOrderWithAddonCustomization() {
        String token = QrTestHelper.freshSessionToken();
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

        TestReporter.section("BUSINESS FLOW");
        TestReporter.data("Flow", "Product → Add-on → Cart → Payment → Order");
        TestReporter.section("PRODUCT");
        TestReporter.data("Product ID", TestDataReader.getCustomAddonItemId());
        TestReporter.data("Product Name", TestReporter.displayItemName(item.getName()));
        TestReporter.data("Quantity", TestReporter.formatQuantity(item.getQuantity()));
        TestReporter.section("CART");
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());

        TestReporter.assertEquals("Product added", cartHttpResponse.statusCode(), 200);
        TestReporter.assertEquals(
                "Cart total validated",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getCustomAddonExpectedTotal(),
                0.01
        );

        CheckoutResult checkout = CheckoutFlow.payAndConfirm(token, QrTestHelper.paymentFromCart(cartRequest));
        TestReporter.assertEquals(
                "Final order total",
                checkout.confirmation().getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getCustomAddonExpectedTotal(),
                0.01
        );
        OrderItem paid = checkout.findItemContaining(TestReporter.displayItemName(item.getName()));
        if (paid == null && checkout.confirmation().getData().getOrderItems() != null
                && !checkout.confirmation().getData().getOrderItems().isEmpty()) {
            paid = checkout.confirmation().getData().getOrderItems().get(0);
        }
        TestReporter.assertNotNull("Add-on product persisted in final order", paid);
        TestReporter.assertEquals(
                "Final order net payable",
                checkout.confirmation().getData().getOrderItemsTotal().getNetPayableAmount(),
                cart.getData().getOrderItemsTotal().getNetPayableAmount(),
                0.01
        );
        pojo.response.oms.OmsOrderItem omsItem =
                checkout.findOmsItemContaining(TestReporter.displayItemName(item.getName()));
        TestReporter.assertNotNull("OMS add-on item present", omsItem);
        if (omsItem != null && omsItem.getCustomizations() != null && !omsItem.getCustomizations().isBlank()) {
            TestReporter.assertTrue(
                    "OMS add-on customization present",
                    !omsItem.getCustomizations().isBlank()
            );
        }
        CheckoutStats.customizationOrder();
        TestReporter.logOrderItems(checkout.confirmation().getData().getOrderItems());
        TestReporter.logTotals(checkout.confirmation().getData().getOrderItemsTotal());
        TestReporter.result("Add-on product successfully ordered.");
    }
}
