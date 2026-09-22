package co.edu.escuelaing;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main web framework that coordinates routing, static files, and server lifecycle.
 */
public class WebFramework {
    private final Router router = new Router();
    private final StaticFileService staticFileService = new StaticFileService();
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private int port = 8080;
    private String appEnv = "development";

    /**
     * Configures the static files directory.
     * 
     * @param path the path prefix (e.g., "/webroot")
     */
    public void staticfiles(String path) {
        // The StaticFileService already uses /webroot as base
        // This method exists for API compatibility
        // Could be extended to support multiple static directories
    }

    /**
     * Registers a GET route handler.
     * 
     * @param path the exact path to match
     * @param handler the handler lambda
     */
    public void get(String path, WebService handler) {
        router.get(path, handler);
    }

    /**
     * Starts the server on the default port (8080).
     */
    public void start() throws IOException {
        start(port);
    }

    /**
     * Starts the server on the specified port.
     * 
     * @param port the port to listen on
     */
    public void start(int port) throws IOException {
        this.port = port;
        this.appEnv = System.getenv().getOrDefault("APP_ENV", "development");
        
        // Register shutdown endpoint only in development
        if ("development".equals(appEnv)) {
            get("/shutdown", (req, resp) -> {
                resp.setContentType("text/plain; charset=utf-8");
                resp.setBody("Server shutting down...\n");
                resp.send();
                stop();
            });
        }
        
        serverSocket = new ServerSocket(port);
        running = true;
        
        System.out.println("Server started on port " + port);
        System.out.println("Environment: " + appEnv);
        System.out.println("Static files: /webroot");
        
        // Main server loop
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                handleRequest(clientSocket);
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
                // If not running, the socket was closed intentionally
            }
        }
        
        System.out.println("Server stopped.");
    }

    /**
     * Handles a single HTTP request.
     */
    private void handleRequest(Socket clientSocket) {
        try {
            Request request = new Request(clientSocket);
            Response response = new Response(clientSocket.getOutputStream());
            
            String method = request.getMethod();
            String path = request.getPath();
            
            // 1. Try dynamic routes first
            WebService handler = router.findHandler(method, path);
            if (handler != null) {
                handler.handle(request, response);
                return;
            }
            
            // 2. Try static files
            if (staticFileService.serve(path, response)) {
                return;
            }
            
            // 3. Not found
            response.setStatus(404);
            response.setContentType("text/plain; charset=utf-8");
            response.setBody("404 Not Found: " + path);
            response.send();
            
        } catch (IOException e) {
            // Try to send error response
            try {
                Response errorResponse = new Response(clientSocket.getOutputStream());
                errorResponse.setStatus(400);
                errorResponse.setContentType("text/plain; charset=utf-8");
                errorResponse.setBody("400 Bad Request: " + e.getMessage());
                errorResponse.send();
            } catch (IOException ex) {
                // Ignore - connection likely closed
            }
            System.err.println("Request handling error: " + e.getMessage());
        } catch (Exception e) {
            // Catch any other exception to prevent server crash
            try {
                Response errorResponse = new Response(clientSocket.getOutputStream());
                errorResponse.setStatus(500);
                errorResponse.setContentType("text/plain; charset=utf-8");
                errorResponse.setBody("500 Internal Server Error");
                errorResponse.send();
            } catch (IOException ex) {
                // Ignore
            }
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Stops the server gracefully.
     */
    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return port;
    }
}