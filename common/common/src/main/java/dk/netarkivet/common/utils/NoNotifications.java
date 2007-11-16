/* File:        $Id: Notifications.java 11 2007-07-24 10:11:24Z kfc $
 * Revision:    $Revision: 11 $
 * Author:      $Author: kfc $
 * Date:        $Date: 2007-07-24 12:11:24 +0200 (Tue, 24 Jul 2007) $
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
 * A notification implementation that does nothing at all on notifications.
 */
public class NoNotifications extends Notifications {
    /**
     * Reacts to a notification by ignoring it silently
     *
     * @param message The error message ignored.
     * @param e       The exception ignored.
     */
    public void errorEvent(String message, Throwable e) {
        //Ignore
    }
}
