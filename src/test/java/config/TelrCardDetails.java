package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Card details for the Telr hosted page.
 * Order: JVM -D property, environment variable, then telr.local.properties.
 * Do not put card numbers in testdata.properties or any committed file.
 *
 * TELR_CARD_NUMBER / TELR_CVV / TELR_EXP_MONTH / TELR_EXP_YEAR
 */
public class TelrCardDetails {

    private static final Properties LOCAL = loadLocalFile();

    public static boolean isConfigured() {
        return isPresent(getCardNumber())
                && isPresent(getCvv())
                && isPresent(getExpiryMonth())
                && isPresent(getExpiryYear());
    }

    public static String getCardNumber() {
        return first("TELR_CARD_NUMBER", "card.number");
    }

    public static String getCvv() {
        return first("TELR_CVV", "card.cvv");
    }

    public static String getExpiryMonth() {
        return first("TELR_EXP_MONTH", "card.exp.month");
    }

    public static String getExpiryYear() {
        return first("TELR_EXP_YEAR", "card.exp.year");
    }

    public static boolean isHeadless() {
        String value = first("TELR_HEADLESS", "headless");
        return Boolean.parseBoolean(value);
    }

    private static String first(String key, String fileKey) {
        String property = System.getProperty(key);
        if (isPresent(property)) {
            return property.trim();
        }
        String env = System.getenv(key);
        if (isPresent(env)) {
            return env.trim();
        }
        String fromFile = LOCAL.getProperty(fileKey);
        if (isPresent(fromFile)) {
            return fromFile.trim();
        }
        return "";
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static Properties loadLocalFile() {
        Properties properties = new Properties();
        try (InputStream input = TelrCardDetails.class.getClassLoader()
                .getResourceAsStream("telr.local.properties")) {
            if (input != null) {
                properties.load(input);
                return properties;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read telr.local.properties", e);
        }

        Path[] diskPaths = {
                Path.of("src/test/resources/telr.local.properties"),
                Path.of("telr.local.properties")
        };
        for (Path path : diskPaths) {
            if (Files.exists(path)) {
                try (InputStream input = Files.newInputStream(path)) {
                    properties.load(input);
                    return properties;
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read " + path, e);
                }
            }
        }
        return properties;
    }
}
