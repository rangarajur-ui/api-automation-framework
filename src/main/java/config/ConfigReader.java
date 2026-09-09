package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads environment settings from the classpath.
 * Default is test ({@code config.properties}). Staging is
 * {@code -Denv=staging} or {@code ENV=staging} → {@code config-staging.properties}.
 */
public class ConfigReader {

    private static final String ENV = resolveEnv();
    private static final String CONFIG_FILE = configFileName(ENV);
    private static final Properties CONFIG = load(CONFIG_FILE);

    public static String getActiveEnvironment() {
        return ENV;
    }

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

    public static String getOmsBaseUrl() {
        return required("oms.base.url");
    }

    public static String getOmsAaUrl() {
        return required("oms.aa.url");
    }

    public static String getOmsReferer() {
        return required("oms.referer");
    }

    public static String getOmsAppVersion() {
        return required("oms.app.version");
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

    static String testdataFileName() {
        if ("test".equals(ENV)) {
            return "testdata.properties";
        }
        return "testdata-" + ENV + ".properties";
    }

    static String omsLocalFileName() {
        if ("test".equals(ENV)) {
            return "oms.local.properties";
        }
        return "oms." + ENV + ".local.properties";
    }

    private static String resolveEnv() {
        String property = System.getProperty("env");
        if (property != null && !property.isBlank()) {
            return property.trim().toLowerCase();
        }
        String environment = System.getenv("ENV");
        if (environment != null && !environment.isBlank()) {
            return environment.trim().toLowerCase();
        }
        return "test";
    }

    private static String configFileName(String env) {
        if ("test".equals(env)) {
            return "config.properties";
        }
        return "config-" + env + ".properties";
    }

    private static String required(String key) {
        String value = CONFIG.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing key in " + CONFIG_FILE + ": " + key);
        }
        return value.trim();
    }
}
