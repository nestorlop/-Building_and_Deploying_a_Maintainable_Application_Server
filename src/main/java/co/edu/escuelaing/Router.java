package co.edu.escuelaing;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes HTTP requests to registered handlers.
 */
public class Router {
    private final Map<String, WebService> routes = new HashMap<>();

    /**
     * Registers a GET route handler.
     * 
     * @param path the exact path to match (e.g., "/hello")
     * @param handler the handler for this route
     */
    public void get(String path, WebService handler) {
        routes.put("GET:" + path, handler);
    }

    /**
     * Finds a handler for the given method and path.
     * 
     * @param method HTTP method (e.g., "GET")
     * @param path request path
     * @return the handler, or null if not found
     */
    public WebService findHandler(String method, String path) {
        return routes.get(method + ":" + path);
    }

    /**
     * Checks if a route is registered for the given method and path.
     */
    public boolean hasRoute(String method, String path) {
        return routes.containsKey(method + ":" + path);
    }
}