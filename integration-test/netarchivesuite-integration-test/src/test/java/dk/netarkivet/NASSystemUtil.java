/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet;

public class NASSystemUtil {
    public void startSystem() {
    }

    /**
     * Defines the standard application setup in the DK test system.
     */
    public static Application[] getApplications() {
        return new Application[] {
                new Application("KB-TEST-BAR-013", "BitarchiveServer", null,
                        null, "KBN"),
                new Application("KB-TEST-BAR-014", "BitarchiveServer",
                        "BitApp_1", null, "KBN"),
                new Application("KB-TEST-BAR-014", "BitarchiveServer",
                        "BitApp_2", null, "KBN"),
                new Application("KB-TEST-BAR-014", "BitarchiveServer",
                        "BitApp_3", null, "KBN"),
                new Application("kb-test-acs-001", "ChecksumFileServer", null,
                        null, "CSN"),
                new Application("kb-test-acs-001", "IndexServer", null, null,
                        "KBN"),
                new Application("kb-test-acs-001", "ViewerProxy", null, null,
                        "KBN"),
                new Application("kb-test-adm-001", "ArcRepository", null, null,
                        "KBN"),
                new Application("kb-test-adm-001", "BitarchiveMonitorServer",
                        "KBBM", null, "KBN"),
                new Application("kb-test-adm-001", "BitarchiveMonitorServer",
                        "SBBM", null, "SBN"),
                new Application("kb-test-adm-001", "HarvestMonitorServer",
                        null, null, "KBN"),
                new Application("kb-test-adm-001",
                        "HarvestJobManagerApplication", null, null, "KBN"),
                new Application("kb-test-adm-001", "GUIWebServer", null, null,
                        "KBN"),
                new Application("kb-test-har-001", "HarvestControllerServer",
                        null, "LOWPRIORITY", "KBN"),
                new Application("kb-test-har-002", "HarvestControllerServer",
                        "high", "HIGHPRIORITY", "KBN"),
                new Application("kb-test-har-002", "HarvestControllerServer",
                        "low", "LOWPRIORITY", "KBN"),
                new Application("sb-test-acs-001", "ViewerProxy", null, null,
                        "SBN"),
                new Application("sb-test-bar-001", "BitarchiveServer", null,
                        null, "SBN"),
                new Application("sb-test-har-001", "HarvestControllerServer",
                        null, "HIGHPRIORITY", "SBN") };
    }
}
