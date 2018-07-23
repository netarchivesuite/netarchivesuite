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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.lifecycle.LifeCycleComponent;

/**
 * Defines a ShutdownHook for a class which has a cleanup method.
 */
public class ShutdownHook extends Thread {

    /** The component to hook up to. */
    private LifeCycleComponent app;
    /** The name of the hooked application. */
    private String appName;

    /**
     * Returns a ShutdownHook thread for an object with a cleanup() method.
     *
     * @param app the Object to be cleaned up
     */
    public ShutdownHook(LifeCycleComponent app) {
        ArgumentNotValid.checkNotNull(app, "LifeCycleComponent app");
        this.app = app;
        appName = app.getClass().getName();
    }

    /**
     * Called by the JVM to clean up the object before exiting. The method calls the cleanup() method Note:
     * System.out.println is added in this method because logging may or may not be active at this time.
     */
    public void run() {
        Logger log = null;
        try {
            log = LoggerFactory.getLogger(appName);
            log.info("Shutting down {}", appName);
        } catch (Throwable e) {
            // Ignore
        }
        try {
            app.shutdown();
        } catch (Throwable e) {
            System.out.println("Error while  shutting down " + appName);
            e.printStackTrace();
        }
        try {
            System.out.println("Shutting down " + appName);
            log.info("Shutting down {}", appName);
        } catch (Throwable e) {
            System.out.println("Shutting down " + appName + " but failed to log afterwards");
            e.printStackTrace();
        }
    }

}
