package dk.netarkivet.harvester.tools;

import java.util.Date;

import dk.netarkivet.harvester.datamodel.DBSpecifics;

/**
 * Utility for updating the harvestdatabase. This makes sure that all tables
 * are upgraded to the version required by the NetarchiveSuite.
 */
public class HarvestdatabaseUpdateApplication {

    /**
     * The main method of the HarvestdatabaseUpdateApplication.
     * Updates all tables in the enum class 
     * {@link dk.netarkivet.harvester.datamodel.HarvesterDatabaseTables}
     * to the required version. There is no attempt to undo the update.
     *
     * @param args no Arg
     */
    public static void main(final String[] args) {
        System.out.println("Beginning database upgrade at " + new Date());
        DBSpecifics.getInstance().updateTables();
        System.out.println("Database upgrade finished at " + new Date());
    }
}
