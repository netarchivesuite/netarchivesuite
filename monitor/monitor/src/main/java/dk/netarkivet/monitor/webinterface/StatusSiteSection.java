
package dk.netarkivet.monitor.webinterface;

import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.monitor.Constants;
import dk.netarkivet.monitor.registry.distribute.MonitorRegistryServer;

/**
 * Site section that creates the menu for system status.
 */
public class StatusSiteSection extends SiteSection {
    /** The monitorRegistryServer used by this SiteSection. */
    private MonitorRegistryServer monitorListener;

    /**
     * Create a new status SiteSection object.
     */
    public StatusSiteSection() {
        super("sitesection;monitor", "Monitor", 1,
              new String[][]{
                      {"JMXsummary", "pagetitle;monitor.summary"}
              }, "Status", Constants.TRANSLATIONS_BUNDLE);
    }

    /**
     * Register monitor server when deploying.
     */
    public void initialize() {
        monitorListener = MonitorRegistryServer.getInstance();
    }

    /**
     * Shut down monitor server when undeploying.
     */
    public void close() {
        if (monitorListener != null) {
            monitorListener.cleanup();
        }
        monitorListener = null;
    }
}
