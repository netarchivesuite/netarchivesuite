package dk.netarkivet.harvester.webinterface.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class NASEnvironment {

    /** servletConfig. */
    public ServletConfig servletConfig = null;

    protected Heritrix3JobMonitorThread h3JobMonitorThread;

    public static String contextPath;

    public static String servicePath;

    public NASEnvironment(ServletContext servletContext, ServletConfig theServletConfig) throws ServletException {
        this.servletConfig = theServletConfig;
        h3JobMonitorThread = new Heritrix3JobMonitorThread();
        h3JobMonitorThread.start();
    }

    /**
     * Do some cleanup. This waits for the different workflow threads to stop running.
     */
    public void cleanup() {
        servletConfig = null;
    }

}
