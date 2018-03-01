/*
 * #%L
 * NetarchiveSuite System test
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
package dk.netarkivet.systemtest;

public class NASSystemUtil {
    public void startSystem() {
    }

    /**
     * Defines the standard application setup in the DK test system.
     */
    public static Application[] getApplications() {
        return new Application[] {
                new Application("KB-test-bar-01", "BitarchiveApplication", null, null, "KBN"),
                new Application("KB-TEST-BAR-016", "BitarchiveApplication", "BitApp_1", null, "KBN"),
                new Application("KB-TEST-BAR-016", "BitarchiveApplication", "BitApp_2", null, "KBN"),
                new Application("KB-TEST-BAR-016", "BitarchiveApplication", "BitApp_3", null, "KBN"),
                new Application("kb-test-acs-001", "ChecksumFileApplication", null, null, "CSN"),
                new Application("kb-test-acs-001", "IndexServerApplication", null, null, "KBN"),
                new Application("kb-test-acs-001", "ViewerProxyApplication", null, null, "KBN"),
                new Application("kb-test-adm-001", "ArcRepositoryApplication", null, null, "KBN"),
                new Application("kb-test-adm-001", "BitarchiveMonitorApplication", "KBBM", null, "KBN"),
                new Application("kb-test-adm-001", "BitarchiveMonitorApplication", "SBBM", null, "SBN"),
                new Application("kb-test-adm-001", "HarvestJobManagerApplication", null, null, "KBN"),
                new Application("kb-test-adm-001", "GUIApplication", null, null, "KBN"),
                new Application("kb-test-har-003", "HarvestControllerApplication", "kblow001_h3", "LOWPRIORITY", "KBN"),
                new Application("kb-test-har-004", "HarvestControllerApplication", "kbhigh_h3", "HIGHPRIORITY", "KBN"),
                new Application("kb-test-har-004", "HarvestControllerApplication", "kblow002_h3", "LOWPRIORITY", "KBN"),
                new Application("sb-test-har-001", "HarvestControllerApplication", "sbhigh_h3", "HIGHPRIORITY", "SBN"),
                new Application("sb-test-bar-001", "BitarchiveApplication", null, null, "SBN"),
                new Application("kb-test-way-001", "WaybackIndexerApplication", null, null, "KBN"),
                new Application("kb-test-way-001", "AggregatorApplication", null, null, "KBN"),
        };
    }
}
