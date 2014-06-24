package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Factory class for instantiating a specific implementation 
 * of {@link HeritrixLauncher}. The implementation class is defined 
 * by the setting 
 * <em>dk.netarkivet.harvester.harvesting.heritrixLauncher.class</em>.
 */
public class HeritrixLauncherFactory extends SettingsFactory<HeritrixLauncher> {

    /**
     * Returns an instance of the default {@link HeritrixLauncher} 
     * implementation defined by the setting
     * dk.netarkivet.harvester.harvesting.heritrixLauncher.class .
     * This class must have a constructor or factory method with a
     * signature matching the array args.
     * @param args the arguments to the constructor or factory method
     * @throws ArgumentNotValid if the instance cannot be constructed.
     * @return the {@link HeritrixLauncher} instance.
     */
    public static HeritrixLauncher getInstance(Object ...args) 
    throws ArgumentNotValid {
        return SettingsFactory.getInstance(
                HarvesterSettings.HERITRIX_LAUNCHER_CLASS, args);
    }

}
