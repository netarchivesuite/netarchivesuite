/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.systemtest;

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
                new Application("kb-test-adm-001",
                        "HarvestJobManagerApplication", null, null, "KBN"),
                new Application("kb-test-adm-001", "GUIWebServer", null, null,
                        "KBN"),
                new Application("kb-test-har-001", "HarvestControllerServer",
                        "kblow001", "LOWPRIORITY", "KBN"),
                new Application("kb-test-har-002", "HarvestControllerServer",
                        "kbhigh", "HIGHPRIORITY", "KBN"),
                new Application("kb-test-har-002", "HarvestControllerServer",
                        "kblow002", "LOWPRIORITY", "KBN"),
                new Application("sb-test-acs-001", "ViewerProxy", null, null,
                        "SBN"),
                new Application("sb-test-bar-001", "BitarchiveServer", null,
                        null, "SBN"),
                new Application("sb-test-har-001", "HarvestControllerServer",
                        "sbhigh", "HIGHPRIORITY", "SBN") };
    }
}
