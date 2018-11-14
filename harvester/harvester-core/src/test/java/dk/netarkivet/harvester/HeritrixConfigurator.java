package dk.netarkivet.harvester;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Contains utility methods to switch between use of Heritrix1 and Heritrix3.
 */
public class HeritrixConfigurator {

    /**
     * No instances.
     */
    private HeritrixConfigurator(){}

    /**
     * Use Heritrix1.
     */
    public static void setUseH1() {
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, "dk.netarkivet.harvester.harvesting.controller.JMXHeritrixController");
        Settings.set(HarvesterSettings.HERITRIX_LAUNCHER_CLASS, "dk.netarkivet.harvester.harvesting.controller.BnfHeritrixLauncher");
    }

    /**
     * Use Heritrix3.
     */
    public static void setUseH3() {
         Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, "dk.netarkivet.harvester.heritrix3.controller.HeritrixController" );
         Settings.set(HarvesterSettings.HERITRIX_LAUNCHER_CLASS, "dk.netarkivet.harvester.heritrix3.controller.HeritrixLauncher");
     }

}
