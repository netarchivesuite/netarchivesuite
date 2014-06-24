package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.harvester.harvesting.distribute.HarvestControllerServer;

/**
 * This application controls the Heritrix harvester which does the actual
 * harvesting, and is also responsible for uploading the harvested data to the
 * ArcRepository.
 *
 * @see HarvestControllerServer
 */
public class HarvestControllerApplication {
    /**
     * Runs the HarvestController Application. Settings are read from config
     * files.
     *
     * @param args an empty array
     */
    public static void main(String[] args) {
        ApplicationUtils.startApp(HarvestControllerServer.class, args);
    }
}
