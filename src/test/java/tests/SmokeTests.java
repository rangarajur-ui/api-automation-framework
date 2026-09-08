package tests;

import api.CartApi;
import api.PaymentApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.request.PaymentRequest;
import pojo.response.CartResponse;
import pojo.response.PaymentResponse;
import pojo.response.QrMenuResponse;
import tests.support.QrTestHelper;

/**
 * Fast happy-path checks. No Telr browser step.
 */
public class SmokeTests {

    @Test(groups = {"smoke", "regression"})
    public void qrMenuReturnsSessionToken() {
        Response menuResponse = QrTestHelper.menuResponse();

        Assert.assertEquals(menuResponse.statusCode(), 200, "QR Menu API should return HTTP 200");
        QrMenuResponse menu = menuResponse.as(QrMenuResponse.class);
        Assert.assertNotNull(menu.getData().getToken(), "Smoke: session token must be present");
        Assert.assertFalse(menu.getData().getToken().isBlank(), "Smoke: session token must not be blank");
    }

    @Test(groups = {"smoke", "regression"})
    public void baselineCartReturnsExpectedTotal() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();

        Response cartResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);

        Assert.assertEquals(cartResponse.statusCode(), 200, "Cart API should return HTTP 200");
        CartResponse cart = cartResponse.as(CartResponse.class);
        Assert.assertEquals(
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getExpectedTotalAmount(),
                0.01,
                "Smoke: baseline cart total should be 20.0"
        );
    }

    @Test(groups = {"smoke", "regression"})
    public void initiatePaymentReturnsTelrUrl() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();
        PaymentRequest paymentRequest = QrTestHelper.paymentFromCart(cartRequest);

        Response paymentResponse = new PaymentApi().initiatePayment(
                TestDataReader.getQrCode(),
                token,
                paymentRequest
        );

        Assert.assertEquals(paymentResponse.statusCode(), 200, "Initiate Payment API should return HTTP 200");
        PaymentResponse payment = paymentResponse.as(PaymentResponse.class);
        Assert.assertTrue(payment.getData().isSuccess(), "Smoke: initiate payment should succeed");
        Assert.assertNotNull(payment.getData().getPaymentUrl(), "Smoke: Telr payment_url must be present");
        Assert.assertTrue(
                payment.getData().getPaymentUrl().startsWith("https://secure.telr.com"),
                "Smoke: payment_url should point at Telr"
        );
    }
}
