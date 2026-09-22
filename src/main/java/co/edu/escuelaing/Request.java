package co.edu.escuelaing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an HTTP request with parsed components.
 */
public class Request {
    private final String method;
    private final String uri;
    private final String path;
    private final String queryString;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;

    public Request(Socket clientSocket) throws IOException {
        this.queryParams = new HashMap<>();
        this.headers = new HashMap<>();
        
        BufferedReader in = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.ISO_8859_1)
        );
        
        // Parse request line
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Empty request line");
        }
        
        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 3) {
            throw new IOException("Invalid request line: " + requestLine);
        }
        
        this.method = requestParts[0];
        this.uri = requestParts[1];
        
        // Parse URI into path and query string
        int queryIndex = this.uri.indexOf('?');
        if (queryIndex >= 0) {
            this.path = this.uri.substring(0, queryIndex);
            this.queryString = this.uri.substring(queryIndex + 1);
            parseQueryString(this.queryString);
        } else {
            this.path = this.uri;
            this.queryString = "";
        }
        
        // Parse headers
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String headerName = line.substring(0, colonIndex).trim().toLowerCase();
                String headerValue = line.substring(colonIndex + 1).trim();
                this.headers.put(headerName, headerValue);
            }
        }
    }
    
    private void parseQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return;
        }
        
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int eqIndex = pair.indexOf('=');
            String key;
            String value;
            if (eqIndex >= 0) {
                key = pair.substring(0, eqIndex);
                value = pair.substring(eqIndex + 1);
            } else {
                key = pair;
                value = "";
            }
            try {
                key = URLDecoder.decode(key, StandardCharsets.UTF_8.name());
                value = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                // If decoding fails, keep original
            }
            // Only keep first value for getValue()
            if (!this.queryParams.containsKey(key)) {
                this.queryParams.put(key, value);
            }
        }
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    /**
     * Returns the first value for the given query parameter name,
     * or null if the parameter doesn't exist.
     */
    public String getValue(String name) {
        return queryParams.get(name);
    }
    
    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }
}