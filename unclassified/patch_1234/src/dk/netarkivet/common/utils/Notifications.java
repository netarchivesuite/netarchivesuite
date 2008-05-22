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

/**
 * This class encapsulates reacting to a serious error message.
 *
 */
public abstract class Notifications {
    /**
     * Notify about an error event. This is the same as calling
     * {@link #errorEvent(String, Throwable)} with null as the second parameter.
     *
     * @param message The error message to notify about.
     */
    public void errorEvent(String message) {
        errorEvent(message, null);
    }

    /**
     * Notifies about an error event including an exception.
     *
     * @param message The error message to notify about.
     * @param e       The exception to notify about.
     * May be null for no exception.
     */
    public abstract void errorEvent(String message, Throwable e);

}
