package co.edu.escuelaing;

import co.edu.escuelaing.WebService;
import java.util.HashMap;
import java.util.Map;

public class WebFramework{
    Map<String, WebService> webServices = new HashMap();

    public void get(String route, WebService ws){
        webServices.put(route,ws);
    }
    public static void start(){}
    
}