package dk.netarkivet.harvester.webinterface.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ResourceAbstract {

    public void resources_init(NASEnvironment environment);

    public void resources_add(ResourceManagerAbstract resourceManager);

    public void resource_service(ServletContext servletContext, NASUser nas_user, HttpServletRequest req, HttpServletResponse resp, int resource_id, List<Integer> numerics, String pathInfo) throws IOException;

}
