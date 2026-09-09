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
 * Cart variants that represent a real order, completed through Telr.
 */
public class CartVariantTests {

    @Test(groups = {"sanity", "regression", "checkout"},
            description = "Validates a single-item cart is paid and accepted as an order.")
    public void shouldCreateOrderWithSingleItem() {
        String token = QrTestHelper.freshSessionToken();
        CartRequest cartRequest = QrTestHelper.guestCart();
        cartRequest.addItem(
                TestDataReader.getItem1Id(),
                TestDataReader.getItem1Price(),
                TestDataReader.getItem1Quantity()
        );

        Response response = viewCart(token, cartRequest);
        CartResponse cart = response.as(CartResponse.class);
        OrderItem item = cart.getData().getOrderItems().get(0);

        TestReporter.section("BUSINESS FLOW");
        TestReporter.data("Flow", "Single product → Cart → Payment → Order");
        TestReporter.data("Product ID", item.getItemObjectId());
        TestReporter.data("Product Name", TestReporter.displayItemName(item.getName()));
        TestReporter.data("Quantity", TestReporter.formatQuantity(item.getQuantity()));
        TestReporter.section("CART");
        TestReporter.logCartSummary(cart, response.statusCode());

        TestReporter.assertEquals("Cart created", response.statusCode(), 200);
        TestReporter.assertEquals("Expected item count", cart.getData().getOrderItems().size(), 1);
        TestReporter.assertEquals(
                "Product ID",
                String.valueOf(item.getItemObjectId()),
                TestDataReader.getItem1Id()
        );
        TestReporter.assertEquals(
                "Cart total",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getItem1OnlyExpectedTotal(),
                0.01
        );

        CheckoutResult checkout = CheckoutFlow.payAndConfirm(token, QrTestHelper.paymentFromCart(cartRequest));
        CheckoutFlow.assertOrderMatchesCart(cart, checkout.confirmation());
        TestReporter.logOrderItems(checkout.confirmation().getData().getOrderItems());
        TestReporter.logTotals(checkout.confirmation().getData().getOrderItemsTotal());
        TestReporter.result("Single-item order created and accepted.");
    }

    @Test(groups = {"sanity", "regression", "checkout"},
            description = "Validates that raising quantity is paid and appears on the accepted order.")
    public void shouldCreateOrderAfterIncreasingQuantity() {
        String token = QrTestHelper.freshSessionToken();

        CartRequest qtyTwo = QrTestHelper.guestCart();
        qtyTwo.addItem(
                TestDataReader.getItem1Id(),
                TestDataReader.getItem1Price(),
                TestDataReader.getItem1Quantity()
        );
        Response firstResponse = viewCart(token, qtyTwo);
        CartResponse first = firstResponse.as(CartResponse.class);
        double firstTotal = first.getData().getOrderItemsTotal().getTotalAmount();

        CartRequest qtyThree = QrTestHelper.guestCart();
        qtyThree.addItem(
                TestDataReader.getItem1Id(),
                TestDataReader.getItem1Price(),
                TestDataReader.getItem1Qty3()
        );
        Response secondResponse = viewCart(token, qtyThree);
        CartResponse second = secondResponse.as(CartResponse.class);
        OrderItem item = second.getData().getOrderItems().get(0);
        double secondTotal = second.getData().getOrderItemsTotal().getTotalAmount();

        TestReporter.section("BUSINESS FLOW");
        TestReporter.data("Flow", "Increase quantity → Cart → Payment → Order");
        TestReporter.data("Product ID", item.getItemObjectId());
        TestReporter.data("Product Name", TestReporter.displayItemName(item.getName()));
        TestReporter.data("Quantity", TestReporter.formatQuantity(item.getQuantity()));
        TestReporter.money("Previous Total", firstTotal);
        TestReporter.section("CART");
        TestReporter.logCartSummary(second, secondResponse.statusCode());

        TestReporter.assertEquals("Cart created", firstResponse.statusCode(), 200);
        TestReporter.assertEquals("Updated cart created", secondResponse.statusCode(), 200);
        TestReporter.assertEquals(
                "Quantity 2 cart total",
                firstTotal,
                TestDataReader.getItem1OnlyExpectedTotal(),
                0.01
        );
        TestReporter.assertEqualsRaw(
                "Updated quantity",
                item.getQuantity(),
                TestDataReader.getItem1Qty3(),
                0.0
        );
        TestReporter.assertEquals(
                "Quantity 3 cart total",
                secondTotal,
                TestDataReader.getItem1Qty3ExpectedTotal(),
                0.01
        );
        TestReporter.assertTrue(
                "Raising quantity increases cart total",
                secondTotal > firstTotal
        );

        CheckoutResult checkout = CheckoutFlow.payAndConfirm(token, QrTestHelper.paymentFromCart(qtyThree));
        CheckoutFlow.assertOrderMatchesCart(second, checkout.confirmation());
        OrderItem paid = checkout.findItemByName(item.getName());
        TestReporter.assertEqualsRaw(
                "Final quantity",
                paid == null ? 0 : paid.getQuantity(),
                TestDataReader.getItem1Qty3(),
                0.0
        );
        TestReporter.logOrderItems(checkout.confirmation().getData().getOrderItems());
        TestReporter.logTotals(checkout.confirmation().getData().getOrderItemsTotal());
        TestReporter.result("Quantity change was paid and persisted on the order.");
    }

    @Test(groups = {"sanity", "regression", "checkout"},
            description = "Validates baseline products plus a size customization are paid and accepted.")
    public void shouldCreateOrderWithBaselineAndSizeCustomization() {
        String token = QrTestHelper.freshSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();
        cartRequest.addCustomizedItem(
                TestDataReader.getCustomItemId(),
                TestDataReader.getCustomItemPrice(),
                TestDataReader.getCustomItemQuantity(),
                TestDataReader.getCustomOptionId(),
                TestDataReader.getCustomOptionPrice(),
                TestDataReader.getCustomOptionMappingId()
        );

        Response response = viewCart(token, cartRequest);
        CartResponse cart = response.as(CartResponse.class);

        TestReporter.section("BUSINESS FLOW");
        TestReporter.data("Flow", "Baseline products → Size customization → Payment → Order");
        TestReporter.section("CART");
        TestReporter.logCartSummary(cart, response.statusCode());
        TestReporter.logOrderItems(cart.getData().getOrderItems());

        TestReporter.assertEquals("Cart created", response.statusCode(), 200);
        TestReporter.assertEquals("Expected item count", cart.getData().getOrderItems().size(), 3);
        TestReporter.assertEquals(
                "Cart total",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getBaselinePlusCustomExpectedTotal(),
                0.01
        );

        OrderItem juice = null;
        for (OrderItem item : cart.getData().getOrderItems()) {
            if (item.getName() != null
                    && item.getName().contains(TestDataReader.getCustomExpectedNameContains())) {
                juice = item;
                break;
            }
        }
        TestReporter.assertNotNull("Customizable juice is in the cart", juice);
        TestReporter.assertEquals(
                "Juice line total",
                juice == null ? 0 : juice.getNetPrice(),
                TestDataReader.getCustomExpectedTotal(),
                0.01
        );

        CheckoutResult checkout = CheckoutFlow.payAndConfirm(token, QrTestHelper.paymentFromCart(cartRequest));
        CheckoutFlow.assertOrderMatchesCart(cart, checkout.confirmation());
        OrderItem paidJuice = checkout.findItemContaining(TestDataReader.getCustomExpectedNameContains());
        TestReporter.assertNotNull("Customization persisted in final order", paidJuice);
        CheckoutStats.customizationOrder();
        TestReporter.logTotals(checkout.confirmation().getData().getOrderItemsTotal());
        TestReporter.result("Combined cart was paid and accepted.");
    }

    private Response viewCart(String token, CartRequest cartRequest) {
        return new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
    }
}
