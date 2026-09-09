package tests;

import api.CartApi;
import api.PaymentApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.request.PaymentRequest;
import pojo.response.CartResponse;
import pojo.response.PaymentResponse;
import tests.report.TestReporter;
import tests.support.QrTestHelper;

/**
 * Same baseline items, different guest name / country code / mobile.
 * One method, three rows — TestNG runs them as separate results.
 * Stops at initiate_payment (no Telr). Cart does not echo the name back.
 */
public class CustomerDetailsTests {

    @DataProvider(name = "customers")
    public Object[][] customers() {
        return TestDataReader.customerVariants();
    }

    @Test(dataProvider = "customers", groups = {"sanity", "regression"},
            description = "Validates that guest name and mobile variants can start payment without changing item totals.")
    public void placeOrderWithCustomerDetails(String name, String countryCode, String mobile) {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCartFor(name, countryCode, mobile);

        Response cartHttpResponse =
                new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);

        PaymentRequest paymentRequest = QrTestHelper.paymentFromCart(cartRequest);
        paymentRequest.setUserMobileNumber(QrTestHelper.paymentMobile(countryCode, mobile));

        Response paymentHttpResponse = new PaymentApi().initiatePayment(
                TestDataReader.getQrCode(),
                token,
                paymentRequest
        );
        PaymentResponse payment = paymentHttpResponse.as(PaymentResponse.class);

        TestReporter.data("Customer Name", name);
        TestReporter.data("Country Code", countryCode);
        TestReporter.data("Mobile", TestReporter.maskedMobile(mobile));
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());
        TestReporter.data("Payment HTTP Status", paymentHttpResponse.statusCode());
        TestReporter.data("Payment Success", payment.getData().isSuccess());
        if (payment.getData().getOrderId() != null) {
            TestReporter.data("Order ID", payment.getData().getOrderId());
        }

        TestReporter.assertEquals("HTTP status validation", cartHttpResponse.statusCode(), 200);
        TestReporter.assertEquals(
                "Cart total unchanged by customer details",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getExpectedTotalAmount(),
                0.01
        );
        TestReporter.assertEquals("Initiate payment HTTP status", paymentHttpResponse.statusCode(), 200);
        TestReporter.assertTrue("Payment session created", payment.getData().isSuccess());
        TestReporter.assertNotNull("Order ID generated", payment.getData().getOrderId());
        TestReporter.result("Customer details accepted and payment session created.");
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
