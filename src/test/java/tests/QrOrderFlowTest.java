package tests;

import api.CartApi;
import api.PaymentApi;
import api.QrMenuApi;
import config.TelrCardDetails;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.request.ConfirmationCartRequest;
import pojo.request.PaymentRequest;
import pojo.response.CartResponse;
import pojo.response.NewOrderItem;
import pojo.response.OrderItem;
import pojo.response.OrderItemsTotal;
import pojo.response.PaymentResponse;
import pojo.response.PaymentStatusResponse;
import pojo.response.QrMenuResponse;
import tests.report.TestReporter;
import ui.TelrHostedPage;
import utils.PaymentReturnUrl;

public class QrOrderFlowTest {

    @Test(description = "Validates the dine-in flow from menu and cart through Telr payment and order acceptance.")
    public void verifyQrMenuCartAndInitiatePayment() {

        String code = TestDataReader.getQrCode();

        QrMenuApi qrMenuApi = new QrMenuApi();
        CartApi cartApi = new CartApi();
        PaymentApi paymentApi = new PaymentApi();

        Response menuResponse = qrMenuApi.getQrMenuDetails(code);
        QrMenuResponse menu = menuResponse.as(QrMenuResponse.class);

        int accountId = menu.getData().getAccount().getId();
        String accountName = menu.getData().getAccount().getName();
        String sessionToken = menu.getData().getToken();

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

        Response cartHttpResponse = cartApi.viewCart(code, sessionToken, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);

        TestReporter.section("CART");
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());

        TestReporter.assertEquals("Cart HTTP status", cartHttpResponse.statusCode(), 200);
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

        PaymentRequest paymentRequest = PaymentRequest.fromCart(cartRequest);
        paymentRequest.setPayMode(TestDataReader.getPayMode());
        paymentRequest.setTablePreferenceValue(TestDataReader.getTablePreferenceValue());
        paymentRequest.setOrderType(TestDataReader.getOrderType());
        paymentRequest.setUserMobileNumber(TestDataReader.getPaymentUserMobile());

        Response paymentHttpResponse = paymentApi.initiatePayment(code, sessionToken, paymentRequest);
        PaymentResponse payment = paymentHttpResponse.as(PaymentResponse.class);
        String paymentUrl = payment.getData().getPaymentUrl();

        TestReporter.logPaymentSummary(payment.getData());
        TestReporter.data("Payment URL", paymentUrl == null || paymentUrl.isBlank() ? "missing" : "present");

        TestReporter.assertEquals("Initiate payment HTTP status", paymentHttpResponse.statusCode(), 200);
        TestReporter.assertTrue("Payment session created", payment.getData().isSuccess());
        TestReporter.assertEquals(
                "Initiate payment provider",
                payment.getData().getPgName(),
                TestDataReader.getExpectedPaymentProvider()
        );
        TestReporter.assertNotNull("Telr payment URL is present", paymentUrl);
        TestReporter.assertTrue(
                "Payment URL points at Telr",
                paymentUrl != null && paymentUrl.startsWith("https://secure.telr.com")
        );
        TestReporter.assertNotNull("Order ID generated", payment.getData().getOrderId());
        TestReporter.assertTrue(
                "Order ID is a positive number",
                payment.getData().getOrderId() != null && payment.getData().getOrderId() > 0
        );
        TestReporter.assertNotNull("Payment log ID generated", payment.getData().getPaymentLogId());

        if (!TelrCardDetails.isConfigured()) {
            TestReporter.result(
                    "Payment session created. Hosted payment was skipped because card details are not configured, so no order was created."
            );
            return;
        }

        String returnUrl = new TelrHostedPage().completePaymentAndReturnPaytmUrl(
                payment.getData().getPaymentUrl()
        );
        String ct = PaymentReturnUrl.extractCt(returnUrl);
        TestReporter.assertTrue("Telr return link contains payment continuation token", ct != null && !ct.isBlank());

        Response paymentStatusHttpResponse = paymentApi.fetchPaymentStatus(code, sessionToken, ct);
        PaymentStatusResponse orderConfirmation = paymentStatusHttpResponse.as(PaymentStatusResponse.class);

        boolean paymentSuccess = TestDataReader.getExpectedAfterPaymentStatus()
                .equals(orderConfirmation.getData().getPaymentStatus());
        TestReporter.logPaymentSummary(
                payment.getData().getPgName(),
                paymentSuccess,
                orderConfirmation.getData().getPaymentStatus()
        );
        TestReporter.logOrderSummary(orderConfirmation.getData());

        TestReporter.assertEquals("Payment status HTTP status", paymentStatusHttpResponse.statusCode(), 200);
        TestReporter.assertTrue("Payment reached terminal status", orderConfirmation.getData().isTerminalStatus());
        TestReporter.assertEquals(
                "Payment status",
                orderConfirmation.getData().getPaymentStatus(),
                TestDataReader.getExpectedAfterPaymentStatus()
        );
        TestReporter.assertEquals(
                "Order status",
                orderConfirmation.getData().getOrderStatus(),
                TestDataReader.getExpectedAfterOrderStatus()
        );
        TestReporter.assertEquals(
                "Confirmed order ID matches payment session",
                orderConfirmation.getData().getOrderId(),
                payment.getData().getOrderId()
        );
        TestReporter.assertNotBlank("Order number generated", orderConfirmation.getData().getOrderNumber());

        ConfirmationCartRequest confirmationRequest = new ConfirmationCartRequest();
        confirmationRequest.setOrderSource(TestDataReader.getOrderSource());
        confirmationRequest.setOrderType(TestDataReader.getOrderType());
        confirmationRequest.setDeliveryType(TestDataReader.getDeliveryType());
        confirmationRequest.setOrderMode(TestDataReader.getOrderMode());

        Response afterPaymentHttpResponse =
                cartApi.viewCustomerCart(code, sessionToken, ct, confirmationRequest);
        CartResponse afterPayment = afterPaymentHttpResponse.as(CartResponse.class);

        TestReporter.logOrderItems(afterPayment.getData().getOrderItems());
        TestReporter.logTotals(afterPayment.getData().getOrderItemsTotal());

        TestReporter.assertEquals("After-payment HTTP status", afterPaymentHttpResponse.statusCode(), 200);
        assertSameItemAndAmountSummary(cart, afterPayment);
        TestReporter.assertEquals(
                "After-payment payment status",
                afterPayment.getData().getPaymentStatus(),
                TestDataReader.getExpectedAfterPaymentStatus()
        );

        String pageOrderNumber = afterPayment.getData().getOrderNumber();
        TestReporter.assertNotBlank("After-payment order number generated", pageOrderNumber);
        TestReporter.assertEquals(
                "After-payment order number matches payment status",
                pageOrderNumber,
                orderConfirmation.getData().getOrderNumber()
        );

        boolean confirmationLinesMatch = true;
        for (NewOrderItem line : afterPayment.getData().getNewOrderItems()) {
            if (!pageOrderNumber.equals(line.getOrderNumber())) {
                confirmationLinesMatch = false;
                break;
            }
        }
        TestReporter.assertTrue(
                "Every confirmation line shows the same order number",
                confirmationLinesMatch
        );

        TestReporter.result("Payment completed successfully and order was accepted.");
    }

    private void assertSameItemAndAmountSummary(CartResponse summary, CartResponse afterPayment) {
        TestReporter.assertEquals(
                "After-payment item count",
                afterPayment.getData().getOrderItems().size(),
                summary.getData().getOrderItems().size()
        );

        for (OrderItem before : summary.getData().getOrderItems()) {
            OrderItem after = findItemByName(afterPayment, before.getName());
            String itemName = TestReporter.displayItemName(before.getName());
            TestReporter.assertNotNull("After-payment item present: " + itemName, after);
            TestReporter.assertEqualsRaw(
                    "Quantity for " + itemName,
                    after == null ? 0 : after.getQuantity(),
                    before.getQuantity(),
                    0.0
            );
            TestReporter.assertEquals(
                    "Line total for " + itemName,
                    after == null ? 0 : after.getNetPrice(),
                    before.getNetPrice(),
                    0.01
            );
        }

        OrderItemsTotal beforeTotals = summary.getData().getOrderItemsTotal();
        OrderItemsTotal afterTotals = afterPayment.getData().getOrderItemsTotal();

        TestReporter.assertEquals(
                "After-payment total",
                afterTotals.getTotalAmount(),
                beforeTotals.getTotalAmount(),
                0.01
        );
        TestReporter.assertEquals(
                "After-payment tax",
                afterTotals.getOriginalTaxTotal(),
                beforeTotals.getOriginalTaxTotal(),
                0.01
        );
        TestReporter.assertEquals(
                "After-payment net payable",
                afterTotals.getNetPayableAmount(),
                beforeTotals.getNetPayableAmount(),
                0.01
        );
    }

    private OrderItem findItemByName(CartResponse cart, String name) {
        for (OrderItem item : cart.getData().getOrderItems()) {
            if (name.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }
}
