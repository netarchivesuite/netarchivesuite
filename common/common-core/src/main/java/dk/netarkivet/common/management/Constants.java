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
package dk.netarkivet.common.management;

/**
 * Contains the constants for the management classes.
 */
public final class Constants {

    /**
     * Constructor. Private due to constants only class.
     */
    private Constants() {
    }

    /**
     * These constant priority keys are also used by the monitor to find the value in the Translation files.
     */

    /** The location key word. */
    public static final String PRIORITY_KEY_LOCATION = "location";
    /** The machine key word. */
    public static final String PRIORITY_KEY_MACHINE = "machine";
    /** The application name key word. */
    public static final String PRIORITY_KEY_APPLICATIONNAME = "applicationname";
    /** The application instance id key word. */
    public static final String PRIORITY_KEY_APPLICATIONINSTANCEID = "applicationinstanceid";
    /** The http port key word. */
    public static final String PRIORITY_KEY_HTTP_PORT = "httpport";
    /** The harvest channel key word. */
    public static final String PRIORITY_KEY_CHANNEL = "channel";
    /** The replica key word. */
    public static final String PRIORITY_KEY_REPLICANAME = "replicaname";
    /** The index key word. */
    public static final String PRIORITY_KEY_INDEX = "index";
    /** The remove jmx application keyword. */
    public static final String REMOVE_JMX_APPLICATION = "removeapplication";

}
