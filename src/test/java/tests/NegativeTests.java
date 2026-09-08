package tests;

import api.CartApi;
import api.PaymentApi;
import api.QrMenuApi;
import config.TestDataReader;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import pojo.request.CartRequest;
import tests.support.QrTestHelper;

/**
 * Invalid inputs. Assertions use live error messages, not guessed ones.
 */
public class NegativeTests {

    @Test(groups = {"negative", "regression"})
    public void invalidQrCodeIsRejected() {
        Response menuResponse = new QrMenuApi().getQrMenuDetails(TestDataReader.getInvalidQrCode());

        Assert.assertEquals(menuResponse.statusCode(), 422, "Invalid QR should return HTTP 422");
        Assert.assertTrue(
                QrTestHelper.apiErrorMessage(menuResponse).contains("Invalid QR Code"),
                "Negative: error should say Invalid QR Code"
        );
    }

    @Test(groups = {"negative", "regression"})
    public void unknownItemCannotBeAddedToCart() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.guestCart();
        cartRequest.addItem(TestDataReader.getInvalidItemId(), 8, 1);

        Response cartResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);

        Assert.assertEquals(cartResponse.statusCode(), 422, "Unknown item should return HTTP 422");
        Assert.assertTrue(
                QrTestHelper.apiErrorMessage(cartResponse).contains("can't be ordered"),
                "Negative: unknown item should be rejected"
        );
    }

    @Test(groups = {"negative", "regression"})
    public void emptyCartIsRejected() {
        String token = QrTestHelper.newSessionToken();
        CartRequest cartRequest = QrTestHelper.guestCart();
        cartRequest.addHeaderRowOnly();

        Response cartResponse = new CartApi().viewCart(TestDataReader.getQrCode(), token, cartRequest);

        Assert.assertEquals(cartResponse.statusCode(), 422, "Empty cart should return HTTP 422");
        Assert.assertTrue(
                QrTestHelper.apiErrorMessage(cartResponse).contains("cart is empty"),
                "Negative: empty cart should be rejected"
        );
    }

    @Test(groups = {"negative", "regression"})
    public void invalidPaymentCtIsRejected() {
        String token = QrTestHelper.newSessionToken();

        Response statusResponse = new PaymentApi().fetchPaymentStatus(
                TestDataReader.getQrCode(),
                token,
                "not-a-real-ct"
        );

        Assert.assertEquals(statusResponse.statusCode(), 422, "Invalid ct should return HTTP 422");
        Assert.assertTrue(
                QrTestHelper.apiErrorMessage(statusResponse).contains("Invalid link"),
                "Negative: bad ct should return Invalid link"
        );
    }
}
