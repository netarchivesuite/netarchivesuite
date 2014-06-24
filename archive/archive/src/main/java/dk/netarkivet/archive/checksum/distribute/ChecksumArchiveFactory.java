    package dk.netarkivet.archive.checksum.distribute;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.checksum.ChecksumArchive;
import dk.netarkivet.common.utils.SettingsFactory;

public class ChecksumArchiveFactory extends SettingsFactory<ChecksumArchive> {
    
    /**
     * Method for retrieving the current ChecksumArchive instance defined
     * in the settings. 
     * 
     * @return The ChecksumArchive defined in the settings.
     */
    public static ChecksumArchive getInstance() {
        return SettingsFactory.getInstance(
                ArchiveSettings.CHECKSUM_ARCHIVE_CLASS);
    }
}
