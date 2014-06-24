package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.Job;

/** 
 * Implements another way of prefixing archive files in Netarchivesuite. 
 * I.e. collectionName-jobid-harvestid
 */
public class CollectionPrefixNamingConvention implements ArchiveFileNaming {
    
    /** The default place in classpath where the settings file can be found. */
    private static String defaultSettingsClasspath
            = "dk/netarkivet/harvester/harvesting/"
                +"CollectionPrefixNamingConventionSettings.xml";

    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(defaultSettingsClasspath);
    }
    /** The setting for the collectionName. */
    private static String COLLECTION_SETTING = "settings.harvester.harvesting.heritrix"
            + ".archiveNaming.collectionName";
    /** The name of the collection embedded in the names. */
    private static String CollectionName = Settings.get(COLLECTION_SETTING);
    
    
    public CollectionPrefixNamingConvention() {
    }

    @Override
    public String getPrefix(Job theJob) {
        return CollectionName + "-" 
                + theJob.getJobID() + "-" + theJob.getOrigHarvestDefinitionID();
    }
      
}
