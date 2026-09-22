package co.edu.escuelaing;

import java.io.IOException;

/**
 * Functional interface for handling HTTP requests.
 * Implementations can be lambdas: (req, resp) -> "response body"
 */
@FunctionalInterface
public interface WebService {
    /**
     * Handles the HTTP request and writes the response.
     * 
     * @param req the HTTP request
     * @param resp the HTTP response to populate
     * @return response body as String (for text responses), or null if response was written directly
     */
    void handle(Request req, Response resp) throws IOException;
}