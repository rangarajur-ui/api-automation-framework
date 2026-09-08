package tests;

import api.CartApi;
import api.PaymentApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.request.PaymentRequest;
import pojo.response.CartResponse;
import pojo.response.PaymentResponse;
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

    @Test(dataProvider = "customers", groups = {"sanity", "regression"})
    public void placeOrderWithCustomerDetails(String name, String countryCode, String mobile) {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCartFor(name, countryCode, mobile);

        Response cartHttpResponse =
                new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        Assert.assertEquals(cartHttpResponse.statusCode(), 200, "Cart should accept customer " + name);

        CartResponse cart = cartHttpResponse.as(CartResponse.class);
        Assert.assertEquals(
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getExpectedTotalAmount(),
                0.01,
                "Items stay 10667+10668; customer change must not change the 20.0 total"
        );

        PaymentRequest paymentRequest = QrTestHelper.paymentFromCart(cartRequest);
        paymentRequest.setUserMobileNumber(QrTestHelper.paymentMobile(countryCode, mobile));

        Response paymentHttpResponse = new PaymentApi().initiatePayment(
                TestDataReader.getQrCode(),
                token,
                paymentRequest
        );
        Assert.assertEquals(paymentHttpResponse.statusCode(), 200, "Initiate payment should accept " + name);

        PaymentResponse payment = paymentHttpResponse.as(PaymentResponse.class);
        Assert.assertTrue(payment.getData().isSuccess(), "Payment session should start for " + name);
        Assert.assertNotNull(payment.getData().getOrderId());

        System.out.println(
                "Customer " + name
                        + " | " + countryCode + " " + mobile
                        + " | order_id " + payment.getData().getOrderId()
        );
    }

    @Test(groups = {"negative", "regression"})
    public void shortMobileIsRejected() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCartFor(
                "Short Mobile",
                TestDataReader.getUserCountryCode(),
                TestDataReader.getInvalidShortMobile()
        );

        Response cartHttpResponse =
                new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);

        Assert.assertEquals(cartHttpResponse.statusCode(), 422, "Short mobile should return HTTP 422");
        Assert.assertTrue(
                QrTestHelper.apiErrorMessage(cartHttpResponse).contains("couldn't process"),
                "Short mobile should be rejected"
        );
    }
}
