package api;

import io.restassured.response.Response;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.Callable;
import javax.net.ssl.SSLException;

/**
 * Retries a request when the remote host drops TLS or the socket.
 * Business 4xx/5xx responses are returned as-is.
 */
public final class ApiCall {

    private static final int ATTEMPTS = 3;

    private ApiCall() {
    }

    public static Response retry(Callable<Response> request) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                Response response = request.call();
                if (response.statusCode() != 429 || attempt == ATTEMPTS) {
                    return response;
                }
            } catch (Exception error) {
                last = wrap(error);
                if (!isTransient(error) || attempt == ATTEMPTS) {
                    throw last;
                }
            }
            sleep(attempt);
        }
        throw last == null ? new IllegalStateException("API call failed") : last;
    }

    private static boolean isTransient(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SSLException
                    || current instanceof SocketException
                    || current instanceof IOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static RuntimeException wrap(Exception error) {
        if (error instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException(error.getMessage(), error);
    }

    private static void sleep(int attempt) {
        try {
            Thread.sleep(2000L * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
