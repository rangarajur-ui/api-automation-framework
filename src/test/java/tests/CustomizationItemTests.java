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
 * Add a customization / add-on using the live nested-table format.
 * Does not change the 10667 + 10668 baseline cart.
 */
public class CustomizationItemTests {

    @Test(groups = {"customization", "regression"})
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
        Assert.assertEquals(cartHttpResponse.statusCode(), 200, "Customized cart should return HTTP 200");

        CartResponse cart = cartHttpResponse.as(CartResponse.class);
        OrderItem item = cart.getData().getOrderItems().get(0);

        System.out.println("Custom item : " + item.getName());
        System.out.println("Custom total: " + cart.getData().getOrderItemsTotal().getTotalAmount());

        Assert.assertTrue(
                item.getName().contains(TestDataReader.getCustomExpectedNameContains()),
                "Item name should include the selected size (2 pieces)"
        );
        Assert.assertEquals(
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getCustomExpectedTotal(),
                0.01,
                "Papaya Juice 2 pieces should total 9.0"
        );
        Assert.assertEquals(
                item.getNetPrice(),
                TestDataReader.getCustomExpectedTotal(),
                0.01,
                "Line net should match the selected size price"
        );
    }

    @Test(groups = {"customization", "regression"})
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
        Assert.assertEquals(cartHttpResponse.statusCode(), 200, "Addon cart should return HTTP 200");

        CartResponse cart = cartHttpResponse.as(CartResponse.class);
        double total = cart.getData().getOrderItemsTotal().getTotalAmount();

        System.out.println("Addon item : " + cart.getData().getOrderItems().get(0).getName());
        System.out.println("Addon total: " + total);

        Assert.assertEquals(
                total,
                TestDataReader.getCustomAddonExpectedTotal(),
                0.01,
                "Biryani + Salan Dip should total 48.0"
        );
    }
}
