package tests;

import api.CartApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import tests.support.QrTestHelper;

/**
 * Extra cart cases on the happy-flow APIs. No Telr.
 * This menu has no combo items, so combo is not added.
 */
public class CartVariantTests {

    @Test(groups = {"sanity", "regression"})
    public void singleItemCartUsesItem1Total() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.guestCart();
        cartRequest.addItem(
                TestDataReader.getItem1Id(),
                TestDataReader.getItem1Price(),
                TestDataReader.getItem1Quantity()
        );

        CartResponse cart = viewCart(token, cartRequest);

        Assert.assertEquals(cart.getData().getOrderItems().size(), 1, "Cart should have only item 1");
        Assert.assertEquals(
                String.valueOf(cart.getData().getOrderItems().get(0).getItemObjectId()),
                TestDataReader.getItem1Id()
        );
        Assert.assertEquals(
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getItem1OnlyExpectedTotal(),
                0.01,
                "Item 1 qty 2 should total 16.0"
        );
    }

    @Test(groups = {"sanity", "regression"})
    public void increasingItem1QuantityIncreasesTotal() {
        String token = QrTestHelper.newSessionToken();

        CartRequest qtyTwo = QrTestHelper.guestCart();
        qtyTwo.addItem(
                TestDataReader.getItem1Id(),
                TestDataReader.getItem1Price(),
                TestDataReader.getItem1Quantity()
        );
        CartResponse first = viewCart(token, qtyTwo);
        Assert.assertEquals(
                first.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getItem1OnlyExpectedTotal(),
                0.01
        );

        CartRequest qtyThree = QrTestHelper.guestCart();
        qtyThree.addItem(
                TestDataReader.getItem1Id(),
                TestDataReader.getItem1Price(),
                TestDataReader.getItem1Qty3()
        );
        CartResponse second = viewCart(token, qtyThree);

        Assert.assertEquals(
                second.getData().getOrderItems().get(0).getQuantity(),
                (double) TestDataReader.getItem1Qty3(),
                0.0
        );
        Assert.assertEquals(
                second.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getItem1Qty3ExpectedTotal(),
                0.01,
                "Item 1 qty 3 should total 24.0"
        );
        Assert.assertTrue(
                second.getData().getOrderItemsTotal().getTotalAmount()
                        > first.getData().getOrderItemsTotal().getTotalAmount(),
                "Raising quantity should raise the cart total"
        );
    }

    @Test(groups = {"sanity", "regression"})
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

        CartResponse cart = viewCart(token, cartRequest);

        Assert.assertEquals(cart.getData().getOrderItems().size(), 3, "Baseline plus juice should be 3 lines");
        Assert.assertEquals(
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getBaselinePlusCustomExpectedTotal(),
                0.01,
                "10667 + 10668 + juice 2 pieces should total 29.0"
        );

        OrderItem juice = null;
        for (OrderItem item : cart.getData().getOrderItems()) {
            if (item.getName() != null
                    && item.getName().contains(TestDataReader.getCustomExpectedNameContains())) {
                juice = item;
                break;
            }
        }
        Assert.assertNotNull(juice, "Combined cart should include the 2 pieces juice");
        Assert.assertEquals(
                juice.getNetPrice(),
                TestDataReader.getCustomExpectedTotal(),
                0.01
        );
    }

    private CartResponse viewCart(String token, CartRequest cartRequest) {
        Response response = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        Assert.assertEquals(response.statusCode(), 200, "Cart API should return HTTP 200");
        return response.as(CartResponse.class);
    }
}
