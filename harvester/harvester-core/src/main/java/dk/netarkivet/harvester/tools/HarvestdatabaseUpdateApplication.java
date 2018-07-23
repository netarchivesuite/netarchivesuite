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
package dk.netarkivet.harvester.tools;

import java.util.Date;

import dk.netarkivet.harvester.datamodel.DBSpecifics;

/**
 * Utility for updating the harvestdatabase. This makes sure that all tables are upgraded to the version required by the
 * NetarchiveSuite.
 */
public class HarvestdatabaseUpdateApplication {

    /**
     * The main method of the HarvestdatabaseUpdateApplication. Updates all tables in the enum class
     * {@link dk.netarkivet.harvester.datamodel.HarvesterDatabaseTables} to the required version. There is no attempt to
     * undo the update.
     *
     * @param args no Arg
     */
    public static void main(final String[] args) {
        System.out.println("Beginning database upgrade at " + new Date());
        DBSpecifics.getInstance().updateTables();
        System.out.println("Database upgrade finished at " + new Date());
    }
}
