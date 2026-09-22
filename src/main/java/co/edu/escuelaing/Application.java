package co.edu.escuelaing;

/**
 * Main application entry point.
 * Configures routes and starts the server.
 */
public class Application {

    public static void main(String[] args) throws Exception {
        WebFramework framework = new WebFramework();
        
        // Configure static files
        framework.staticfiles("/webroot");
        
        // Register GET endpoints using lambdas
        framework.get("/hello", (req, resp) -> {
            String name = req.getValue("name");
            
            if (name == null || name.isBlank()) {
                name = "world";
            }
            
            String prefix = System.getenv().getOrDefault("GREETING_PREFIX", "Hello");
            
            resp.setContentType("text/plain; charset=utf-8");
            resp.setBody(prefix + " " + name);
            resp.send();
        });
        
        framework.get("/pi", (req, resp) -> {
            resp.setContentType("text/plain; charset=utf-8");
            resp.setBody(String.valueOf(Math.PI));
            resp.send();
        });
        
        // Start server (reads PORT from environment)
        String portStr = System.getenv("PORT");
        int port = 8080;
        if (portStr != null && !portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT value: " + portStr + ", using default 8080");
            }
        }
        framework.start(port);
    }
}