package tests;

import api.CartApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import pojo.request.PaymentRequest;
import pojo.response.CartResponse;
import pojo.response.OrderItem;
import tests.report.TestReporter;
import tests.support.CheckoutFlow;
import tests.support.CheckoutResult;
import tests.support.CheckoutStats;
import tests.support.QrTestHelper;

/**
 * Item-level instruction and order-level cooking details must survive checkout.
 */
public class InstructionTests {

    @Test(groups = {"sanity", "regression", "checkout"},
            description = "Validates that an item instruction is preserved through payment and the final order.")
    public void shouldCreateOrderWithItemInstruction() {
        String token = QrTestHelper.freshSessionToken();
        CartRequest cartRequest = cartWithItemInstruction();

        Response cartHttpResponse =
                new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);
        OrderItem first = cart.getData().getOrderItems().get(0);
        OrderItem second = cart.getData().getOrderItems().get(1);

        TestReporter.section("BUSINESS FLOW");
        TestReporter.data("Flow", "Cart → Instruction → Customer → Payment → Order");
        TestReporter.section("ITEM NOTES");
        TestReporter.data("Item", TestReporter.displayItemName(first.getName()));
        TestReporter.data("Item-level notes", first.getNotes());
        TestReporter.section("CART");
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());

        TestReporter.assertEquals("Cart created", cartHttpResponse.statusCode(), 200);
        TestReporter.assertEquals(
                "Instruction stored",
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

        PaymentRequest paymentRequest = QrTestHelper.paymentFromCart(cartRequest);
        CheckoutResult checkout = CheckoutFlow.payAndConfirm(token, paymentRequest);
        CheckoutFlow.assertOrderMatchesCart(cart, checkout.confirmation());

        OrderItem paidFirst = checkout.findItemByName(first.getName());
        TestReporter.assertNotNull("Final order contains instructed item", paidFirst);
        TestReporter.assertEquals(
                "Instruction persisted to final order",
                paidFirst == null ? null : paidFirst.getNotes(),
                TestDataReader.getItemInstruction()
        );
        CheckoutFlow.assertOmsItemNotes(
                checkout,
                TestReporter.displayItemName(first.getName()),
                TestDataReader.getItemInstruction()
        );
        CheckoutFlow.assertOmsItemHasNoNotes(
                checkout,
                TestReporter.displayItemName(second.getName())
        );
        CheckoutStats.instructionOrder();
        TestReporter.logOrderItems(checkout.confirmation().getData().getOrderItems());
        TestReporter.logTotals(checkout.confirmation().getData().getOrderItemsTotal());
        TestReporter.result(
                "Instruction successfully persisted through checkout and order creation."
        );
    }

    @Test(groups = {"sanity", "regression", "checkout"},
            description = "Validates that cooking details are sent through Telr payment and the order is accepted.")
    public void shouldCreateOrderWithCookingDetails() {
        String token = QrTestHelper.freshSessionToken();
        CartRequest cartRequest = QrTestHelper.baselineCart();

        Response cartHttpResponse =
                new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        CartResponse cart = cartHttpResponse.as(CartResponse.class);

        TestReporter.section("BUSINESS FLOW");
        TestReporter.data("Flow", "Cart → Cooking details → Customer → Payment → Order");
        TestReporter.section("CART");
        TestReporter.logCartSummary(cart, cartHttpResponse.statusCode());
        TestReporter.data("Cooking Details Sent", TestDataReader.getOrderInstruction());

        TestReporter.assertEquals("Cart created", cartHttpResponse.statusCode(), 200);
        TestReporter.assertEquals(
                "Cart total",
                cart.getData().getOrderItemsTotal().getTotalAmount(),
                TestDataReader.getExpectedTotalAmount(),
                0.01
        );

        PaymentRequest paymentRequest = QrTestHelper.paymentFromCart(cartRequest);
        paymentRequest.setCookingDetails(TestDataReader.getOrderInstruction());

        CheckoutResult checkout = CheckoutFlow.payAndConfirm(token, paymentRequest);
        CheckoutFlow.assertOrderMatchesCart(cart, checkout.confirmation());

        String persisted = checkout.findPersistedText(TestDataReader.getOrderInstruction());
        TestReporter.data(
                "Cooking Details In Order",
                persisted == null
                        ? "not returned by confirmation or OMS details APIs"
                        : persisted
        );
        if (persisted != null) {
            TestReporter.assertContains(
                    "Cooking details persisted to final order",
                    persisted,
                    TestDataReader.getOrderInstruction()
            );
        }
        CheckoutStats.cookingOrder();
        TestReporter.logOrderItems(checkout.confirmation().getData().getOrderItems());
        TestReporter.logTotals(checkout.confirmation().getData().getOrderItemsTotal());
        TestReporter.result(
                persisted == null
                        ? "Cooking details were sent and the OMS order was accepted. OMS details does not echo cooking_details."
                        : "Cooking details accepted and order was created and accepted."
        );
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
