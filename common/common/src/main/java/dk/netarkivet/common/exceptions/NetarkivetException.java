package dk.netarkivet.common.exceptions;


/**
 * Base exception for all Netarkivet exceptions.
 * Note that RuntimeException is extended
 */
@SuppressWarnings({ "serial"})
public abstract class NetarkivetException extends RuntimeException {

  /**
   * Constructs new NetarkivetException with the specified detail message.
   * @param message The detail message
   */
    public NetarkivetException(String message) {
        super(message);
    }

  /**
   * Constructs new NetarkivetException with the specified
   * detail message and cause.
   * @param message The detail message
   * @param cause The cause
   */
    public NetarkivetException(String message, Throwable cause) {
        super(message, cause);
    }
}
