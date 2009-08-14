package dk.netarkivet.archive.arcrepository.bitpreservation;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * A factory for the ActiveBitPreservation interface. <br/>
 * Currently it can be either of the two classes:<br/>
 * <li>FileBasedActiveBitPreservation. </li>
 * <li>DatabaseBasedActiveBitPreservation.</li>
 * @see dk.netarkivet.archive.arcrepository.bitpreservation.FileBasedActiveBitPreservation
 * @see dk.netarkivet.archive.arcrepository.bitpreservation.DatabaseBasedActiveBitPreservation
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
