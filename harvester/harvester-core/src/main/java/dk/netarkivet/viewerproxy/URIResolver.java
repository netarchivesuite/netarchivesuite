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

/**
 * Interface for all classes that may resolve requests and update response with result.
 */
public interface URIResolver {
    /** Not HTTP response code used for unresolvable URI, i.e. not in archive. */
    int NOT_FOUND = -1;

    /**
     * Do a lookup on a request and update response with the result.
     *
     * @param request The request to look up.
     * @param response The response to optionally update.
     * @return The HTTP response of this value, or NOT_FOUND for URLs that cannot be resolved. If HTTP response is
     * returned it should ALWAYS be the same code as the code set in the response object. if NOT_FOUND is returned, the
     * response object should contain a proper error page.
     */
    int lookup(Request request, Response response);
}
