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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.harvester.datamodel.H3HeritrixTemplate;

/**
 * Wrapper for an URIResolver, which gives failures on specific specific URLs, and forwards all others to the wrapped
 * handler. This allows you to reserve a specific host for commands and get a well-defined error if giving an undefined
 * command.
 */
public class UnknownCommandResolver extends CommandResolver {
    /** Logger for this class. */
	 private static final Logger log = LoggerFactory.getLogger(H3HeritrixTemplate.class);

    /**
     * Make a new UnknownCommandResolver, which gives an error for any command- like URL and forwards other URLs to the
     * given URIResolver.
     *
     * @param ur The URIResolver to handle all other uris.
     * @throws ArgumentNotValid if either argument is null.
     */
    public UnknownCommandResolver(URIResolver ur) {
        super(ur);
    }

    /**
     * Helper method that checks if this is a command URL and throw an error if it is.
     *
     * @param request The request to check
     * @param response The response to give command results to if it is a command
     * @return Whether this was a command URL
     */
    protected boolean executeCommand(Request request, Response response) {
        // If the url is for this host (potential command)
        if (isCommandHostRequest(request)) {
            log.debug("Blocking unknown command " + request.getURI());
            throw new IOFailure("Bad request: '" + request.getURI() + "':\n" + "Unknown command");
        }
        return false;
    }
}
