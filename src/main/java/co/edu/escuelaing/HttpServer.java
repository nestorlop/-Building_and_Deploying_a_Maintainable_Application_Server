package co.edu.escuelaing;

import java.io.IOException;

/**
 * HTTP Server entry point.
 * Delegates all request handling to WebFramework.
 */
public class HttpServer {

    public static void main(String[] args) throws IOException {
        // Read port from environment variable, default to 8080
        String portStr = System.getenv("PORT");
        int port = 8080;
        if (portStr != null && !portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT value: " + portStr + ", using default 8080");
            }
        }
        
        WebFramework framework = new WebFramework();
        framework.start(port);
    }
}