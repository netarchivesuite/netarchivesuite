package dk.netarkivet.harvester.datamodel;

import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.harvester.webinterface.DefinitionsSiteSection;

public class MigrateDefaultHarvestDatabase {

    /**
     * @param args
     */
    public static void main(String[] args) {
        SiteSection sitesection = new DefinitionsSiteSection();
        sitesection.initialize();
    }
}
