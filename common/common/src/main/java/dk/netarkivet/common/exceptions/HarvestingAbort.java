package dk.netarkivet.common.exceptions;

/**
 * This exception is used to signal that harvest is aborted.
 */
@SuppressWarnings({ "serial"})
public class HarvestingAbort extends NetarkivetException {

    /** Create a new HarvestAbort exception based on an old exception.
    *
    * @param message Explanatory message
    */
    public HarvestingAbort(String message) {
        super(message);
    }

    /** Create a new HarvestAbort exception based on an old exception.
    *
    * @param message Explanatory message
    * @param cause The exception that prompted the exception
    */
    public HarvestingAbort(String message, Throwable cause) {
        super(message, cause);
    }
}
