
package dk.netarkivet.viewerproxy;

/**
 * Interface for classes that use an URI resolver.
 *
 */
public interface URIResolverHandler {
    /** Sets the current URIResolver.
     *
     * @param ur The URI resolver to use when looking up URIs.
     */
    void setURIResolver(URIResolver ur);

}
