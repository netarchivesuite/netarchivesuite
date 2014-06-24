
package dk.netarkivet.common.exceptions;

/**
 * An object was not in the right state for the operation attempted.
 *
 */

@SuppressWarnings({ "serial"})
public class IllegalState extends NetarkivetException {
    /**
     * Constructs new IllegalState with the specified detail message and cause.
     * @param message The detail message
     */
    public IllegalState(String message) {
        super(message);
    }

    /**
     * Constructs new IllegalState with the specified detail message and cause.
     * @param message The detail message
     * @param cause The cause
     */
    public IllegalState(String message, Throwable cause) {
        super(message, cause);
    }

}
