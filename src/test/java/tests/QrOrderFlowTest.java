package tests;

import api.CartApi;
import api.PaymentApi;
import api.QrMenuApi;
import config.TelrCardDetails;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.Reporter;
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
import ui.TelrHostedPage;
import utils.PaymentReturnUrl;
import utils.TokenMasker;

public class QrOrderFlowTest {

    @Test
    public void verifyQrMenuCartAndInitiatePayment() {

        String code = TestDataReader.getQrCode();

        QrMenuApi qrMenuApi = new QrMenuApi();
        CartApi cartApi = new CartApi();
        PaymentApi paymentApi = new PaymentApi();

        // ==========================================
        // Step 1: Get menu details and session token
        // ==========================================

        Response menuResponse =
                qrMenuApi.getQrMenuDetails(code);

        Assert.assertEquals(
                menuResponse.statusCode(),
                200,
                "QR Menu API should return HTTP 200"
        );

        QrMenuResponse menu =
                menuResponse.as(QrMenuResponse.class);

        int accountId =
                menu.getData().getAccount().getId();

        String accountName =
                menu.getData().getAccount().getName();

        String sessionToken =
                menu.getData().getToken();

        System.out.println("Account ID   : " + accountId);
        System.out.println("Account Name : " + accountName);
        System.out.println("Token        : " + TokenMasker.mask(sessionToken));

        Assert.assertEquals(
                accountId,
                TestDataReader.getExpectedAccountId(),
                "QR Menu should return the expected account ID"
        );
        Assert.assertEquals(
                accountName,
                TestDataReader.getExpectedAccountName(),
                "QR Menu should return the expected account name"
        );
        Assert.assertNotNull(sessionToken, "QR Menu should return a session token");
        Assert.assertFalse(sessionToken.isBlank(), "Session token should not be blank");

        // ==========================================
        // Step 2: Add items / view cart
        // ==========================================

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

        Response cartHttpResponse =
                cartApi.viewCart(
                        code,
                        sessionToken,
                        cartRequest
                );

        System.out.println(
                "Cart Status Code: " + cartHttpResponse.statusCode()
        );

        Assert.assertEquals(
                cartHttpResponse.statusCode(),
                200,
                "Cart API should return HTTP 200"
        );

        CartResponse cart =
                cartHttpResponse.as(CartResponse.class);

        double totalAmount =
                cart.getData()
                        .getOrderItemsTotal()
                        .getTotalAmount();

        double taxTotal =
                cart.getData()
                        .getOrderItemsTotal()
                        .getOriginalTaxTotal();

        double netPayableAmount =
                cart.getData()
                        .getOrderItemsTotal()
                        .getNetPayableAmount();

        System.out.println("Total Amount       : " + totalAmount);
        System.out.println("Tax Total          : " + taxTotal);
        System.out.println("Net Payable Amount : " + netPayableAmount);

        Assert.assertEquals(
                totalAmount,
                TestDataReader.getExpectedTotalAmount(),
                "Cart total amount should match the selected items"
        );
        Assert.assertEquals(
                taxTotal,
                TestDataReader.getExpectedTax(),
                "Cart tax should match the selected items"
        );
        Assert.assertEquals(
                netPayableAmount,
                TestDataReader.getExpectedNetPayable(),
                "Cart net payable should match the selected items"
        );

        String paymentStatus =
                cart.getData().getPaymentStatus();

        String paymentProvider =
                cart.getData().getPaymentProviderName();

        System.out.println("Payment Status     : " + paymentStatus);
        System.out.println("Payment Provider   : " + paymentProvider);

        Assert.assertEquals(
                paymentStatus,
                TestDataReader.getExpectedPaymentStatus(),
                "Cart should still be waiting for payment"
        );
        Assert.assertEquals(
                paymentProvider,
                TestDataReader.getExpectedPaymentProvider(),
                "Cart should use the expected payment provider"
        );

        Assert.assertEquals(
                cart.getData().getOrderItems().size(),
                2,
                "Cart should contain two items"
        );

        OrderItem firstItem =
                cart.getData().getOrderItems().get(0);

        System.out.println(
                "First Item ID      : " + firstItem.getItemObjectId()
        );
        System.out.println(
                "First Item Name    : " + firstItem.getName()
        );
        System.out.println(
                "First Item Quantity: " + firstItem.getQuantity()
        );

        Assert.assertEquals(
                String.valueOf(firstItem.getItemObjectId()),
                TestDataReader.getItem1Id(),
                "First cart item ID should match test data"
        );
        Assert.assertEquals(
                firstItem.getQuantity(),
                (double) TestDataReader.getItem1Quantity(),
                "First cart item quantity should match test data"
        );

        OrderItem secondItem =
                cart.getData().getOrderItems().get(1);

        Assert.assertEquals(
                String.valueOf(secondItem.getItemObjectId()),
                TestDataReader.getItem2Id(),
                "Second cart item ID should match test data"
        );
        Assert.assertEquals(
                secondItem.getQuantity(),
                (double) TestDataReader.getItem2Quantity(),
                "Second cart item quantity should match test data"
        );

        // ==========================================
        // Step 3: Initiate payment
        // Live response fields: success, payment_url, pg_name, order_id, payment_log_id.
        // There is no ct in this response — do not invent one.
        // ==========================================

        PaymentRequest paymentRequest = PaymentRequest.fromCart(cartRequest);
        paymentRequest.setPayMode(TestDataReader.getPayMode());
        paymentRequest.setTablePreferenceValue(TestDataReader.getTablePreferenceValue());
        paymentRequest.setOrderType(TestDataReader.getOrderType());
        paymentRequest.setUserMobileNumber(TestDataReader.getPaymentUserMobile());

        Response paymentHttpResponse =
                paymentApi.initiatePayment(
                        code,
                        sessionToken,
                        paymentRequest
                );

        Assert.assertEquals(
                paymentHttpResponse.statusCode(),
                200,
                "Initiate Payment API should return HTTP 200"
        );

        PaymentResponse payment =
                paymentHttpResponse.as(PaymentResponse.class);

        Assert.assertTrue(
                payment.getData().isSuccess(),
                "Initiate payment should return data.success = true"
        );
        Assert.assertEquals(
                payment.getData().getPgName(),
                TestDataReader.getExpectedPaymentProvider(),
                "Initiate payment should use the expected payment provider"
        );
        Assert.assertNotNull(
                payment.getData().getPaymentUrl(),
                "Initiate payment should return a Telr payment_url"
        );
        Assert.assertTrue(
                payment.getData().getPaymentUrl().startsWith("https://secure.telr.com"),
                "payment_url should point at the Telr hosted page"
        );
        Assert.assertNotNull(
                payment.getData().getOrderId(),
                "Initiate payment should return an order_id"
        );
        Assert.assertTrue(
                payment.getData().getOrderId() > 0,
                "order_id should be a positive number"
        );
        Assert.assertNotNull(
                payment.getData().getPaymentLogId(),
                "Initiate payment should return a payment_log_id"
        );

        System.out.println("Payment success : " + payment.getData().isSuccess());
        System.out.println("Payment PG      : " + payment.getData().getPgName());
        System.out.println("Order ID        : " + payment.getData().getOrderId());
        System.out.println("Payment log ID  : " + payment.getData().getPaymentLogId());

        if (!TelrCardDetails.isConfigured()) {
            printLine("ORDER NUMBER    : not created yet (Telr payment was skipped)");
            printLine(
                    "Set TELR_CARD_NUMBER, TELR_CVV, TELR_EXP_MONTH, TELR_EXP_YEAR "
                            + "in IntelliJ Run Configuration env, -D VM options, "
                            + "or gitignored telr.local.properties."
            );
            return;
        }

        // ==========================================
        // Step 4: Telr hosted page (browser)
        // ct arrives on the Paytm return URL after Telr redirects.
        // ==========================================

        String returnUrl =
                new TelrHostedPage().completePaymentAndReturnPaytmUrl(
                        payment.getData().getPaymentUrl()
                );

        String ct = PaymentReturnUrl.extractCt(returnUrl);
        Assert.assertFalse(ct.isBlank(), "Telr return URL should contain ct");
        System.out.println("Return ct       : " + TokenMasker.mask(ct));

        // ==========================================
        // Step 5: Confirm order was created
        // ==========================================

        Response paymentStatusHttpResponse =
                paymentApi.fetchPaymentStatus(code, sessionToken, ct);

        Assert.assertEquals(
                paymentStatusHttpResponse.statusCode(),
                200,
                "Payment status API should return HTTP 200"
        );

        PaymentStatusResponse orderConfirmation =
                paymentStatusHttpResponse.as(PaymentStatusResponse.class);

        Assert.assertTrue(
                orderConfirmation.getData().isTerminalStatus(),
                "Payment should have reached a terminal status"
        );
        Assert.assertEquals(
                orderConfirmation.getData().getPaymentStatus(),
                TestDataReader.getExpectedAfterPaymentStatus(),
                "Payment should be successful after Telr return"
        );
        Assert.assertEquals(
                orderConfirmation.getData().getOrderStatus(),
                TestDataReader.getExpectedAfterOrderStatus(),
                "Order should be accepted after successful payment"
        );
        Assert.assertEquals(
                orderConfirmation.getData().getOrderId(),
                payment.getData().getOrderId(),
                "Confirmed order_id should match initiate_payment order_id"
        );
        Assert.assertNotNull(
                orderConfirmation.getData().getOrderNumber(),
                "Confirmed order should have an order_number"
        );
        Assert.assertFalse(
                orderConfirmation.getData().getOrderNumber().isBlank(),
                "order_number should not be blank"
        );

        printOrderNumber(orderConfirmation.getData().getOrderNumber());
        printLine("Order status    : " + orderConfirmation.getData().getOrderStatus());
        printLine("Final payment   : " + orderConfirmation.getData().getPaymentStatus());

        // ==========================================
        // Step 6: After-payment page vs cart summary
        // Same items and amounts must appear after payment.
        // ==========================================

        ConfirmationCartRequest confirmationRequest = new ConfirmationCartRequest();
        confirmationRequest.setOrderSource(TestDataReader.getOrderSource());
        confirmationRequest.setOrderType(TestDataReader.getOrderType());
        confirmationRequest.setDeliveryType(TestDataReader.getDeliveryType());
        confirmationRequest.setOrderMode(TestDataReader.getOrderMode());

        Response afterPaymentHttpResponse =
                cartApi.viewCustomerCart(code, sessionToken, ct, confirmationRequest);

        Assert.assertEquals(
                afterPaymentHttpResponse.statusCode(),
                200,
                "After-payment cart API should return HTTP 200"
        );

        CartResponse afterPayment =
                afterPaymentHttpResponse.as(CartResponse.class);

        printSummary("CART SUMMARY (before payment)", cart);
        printSummary("AFTER PAYMENT PAGE", afterPayment);
        System.out.println(
                "Compared: item name, qty, line net, total, tax, net payable. "
                        + "Unit price can differ after payment (tax-exclusive)."
        );

        assertSameItemAndAmountSummary(cart, afterPayment);
        Assert.assertEquals(
                afterPayment.getData().getPaymentStatus(),
                TestDataReader.getExpectedAfterPaymentStatus(),
                "After-payment page should show payment_success"
        );

        String pageOrderNumber = afterPayment.getData().getOrderNumber();
        Assert.assertNotNull(
                pageOrderNumber,
                "After-payment page new_order_items should contain order_number"
        );
        Assert.assertFalse(
                pageOrderNumber.isBlank(),
                "After-payment page order_number should not be blank"
        );
        Assert.assertEquals(
                pageOrderNumber,
                orderConfirmation.getData().getOrderNumber(),
                "After-payment page order_number should match fetch_payment_status"
        );

        for (NewOrderItem line : afterPayment.getData().getNewOrderItems()) {
            Assert.assertEquals(
                    line.getOrderNumber(),
                    pageOrderNumber,
                    "Every confirmation line should show the same order_number"
            );
        }

        printOrderNumber(pageOrderNumber);
    }

    private void printOrderNumber(String orderNumber) {
        printLine("========================================");
        printLine("ORDER NUMBER    : " + orderNumber);
        printLine("========================================");
    }

    private void printLine(String message) {
        System.out.println(message);
        Reporter.log(message);
    }

    private void printSummary(String title, CartResponse cart) {
        System.out.println("----- " + title + " -----");
        for (OrderItem item : cart.getData().getOrderItems()) {
            System.out.println(
                    "Item " + item.getItemObjectId()
                            + " | " + item.getName()
                            + " | qty " + item.getQuantity()
                            + " | price " + item.getPrice()
                            + " | net " + item.getNetPrice()
            );
        }
        OrderItemsTotal totals = cart.getData().getOrderItemsTotal();
        System.out.println("Total  : " + totals.getTotalAmount());
        System.out.println("Tax    : " + totals.getOriginalTaxTotal());
        System.out.println("Net    : " + totals.getNetPayableAmount());
        System.out.println("Pay st : " + cart.getData().getPaymentStatus());
        if (cart.getData().getOrderNumber() != null) {
            System.out.println("ORDER NUMBER    : " + cart.getData().getOrderNumber());
        }
    }

    private void assertSameItemAndAmountSummary(CartResponse summary, CartResponse afterPayment) {
        Assert.assertEquals(
                afterPayment.getData().getOrderItems().size(),
                summary.getData().getOrderItems().size(),
                "After-payment page should show the same number of items as the cart summary"
        );

        for (OrderItem before : summary.getData().getOrderItems()) {
            OrderItem after = findItemByName(afterPayment, before.getName());
            Assert.assertNotNull(
                    after,
                    "After-payment page is missing cart item: " + before.getName()
            );
            Assert.assertEquals(
                    after.getQuantity(),
                    before.getQuantity(),
                    0.0,
                    "Quantity for " + before.getName() + " should match cart summary"
            );
            Assert.assertEquals(
                    after.getNetPrice(),
                    before.getNetPrice(),
                    0.01,
                    "Line amount for " + before.getName() + " should match cart summary"
            );
        }

        OrderItemsTotal beforeTotals = summary.getData().getOrderItemsTotal();
        OrderItemsTotal afterTotals = afterPayment.getData().getOrderItemsTotal();

        Assert.assertEquals(
                afterTotals.getTotalAmount(),
                beforeTotals.getTotalAmount(),
                0.01,
                "Total amount on after-payment page should match cart summary"
        );
        Assert.assertEquals(
                afterTotals.getOriginalTaxTotal(),
                beforeTotals.getOriginalTaxTotal(),
                0.01,
                "Tax on after-payment page should match cart summary"
        );
        Assert.assertEquals(
                afterTotals.getNetPayableAmount(),
                beforeTotals.getNetPayableAmount(),
                0.01,
                "Net payable on after-payment page should match cart summary"
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
