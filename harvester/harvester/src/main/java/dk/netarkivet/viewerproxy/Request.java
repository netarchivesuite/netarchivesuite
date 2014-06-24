
package dk.netarkivet.viewerproxy;

import java.net.URI;
import java.util.Map;

/**
 * The Request interface is a very minimal version of a HTTP request.
 * We use this to decouple the main parts of the proxy server from
 * a given implementation.
 *
 * This should be kept to a proper subset of javax.servlet.ServletRequest
 *
 */
public interface Request {

    /** Get the URI from this request.
     *
     * @return The URI from this request.
     */
    URI getURI();

    /** Get all parameters in this request.
     * Note: This may only be accessible while handling the request, and
     * invalidated when the request is handled.
     * @return a map from parameter names to parameter values
     */
    Map<String, String[]> getParameterMap();
}
