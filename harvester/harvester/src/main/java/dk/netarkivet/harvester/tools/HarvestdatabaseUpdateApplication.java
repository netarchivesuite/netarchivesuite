/*$Id$
* $Revision$
* $Author$
* $Date$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Sodftware Foundation; either
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
package dk.netarkivet.harvester.tools;

import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDBDAO;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;
import dk.netarkivet.harvester.datamodel.ScheduleDAO;
import dk.netarkivet.harvester.datamodel.TemplateDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypeDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDBDAO;

/**
 * Utility for updating the harvestdatabase. This makes sure that all tables
 * are upgraded to the version required by the NetarchiveSuite.
 */
public class HarvestdatabaseUpdateApplication {

    /**
     * The main method of the HarvestdatabaseUpdateApplication.
     * @param args no Arg
     */
    public static void main(final String[] args) {
        System.out.println("Beginning database upgrade");
        ExtendedFieldDBDAO.getInstance();
        ExtendedFieldTypeDBDAO.getInstance();
        ExtendedFieldValueDBDAO.getInstance();
        TemplateDAO.getInstance();
        RunningJobsInfoDAO.getInstance();
        JobDBDAO.getInstance();
        DomainDAO.getInstance();
        ScheduleDAO.getInstance();
        HarvestDefinitionDAO.getInstance();
        GlobalCrawlerTrapListDAO.getInstance();
        System.out.println("Database upgrade finished");
    }   
 }
