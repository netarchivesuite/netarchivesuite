
package dk.netarkivet.common.webinterface;

import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This class is used to start the GUI web applications server.
 *
 */
public class GUIApplication {
   /**
    * Runs the GUI web server. Settings are read from config files.
    *
    * @param args an empty array
    */
   public static void main(String[] args) {
       ApplicationUtils.startApp(GUIWebServer.class, args);
   }
}
