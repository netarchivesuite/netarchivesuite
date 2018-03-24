/*
 * #%L
 * Netarchivesuite - monitor
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

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
        super("sitesection;monitor", "Monitor", 1, new String[][] {{"JMXsummary", "pagetitle;monitor.summary"}},
                "Status", Constants.TRANSLATIONS_BUNDLE);
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
