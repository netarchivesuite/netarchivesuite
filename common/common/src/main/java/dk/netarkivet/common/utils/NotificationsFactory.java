
package dk.netarkivet.common.utils;

import dk.netarkivet.common.CommonSettings;

/**
 * Get a notifications handler for serious errors.
 *
 */
public class NotificationsFactory extends SettingsFactory<Notifications> {
    /** Get a notification instance to handle serious errors, as defined
     * by the setting settings.common.notifications.class.
     * @return The Notifications instance.
     */
    public static Notifications getInstance() {
        return SettingsFactory.getInstance(CommonSettings.NOTIFICATIONS_CLASS);
    }
}
