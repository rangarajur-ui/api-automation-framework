package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads environment settings from config.properties on the classpath.
 * Used by API clients so the base URL is not hardcoded in every class.
 */
public class ConfigReader {

    private static final Properties CONFIG = load("config.properties");

    public static String getBaseUrl() {
        return required("base.url");
    }

    public static String getFrontendBuildVersion() {
        return required("frontend.build.version");
    }

    public static String getReportTesterName() {
        return required("report.tester.name");
    }

    public static String getReportEnvironment() {
        return required("report.environment");
    }

    static Properties load(String fileName) {
        Properties properties = new Properties();
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing classpath file: " + fileName
                                + ". Expected under src/test/resources."
                );
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + fileName, e);
        }
        return properties;
    }

    private static String required(String key) {
        String value = CONFIG.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing key in config.properties: " + key);
        }
        return value.trim();
    }
}
