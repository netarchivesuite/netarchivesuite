package dk.netarkivet.heritrix3.monitor;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dk.netarkivet.heritrix3.monitor.HttpLocaleHandler.HttpLocale;

public interface ResourceAbstract {

    public void resources_init(NASEnvironment environment);

    public void resources_add(ResourceManagerAbstract resourceManager);

    public void resource_service(ServletContext servletContext, NASUser nas_user, HttpServletRequest req, HttpServletResponse resp, HttpLocale httpLocale, int resource_id, List<Integer> numerics, String pathInfo) throws IOException;

}
