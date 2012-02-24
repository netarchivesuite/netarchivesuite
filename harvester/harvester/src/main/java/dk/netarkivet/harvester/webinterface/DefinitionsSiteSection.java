/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.*;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;
import dk.netarkivet.harvester.tools.HarvestTemplateApplication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Site section that creates the menu for data definitions.
 */
public class DefinitionsSiteSection extends SiteSection {
    /** Logger for this class. */
    private Log log = LogFactory.getLog(getClass().getName());
    /** number of pages visible in the left menu. */
    private static final int PAGES_VISIBLE_IN_MENU = 10;
    
    /**
     * Create a new definition SiteSection object.
     */
    public DefinitionsSiteSection() {
        super("sitesection;definitions", "Definitions", PAGES_VISIBLE_IN_MENU,
              new String[][]{
                      {"selective-harvests", "pagetitle;selective.harvests"},
                      {"snapshot-harvests", "pagetitle;snapshot.harvests"},
                      {"schedules", "pagetitle;schedules"},
                      {"find-domains", "pagetitle;find.domains"},
                      {"create-domain", "pagetitle;create.domain"},
                      {"domain-statistics", "pagetitle;domain.statistics"},
                      {"alias-summary", "pagetitle;alias.summary"},
                      {"edit-harvest-templates", 
                          "pagetitle;edit.harvest.templates"},
                      {"edit-global-crawler-traps",
                          "pagetitle;edit.global.crawler.traps"},
                      {"list-extendedfields",
                      "pagetitle;list-extendedfields"},
                      // The pages listed below are not visible in the left menu
                      {"upload-harvest-template",
                              "pagetitle;upload.template"},
                      {"download-harvest-template",
                              "pagetitle;download.template"},
                      {"edit-snapshot-harvest", "pagetitle;snapshot.harvest"},
                      {"edit-selective-harvest", "pagetitle;selective.harvest"},
                      {"edit-domain", "pagetitle;edit.domain"},
                      {"ingest-domains", "pagetitle;ingest.domains"},
                      {"add-event-seeds", "pagetitle;add.seeds"},
                      {"edit-domain-config", "pagetitle;edit.configuration"},
                      {"edit-domain-seedlist", "pagetitle;edit.seed.list"},
                      {"edit-schedule", "pagetitle;edit.schedule"},
                      {"edit-extendedfield", "pagetitle;edit.extendedfield"}
              }, "HarvestDefinition",
                 dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    }

    /**
     * Initialise the site section. This forces migration of all DAOs, validates
     * that a default order.xml template exists.
     *
     * @throws UnknownID If the default order.xml does not exist.
     */
    public void initialize() {
        // Force migration if needed
        TemplateDAO templateDao = TemplateDAO.getInstance();
        // Enforce, that the default harvest-template set by
        // Settings.DOMAIN_DEFAULT_ORDERXML should exist.
        if (!templateDao.exists(Settings.get(
                HarvesterSettings.DOMAIN_DEFAULT_ORDERXML))) {
            String message = "The default order template '"
                    + Settings.get(HarvesterSettings.DOMAIN_DEFAULT_ORDERXML)
                    + "' does not exist in the template DAO. Please use the "
                    + HarvestTemplateApplication.class.getName()
                    + " tool to upload this template before"
                    + " loading the Definitions site section in the"
                    + " GUIApplication";
            log.fatal(message);
            throw new UnknownID(message);
        }

        DomainDAO.getInstance();
        ScheduleDAO.getInstance();
        HarvestDefinitionDAO.getInstance();
        JobDAO.getInstance();
        GlobalCrawlerTrapListDAO.getInstance();
        // Start the harvest monitor sever
        HarvestMonitor.getInstance();
    }
    
    /** Release DB resources. */
    public void close() {
        HarvestDBConnection.cleanup();
    }
}
