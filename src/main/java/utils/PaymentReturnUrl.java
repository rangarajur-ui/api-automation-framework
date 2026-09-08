package utils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * After Telr, the browser is redirected to a Paytm URL that contains ct.
 * Example path: /qr_menu/?code=...&from_pg=true&status=authorised&ct=...
 * initiate_payment does not return ct — extract it from that return URL.
 */
public class PaymentReturnUrl {

    public static String extractCt(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) {
            throw new IllegalArgumentException("Telr return URL is missing");
        }

        String query = URI.create(returnUrl).getRawQuery();
        if (query == null) {
            throw new IllegalArgumentException("Telr return URL has no query string");
        }

        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8);
            if ("ct".equals(key)) {
                String ct = URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
                if (ct.isBlank()) {
                    throw new IllegalArgumentException("ct query parameter is blank");
                }
                return ct;
            }
        }

        throw new IllegalArgumentException("Telr return URL does not contain ct");
    }
}
