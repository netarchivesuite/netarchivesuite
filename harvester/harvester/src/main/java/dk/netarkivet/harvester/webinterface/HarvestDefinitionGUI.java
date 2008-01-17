/* File:        $Id$
* Revision:     $Revision$
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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.webinterface.GUIWebServer;
import dk.netarkivet.harvester.datamodel.DBSpecifics;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.ScheduleDAO;
import dk.netarkivet.harvester.datamodel.TemplateDAO;
import dk.netarkivet.harvester.scheduler.HarvestScheduler;

/**
 * This class starts a HarvestScheduler and begins scheduling jobs. It also
 * starts a GUI in which new Harvest definitions can be created.
 *
 * Note that this special gui application is really a "hack" to make
 * the webapplications share the same VM as the HarvestScheduler.
 *
 */
public class HarvestDefinitionGUI extends GUIWebServer {

    /** The scheduler being started to schedule and monitor jobs. */
    private HarvestScheduler theScheduler;

    protected static final Log log =
            LogFactory.getLog(HarvestDefinitionGUI.class.getName());

    /**
     * Private to enforce singletonicity.
     */
    private HarvestDefinitionGUI() {
        super();

        // Force migration if needed
        TemplateDAO templateDao = TemplateDAO.getInstance();
        // Enforce, that the default harvest-template set by
        // Settings.DOMAIN_DEFAULT_ORDERXML should exist.
        if (!templateDao.exists(Settings.get(
                Settings.DOMAIN_DEFAULT_ORDERXML))) {
            String message = "The default order template '"
                    + Settings.get(Settings.DOMAIN_DEFAULT_ORDERXML)
                    + "' does not exist in the template DAO. Please use the"
                    + " dk.netarkivet.harvester.datamodel.HarvestTemplate"
                    + "Application tool to upload this template before"
                    + " starting the HarvestDefinitionApplication";
            log.fatal(message);
            throw new UnknownID(message);
        }

        DomainDAO.getInstance();
        ScheduleDAO.getInstance();
        HarvestDefinitionDAO.getInstance();
        JobDAO.getInstance();

        super.startServer();
        theScheduler = HarvestScheduler.getInstance();
    }

    /**
     * Returns the unique instance of this class. If instance is new, starts a
     * GUI web server and the scheduler.
     * @return the instance
     * @throws IllegalState if a GUI is started which is not a
     * HarvestDefinitionGUI
     */
    public static synchronized HarvestDefinitionGUI getInstance() {
        if (instance == null) {
            instance = new HarvestDefinitionGUI();
        } else if (!(instance instanceof HarvestDefinitionGUI)) {
            throw new IllegalState("A GUI not of type HarvestDefinition was "
                                   + "already instantiated.");

        }
        return (HarvestDefinitionGUI) instance;
    }


    /**
     * Closes the GUI webserver, stop scheduling, and nullifies this instance.
     */
    public void close() {
        log.info("Closing the HarvestDefinitionGUI");
        if (theScheduler != null) {
            theScheduler.close();
        }
        cleanup();
        log.info("Closed the HarvestDefinitionGUI");
    }

    /**
     * Closes the GUI webserver, and nullifies this instance quietly.
     * @see CleanupIF#cleanup
     */
    public void cleanup() {
        super.cleanup();
        if (theScheduler != null) {
            theScheduler.cleanup();
        }
        theScheduler = null;
        DBSpecifics.getInstance().shutdownDatabase();
    }
}
