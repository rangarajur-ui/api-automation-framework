package api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import pojo.request.PaymentRequest;

public class PaymentApi {

    public Response initiatePayment(String code, String sessionToken, PaymentRequest request) {

        return RestAssured
                .given()
                .spec(ApiRequestSpec.commonSpec())
                .header("Referer", ApiRequestSpec.getBaseUrl() + "/qr_menu/cart/")
                .header("session-token", sessionToken)
                .body(request)
                .when()
                .post("/initiate_payment/" + code)
                .then()
                .extract()
                .response();
    }

    /**
     * Polls order/payment result after Telr redirects back.
     * ct is a query param on the Paytm return URL, not a field from initiate_payment.
     */
    public Response fetchPaymentStatus(String code, String sessionToken, String ct) {

        return RestAssured
                .given()
                .spec(ApiRequestSpec.commonSpec())
                .header("Referer", ApiRequestSpec.getBaseUrl() + "/qr_menu/paybill/?ct=" + ct)
                .header("session-token", sessionToken)
                .queryParam("ct", ct)
                .when()
                .get("/fetch_payment_status_by_session_id/" + code)
                .then()
                .extract()
                .response();
    }
}
