package co.edu.escuelaing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Serves static files from the classpath (src/main/resources/webroot).
 * Protects against path traversal attacks.
 */
public class StaticFileService {
    private static final String WEBROOT_BASE = "webroot";
    private final Map<String, String> mimeTypes = new HashMap<>();

    public StaticFileService() {
        // Initialize MIME types
        mimeTypes.put(".html", "text/html; charset=utf-8");
        mimeTypes.put(".htm", "text/html; charset=utf-8");
        mimeTypes.put(".css", "text/css; charset=utf-8");
        mimeTypes.put(".js", "application/javascript; charset=utf-8");
        mimeTypes.put(".png", "image/png");
        mimeTypes.put(".jpg", "image/jpeg");
        mimeTypes.put(".jpeg", "image/jpeg");
        mimeTypes.put(".gif", "image/gif");
        mimeTypes.put(".txt", "text/plain; charset=utf-8");
        mimeTypes.put(".ico", "image/x-icon");
        mimeTypes.put(".svg", "image/svg+xml");
        mimeTypes.put(".json", "application/json; charset=utf-8");
        mimeTypes.put(".woff", "font/woff");
        mimeTypes.put(".woff2", "font/woff2");
        mimeTypes.put(".ttf", "font/ttf");
        mimeTypes.put(".eot", "application/vnd.ms-fontobject");
    }

    /**
     * Attempts to serve a static file for the given path.
     * 
     * @param requestPath the request path (e.g., "/index.html", "/images/logo.png")
     * @param response the response object to write to
     * @return true if file was found and served, false otherwise
     */
    public boolean serve(String requestPath, Response response) {
        // Normalize path - remove leading slash
        String normalizedPath = requestPath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // Handle root path - default to index.html
        if (normalizedPath.isEmpty()) {
            normalizedPath = "index.html";
        }
        
        // Prevent path traversal - decode URL encoding first
        try {
            normalizedPath = java.net.URLDecoder.decode(normalizedPath, "UTF-8");
        } catch (Exception e) {
            // If decoding fails, use as-is
        }
        
        // Check for path traversal attempts
        if (containsPathTraversal(normalizedPath)) {
            return false;
        }
        
        // Construct resource path
        String resourcePath = WEBROOT_BASE + "/" + normalizedPath;
        
        // Get resource as stream
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            return false;
        }
        
        try {
            byte[] content = inputStream.readAllBytes();
            
            // Determine MIME type
            String mimeType = getMimeType(normalizedPath);
            response.setContentType(mimeType);
            response.setBody(content);
            response.send();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Checks if the path contains path traversal attempts.
     */
    private boolean containsPathTraversal(String path) {
        // Split by path separators and check each segment
        String[] segments = path.split("[/\\\\]");
        for (String segment : segments) {
            // Check for directory traversal
            if (segment.equals("..") || segment.equals(".")) {
                return true;
            }
            // Check for URL-encoded variants
            String lower = segment.toLowerCase();
            if (lower.contains("%2e%2e") ||  // .. encoded
                lower.contains("%2e") ||     // . encoded
                lower.contains("%2f") ||     // / encoded
                lower.contains("%5c")) {     // \ encoded
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines MIME type based on file extension.
     */
    private String getMimeType(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            String extension = path.substring(lastDot).toLowerCase();
            return mimeTypes.getOrDefault(extension, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}