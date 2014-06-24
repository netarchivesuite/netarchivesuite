
package dk.netarkivet.common.utils;

/**
 * This class encapsulates reacting to a serious error or warning message.
 *
 */
public abstract class Notifications {
    /**
     * Notify about an event. This is the same as calling
     * {@link #notify(String, NotificationType, Throwable)} with null as the second parameter.
     *
     * @param message The error message to notify about.
     */
    public void notify(String message, NotificationType eventType) {
        notify(message, eventType, null);
    }

    /**
     * Notifies about an event including an exception.
     *
     * @param message The message to notify about.
     * @param eventType The type of event
     * @param e  An exception related to the event, if not the event itself
     * May be null for no exception.
     */
    public abstract void notify(String message, NotificationType eventType, 
            Throwable e);
}
