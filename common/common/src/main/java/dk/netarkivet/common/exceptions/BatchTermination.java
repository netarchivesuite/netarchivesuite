
package dk.netarkivet.common.exceptions;

/**
 * Exception to tell running batchjobs to terminate.
 */
@SuppressWarnings({ "serial"})
public class BatchTermination extends NetarkivetException {
    /**
     * Constructs new BatchTermination exception with the given message.
     * @param message The exception message.
     */
    public BatchTermination(String message) {
        super(message);
    }

    /**
     * Constructs new BatchTermination exception with the given message and
     * cause.
     * @param message The exception message.
     * @param cause The cause of the exception.
     */
    public BatchTermination(String message, Throwable cause) {
        super(message, cause);
    }
}
