
package dk.netarkivet.common.utils;

import dk.netarkivet.common.CommonSettings;

/**
 * Factory for FreeSpaceProvider.
 *
 */
public class FreeSpaceProviderFactory extends SettingsFactory<Notifications> {
    /** Get a FreeSpaceProvider instance to inform about the free space.
     * @return The FreeSpaceProvider instance.
     */
    public static FreeSpaceProvider getInstance() {
        return SettingsFactory.getInstance(
                CommonSettings.FREESPACE_PROVIDER_CLASS);
    }
}
