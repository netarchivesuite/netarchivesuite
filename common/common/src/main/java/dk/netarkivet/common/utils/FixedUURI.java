/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.common.utils;

import org.apache.commons.httpclient.URIException;
import org.archive.net.UURI;

/**
 * Fixed UURI which extends UURI to fix an NPE bug in getReferencedHost.
 *
 * Pending fix of bug in Heritrix. The bug has been reported, and
 * has number 616: http://webteam.archive.org/jira/browse/HER-616 
 */

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
