/* File:        $Id: HarvestChannelSiteSection.java 2251 2012-02-08 13:03:03Z mss $
 * Revision:    $Revision: 2251 $
 * Author:      $Author: mss $
 * Date:        $Date: 2012-02-08 14:03:03 +0100 (Wed, 08 Feb 2012) $
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

package dk.netarkivet.harvester.webinterface;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;

/**
 * Site section that creates the menu for harvest channel and mappings.
 */
public class HarvestChannelSiteSection extends SiteSection {
    /** Logger for this class. */
    private Log log = LogFactory.getLog(getClass().getName());
    /** number of pages visible in the left menu. */
    private static final int PAGES_VISIBLE_IN_MENU = 2;
    
    /**
     * Create a new definition SiteSection object.
     */
    public HarvestChannelSiteSection() {
        super("sitesection;HarvestChannel", "HarvestChannel", PAGES_VISIBLE_IN_MENU,
              new String[][]{
        		      {"edit-harvest-mappings", "pagetitle;edit.harvest.mappings"},
                      {"edit-harvest-channels", "pagetitle;edit.harvest.channels"}                      
                      // The pages listed below are not visible in the left menu
              }, "HarvestChannel",
                 dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    }

    /**
     * Initialise the site section. This forces migration of all DAOs, validates
     * that a default order.xml template exists.
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
