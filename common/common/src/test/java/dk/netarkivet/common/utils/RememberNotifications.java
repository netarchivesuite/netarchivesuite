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
 * Mockup that simply remembers its last calls in two public fields.
 *
 */

public class RememberNotifications extends Notifications {
    public String message;
    public Throwable e;

    private static RememberNotifications instance;

    private RememberNotifications() {}

    public static synchronized RememberNotifications getInstance() {
        if (instance == null) {
            instance = new RememberNotifications();
        }
        return instance;
    }

    /**
     * Remember the variables, and print a message to stdout.
     *
     * @param errorMessage The error message to remember.
     * @param exception The exception to rmeember.
     */
    public void errorEvent(String errorMessage, Throwable exception) {
        this.message = errorMessage;
        this.e = exception;
        System.out.println("[errorNotification] "
                + errorMessage);
        if (exception != null) {
            exception.printStackTrace(System.out);
        }
    }

    public static synchronized void resetSingleton() {
        instance = null;
    }
}
