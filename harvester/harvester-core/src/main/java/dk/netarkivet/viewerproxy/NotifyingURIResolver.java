/*
 * #%L
 * Netarchivesuite - harvester
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

package dk.netarkivet.viewerproxy;

import java.net.URI;
import java.util.Observable;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A wrapper class for URI resolver, which also notifies an URIObserver about all URIs visited and their response codes.
 */
public class NotifyingURIResolver extends Observable implements URIResolver, URIResolverHandler {
    /** The URIResolver used by this NotifyingURIResolver. */
    private URIResolver ur;

    /**
     * Initialise the wrapper. Accepts the class to wrap and the observer to notify.
     *
     * @param ur The Wrapped URI resolver
     * @param uo The URI Observer to notify on each url.
     * @throws ArgumentNotValid if either argument is null.
     */
    public NotifyingURIResolver(URIResolver ur, URIObserver uo) {
        ArgumentNotValid.checkNotNull(uo, "URIObserver uo");
        addObserver(uo);
        setURIResolver(ur);
    }

    /**
     * Sets the current URIResolver wrapped.
     *
     * @param anUR URI resolver to wrap.
     * @throws ArgumentNotValid if argument is null.
     */
    public void setURIResolver(URIResolver anUR) {
        ArgumentNotValid.checkNotNull(anUR, "URIResolver anUR");
        this.ur = anUR;
    }

    /**
     * Passes the uri to the current wrapped resolver and notifies the observer of the result.
     *
     * @param request A given request
     * @param response A given response
     * @return the response code from the wrapped class
     * @see URIResolver#lookup(Request, Response)
     */
    public int lookup(Request request, Response response) {
        int responseCode = ur.lookup(request, response);
        setChanged();
        URI uri = null;
        if (request != null) {
            uri = request.getURI();
        }
        notifyObservers(new URIObserver.URIResponseCodePair(uri, responseCode));
        return responseCode;
    }
}
