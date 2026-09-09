package tests;

import api.CartApi;
import api.PaymentApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.request.PaymentRequest;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import pojo.response.PaymentResponse;
import tests.report.TestReporter;
import tests.support.QrTestHelper;

/**
 * Item-level instruction (order_items item_instruction) and order-level
 * cooking_details from the captured curls. Baseline items stay 10667 + 10668.
 */
public class InstructionTests {

    @Test(groups = {"sanity", "regression"},
            description = "Validates that an item instruction is stored as notes without changing totals.")
    public void itemInstructionIsStoredAsNotes() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = cartWithItemInstruction();

        Response cartHttpResponse =
                new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);
        OrderItem first = cart.getData().getOrderItems().get(0);
        OrderItem second = cart.getData().getOrderItems().get(1);

        TestReporter.data("Item Notes", first.getNotes());
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());

        TestReporter.assertEquals("HTTP status validation", cartHttpResponse.statusCode(), 200);
        TestReporter.assertEquals(
                "Item instruction stored as notes",
                first.getNotes(),
                TestDataReader.getItemInstruction()
        );
        TestReporter.assertTrue(
                "Second item has no instruction",
                second.getNotes() == null || second.getNotes().isBlank()
        );
        TestReporter.assertEquals(
                "Cart total unchanged by instruction",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getExpectedTotalAmount(),
                0.01
        );
        TestReporter.result("Item instruction stored as notes.");
    }

    @Test(groups = {"sanity", "regression"},
            description = "Validates that order-level cooking details are accepted when payment is initiated.")
    public void orderLevelCookingDetailsCanInitiatePayment() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = cartWithItemInstruction();

        PaymentRequest paymentRequest = QrTestHelper.paymentFromCart(cartRequest);
        paymentRequest.setCookingDetails(TestDataReader.getOrderInstruction());

        Response paymentHttpResponse = new PaymentApi().initiatePayment(
                TestDataReader.getQrCode(),
                token,
                paymentRequest
        );
        PaymentResponse payment = paymentHttpResponse.as(PaymentResponse.class);

        TestReporter.data("Cooking Details Sent", TestDataReader.getOrderInstruction());
        TestReporter.data("HTTP Status", paymentHttpResponse.statusCode());
        TestReporter.data("Payment Success", payment.getData().isSuccess());
        if (payment.getData().getOrderId() != null) {
            TestReporter.data("Order ID", payment.getData().getOrderId());
        }

        TestReporter.assertEquals("HTTP status validation", paymentHttpResponse.statusCode(), 200);
        TestReporter.assertTrue("Payment session accepts cooking details", payment.getData().isSuccess());
        TestReporter.assertNotNull("Order ID generated", payment.getData().getOrderId());
        TestReporter.result("Cooking details accepted and payment session created.");
    }

    private CartRequest cartWithItemInstruction() {
        CartRequest cartRequest = QrTestHelper.guestCart();
        cartRequest.addItem(
                TestDataReader.getItem1Id(),
                TestDataReader.getItem1Price(),
                TestDataReader.getItem1Quantity(),
                TestDataReader.getItemInstruction()
        );
        cartRequest.addItem(
                TestDataReader.getItem2Id(),
                TestDataReader.getItem2Price(),
                TestDataReader.getItem2Quantity()
        );
        return cartRequest;
    }
}
