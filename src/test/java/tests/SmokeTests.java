package tests;

import api.CartApi;
import api.PaymentApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.request.PaymentRequest;
import pojo.response.CartResponse;
import pojo.response.PaymentResponse;
import pojo.response.QrMenuResponse;
import tests.report.TestReporter;
import tests.support.QrTestHelper;

/**
 * Fast happy-path checks. No Telr browser step.
 */
public class SmokeTests {

    @Test(groups = {"smoke", "regression"},
            description = "Validates that a dine-in QR menu session can be opened.")
    public void qrMenuReturnsSessionToken() {
        Response menuResponse = QrTestHelper.menuResponse();
        QrMenuResponse menu = menuResponse.as(QrMenuResponse.class);
        String token = menu.getData().getToken();

        TestReporter.data("HTTP Status", menuResponse.statusCode());
        TestReporter.data("Session Token", token == null || token.isBlank() ? "missing" : "present");

        TestReporter.assertEquals("HTTP status validation", menuResponse.statusCode(), 200);
        TestReporter.assertNotNull("Session token is present", token);
        TestReporter.assertTrue("Session token is not blank", !token.isBlank());
        TestReporter.result("QR menu session opened successfully.");
    }

    @Test(groups = {"smoke", "regression"},
            description = "Validates that the baseline two-item cart totals 20.00.")
    public void baselineCartReturnsExpectedTotal() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();
        Response cartResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartResponse.as(CartResponse.class);

        TestReporter.logCartSummary(cart, cartResponse.statusCode());

        TestReporter.assertEquals("HTTP status validation", cartResponse.statusCode(), 200);
        TestReporter.assertEquals(
                "Cart total",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getExpectedTotalAmount(),
                0.01
        );
        TestReporter.result("Baseline cart total validated.");
    }

    @Test(groups = {"smoke", "regression"},
            description = "Validates that payment can be initiated with Telr as the provider.")
    public void initiatePaymentReturnsTelrUrl() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();
        PaymentRequest paymentRequest = QrTestHelper.paymentFromCart(cartRequest);

        Response paymentResponse = new PaymentApi().initiatePayment(
                TestDataReader.getQrCode(),
                token,
                paymentRequest
        );
        PaymentResponse payment = paymentResponse.as(PaymentResponse.class);
        String paymentUrl = payment.getData().getPaymentUrl();

        TestReporter.data("HTTP Status", paymentResponse.statusCode());
        TestReporter.data("Payment Success", payment.getData().isSuccess());
        TestReporter.data("Payment URL", paymentUrl == null || paymentUrl.isBlank() ? "missing" : "present");
        if (payment.getData().getOrderId() != null) {
            TestReporter.data("Order ID", payment.getData().getOrderId());
        }

        TestReporter.assertEquals("HTTP status validation", paymentResponse.statusCode(), 200);
        TestReporter.assertTrue("Payment session created", payment.getData().isSuccess());
        TestReporter.assertNotNull("Telr payment URL is present", paymentUrl);
        TestReporter.assertTrue(
                "Payment URL points at Telr",
                paymentUrl != null && paymentUrl.startsWith("https://secure.telr.com")
        );
        TestReporter.result("Telr payment session created.");
    }
}
