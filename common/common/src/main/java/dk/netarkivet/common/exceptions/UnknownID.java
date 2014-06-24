package dk.netarkivet.common.exceptions;

/**
 * Identifier could not be resolved.
 */
@SuppressWarnings({ "serial"})
public class UnknownID extends NetarkivetException  {
  /**
   * Constructs new UnknownID with the specified detail message.
   * @param message The detail message
   */
    public UnknownID(String message) {
        super(message);
    }

  /**
   * Constructs new UnknownID with the specified detail message and cause.
   * @param message The detail message
   * @param cause The cause
   */
    public UnknownID(String message, Throwable cause) {
        super(message, cause);
    }
}
