
package dk.netarkivet.viewerproxy;

/**
 * Interface for all classes that may resolve requests and update response with
 * result.
 *
 */
public interface URIResolver {
    /** Not HTTP response code used for unresolvable URI, i.e. not in archive.*/
    int NOT_FOUND = -1;

    /** Do a lookup on a request and update response with the result.
     *
     * @param request The request to look up.
     * @param response The response to optionally update.
     * @return The HTTP response of this value, or NOT_FOUND for URLs that
     * cannot be resolved. If HTTP response is returned it should ALWAYS be the
     * same code as the code set in the response object. if NOT_FOUND is
     * returned, the response object should contain a proper error page.
     */
    int lookup(Request request, Response response);
}
