package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Back-office OMS session tokens.
 * Order: JVM -D property, environment variable, then the env-specific local file
 * ({@code oms.local.properties} for test, {@code oms.staging.local.properties} for staging).
 * Do not put these tokens in testdata.properties or any committed file.
 */
public class OmsCredentials {

    private static final Properties LOCAL = loadLocalFile();

    public static boolean isConfigured() {
        return isPresent(getAuthToken())
                && isPresent(getAaToken())
                && isPresent(getSwitchToken())
                && isPresent(getSwitchedUserToken())
                && isPresent(getTenantToken())
                && isPresent(getEsSessionId());
    }

    public static String getAuthToken() {
        return first("OMS_AUTH_TOKEN", "auth.token");
    }

    public static String getAaToken() {
        return first("OMS_AA_TOKEN", "aa.token");
    }

    public static String getSwitchToken() {
        return first("OMS_SWITCH_TOKEN", "switch.token");
    }

    public static String getSwitchedUserToken() {
        return first("OMS_SWITCHED_USER_TOKEN", "switched.user.token");
    }

    public static String getTenantToken() {
        return first("OMS_TENANT_TOKEN", "tenant.token");
    }

    public static String getEsSessionId() {
        return first("OMS_ES_SESSION_ID", "es.session.id");
    }

    public static String getCounterShiftId() {
        return first("OMS_COUNTER_SHIFT_ID", "counter.shift.id");
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
        String fileName = ConfigReader.omsLocalFileName();
        try (InputStream input = OmsCredentials.class.getClassLoader()
                .getResourceAsStream(fileName)) {
            if (input != null) {
                properties.load(input);
                return properties;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + fileName, e);
        }

        Path[] diskPaths = {
                Path.of("src/test/resources/" + fileName),
                Path.of(fileName)
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
