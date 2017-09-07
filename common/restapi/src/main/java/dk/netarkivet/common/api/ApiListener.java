package dk.netarkivet.common.api;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import dk.netarkivet.monitor.registry.distribute.MonitorRegistryServer;

/**
 * Created by csr on 9/6/17.
 */
public class ApiListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        MonitorRegistryServer.getInstance();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
