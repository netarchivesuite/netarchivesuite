package dk.netarkivet.harvester.tools.dummy;

import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This application starts the FaultyHarvestControllerServer.
 *
 * @see HarvestControllerServer
 */
public class HarvestControllerApplication {

	/**
	 * Runs the HarvestController Application. Settings are read from config files.
	 *
	 * @param args an empty array
	 */
	public static void main(String[] args) {
		ApplicationUtils.startApp(FaultyHarvestControllerServer.class, args);
	}

}
