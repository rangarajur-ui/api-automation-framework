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
import pojo.response.OrderItem;
import pojo.response.PaymentResponse;
import tests.support.QrTestHelper;

/**
 * Item-level instruction (order_items item_instruction) and order-level
 * cooking_details from the captured curls. Baseline items stay 10667 + 10668.
 */
public class InstructionTests {

    @Test(groups = {"sanity", "regression"})
    public void itemInstructionIsStoredAsNotes() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = cartWithItemInstruction();

        Response cartHttpResponse =
                new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        Assert.assertEquals(cartHttpResponse.statusCode(), 200);

        CartResponse cart = cartHttpResponse.as(CartResponse.class);
        OrderItem first = cart.getData().getOrderItems().get(0);
        OrderItem second = cart.getData().getOrderItems().get(1);

        Assert.assertEquals(
                first.getNotes(),
                TestDataReader.getItemInstruction(),
                "item_instruction should come back as notes on the first item"
        );
        Assert.assertTrue(
                second.getNotes() == null || second.getNotes().isBlank(),
                "Second item has no instruction"
        );
        Assert.assertEquals(
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getExpectedTotalAmount(),
                0.01,
                "Instructions must not change the 20.0 total"
        );

        System.out.println("Item notes : " + first.getNotes());
    }

    @Test(groups = {"sanity", "regression"})
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
        Assert.assertEquals(paymentHttpResponse.statusCode(), 200);

        PaymentResponse payment = paymentHttpResponse.as(PaymentResponse.class);
        Assert.assertTrue(
                payment.getData().isSuccess(),
                "initiate_payment should accept cooking_details"
        );
        Assert.assertNotNull(payment.getData().getOrderId());

        System.out.println("Order instruction sent : " + TestDataReader.getOrderInstruction());
        System.out.println("Payment order_id       : " + payment.getData().getOrderId());
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
