/*
 * #%L
 * Netarchivesuite - common
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

package dk.netarkivet.common.utils;

/**
 * A notification implementation that prints notifications on System.err.
 */
public class PrintNotifications extends Notifications {

    /**
     * Reacts to a notification by printing the notification to System.err.
     *
     * @param message The error message to print.
     * @param e The exception to print, if not null.
     */
    public void notify(String message, NotificationType eventType, Throwable e) {
        System.err.println("[" + eventType + "]:" + message);
        if (e != null) {
            e.printStackTrace(System.err);
        }
    }

}
