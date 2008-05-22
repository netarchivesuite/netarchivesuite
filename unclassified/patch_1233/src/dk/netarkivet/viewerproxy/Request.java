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

import java.net.URI;
import java.util.Map;

/**
 * The Request interface is a very minimal version of a HTTP request.
 * We use this to decouple the main parts of the proxy server from
 * a given implementation.
 *
 * This should be kept to a proper subset of javax.servlet.ServletRequest
 *
 */
public interface Request {

    /** Get the URI from this request.
     *
     * @return The URI from this request.
     */
    public URI getURI();

    /** Get all parameters in this request.
     * Note: This may only be accessible while handling the request, and
     * invalidated when the request is handled.
     * @return a map from parameter names to parameter values
     */
    public Map<String,String[]> getParameterMap();
}
