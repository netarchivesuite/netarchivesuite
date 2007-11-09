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

package dk.netarkivet.viewerproxy;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * An abstract superclass for URIResolvers that handle commands given to
 * the server host (http://<<serverhost>>/<<command>>/<<param>>=<<value>>*
 *
 */

public abstract class CommandResolver
        implements URIResolverHandler, URIResolver {
    /**
     * The URI resolver which is wrapped, and which handles all non-command
     * URIs
     */
    protected URIResolver ur;
    /** Fake host used as hostname when doing commands. */
    public static final String VIEWERPROXY_COMMAND_NAME
            = "netarchivesuite.viewerproxy.invalid";

    /** Constructor which sets the next resolver in the chain.
     *
     * @param ur The URIResolver that handles URIs that are not handled by
     * this resolver.
     */
    public CommandResolver(URIResolver ur) {
        setURIResolver(ur);
    }

    /**
     * Change the URI resolver which handles URIs that we don't handle here.
     *
     * @param ur The URI resolver to handle unhandled URIs.
     * @throws ArgumentNotValid if either argument is null.
     */
    public final void setURIResolver(URIResolver ur) {
        ArgumentNotValid.checkNotNull(ur, "URIResolver ur");
        this.ur = ur;
    }

    /**
     * Parses the given URI and executes commands for all command URLs. The
     * possible commands are of the form
     *   http://<<localhostname>>/<<command>>?<<param>>=<<value>>*
     * where command and param are defined in the subclass.
     *
     * If uri is none of these, the uri and response are forwarded to the
     * wrapped URI resolver.
     *
     * @param request The HTTP request we are working on
     * @param response HTTP response to generate effect on or to forward
     * @return response code
     */
    public final int lookup(Request request, Response response) {
        boolean command = executeCommand(request, response);
        if (!command) {
            return ur.lookup(request, response);
        } else {
            return response.getStatus();
        }
    }

    /**
     * Abstract method for parsing of the URL and delegating to relevant
     * methods.  This should start by calling isCommandHostRequest.
     *
     * @param request  The request to check
     * @param response The response to give command results to if it is a
     *                 command
     * @return Whether this was a command URL
     */
    protected abstract boolean executeCommand(Request request,
                                              Response response);

    /** Returns true if the request specifies the host that we're running
     * on.  Alternative specifications such as 'localhost' or '127.0.0.1' or
     * an actual IP of this machine are not considered command hosts.  Local
     * hosts would only be command hosts if we happen to get a request from
     * a browser running on this machine.
     *
     * @param request An HTTP request.
     * @return True if the HTTP request specifies a URI that indicates this
     * machine, false otherwise.
     */
    protected static boolean isCommandHostRequest(Request request) {
        return request != null && request.getURI() != null
            && request.getURI().getHost() != null
            && request.getURI().getHost().equals(
                VIEWERPROXY_COMMAND_NAME);
    }
}
