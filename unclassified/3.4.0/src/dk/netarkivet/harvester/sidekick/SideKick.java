/* $Id$
 * $Date$
 * $Revision$
 * $Author$
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
package dk.netarkivet.harvester.sidekick;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.management.MBeanConnectorCreator;
import dk.netarkivet.common.utils.LivenessLogger;
import dk.netarkivet.common.utils.ProcessUtils;

/**
 */
public class SideKick implements Runnable {
    /** Logger. */
    private final Log log = LogFactory.getLog(this.getClass().getName());

    /** A hook associated with an application. 
     * As long as mh.isRunning() returns true, the application
     * is alive.
     */ 
    private MonitorHook mh;
    /** Last we checked, the application was still running. */
    private boolean seenRunning;
    /** Script for restarting the application. */
    private String shellScript;

    public SideKick(String monitorClass, String theShellScript) {
        log.info("Creating sidekick for script: " + theShellScript);
        this.seenRunning = false;
        this.shellScript = theShellScript;
        this.mh = getMonitorHook(monitorClass);
        log.info("Created monitor hook: " + this.mh.toString());
    }

    /**
     * Takes the monitor object as argument.
     *
     * TODO: Use rmi registry instead of argument.
     *
     * @param args
     *            fx. dk.netarkivet.harvester.sidekick.HarvestControllerServerMonitorHook
     */
    public static void main(String[] args) {
        ArgumentNotValid.checkNotNullOrEmpty(args[0], "Monitor Class");
        ArgumentNotValid.checkNotNullOrEmpty(args[1], "Shell Script");
        Settings.set(Settings.APPLICATIONNAME, SideKick.class.getName());
        MBeanConnectorCreator.exposeJMXMBeanServer();
        new Thread(new SideKick(args[0], args[1])).start();
        /** Start liveness logger for the sidekick. */
        new Thread(new LivenessLogger(SideKick.class)).start();
    }

    /**
     * If MonitorHook return the application is running then define the
     * seenRunning = true.
     *
     * If the seenRunning is false and the running-file doesn't exist we assume
     * the application hasn't started yet.
     *
     * If the application has been seen running but the running-file doesn't
     * exist, the application is restartet and seenRunning is reset to false.
     */
    public void run() {
        while (true) {
            if (mh.isRunning()) {
                if (!seenRunning) {
                    log.info(mh + " is running.");
                    seenRunning = true;
                }
            } else {
                if (seenRunning) {
                    log.info(mh + " has terminated.");
                    runShellScript();
                    log.info(mh + " has been restarted.");
                    seenRunning = false;
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Runs the shellscript defined by the startup parameters.
     */
    private void runShellScript() {
        log.info("Starting script");
        try {
            Process p = Runtime.getRuntime().exec(shellScript);
            ProcessUtils.discardProcessOutput(p.getInputStream());
            ProcessUtils.discardProcessOutput(p.getErrorStream());
            final int exitValue = p.waitFor();
            if (exitValue != 0) {
                throw new IOFailure("Error running df: status " + exitValue);
            }
        } catch (IOException e) {
            throw new IOFailure("Couldn't execute shellscript (" + shellScript
                    + ")", e);
        } catch (InterruptedException e) {
            throw new IOFailure("Error while waiting for shellscript ("
                    + shellScript + ") to finish.", e);
        }
    }



    /**
     * @param monitorClass The name of the monitorHook class to instantiate and return
     * @return a MonitorHook of the type defined in the startup parameters.
     * @throws IOFailure
     *             if an instance couldn't be instanciated
     */
    private MonitorHook getMonitorHook(String monitorClass) throws IOFailure {
        try {
            return (MonitorHook) Class.forName(monitorClass).newInstance();
        } catch (Exception e) {
            throw new IOFailure(
                    "Could not make an instance of " + monitorClass, e);
        }
    }
}
