
package dk.netarkivet.common.utils;

import org.apache.commons.httpclient.URIException;
import org.archive.net.UURI;

/**
 * Fixed UURI which extends UURI to fix an NPE bug in getReferencedHost.
 *
 * Pending fix of bug in Heritrix. The bug has been reported, and
 * has number 616: http://webteam.archive.org/jira/browse/HER-616 
 */

@SuppressWarnings({ "serial"})
public class FixedUURI extends UURI {
    protected FixedUURI() {
        super();

    }

    protected FixedUURI(String string, boolean b, String string1)
            throws URIException {
        super(string, b, string1);
    }

    protected FixedUURI(UURI uuri, UURI uuri1) throws URIException {
        super(uuri, uuri1);
    }

    public FixedUURI(String string, boolean b) throws URIException {
        super(string, b);
    }

    /** Return the hostname for this URI, giving the looked-up host on dns-URLS.
     *
     * @return hostname for this URI, or null if this cannot be calculated.
     * @throws URIException on serious parse errors.
     * @see UURI#getReferencedHost()
     */
    public String getReferencedHost() throws URIException {
        if (getHost() == null && getScheme() == null) {
            return null;
        }
        return super.getReferencedHost();
    }
}
