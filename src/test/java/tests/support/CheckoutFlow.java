package tests.support;

import api.CartApi;
import api.OmsApi;
import api.PaymentApi;
import config.OmsCredentials;
import config.TelrCardDetails;
import config.TestDataReader;
import io.restassured.response.Response;
import pojo.request.ConfirmationCartRequest;
import pojo.request.PaymentRequest;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import pojo.response.OrderItemsTotal;
import pojo.response.PaymentResponse;
import pojo.response.PaymentStatusResponse;
import pojo.response.oms.OmsCustomer;
import pojo.response.oms.OmsOrderData;
import pojo.response.oms.OmsOrderDetailsResponse;
import pojo.response.oms.OmsOrderItem;
import tests.report.TestReporter;
import ui.TelrHostedPage;
import utils.PaymentReturnUrl;

/**
 * Completes the real Telr checkout and loads the paid order.
 * A test that calls this must fail if payment is skipped or the order is not accepted.
 */
public final class CheckoutFlow {

    private CheckoutFlow() {
    }

    public static CheckoutResult payAndConfirm(String sessionToken, PaymentRequest paymentRequest) {
        TestReporter.assertTrue(
                "Telr test credentials are configured",
                TelrCardDetails.isConfigured()
        );

        String code = TestDataReader.getQrCode();
        PaymentApi paymentApi = new PaymentApi();
        CartApi cartApi = new CartApi();

        Response paymentHttp = paymentApi.initiatePayment(code, sessionToken, paymentRequest);
        TestReporter.assertEquals("Initiate payment HTTP status", paymentHttp.statusCode(), 200);

        PaymentResponse payment = paymentHttp.as(PaymentResponse.class);
        String paymentUrl = payment.getData().getPaymentUrl();

        TestReporter.section("PAYMENT");
        TestReporter.data("Provider", payment.getData().getPgName());
        TestReporter.data("Payment session", payment.getData().isSuccess());
        if (payment.getData().getOrderId() != null) {
            TestReporter.data("Payment Order ID", payment.getData().getOrderId());
        }
        TestReporter.data("Payment URL", paymentUrl == null || paymentUrl.isBlank() ? "missing" : "present");

        TestReporter.assertTrue("Payment session created", payment.getData().isSuccess());
        TestReporter.assertNotNull("Telr payment URL is present", paymentUrl);
        TestReporter.assertTrue(
                "Payment URL points at Telr",
                paymentUrl != null && paymentUrl.startsWith("https://secure.telr.com")
        );
        TestReporter.assertNotNull("Payment session order ID generated", payment.getData().getOrderId());

        String returnUrl;
        try {
            returnUrl = new TelrHostedPage().completePaymentAndReturnPaytmUrl(paymentUrl);
        } catch (RuntimeException error) {
            CheckoutStats.paymentFailed();
            throw error;
        }

        String ct = PaymentReturnUrl.extractCt(returnUrl);
        TestReporter.assertTrue("Telr return link contains payment continuation token", ct != null && !ct.isBlank());

        Response statusHttp = paymentApi.fetchPaymentStatus(code, sessionToken, ct);
        TestReporter.assertEquals("Payment status HTTP status", statusHttp.statusCode(), 200);

        PaymentStatusResponse status = statusHttp.as(PaymentStatusResponse.class);
        String actualPaymentStatus = status.getData().getPaymentStatus();
        String actualOrderStatus = status.getData().getOrderStatus();
        String orderNumber = status.getData().getOrderNumber();

        boolean paymentSucceeded = TestDataReader.getExpectedAfterPaymentStatus().equals(actualPaymentStatus);
        if (paymentSucceeded) {
            CheckoutStats.paymentSuccess();
        } else {
            CheckoutStats.paymentFailed();
        }

        TestReporter.data("Payment Success", paymentSucceeded);
        TestReporter.data("Payment Status", actualPaymentStatus);

        TestReporter.section("ORDER");
        TestReporter.data("Order ID", status.getData().getOrderId());
        TestReporter.data("Order Number", orderNumber);
        TestReporter.data("Order Status", actualOrderStatus);

        TestReporter.assertTrue("Payment reached terminal status", status.getData().isTerminalStatus());
        TestReporter.assertEquals(
                "Payment status",
                actualPaymentStatus,
                TestDataReader.getExpectedAfterPaymentStatus()
        );
        TestReporter.assertEquals(
                "Order status",
                actualOrderStatus,
                TestDataReader.getExpectedAfterOrderStatus()
        );
        TestReporter.assertEquals(
                "Confirmed order ID matches payment session",
                status.getData().getOrderId(),
                payment.getData().getOrderId()
        );
        TestReporter.assertNotBlank("Order number generated", orderNumber);

        ConfirmationCartRequest confirmationRequest = new ConfirmationCartRequest();
        confirmationRequest.setOrderSource(TestDataReader.getOrderSource());
        confirmationRequest.setOrderType(TestDataReader.getOrderType());
        confirmationRequest.setDeliveryType(TestDataReader.getDeliveryType());
        confirmationRequest.setOrderMode(TestDataReader.getOrderMode());

        Response confirmationHttp = cartApi.viewCustomerCart(code, sessionToken, ct, confirmationRequest);
        TestReporter.assertEquals("After-payment HTTP status", confirmationHttp.statusCode(), 200);

        CartResponse confirmation = confirmationHttp.as(CartResponse.class);
        TestReporter.assertEquals(
                "After-payment payment status",
                confirmation.getData().getPaymentStatus(),
                TestDataReader.getExpectedAfterPaymentStatus()
        );
        TestReporter.assertNotBlank("After-payment order number generated", confirmation.getData().getOrderNumber());
        TestReporter.assertEquals(
                "After-payment order number matches payment status",
                confirmation.getData().getOrderNumber(),
                orderNumber
        );

        CheckoutStats.orderCreated();

        TestReporter.section("OMS");
        TestReporter.data("OMS Credentials", OmsCredentials.isConfigured() ? "present" : "missing");
        TestReporter.data("OMS Verification Status", OmsCredentials.isConfigured() ? "started" : "blocked");
        if (!OmsCredentials.isConfigured()) {
            TestReporter.data("OMS Required Variables", OmsCredentials.missingRequiredNames());
        }
        TestReporter.assertTrue("OMS credentials are configured", OmsCredentials.isConfigured());
        Integer orderId = status.getData().getOrderId();
        Response omsHttp = new OmsApi().getOrderDetails(orderId);
        TestReporter.assertEquals("OMS order details HTTP status", omsHttp.statusCode(), 200);

        OmsOrderDetailsResponse oms = omsHttp.as(OmsOrderDetailsResponse.class);
        OmsOrderData omsOrder = oms.getData();
        TestReporter.assertNotNull("OMS order data present", omsOrder);

        TestReporter.section("OMS ORDER");
        TestReporter.data("OMS Order ID", omsOrder.getId());
        TestReporter.data("OMS Order Number", omsOrder.getOrderNumber());
        TestReporter.data("OMS KOT Number", omsOrder.getKotNumber());
        TestReporter.data("OMS Order Type", omsOrder.getOrderType());
        TestReporter.data("OMS Order Status", omsOrder.getStatus());
        TestReporter.data("OMS Payment Status", omsOrder.getPaymentStatus());
        TestReporter.data("OMS Payment Method", omsOrder.getPaymentMethod());
        TestReporter.data("OMS Table", omsOrder.getTableNumbers());
        TestReporter.data("OMS Outlet", omsOrder.getOutletName());
        TestReporter.data("OMS Account ID", omsOrder.getTenantId());
        TestReporter.money("OMS Total", omsOrder.getTotalAmount());
        if (omsOrder.getCustomer() != null) {
            TestReporter.data("OMS Customer", omsOrder.getCustomer().getName());
            TestReporter.data("OMS Mobile", TestReporter.maskedMobile(omsOrder.getCustomer().getPhone()));
        }
        if (omsOrder.getOrderTimeline() != null) {
            TestReporter.data("OMS Timeline Accepted", omsOrder.getOrderTimeline().isAccepted());
        }
        if (omsOrder.getItems() != null) {
            int index = 1;
            for (OmsOrderItem item : omsOrder.getItems()) {
                TestReporter.data("OMS Item " + index, TestReporter.displayItemName(item.getName()));
                TestReporter.data("OMS Qty " + index, TestReporter.formatQuantity(item.getQuantity()));
                TestReporter.money("OMS Price " + index, item.getPrice());
                if (item.getNotes() != null && !item.getNotes().isBlank()) {
                    TestReporter.data("OMS Notes " + index, item.getNotes());
                }
                if (item.getCustomizations() != null && !item.getCustomizations().isBlank()) {
                    TestReporter.data("OMS Customization " + index, item.getCustomizations());
                }
                index++;
            }
        }

        TestReporter.assertEquals("OMS order ID matches payment", omsOrder.getId(), orderId);
        TestReporter.assertEquals("OMS order number", omsOrder.getOrderNumber(), orderNumber);
        TestReporter.assertNotBlank("OMS KOT number generated", omsOrder.getKotNumber());
        TestReporter.assertEquals("OMS order type", omsOrder.getOrderType(), TestDataReader.getExpectedOmsOrderType());
        TestReporter.assertEquals("OMS order status", omsOrder.getStatus(), TestDataReader.getExpectedAfterOrderStatus());
        TestReporter.assertEquals("OMS payment status", omsOrder.getPaymentStatus(), TestDataReader.getExpectedOmsPaymentStatus());
        TestReporter.assertEquals("OMS payment method", omsOrder.getPaymentMethod(), TestDataReader.getExpectedOmsPaymentMethod());
        TestReporter.assertEquals("OMS account ID", omsOrder.getTenantId(), TestDataReader.getExpectedAccountId());
        TestReporter.assertTrue(
                "OMS timeline shows accepted",
                omsOrder.getOrderTimeline() != null && omsOrder.getOrderTimeline().isAccepted()
        );
        TestReporter.assertTrue("OMS audit has payment_completed", omsOrder.hasAuditEvent("payment_completed"));
        TestReporter.assertTrue("OMS audit has order_accepted", omsOrder.hasAuditEvent("order_accepted"));
        TestReporter.assertEquals(
                "OMS total",
                omsOrder.getTotalAmount(),
                confirmation.getData().getOrderItemsTotal().getTotalAmount(),
                0.01
        );
        TestReporter.data("OMS Verification Status", "passed");

        return new CheckoutResult(payment, status, confirmation, confirmationHttp, oms, omsHttp);
    }

    public static void assertOmsCustomer(CheckoutResult checkout, String expectedName) {
        OmsCustomer customer = checkout.omsData() == null ? null : checkout.omsData().getCustomer();
        TestReporter.assertNotNull("OMS customer present", customer);
        TestReporter.assertEquals(
                "OMS customer name",
                customer == null ? null : customer.getName(),
                expectedName
        );
    }

    public static void assertOmsItemNotes(CheckoutResult checkout, String nameContains, String expectedNotes) {
        OmsOrderItem item = checkout.findOmsItemContaining(nameContains);
        TestReporter.assertNotNull("OMS item present: " + nameContains, item);
        TestReporter.assertEquals(
                "OMS item-level notes",
                item == null ? null : item.getNotes(),
                expectedNotes
        );
    }

    public static void assertOmsItemHasNoNotes(CheckoutResult checkout, String nameContains) {
        OmsOrderItem item = checkout.findOmsItemContaining(nameContains);
        TestReporter.assertNotNull("OMS item present: " + nameContains, item);
        TestReporter.assertTrue(
                "OMS item-level notes empty for " + nameContains,
                item == null || item.getNotes() == null || item.getNotes().isBlank()
        );
    }

    public static void assertOrderMatchesCart(CartResponse cart, CartResponse confirmation) {
        TestReporter.assertNotNull("Final order items present", confirmation.getData().getOrderItems());
        TestReporter.assertEquals(
                "Final order item count",
                confirmation.getData().getOrderItems() == null ? 0 : confirmation.getData().getOrderItems().size(),
                cart.getData().getOrderItems().size()
        );

        for (OrderItem before : cart.getData().getOrderItems()) {
            OrderItem after = findItemByName(confirmation, before.getName());
            String itemName = TestReporter.displayItemName(before.getName());
            TestReporter.assertNotNull("Final order contains " + itemName, after);
            TestReporter.assertEqualsRaw(
                    "Final quantity for " + itemName,
                    after == null ? 0 : after.getQuantity(),
                    before.getQuantity(),
                    0.0
            );
            TestReporter.assertEquals(
                    "Final line total for " + itemName,
                    after == null ? 0 : after.getNetPrice(),
                    before.getNetPrice(),
                    0.01
            );
        }

        OrderItemsTotal beforeTotals = cart.getData().getOrderItemsTotal();
        OrderItemsTotal afterTotals = confirmation.getData().getOrderItemsTotal();
        TestReporter.assertEquals("Final order total", afterTotals.getTotalAmount(), beforeTotals.getTotalAmount(), 0.01);
        TestReporter.assertEquals("Final order tax", afterTotals.getOriginalTaxTotal(), beforeTotals.getOriginalTaxTotal(), 0.01);
        TestReporter.assertEquals(
                "Final order net payable",
                afterTotals.getNetPayableAmount(),
                beforeTotals.getNetPayableAmount(),
                0.01
        );
    }

    private static OrderItem findItemByName(CartResponse cart, String name) {
        if (cart.getData().getOrderItems() == null) {
            return null;
        }
        for (OrderItem item : cart.getData().getOrderItems()) {
            if (name.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }
}
