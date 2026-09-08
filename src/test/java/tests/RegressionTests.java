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
import tests.support.QrTestHelper;

/**
 * API-only regression of the happy chain: menu → cart → initiate payment.
 * Telr hosted page stays in QrOrderFlowTest.
 */
public class RegressionTests {

    @Test(groups = {"regression"})
    public void menuCartAndInitiatePaymentStayAligned() {
        String code = TestDataReader.getQrCode();
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();

        Response cartHttpResponse = new CartApi().viewCart(code, token, cartRequest);
        Assert.assertEquals(cartHttpResponse.statusCode(), 200);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);

        Assert.assertEquals(
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getExpectedTotalAmount(),
                0.01
        );
        Assert.assertEquals(
                cart.getData().getOrderItemsTotal().getNetPayableAmount(),
                TestDataReader.getExpectedNetPayable(),
                0.01
        );
        Assert.assertEquals(
                cart.getData().getPaymentStatus(),
                TestDataReader.getExpectedPaymentStatus()
        );

        PaymentRequest paymentRequest = QrTestHelper.paymentFromCart(cartRequest);
        Response paymentHttpResponse = new PaymentApi().initiatePayment(code, token, paymentRequest);
        Assert.assertEquals(paymentHttpResponse.statusCode(), 200);

        PaymentResponse payment = paymentHttpResponse.as(PaymentResponse.class);
        Assert.assertTrue(payment.getData().isSuccess());
        Assert.assertEquals(
                payment.getData().getPgName(),
                TestDataReader.getExpectedPaymentProvider()
        );
        Assert.assertNotNull(payment.getData().getOrderId());
        Assert.assertTrue(payment.getData().getOrderId() > 0);

        System.out.println("Regression order_id : " + payment.getData().getOrderId());
    }
}
