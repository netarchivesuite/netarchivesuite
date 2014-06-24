
package dk.netarkivet.harvester.harvesting.controller;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 *  A factory class for HeritrixController instances.
 */
public class HeritrixControllerFactory
        extends SettingsFactory<HeritrixController> {

    /**
     * Returns an instance of the default HeritrixController implementation
     * defined by the setting
     * dk.netarkivet.harvester.harvesting.heritrixController.class .
     * This class must have a constructor or factory method with a
     * signature matching the array args.
     * @param args the arguments to the constructor or factory method
     * @throws ArgumentNotValid if the instance cannot be constructed.
     * @return the HeritrixController instance.
     */
    public static HeritrixController
    getDefaultHeritrixController(Object ...args) throws ArgumentNotValid {
        return SettingsFactory.getInstance(
                HarvesterSettings.HERITRIX_CONTROLLER_CLASS, args);
    }
}
