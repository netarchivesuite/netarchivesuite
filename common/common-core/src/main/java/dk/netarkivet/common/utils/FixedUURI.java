/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.common.utils;

import org.apache.commons.httpclient.URIException;
import org.archive.url.UsableURI;

/**
 * Fixed UURI which extends UURI to fix an NPE bug in getReferencedHost.
 * <p>
 * Pending fix of bug in Heritrix. The bug has been reported, and has number 616:
 * http://webteam.archive.org/jira/browse/HER-616
 */
@SuppressWarnings({"serial"})
public class FixedUURI extends UsableURI {

    protected FixedUURI() {
        super();
    }

    protected FixedUURI(String string, boolean b, String string1) throws URIException {
        super(string, b, string1);
    }

    protected FixedUURI(UsableURI uuri, UsableURI uuri1) throws URIException {
        super(uuri, uuri1);
    }

    public FixedUURI(String string, boolean b) throws URIException {
        super(string, b);
    }

    /**
     * Return the hostname for this URI, giving the looked-up host on dns-URLS.
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
