package api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import pojo.request.CartRequest;
import pojo.request.ConfirmationCartRequest;

public class CartApi {

    public Response viewCart(String code, String sessionToken, CartRequest request) {
        return ApiCall.retry(() -> RestAssured
                .given()
                .spec(ApiRequestSpec.commonSpec())
                .header("Referer", ApiRequestSpec.getBaseUrl() + "/qr_menu/cart/")
                .header("session-token", sessionToken)
                .body(request)
                .when()
                .post("/qr_view_cart/" + code)
                .then()
                .extract()
                .response());
    }

    /**
     * After-payment confirmation page. ct comes from the Telr return URL.
     * Do not store MQTT / AWS tokens from this payload.
     */
    public Response viewCustomerCart(
            String code,
            String sessionToken,
            String ct,
            ConfirmationCartRequest request
    ) {

        return ApiCall.retry(() -> RestAssured
                .given()
                .spec(ApiRequestSpec.commonSpec())
                .header("Referer", ApiRequestSpec.getBaseUrl() + "/qr_menu/paybill/?ct=" + ct)
                .header("session-token", sessionToken)
                .queryParam("view", "customer")
                .queryParam("ct", ct)
                .body(request)
                .when()
                .post("/qr_view_cart/" + code + ".json")
                .then()
                .extract()
                .response());
    }
}
