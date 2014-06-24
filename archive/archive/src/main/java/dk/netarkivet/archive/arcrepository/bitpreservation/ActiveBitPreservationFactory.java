package dk.netarkivet.archive.arcrepository.bitpreservation;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * A factory for the ActiveBitPreservation interface. <br/>
 * Creates an instance of the ActiveBitPreservation from on the setting
 * settings.archive.bitpreservation.class.
 * 
 * @see 
 * dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservation
 */
public class ActiveBitPreservationFactory 
        extends SettingsFactory<ActiveBitPreservation>{

    /**
     * Method for retrieving the current ActiveBitPreservation instance defined
     * in the settings. 
     * 
     * @return The ActiveBitPreservation defined in the settings.
     */
    public static ActiveBitPreservation getInstance() {
        return SettingsFactory.getInstance(
                ArchiveSettings.CLASS_ARCREPOSITORY_BITPRESERVATION);
    }
}
