package dk.netarkivet.wayback;

import dk.netarkivet.common.utils.Settings;

/**
 * Settings specific to the wayback module of NetarchiveSuite.
 */
public class WaybackSettings {
      /** The default place in classpath where the settings
       * file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/wayback/settings.xml";

     /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH
        );
    }

    public static String URL_CANONICALIZER_CLASSNAME = 
            "settings.wayback.urlcanonicalizer.classname";
}
