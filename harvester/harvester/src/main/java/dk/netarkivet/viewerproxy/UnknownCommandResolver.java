
package dk.netarkivet.viewerproxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Wrapper for an URIResolver, which gives failures on specific
 * specific URLs, and forwards all others to the wrapped handler. This
 * allows you to reserve a specific host for commands and get a well-defined
 * error if giving an undefined command.
 *
 */
public class UnknownCommandResolver extends CommandResolver {
    /** Logger for this class. */
    private Log log = LogFactory.getLog(getClass().getName());

    /**
     * Make a new UnknownCommandResolver, which gives an error for any command-
     * like URL and forwards other URLs to the given URIResolver.
     *
     * @param ur The URIResolver to handle all other uris.
     * @throws ArgumentNotValid if either argument is null.
     */
    public UnknownCommandResolver(URIResolver ur) {
        super(ur);
    }

    /**
     * Helper method that checks if this is a command URL and throw an error
     * if it is.
     *
     * @param request  The request to check
     * @param response The response to give command results to if it is a
     *                 command
     * @return Whether this was a command URL
     */
     protected boolean executeCommand(Request request, Response response) {
        //If the url is for this host (potential command)
        if (isCommandHostRequest(request)) {
            log.debug("Blocking unknown command " + request.getURI());
            throw new IOFailure("Bad request: '" + request.getURI()
                                + "':\n" + "Unknown command");
        }
        return false;
    }
}
