
package dk.netarkivet.common.exceptions;

/**
 * This exception indicates that we have forwarded to a JSP error page and
 * thus should stop all processing and just return at the top level JSP.
 */
@SuppressWarnings({ "serial"})
public class ForwardedToErrorPage extends NetarkivetException {
    /** Create a new ForwardedToErrorPage exception.
     *
     * @param message Explanatory message
     */
    public ForwardedToErrorPage(String message) {
        super(message);
    }

    /** Create a new ForwardedToErrorPage exception based on an old exception.
     *
     * @param message Explanatory message
     * @param cause The exception that prompted the forwarding.
     */
    public ForwardedToErrorPage(String message, Throwable cause) {
        super(message, cause);
    }
}
