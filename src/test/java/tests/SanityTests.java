package tests;

import api.CartApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import pojo.response.QrMenuResponse;
import tests.support.QrTestHelper;

/**
 * Core business checks on the happy-flow menu and cart.
 */
public class SanityTests {

    @Test(groups = {"sanity", "regression"})
    public void qrMenuReturnsExpectedAccount() {
        Response menuResponse = QrTestHelper.menuResponse();
        QrMenuResponse menu = menuResponse.as(QrMenuResponse.class);

        Assert.assertEquals(
                menu.getData().getAccount().getId(),
                TestDataReader.getExpectedAccountId(),
                "Sanity: account id should match testdata"
        );
        Assert.assertEquals(
                menu.getData().getAccount().getName(),
                TestDataReader.getExpectedAccountName(),
                "Sanity: account name should match testdata"
        );
    }

    @Test(groups = {"sanity", "regression"})
    public void baselineCartHasBothItemsAndPendingTelr() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();
        Response cartHttpResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);

        Assert.assertEquals(cart.getData().getOrderItems().size(), 2, "Sanity: cart should have two items");

        OrderItem first = cart.getData().getOrderItems().get(0);
        OrderItem second = cart.getData().getOrderItems().get(1);

        Assert.assertEquals(String.valueOf(first.getItemObjectId()), TestDataReader.getItem1Id());
        Assert.assertEquals(first.getQuantity(), (double) TestDataReader.getItem1Quantity(), 0.0);
        Assert.assertEquals(String.valueOf(second.getItemObjectId()), TestDataReader.getItem2Id());
        Assert.assertEquals(second.getQuantity(), (double) TestDataReader.getItem2Quantity(), 0.0);

        Assert.assertEquals(
                cart.getData().getPaymentStatus(),
                TestDataReader.getExpectedPaymentStatus(),
                "Sanity: cart should still be payment_pending"
        );
        Assert.assertEquals(
                cart.getData().getPaymentProviderName(),
                TestDataReader.getExpectedPaymentProvider(),
                "Sanity: provider should be Telr"
        );
        Assert.assertEquals(
                cart.getData().getOrderItemsTotal().getOriginalTaxTotal(),
                TestDataReader.getExpectedTax(),
                0.01,
                "Sanity: tax should match baseline"
        );
        Assert.assertEquals(
                cart.getData().getOrderItemsTotal().getNetPayableAmount(),
                TestDataReader.getExpectedNetPayable(),
                0.01,
                "Sanity: net payable should match baseline"
        );
    }

    @Test(groups = {"sanity", "regression"})
    public void menuWithOptionsIncludesCustomizableItem() {
        Response menuResponse = QrTestHelper.menuWithOptionsResponse();

        Assert.assertEquals(menuResponse.statusCode(), 200);

        int customItemId = Integer.parseInt(TestDataReader.getCustomItemId());
        java.util.List<java.util.Map<String, Object>> items =
                menuResponse.jsonPath().getList("data.categories.items.flatten()");
        if (items == null || items.isEmpty()) {
            items = menuResponse.jsonPath().getList("data.categories.collectMany { it.items }");
        }
        Assert.assertNotNull(items, "Menu should return category items");

        java.util.Map<String, Object> customItem = null;
        for (java.util.Map<String, Object> item : items) {
            if (item != null && customItemId == ((Number) item.get("id")).intValue()) {
                customItem = item;
                break;
            }
        }
        Assert.assertNotNull(customItem, "Sanity: menu should contain the customization item");
        Assert.assertEquals(
                customItem.get("is_customizable"),
                true,
                "Sanity: Papaya Juice should be customizable on the menu"
        );
    }
}
