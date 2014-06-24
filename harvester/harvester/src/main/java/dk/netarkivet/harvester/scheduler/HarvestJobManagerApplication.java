package dk.netarkivet.harvester.scheduler;

import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This wrapper class is used to start the {@link HarvestJobManager}
 * application.
 */
public class HarvestJobManagerApplication {

    /**
     * Runs the <code>HarvestJobManager</code>. Settings are read from config 
     * files so the arguments array should be empty.
     * @param args an empty array.
     */
    public static void main(String[] args) {
        ApplicationUtils.startApp(new HarvestJobManager());
    }
}
