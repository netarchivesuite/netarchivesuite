
package dk.netarkivet.common.utils;

/**
 * Mockup that simply remembers its last calls in two public fields.
 *
 */
public class RememberNotifications extends Notifications {
    public NotificationType type;
    public String message;
    public Throwable e;

    private static RememberNotifications instance;

    private RememberNotifications() {}

    public static synchronized RememberNotifications getInstance() {
        if (instance == null) {
            instance = new RememberNotifications();
        }
        return instance;
    }

    /**
     * Remember the variables, and print a message to stdout.
     *
     * @param message The message to remember.
     * @param eventType The type of notification event
     * @param exception The exception to remember.
     */
    public void notify(String message, NotificationType eventType, Throwable exception) {
        this.message = message;
        this.e = exception;
        System.out.println("[" + eventType + "-Notification] "
                + message);
        if (exception != null) {
            exception.printStackTrace(System.out);
        }
    }

    public static synchronized void resetSingleton() {
        instance = null;
    }
}
