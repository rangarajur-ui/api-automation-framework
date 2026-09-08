package utils;

/**
 * Avoids printing full session tokens in console logs.
 * Example: abcd1234efgh5678 → ****5678
 */
public class TokenMasker {

    public static String mask(String token) {
        if (token == null || token.isBlank()) {
            return "****";
        }
        if (token.length() <= 4) {
            return "****";
        }
        return "****" + token.substring(token.length() - 4);
    }
}
