
package dk.netarkivet.common.exceptions;

/**
 * An exception to throw when an unfinished function is called.
 *
 */

@SuppressWarnings({ "serial"})
public class NotImplementedException extends NetarkivetException {
    /**
     * Constructs new NotImplementedException with the specified detail message.
     * @param message The detail message
     */
    public NotImplementedException(String message) {
        super(message);
    }

    /**
     * Constructs new NotImplementedException with the
     * specified detail message and cause.
     * @param message The detail message
     * @param cause The cause
     */
    public NotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }
}
