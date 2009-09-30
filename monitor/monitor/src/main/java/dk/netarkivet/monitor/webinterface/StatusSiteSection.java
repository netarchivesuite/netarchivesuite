/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.monitor.webinterface;

import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.monitor.Constants;
import dk.netarkivet.monitor.registry.distribute.MonitorRegistryServer;

/**
 * Site section that creates the menu for system status.
 */
public class StatusSiteSection extends SiteSection {
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
     * Shut down monitor server when undeploying,
     */
    public void close() {
        if (monitorListener != null) {
            monitorListener.cleanup();
        }
        monitorListener = null;
    }
}
