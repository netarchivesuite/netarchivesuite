/*
 * #%L
 * Netarchivesuite - harvester
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

package dk.netarkivet.harvester.webinterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;

/**
 * Site section that creates the menu for harvest channel and mappings.
 */
@SuppressWarnings({"unused"})
public class HarvestChannelSiteSection extends SiteSection {
    /** Logger for this class. */
    //private Log log = LogFactory.getLog(getClass().getName());
    private static final Logger log = LoggerFactory.getLogger(HarvestChannelSiteSection.class);
    /** number of pages visible in the left menu. */
    private static final int PAGES_VISIBLE_IN_MENU = 2;

    /**
     * Create a new definition SiteSection object.
     */
    public HarvestChannelSiteSection() {
        super("sitesection;HarvestChannel", "HarvestChannel", PAGES_VISIBLE_IN_MENU, new String[][] {
                {"edit-harvest-mappings", "pagetitle;edit.harvest.mappings"},
                {"edit-harvest-channels", "pagetitle;edit.harvest.channels"}
        // The pages listed below are not visible in the left menu
                }, "HarvestChannel", dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    }

    /**
     * Initialise the site section.
     *
     * @throws UnknownID If the default order.xml does not exist.
     */
    public void initialize() {
        HarvestDefinitionDAO.getInstance();
        HarvestChannelDAO.getInstance();
    }

    /** Release DB resources. */
    public void close() {
        HarvestDBConnection.cleanup();
    }
}
