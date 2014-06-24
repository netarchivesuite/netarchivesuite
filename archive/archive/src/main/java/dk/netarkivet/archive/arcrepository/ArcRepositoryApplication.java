
package dk.netarkivet.archive.arcrepository;

import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This class is used to start the ArcRepository application.
 *
 */
public final class ArcRepositoryApplication {

    /**
     * Constructor. Private to ensure that this utility class cannot be 
     * instantiated.
     */
    private ArcRepositoryApplication() {}

    /**
     * Runs the ArcRepository Application. Settings are read from
     * config files
     *
     * @param args an empty array
     */
    public static void main(final String[] args) {
        ApplicationUtils.startApp(ArcRepository.class, args);
    }

}
