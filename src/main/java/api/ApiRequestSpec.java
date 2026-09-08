package api;

import config.ConfigReader;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

/**
 * Shared RestAssured setup for every Paytm Checkout API call.
 * Base URL and frontend-build-version come from config.properties.
 */
public class ApiRequestSpec {

    public static String getBaseUrl() {
        return ConfigReader.getBaseUrl();
    }

    public static RequestSpecification commonSpec() {
        String baseUrl = getBaseUrl();
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setRelaxedHTTPSValidation()
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Origin", baseUrl)
                .addHeader("frontend-build-version", ConfigReader.getFrontendBuildVersion())
                .build();
    }
}
