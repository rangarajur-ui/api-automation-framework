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
 * Extra cart cases on the happy-flow APIs. No Telr.
 * This menu has no combo items, so combo is not added.
 */
public class CartVariantTests {

    @Test(groups = {"sanity", "regression"},
            description = "Validates cart totals when only the first baseline product is added.")
    public void singleItemCartUsesItem1Total() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.guestCart();
        cartRequest.addItem(
                TestDataReader.getItem1Id(),
                TestDataReader.getItem1Price(),
                TestDataReader.getItem1Quantity()
        );

        Response response = viewCart(token, cartRequest);
        CartResponse cart = response.as(CartResponse.class);
        OrderItem item = cart.getData().getOrderItems().get(0);

        TestReporter.data("Product ID", item.getItemObjectId());
        TestReporter.data("Product Name", item.getName());
        TestReporter.data("Quantity", TestReporter.formatQuantity(item.getQuantity()));
        TestReporter.logCartSummary(cart, response.statusCode());

        TestReporter.assertEquals("HTTP status validation", response.statusCode(), 200);
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
        TestReporter.result("Single-item cart total validated.");
    }

    @Test(groups = {"sanity", "regression"},
            description = "Validates that raising quantity increases the cart total.")
    public void increasingItem1QuantityIncreasesTotal() {
        String token = QrTestHelper.newSessionToken();

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

        TestReporter.data("Product ID", item.getItemObjectId());
        TestReporter.data("Product Name", item.getName());
        TestReporter.data("Quantity", TestReporter.formatQuantity(item.getQuantity()));
        TestReporter.money("Previous Total", firstTotal);
        TestReporter.logCartSummary(second, secondResponse.statusCode());

        TestReporter.assertEquals("HTTP status validation", firstResponse.statusCode(), 200);
        TestReporter.assertEquals("HTTP status validation", secondResponse.statusCode(), 200);
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
        TestReporter.result("Quantity change increased the cart total.");
    }

    @Test(groups = {"sanity", "regression"},
            description = "Validates baseline products plus a size customization in one cart.")
    public void baselineItemsPlusSizeCustomization() {
        String token = QrTestHelper.newSessionToken();
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

        TestReporter.logCartSummary(cart, response.statusCode());
        TestReporter.logOrderItems(cart.getData().getOrderItems());

        TestReporter.assertEquals("HTTP status validation", response.statusCode(), 200);
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
        TestReporter.result("Combined cart totals validated.");
    }

    private Response viewCart(String token, CartRequest cartRequest) {
        return new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
    }
}
