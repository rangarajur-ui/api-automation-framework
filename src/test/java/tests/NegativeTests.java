package tests;

import api.CartApi;
import api.PaymentApi;
import api.QrMenuApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import tests.report.TestReporter;
import tests.support.QrTestHelper;

/**
 * Invalid inputs. Assertions use live error messages, not guessed ones.
 */
public class NegativeTests {

    @Test(groups = {"negative", "regression"},
            description = "Validates that an unknown QR code is rejected.")
    public void invalidQrCodeIsRejected() {
        Response menuResponse = new QrMenuApi().getQrMenuDetails(TestDataReader.getInvalidQrCode());
        String actualMessage = QrTestHelper.apiErrorMessage(menuResponse);

        TestReporter.logNegative(
                "Request with invalid QR code",
                "Unknown QR code",
                422,
                menuResponse.statusCode(),
                "Invalid QR Code",
                actualMessage
        );

        TestReporter.assertEquals("HTTP status validation", menuResponse.statusCode(), 422);
        TestReporter.assertContains("Error response validation", actualMessage, "Invalid QR Code");
        TestReporter.result("Invalid QR code was rejected.");
    }

    @Test(groups = {"negative", "regression"},
            description = "Validates that an unknown product cannot be added to the cart.")
    public void unknownItemCannotBeAddedToCart() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.guestCart();
        cartRequest.addItem(TestDataReader.getInvalidItemId(), 8, 1);

        Response cartResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        String actualMessage = QrTestHelper.apiErrorMessage(cartResponse);

        TestReporter.logNegative(
                "Request with invalid product ID",
                "Unknown catalog item",
                422,
                cartResponse.statusCode(),
                "can't be ordered",
                actualMessage
        );

        TestReporter.assertEquals("HTTP status validation", cartResponse.statusCode(), 422);
        TestReporter.assertContains("Error response validation", actualMessage, "can't be ordered");
        TestReporter.result("Unknown product was rejected.");
    }

    @Test(groups = {"negative", "regression"},
            description = "Validates that checkout is blocked when the cart has no items.")
    public void emptyCartIsRejected() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.guestCart();
        cartRequest.addHeaderRowOnly();

        Response cartResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);
        String actualMessage = QrTestHelper.apiErrorMessage(cartResponse);

        TestReporter.logNegative(
                "Checkout with an empty cart",
                "Header row only, no products",
                422,
                cartResponse.statusCode(),
                "cart is empty",
                actualMessage
        );

        TestReporter.assertEquals("HTTP status validation", cartResponse.statusCode(), 422);
        TestReporter.assertContains("Error response validation", actualMessage, "cart is empty");
        TestReporter.result("Empty cart was rejected.");
    }

    @Test(groups = {"negative", "regression"},
            description = "Validates that payment status cannot be fetched with an invalid return link.")
    public void invalidPaymentCtIsRejected() {
        String token = QrTestHelper.newSessionToken();

        Response statusResponse = new PaymentApi().fetchPaymentStatus(
                TestDataReader.getQrCode(),
                token,
                "not-a-real-ct"
        );
        String actualMessage = QrTestHelper.apiErrorMessage(statusResponse);

        TestReporter.logNegative(
                "Payment status with invalid return link",
                "Invalid payment continuation token",
                422,
                statusResponse.statusCode(),
                "Invalid link",
                actualMessage
        );

        TestReporter.assertEquals("HTTP status validation", statusResponse.statusCode(), 422);
        TestReporter.assertContains("Error response validation", actualMessage, "Invalid link");
        TestReporter.result("Invalid payment link was rejected.");
    }
}
