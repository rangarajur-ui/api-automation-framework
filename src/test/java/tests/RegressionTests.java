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
import tests.report.TestReporter;
import tests.support.QrTestHelper;

/**
 * API-only regression of the happy chain: menu → cart → initiate payment.
 * Telr hosted page stays in QrOrderFlowTest.
 */
public class RegressionTests {

    @Test(groups = {"regression"},
            description = "Validates that menu, cart totals, and Telr payment initiation stay aligned.")
    public void menuCartAndInitiatePaymentStayAligned() {
        String code = TestDataReader.getQrCode();
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();

        Response cartHttpResponse = new CartApi().viewCart(code, token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);

        PaymentRequest paymentRequest = QrTestHelper.paymentFromCart(cartRequest);
        Response paymentHttpResponse = new PaymentApi().initiatePayment(code, token, paymentRequest);
        PaymentResponse payment = paymentHttpResponse.as(PaymentResponse.class);

        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());
        TestReporter.logPaymentSummary(payment.getData());

        TestReporter.assertEquals("HTTP status validation", cartHttpResponse.statusCode(), 200);
        TestReporter.assertEquals(
                "Cart total",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getExpectedTotalAmount(),
                0.01
        );
        TestReporter.assertEquals(
                "Net payable",
                cart.getData().getOrderItemsTotal().getNetPayableAmount(),
                TestDataReader.getExpectedNetPayable(),
                0.01
        );
        TestReporter.assertEquals(
                "Payment status",
                cart.getData().getPaymentStatus(),
                TestDataReader.getExpectedPaymentStatus()
        );
        TestReporter.assertEquals("Initiate payment HTTP status", paymentHttpResponse.statusCode(), 200);
        TestReporter.assertTrue("Payment session created", payment.getData().isSuccess());
        TestReporter.assertEquals(
                "Payment provider",
                payment.getData().getPgName(),
                TestDataReader.getExpectedPaymentProvider()
        );
        TestReporter.assertNotNull("Order ID generated", payment.getData().getOrderId());
        TestReporter.assertTrue(
                "Order ID is a positive number",
                payment.getData().getOrderId() != null && payment.getData().getOrderId() > 0
        );
        TestReporter.result("Menu, cart, and payment initiation stayed aligned.");
    }
}
