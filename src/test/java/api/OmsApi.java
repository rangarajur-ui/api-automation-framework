package api;

import config.ConfigReader;
import config.OmsCredentials;
import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * Back-office OMS order APIs. Tokens come from OmsCredentials, never from logs.
 */
public class OmsApi {

    public Response getOrderDetails(int orderId) {
        return ApiCall.retry(() -> RestAssured
                .given()
                .relaxedHTTPSValidation()
                .baseUri(ConfigReader.getOmsBaseUrl())
                .header("Accept", "application/json")
                .header("app-version", ConfigReader.getOmsAppVersion())
                .header("eid-flow", "true")
                .header("auth-token", OmsCredentials.getAuthToken())
                .header("aa-token", OmsCredentials.getAaToken())
                .header("switch-token", OmsCredentials.getSwitchToken())
                .header("switched-user-token", OmsCredentials.getSwitchedUserToken())
                .header("tenant-token", OmsCredentials.getTenantToken())
                .header("es-session-id", OmsCredentials.getEsSessionId())
                .header("counter-shift-id", OmsCredentials.getCounterShiftId())
                .header("aa-url", ConfigReader.getOmsAaUrl())
                .header("Origin", ConfigReader.getOmsReferer().replaceAll("/$", ""))
                .header("Referer", ConfigReader.getOmsReferer())
                .when()
                .get("/m/oms/orders/" + orderId + "/details.json")
                .then()
                .extract()
                .response());
    }
}
