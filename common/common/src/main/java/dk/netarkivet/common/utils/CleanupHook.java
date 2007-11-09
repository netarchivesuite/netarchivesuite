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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Defines a ShutdownHook for a class which has a cleanup method.
 *
 */
public class CleanupHook extends Thread {
    /** The application, which this CleanupHook, should help to cleanup. */
    private CleanupIF app;
    /** The name of the application, which this CleanupHook,
     * should help to cleanup. */
    private String appName;

    /**
     * Returns a ShutdownHook thread for an object with a cleanup() method.
     * @param app the Object to be cleaned up
     */
    public CleanupHook(CleanupIF app) {
        ArgumentNotValid.checkNotNull(app, "CleanupIF app");
        this.app = app;
        appName = app.getClass().getName();
    }

    /**
     * Called by the JVM to clean up the object before exiting.
     * The method calls the cleanup() method 
     * Note: System.out.println is added in this method
     * because logging may or may not active at this time.
     */
    public void run() {
        Log log = null;
        try {
            System.out.println("Cleaning up " + appName);
            log = LogFactory.getLog(appName);
            log.info("Cleaning up " + appName);
        } catch (Throwable e) {
            System.out.println("Failed to log cleaning up operation on "
                    + appName);
            e.printStackTrace();
        }
        try {
            app.cleanup();
        } catch (Throwable e) {
            System.out.println("Error while cleaning up "
                    + appName);
            e.printStackTrace();
        }
        try {
            System.out.println("Cleaned up " + appName);
            log.info("Cleaned up " + appName);
        } catch (Throwable e) {
            System.out.println("Cleaned up " + appName
                    + " but failed to log afterwards");
            e.printStackTrace();
        }
    }

}
