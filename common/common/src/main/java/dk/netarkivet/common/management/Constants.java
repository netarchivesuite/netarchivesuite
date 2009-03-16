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
package dk.netarkivet.common.management;

/**
 * Contains the constants for the management classes.
 */
public final class Constants {
    /**
     * Constructor.
     * Private due to constants only class.
     */
    private Constants() {}
    
    /**
     * These constant priority keys are also used by the monitor to find 
     * the value in the Translation files.
     */
    
    /** The location key word.*/
    public static final String PRIORITY_KEY_LOCATION = "location";
    /** The machine key word.*/
    public static final String PRIORITY_KEY_MACHINE = "machine";
    /** The application name key word.*/
    public static final String PRIORITY_KEY_APPLICATIONNAME = "applicationname";
    /** The application instance id key word.*/
    public static final String PRIORITY_KEY_APPLICATIONINSTANCEID =
        "applicationinstanceid";
    /** The http port key word.*/
    public static final String PRIORITY_KEY_HTTP_PORT = "httpport";
    /** The priority key word.*/
    public static final String PRIORITY_KEY_PRIORITY = "priority";
    /** The replica key word.*/
    public static final String PRIORITY_KEY_REPLICANAME = "replicaname";
    /** The index key word.*/
    public static final String PRIORITY_KEY_INDEX = "index";
}
