package co.edu.escuelaing;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an HTTP response with status, headers, and body.
 */
public class Response {
    private int statusCode = 200;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] bodyBytes = new byte[0];
    private String bodyString = "";
    private boolean isBinary = false;
    private final OutputStream outputStream;

    public Response(OutputStream outputStream) {
        this.outputStream = outputStream;
        // Default headers
        headers.put("Server", "Java HttpServer");
        headers.put("Connection", "close");
    }

    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatus() {
        return statusCode;
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public void setContentType(String contentType) {
        headers.put("Content-Type", contentType);
    }

    /**
     * Sets the response body as text (UTF-8).
     */
    public void setBody(String body) {
        this.bodyString = body;
        this.bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        this.isBinary = false;
        headers.put("Content-Length", String.valueOf(bodyBytes.length));
    }

    /**
     * Sets the response body as binary data.
     */
    public void setBody(byte[] body) {
        this.bodyBytes = body;
        this.bodyString = "";
        this.isBinary = true;
        headers.put("Content-Length", String.valueOf(bodyBytes.length));
    }

    /**
     * Sends the complete HTTP response.
     */
    public void send() throws IOException {
        String statusMessage = getStatusMessage(statusCode);
        
        // Write status line
        String statusLine = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n";
        outputStream.write(statusLine.getBytes(StandardCharsets.ISO_8859_1));
        
        // Write headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String headerLine = entry.getKey() + ": " + entry.getValue() + "\r\n";
            outputStream.write(headerLine.getBytes(StandardCharsets.ISO_8859_1));
        }
        
        // End of headers
        outputStream.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
        
        // Write body
        if (bodyBytes.length > 0) {
            outputStream.write(bodyBytes);
        }
        
        outputStream.flush();
    }

    private String getStatusMessage(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };
    }
}