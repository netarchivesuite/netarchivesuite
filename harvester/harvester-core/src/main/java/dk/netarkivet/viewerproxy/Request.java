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
import java.util.Map;

/**
 * The Request interface is a very minimal version of a HTTP request. We use this to decouple the main parts of the
 * proxy server from a given implementation.
 * <p>
 * This should be kept to a proper subset of javax.servlet.ServletRequest
 */
public interface Request {

    /**
     * Get the URI from this request.
     *
     * @return The URI from this request.
     */
    URI getURI();

    /**
     * Get all parameters in this request. Note: This may only be accessible while handling the request, and invalidated
     * when the request is handled.
     *
     * @return a map from parameter names to parameter values
     */
    Map<String, String[]> getParameterMap();
}
