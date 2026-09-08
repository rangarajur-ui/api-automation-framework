package tests.support;

import api.QrMenuApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.Assert;
import pojo.request.CartRequest;
import pojo.request.PaymentRequest;
import pojo.response.QrMenuResponse;

/**
 * Shared setup for the new suite classes. QrOrderFlowTest does not use this.
 */
public class QrTestHelper {

    private static Response cachedMenu;
    private static Response cachedMenuWithOptions;
    private static long cachedAtMillis;

    public static Response menuResponse() {
        cachedMenu = fetchMenu(true, cachedMenu);
        return cachedMenu;
    }

    public static Response menuWithOptionsResponse() {
        cachedMenuWithOptions = fetchMenu(false, cachedMenuWithOptions);
        return cachedMenuWithOptions;
    }

    public static String newSessionToken() {
        QrMenuResponse menu = menuResponse().as(QrMenuResponse.class);
        String token = menu.getData().getToken();
        Assert.assertNotNull(token, "QR Menu should return a session token");
        Assert.assertFalse(token.isBlank(), "Session token should not be blank");
        return token;
    }

    private static Response fetchMenu(boolean skipItemOptions, Response existing) {
        long now = System.currentTimeMillis();
        if (existing != null && now - cachedAtMillis < 120_000L && existing.statusCode() == 200) {
            return existing;
        }

        Response menuResponse = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            menuResponse = new QrMenuApi().getQrMenuDetails(TestDataReader.getQrCode(), skipItemOptions);
            if (menuResponse.statusCode() != 429) {
                break;
            }
            try {
                Thread.sleep(3000L * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.assertEquals(menuResponse.statusCode(), 200, "QR Menu API should return HTTP 200");
        cachedAtMillis = now;
        return menuResponse;
    }

    public static CartRequest baselineCart() {
        CartRequest cartRequest = guestCart();
        cartRequest.addItem(
                TestDataReader.getItem1Id(),
                TestDataReader.getItem1Price(),
                TestDataReader.getItem1Quantity()
        );
        cartRequest.addItem(
                TestDataReader.getItem2Id(),
                TestDataReader.getItem2Price(),
                TestDataReader.getItem2Quantity()
        );
        return cartRequest;
    }

    public static CartRequest guestCart() {
        CartRequest cartRequest = new CartRequest();
        cartRequest.setCode(TestDataReader.getQrCode());
        cartRequest.setOrderSource(TestDataReader.getOrderSource());
        cartRequest.setDeliveryType(TestDataReader.getDeliveryType());
        cartRequest.setOrderMode(TestDataReader.getOrderMode());
        cartRequest.setUserName(TestDataReader.getUserName());
        cartRequest.setUserMobileNumber(TestDataReader.getUserMobile());
        cartRequest.setUserCountryCode(TestDataReader.getUserCountryCode());
        return cartRequest;
    }

    public static CartRequest baselineCartFor(String name, String countryCode, String mobile) {
        CartRequest cartRequest = baselineCart();
        cartRequest.setUserName(name);
        cartRequest.setUserCountryCode(countryCode);
        cartRequest.setUserMobileNumber(mobile);
        return cartRequest;
    }

    public static String paymentMobile(String countryCode, String mobile) {
        return countryCode + mobile;
    }

    public static PaymentRequest paymentFromCart(CartRequest cartRequest) {
        PaymentRequest paymentRequest = PaymentRequest.fromCart(cartRequest);
        paymentRequest.setPayMode(TestDataReader.getPayMode());
        paymentRequest.setTablePreferenceValue(TestDataReader.getTablePreferenceValue());
        paymentRequest.setOrderType(TestDataReader.getOrderType());
        paymentRequest.setUserMobileNumber(TestDataReader.getPaymentUserMobile());
        return paymentRequest;
    }

    public static String apiErrorMessage(Response response) {
        return response.jsonPath().getString("errors.base[0].message");
    }
}
