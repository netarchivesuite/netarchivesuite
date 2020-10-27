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
 * This class encapsulates reacting to a serious error or warning message.
 */
public abstract class Notifications {

    /**
     * Notify about an event. This is the same as calling {@link #notify(String, NotificationType, Throwable)} with null
     * as the second parameter.
     *
     * @param message The error message to notify about.
     */
    public void notify(String message, NotificationType eventType) {
        notify(message, eventType, null);
    }

    /**
     * Notifies about an event including an exception.
     *
     * @param message The message to notify about.
     * @param eventType The type of event
     * @param e An exception related to the event, if not the event itself May be null for no exception.
     */
    public abstract void notify(String message, NotificationType eventType, Throwable e);

}
