/*
 * #%L
 * Netarchivesuite - archive
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

package dk.netarkivet.archive.webinterface;

import dk.netarkivet.archive.Constants;
import dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservationFactory;
import dk.netarkivet.common.webinterface.SiteSection;

/**
 * Site section that creates the menu for bit preservation.
 */
public class BitPreservationSiteSection extends SiteSection {
    /**
     * The number of pages visible in the menu. The visible pages: filestatus, batchoverview
     */
    private static final int PAGES_VISIBLE_IN_MENU = 2;

    /**
     * Create a new bit preservation SiteSection object.
     */
    public BitPreservationSiteSection() {
        super("mainname;bitpreservation", "Bitpreservation", PAGES_VISIBLE_IN_MENU, new String[][] {
                {"filestatus", "pagetitle;filestatus"},
                {"batchoverview", "pagetitle;batchjob.overview"},
                // Pages below is not visible in the menu
                {"batchjob", "pagetitle;batchjob"}, {"batchjob-retrieve", "pagetitle;batchjob.retrieve.resultfile"},
                {"batchjob-execute", "pagetitle;batchjob.execute"},
                {"filestatus-checksum", "pagetitle;filestatus.checksum.errors"},
                {"filestatus-missing", "pagetitle;filestatus.files.missing"},
                {"filestatus-update", "pagetitle;filestatus.update"}}, "BitPreservation", Constants.TRANSLATIONS_BUNDLE);
    }

    /**
     * Initialize ActiveBitPreservation singleton. Should speed up the 1st rendering of the JSP pages.
     */
    public void initialize() {
        ActiveBitPreservationFactory.getInstance();
    }

    /** No cleanup necessary in this site section. */
    public void close() {
    }
}
