package dk.netarkivet.common.exceptions;

/**
 * An input/output operation failed.
 */
@SuppressWarnings({ "serial"})
public class IOFailure extends NetarkivetException  {
    /**
     * Constructs new IOFailure with the specified detail message.
     * @param message The detail message
     */
    public IOFailure(String message) {
        super(message);
    }

    /**
     * Constructs new IOFailure with the specified detail message and cause.
     * @param message The detail message
     * @param cause The cause
     */
    public IOFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
