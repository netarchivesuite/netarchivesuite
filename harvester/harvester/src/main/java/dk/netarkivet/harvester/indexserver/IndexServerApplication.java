package dk.netarkivet.harvester.indexserver;

import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This class is used to start the IndexServer application.
 *
 * @see IndexServer
 */
public class IndexServerApplication {
    /**
     * Constructor.
     * Private to avoid internal instantiation of this class.
     */
    private IndexServerApplication() { }
    
   /**
    * Runs the IndexServerApplication. Settings are read from
    * config files
    *
    * @param args an empty array
    */
   public static void main(String[] args) {
       ApplicationUtils.startApp(IndexServer.class, args);
   }
}
