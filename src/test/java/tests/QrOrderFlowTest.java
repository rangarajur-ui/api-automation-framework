package tests;

import api.CartApi;
import api.QrMenuApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import pojo.response.QrMenuResponse;
import tests.report.TestReporter;
import tests.support.CheckoutFlow;
import tests.support.CheckoutResult;
import tests.support.QrTestHelper;

public class QrOrderFlowTest {

    @Test(groups = {"e2e", "checkout"},
            description = "Validates the dine-in journey from menu through Telr payment to an accepted order.")
    public void shouldCreateAcceptedOrderAfterTelrPayment() {
        String code = TestDataReader.getQrCode();
        Response menuResponse = new QrMenuApi().getQrMenuDetails(code);
        QrMenuResponse menu = menuResponse.as(QrMenuResponse.class);

        int accountId = menu.getData().getAccount().getId();
        String accountName = menu.getData().getAccount().getName();
        String sessionToken = menu.getData().getToken();

        TestReporter.section("BUSINESS FLOW");
        TestReporter.data("Flow", "Menu → Cart → Customer → Telr Payment → Order");
        TestReporter.logAccount(accountId, accountName);
        TestReporter.data("Menu HTTP Status", menuResponse.statusCode());
        TestReporter.data("Session Token", sessionToken == null || sessionToken.isBlank() ? "missing" : "present");

        TestReporter.assertEquals("HTTP status validation", menuResponse.statusCode(), 200);
        TestReporter.assertEquals("Account is valid", accountId, TestDataReader.getExpectedAccountId());
        TestReporter.assertEquals("Account name", accountName, TestDataReader.getExpectedAccountName());
        TestReporter.assertNotNull("Session token is present", sessionToken);
        TestReporter.assertTrue("Session token is not blank", sessionToken != null && !sessionToken.isBlank());

        CartRequest cartRequest = new CartRequest();
        cartRequest.setCode(code);
        cartRequest.setOrderSource(TestDataReader.getOrderSource());
        cartRequest.setDeliveryType(TestDataReader.getDeliveryType());
        cartRequest.setOrderMode(TestDataReader.getOrderMode());
        cartRequest.setUserName(TestDataReader.getUserName());
        cartRequest.setUserMobileNumber(TestDataReader.getUserMobile());
        cartRequest.setUserCountryCode(TestDataReader.getUserCountryCode());
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

        Response cartHttpResponse = new CartApi().viewCart(code, sessionToken, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);

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
                "Tax",
                cart.getData().getOrderItemsTotal().getOriginalTaxTotal(),
                TestDataReader.getExpectedTax(),
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
        TestReporter.assertEquals(
                "Payment provider",
                cart.getData().getPaymentProviderName(),
                TestDataReader.getExpectedPaymentProvider()
        );
        TestReporter.assertEquals("Expected item count", cart.getData().getOrderItems().size(), 2);

        OrderItem firstItem = cart.getData().getOrderItems().get(0);
        OrderItem secondItem = cart.getData().getOrderItems().get(1);
        TestReporter.assertEquals(
                "First item ID",
                String.valueOf(firstItem.getItemObjectId()),
                TestDataReader.getItem1Id()
        );
        TestReporter.assertEqualsRaw(
                "First item quantity",
                firstItem.getQuantity(),
                TestDataReader.getItem1Quantity(),
                0.0
        );
        TestReporter.assertEquals(
                "Second item ID",
                String.valueOf(secondItem.getItemObjectId()),
                TestDataReader.getItem2Id()
        );
        TestReporter.assertEqualsRaw(
                "Second item quantity",
                secondItem.getQuantity(),
                TestDataReader.getItem2Quantity(),
                0.0
        );

        CheckoutResult checkout = CheckoutFlow.payAndConfirm(
                sessionToken,
                QrTestHelper.paymentFromCart(cartRequest)
        );
        CheckoutFlow.assertOrderMatchesCart(cart, checkout.confirmation());
        TestReporter.logOrderItems(checkout.confirmation().getData().getOrderItems());
        TestReporter.logTotals(checkout.confirmation().getData().getOrderItemsTotal());
        TestReporter.result("Payment completed successfully and order was created and accepted.");
    }
}
