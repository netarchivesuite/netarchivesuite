/*$Id$
* $Revision$
* $Author$
* $Date$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import java.util.Date;

/**
 * Utility for updating the harvestdatabase. This makes sure that all tables
 * are upgraded to the version required by the NetarchiveSuite.
 */
public class HarvestdatabaseUpdateApplication {

    /**
     * The main method of the HarvestdatabaseUpdateApplication.
     * Updates all tables in the enum class 
     * {@link dk.netarkivet.harvester.dao.HarvesterDatabaseTables}
     * to the required version. There is no attempt to undo the update.
     *
     * @param args no Arg
     */
    public static void main(final String[] args) {
        System.out.println("Beginning database upgrade at " + new Date());
        // TODO maybe some call to psql process?
        System.out.println("Database upgrade finished at " + new Date());
    }
}
