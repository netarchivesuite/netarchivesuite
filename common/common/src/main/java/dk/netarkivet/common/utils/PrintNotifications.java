
package dk.netarkivet.common.utils;

/**
 * A notification implementation that prints notifications on
 * System.err.
 */
public class PrintNotifications extends Notifications {
 
    /**
     * Reacts to a notification by printing the notification to
     * System.err.
     *
     * @param message The error message to print.
     * @param e       The exception to print, if not null.
     */
    public void notify(String message, NotificationType eventType, Throwable e) {
        System.err.println("[" + eventType + "]:" + message);
        if (e != null) {
            e.printStackTrace(System.err);
        }
    }
}
