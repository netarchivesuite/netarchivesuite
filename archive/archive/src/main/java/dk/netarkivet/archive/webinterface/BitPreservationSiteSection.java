/* File:   $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.webinterface;

import dk.netarkivet.archive.Constants;
import dk.netarkivet.common.webinterface.SiteSection;

/**
 * Site section that creates the menu for bit preservation.
 */
public class BitPreservationSiteSection extends SiteSection {
    /**
     * Create a new bit preservation SiteSection object.
     */
    public BitPreservationSiteSection() {
        super("mainname;bitpreservation", "Bitpreservation", 1,
              new String[][]{
                      {"filestatus", "pagetitle;filestatus"},
                      {"filestatus-checksum",
                              "pagetitle;filestatus.checksum.errors"},
                      {"filestatus-missing", 
                                  "pagetitle;filestatus.files.missing"},
                      {"filestatus-update", 
                                  "pagetitle;filestatus.update"}
              }, "BitPreservation",
                 Constants.TRANSLATIONS_BUNDLE);
    }

    /** No initialisation necessary in this site section. */
    public void initialize() {
    }

    /** No cleanup necessary in this site section. */
    public void close() {
    }
}
