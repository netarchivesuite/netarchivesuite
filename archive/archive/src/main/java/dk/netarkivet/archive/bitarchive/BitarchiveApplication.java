package dk.netarkivet.archive.bitarchive;

import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This class is used to start the BitArchive application.
 *
 *
 */
public final class BitarchiveApplication {

    /**
     * Private constructor. Not for initialisation.
     */
    private BitarchiveApplication(){}

   /**
    * Runs the BitarchiveApplication. Settings are read from
    * config files
    *
    * @see BitarchiveServer
    * @param args an empty array
    */
   public static void main(String[] args) {
      ApplicationUtils.startApp(BitarchiveServer.class, args);
   }
}
