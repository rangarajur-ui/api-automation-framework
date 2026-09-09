package tests;

import api.CartApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.request.PaymentRequest;
import pojo.response.CartResponse;
import tests.report.TestReporter;
import tests.support.CheckoutFlow;
import tests.support.CheckoutResult;
import tests.support.QrTestHelper;

/**
 * Guest name / country / mobile variants must complete payment and create an order.
 * Cart does not echo the guest name; only assert it on the final order if the API returns it.
 */
public class CustomerDetailsTests {

    @DataProvider(name = "customers")
    public Object[][] customers() {
        return TestDataReader.customerVariants();
    }

    @Test(dataProvider = "customers", groups = {"sanity", "regression", "checkout"},
            description = "Validates that guest details can complete Telr payment and create an accepted order.")
    public void shouldCreateOrderWithCustomerDetails(String name, String countryCode, String mobile) {
        String token = QrTestHelper.freshSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCartFor(name, countryCode, mobile);

        Response cartHttpResponse =
                new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);

        TestReporter.section("BUSINESS FLOW");
        TestReporter.data("Flow", "Customer details → Cart → Payment → Order");
        TestReporter.data("Customer Name", name);
        TestReporter.data("Country Code", countryCode);
        TestReporter.data("Mobile", TestReporter.maskedMobile(mobile));
        TestReporter.section("CART");
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());

        TestReporter.assertEquals("Cart created", cartHttpResponse.statusCode(), 200);
        TestReporter.assertEquals(
                "Cart total unchanged by customer details",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getExpectedTotalAmount(),
                0.01
        );

        PaymentRequest paymentRequest = QrTestHelper.paymentFromCart(cartRequest);
        paymentRequest.setUserName(name);
        paymentRequest.setUserCountryCode(countryCode);
        paymentRequest.setUserMobileNumber(QrTestHelper.paymentMobile(countryCode, mobile));

        CheckoutResult checkout = CheckoutFlow.payAndConfirm(token, paymentRequest);
        CheckoutFlow.assertOrderMatchesCart(cart, checkout.confirmation());
        CheckoutFlow.assertOmsCustomer(checkout, name);

        TestReporter.logOrderItems(checkout.confirmation().getData().getOrderItems());
        TestReporter.logTotals(checkout.confirmation().getData().getOrderItemsTotal());
        TestReporter.result("Customer details accepted and order was created and accepted.");
    }

    @Test(groups = {"negative", "regression"},
            description = "Validates that a mobile number below the accepted length is rejected.")
    public void shortMobileIsRejected() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCartFor(
                "Short Mobile",
                TestDataReader.getUserCountryCode(),
                TestDataReader.getInvalidShortMobile()
        );

        Response cartHttpResponse =
                new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        String actualMessage = QrTestHelper.apiErrorMessage(cartHttpResponse);

        TestReporter.logNegative(
                "Cart request with a short mobile number",
                "Mobile shorter than accepted length",
                422,
                cartHttpResponse.statusCode(),
                "couldn't process",
                actualMessage
        );

        TestReporter.assertEquals("HTTP status validation", cartHttpResponse.statusCode(), 422);
        TestReporter.assertContains("Error response validation", actualMessage, "couldn't process");
        TestReporter.result("Short mobile number was rejected.");
    }
}
