/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
     * like URL and forwards other URLs to the given URIResolver
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
