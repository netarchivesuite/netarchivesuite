package dk.netarkivet.restapi;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

public final class RestApiRegistration {

    private RestApiRegistration() {
    }

    public static void register(StandardContext context) {
        ResourceConfig config = new ResourceConfig()
                .packages("dk.netarkivet.restapi");

        ServletContainer servlet = new ServletContainer(config);

        Tomcat.addServlet(context, "rest-api", servlet);
        context.addServletMappingDecoded("/api/*", "rest-api");
    }
}