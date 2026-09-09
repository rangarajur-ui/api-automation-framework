package tests;

import api.CartApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.response.CartResponse;
import tests.report.TestReporter;
import tests.support.CheckoutFlow;
import tests.support.CheckoutResult;
import tests.support.QrTestHelper;

/**
 * Baseline menu → cart → Telr payment → accepted order.
 */
public class RegressionTests {

    @Test(groups = {"regression", "checkout"},
            description = "Validates menu, cart, Telr payment, and an accepted order stay aligned.")
    public void shouldCreateOrderFromMenuCartAndPayment() {
        String token = QrTestHelper.freshSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();

        Response cartHttpResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);

        TestReporter.section("BUSINESS FLOW");
        TestReporter.data("Flow", "Menu → Cart → Telr Payment → Order");
        TestReporter.section("CART");
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());

        TestReporter.assertEquals("Cart created", cartHttpResponse.statusCode(), 200);
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
                "Cart payment status",
                cart.getData().getPaymentStatus(),
                TestDataReader.getExpectedPaymentStatus()
        );

        CheckoutResult checkout = CheckoutFlow.payAndConfirm(token, QrTestHelper.paymentFromCart(cartRequest));
        CheckoutFlow.assertOrderMatchesCart(cart, checkout.confirmation());
        TestReporter.logOrderItems(checkout.confirmation().getData().getOrderItems());
        TestReporter.logTotals(checkout.confirmation().getData().getOrderItemsTotal());
        TestReporter.result("Payment completed successfully and order was created and accepted.");
    }
}
