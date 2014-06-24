package dk.netarkivet.viewerproxy;

import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This class is used to start the ViewerProxy application. This starts
 * a GUI which can be used to browse the archive.
 *
 */
public class ViewerProxyApplication {
   /**
    * Runs the ViewerProxyApplication. Settings are read from
    * config files
    *
    * @param args an empty array
    */
   public static void main(String[] args) {
       ApplicationUtils.startApp(ViewerProxy.class, args);
   }
}
