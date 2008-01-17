/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import dk.netarkivet.common.webinterface.SiteSection;

/**
 * Site section that creates the menu for data definitions.
 *
 */
public class DefinitionsSiteSection extends SiteSection {
    /** number of pages visible in the left menu. */
    private final static int PAGES_VISIBLE_IN_MENU = 8;

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
                      {"edit-harvest-templates", "pagetitle;edit.harvest.templates"},
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
                      {"edit-schedule", "pagetitle;edit.schedule"}
              }, "HarvestDefinition",
                 dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    }
}
