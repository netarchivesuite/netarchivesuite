package dk.netarkivet.wayback.indexer;

import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * The entry point for the wayback indexer. This application determines what
 * files in the arcrepository remain to be indexed and indexes them concurrently
 * via batch jobs. The status of all files in the archive is maintained in a
 * persistent object store managed by Hibernate.
 */
public class WaybackIndexerApplication {

    /**
     * Runs the WaybackIndexer. Settings are read from config files so the
     * arguments array should be empty.
     * @param args an empty array.
     */
    public static void main(String[] args) {
        ApplicationUtils.startApp(WaybackIndexer.class, args);
    }

}
