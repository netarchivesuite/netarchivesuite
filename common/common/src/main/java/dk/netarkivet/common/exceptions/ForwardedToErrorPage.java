/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.common.exceptions;

/**
 * This exception indicates that we have forwarded to a JSP error page and
 * thus should stop all processing and just return at the top level JSP.
 */
public class ForwardedToErrorPage extends NetarkivetException {
    /** Create a new ForwardedToErrorPage exception
     *
     * @param message Explanatory message
     */
    public ForwardedToErrorPage(String message) {
        super(message);
    }

    /** Create a new ForwardedToErrorPage exception based on an old exception
     *
     * @param message Explanatory message
     * @param cause The exception that prompted the forwarding.
     */
    public ForwardedToErrorPage(String message, Throwable cause) {
        super(message, cause);
    }
}
