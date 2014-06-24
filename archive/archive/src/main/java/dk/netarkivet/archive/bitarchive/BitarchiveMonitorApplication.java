package dk.netarkivet.archive.bitarchive;

import dk.netarkivet.archive.bitarchive.distribute.BitarchiveMonitorServer;
import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This class is used to start the BitarchiveMonitor application.
 *
 * @see BitarchiveMonitorServer
 */
public class BitarchiveMonitorApplication {
    /**
     * Private constructor to prevent instantiation of this class.
     */
    private BitarchiveMonitorApplication() {}
    
   /**
    * Runs the BitarchiveMonitor. Settings are read from
    * config files
    *
    * @param args an empty array
    */
   public static void main(String[] args) {
       ApplicationUtils.startApp(BitarchiveMonitorServer.class, args);
   }
}
