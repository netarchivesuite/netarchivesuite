package dk.netarkivet.archive;

/**
 * Constants for the Archive module.
 */
public class Constants {
    /**
     * Internationalisation resource bundle.
     */
    public static final String TRANSLATIONS_BUNDLE =
            "dk.netarkivet.archive.Translations";
    
    /**
     * The name of the directory in which files are stored.
     */
    public static final String FILE_DIRECTORY_NAME = "filedir";
    
    /**
     * Temporary directory used during upload, where partial files exist, until
     * moved into directory FILE_DIRECTORY_NAME.
     */
    public static final String TEMPORARY_DIRECTORY_NAME = "tempdir";

    /**
     * Directory where "deleted" files are placed".
     */
    public static final String ATTIC_DIRECTORY_NAME = "atticdir";
}
