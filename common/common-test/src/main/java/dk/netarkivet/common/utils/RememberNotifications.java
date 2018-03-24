/*
 * #%L
 * Netarchivesuite - common - test
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
 * Mockup that simply remembers its last calls in two public fields.
 */
public class RememberNotifications extends Notifications {
    public NotificationType type;
    public String message;
    public Throwable e;

    private static RememberNotifications instance;

    private RememberNotifications() {
    }

    public static synchronized RememberNotifications getInstance() {
        if (instance == null) {
            instance = new RememberNotifications();
        }
        return instance;
    }

    /**
     * Remember the variables, and print a message to stdout.
     *
     * @param message The message to remember.
     * @param eventType The type of notification event
     * @param exception The exception to remember.
     */
    public void notify(String message, NotificationType eventType, Throwable exception) {
        this.message = message;
        this.e = exception;
        System.out.println("[" + eventType + "-Notification] " + message);
        if (exception != null) {
            exception.printStackTrace(System.out);
        }
    }

    public static synchronized void resetSingleton() {
        instance = null;
    }
}
