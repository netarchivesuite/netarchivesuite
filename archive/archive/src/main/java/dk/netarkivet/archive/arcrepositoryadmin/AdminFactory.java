
package dk.netarkivet.archive.arcrepositoryadmin;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * Factory class for the admin instance. 
 * This creates an instance of the admin structure, which is defined by the 
 * settings.
 * @see dk.netarkivet.archive.ArchiveSettings#ADMIN_CLASS
 */
public class AdminFactory extends SettingsFactory<Admin>{
    /**
     * Retrieves the admin instance defined in the settings.
     * 
     * @return The settings defined admin instance.
     */
    public static Admin getInstance() {
        return SettingsFactory.getInstance(
                ArchiveSettings.ADMIN_CLASS);
    }
}
