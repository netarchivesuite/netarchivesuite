
package dk.netarkivet.common.distribute.arcrepository;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * A factory for ArcRepositoryClients.
 *
 * Implementation note: This implementation assumes that only one actual
 * implementation class exists, pointed out by the setting
 * settings.common.arcrepositoryClient.class, and merely gives three different
 * view on that class.
 *
 */

public class ArcRepositoryClientFactory
        extends SettingsFactory<ArcRepositoryClient> {
    /** Returns a new ArcRepositoryClient suitable for use by a harvester.
     *
     * @return An ArcRepositoryClient that implements the methods defined by
     * HarvesterArcRepositoryClient.  At end of use, close() should be called
     * on this to release any resources claimed.
     */
    public static HarvesterArcRepositoryClient getHarvesterInstance() {
        return SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT);
    }

    /** Returns a new ArcRepositoryClient suitable for use by a viewer.
     *
     * @return An ArcRepositoryClient that implements the methods defined by
     * ViewerArcRepositoryClient.  At end of use, close() should be called
     * on this to release any resources claimed.
     */
    public static ViewerArcRepositoryClient getViewerInstance() {
        return SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT);
    }

    /** Returns a new ArcRepositoryClient suitable for use in bit preservation.
     *
     * @return An ArcRepositoryClient that implements the methods defined by
     * PreservationArcRepositoryClient. At end of use, close() should be
     * called on this to release any resources claimed.
     */
    public static PreservationArcRepositoryClient getPreservationInstance() {
        return SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT);
    }
}
