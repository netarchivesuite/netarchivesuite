package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Factory class for instantiating a specific implementation 
 * of {@link ArchiveFileNaming}. The implementation class is defined 
 * by the setting 
 * <em>settings.harvester.harvesting.heritrix.archiveNaming.class</em>.
 */
public class ArchiveFileNamingFactory extends SettingsFactory<ArchiveFileNaming> {

    /**
     * Returns an instance of the default {@link ArchiveFileNaming} 
     * implementation defined by the setting
     * settings.harvester.harvesting.heritrix.archiveNaming.class .
     * This class must have a constructor or factory method with a
     * signature matching the array args.
     * @param args the arguments to the constructor or factory method
     * @throws ArgumentNotValid if the instance cannot be constructed.
     * @return the {@link ArchiveFileNaming} instance.
     */
    public static ArchiveFileNaming getInstance(Object ...args) 
    throws ArgumentNotValid {
        return SettingsFactory.getInstance(
                HarvesterSettings.HERITRIX_ARCHIVE_NAMING_CLASS, args);
    }

}