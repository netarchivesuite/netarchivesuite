
package dk.netarkivet.common.exceptions;

/**
 * Access was denied to a resource or credentials were invalid.
 */
@SuppressWarnings({ "serial"})
public class PermissionDenied extends NetarkivetException  {
    /**
     * Constructs new PermissionDenied with the specified detail message.
     * @param message The detail message
     */
    public PermissionDenied(String message) {
        super(message);
    }

    /**
     * Constructs new PermissionDenied with the specified
     * detail message and cause.
     * @param message The detail message
     * @param cause The cause
     */
    public PermissionDenied(String message, Throwable cause) {
        super(message, cause);
    }
}
