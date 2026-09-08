package api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import pojo.request.QrMenuRequest;

public class QrMenuApi {

    public Response getQrMenuDetails(String code) {
        return getQrMenuDetails(code, true);
    }

    public Response getQrMenuDetails(String code, boolean skipItemOptions) {

        QrMenuRequest request = new QrMenuRequest();
        request.setSkipItemOptions(skipItemOptions);
        request.setIssueQrScanToken(true);

        return RestAssured
                .given()
                .spec(ApiRequestSpec.commonSpec())
                .header("Referer", ApiRequestSpec.getBaseUrl() + "/qr_menu/?code=" + code)
                .body(request)
                .when()
                .post("/qr_online_order/" + code)
                .then()
                .extract()
                .response();
    }
}
