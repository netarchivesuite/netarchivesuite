
package dk.netarkivet.common.distribute.monitorregistry;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * A factory for MonitorRegistryClient.
 */
public class MonitorRegistryClientFactory
        extends SettingsFactory<MonitorRegistryClient> {
    /** Returns a new MonitorRegistryClient as defined by the setting
     * Settings.MONITOR_REGISTRY_CLIENT. 
     * @return A MonitorRegistryClient.
     */
    public static MonitorRegistryClient getInstance() {
        return SettingsFactory.getInstance(
                CommonSettings.MONITOR_REGISTRY_CLIENT);
    }
}