/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.testutils.preconfigured;

import java.security.Permission;

import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Configures the test environment to block calls to System.exit(),
 * throwing a PermissionDenied instead.
 */
public class PreventSystemExit implements TestConfigurationIF {
    /** Saves the original security manager, so that it can be restored in tearDown() */
    private SecurityManager originalManager;
    /** Saves latest value given to System.exit() for inspection */
    int exitValue;
    /** Indicates whether System.exit() has been called after setUp(). */
    boolean exitCalled = false;

    /**
     *  Stores original SecurityManager and set a new one blocking System.exit()
     *  Calls reset().
     */
    public void setUp() {
        originalManager = System.getSecurityManager();
        SecurityManager manager = new DisallowSystemExitSecurityManager();
        System.setSecurityManager(manager);
    }
    /**
     * Resets internal state.
     */
    public void reset(){
        exitCalled = false;
    }
    /**
     * Restores original SecurityManager.
     */
    public void tearDown() {
        System.setSecurityManager(originalManager);
    }
    /**
     * Checks whether System.exit() has been called after reset().
     * @return true if and only if System.exit() has been called after reset().
     */
    public boolean getExitCalled() {
        return exitCalled;
    }
    /**
     * Looks up the value given to the latest invocation of System.exit()
     * @return The int value. Throws UnknownID if System.exit() has not been called
     * after reset().
     */
    public int getExitValue(){
        if(!exitCalled) {
            throw new UnknownID("System.exit() was never called");
        }
        return exitValue;
    }
    /**
     * A SecurityManager that makes System.exit() throw PermissionDenied.
     * Also stores the value given to System.exit() for subsequent inspection.
     */
    private class DisallowSystemExitSecurityManager extends SecurityManager {
        public void checkExit(int status) {
            exitValue = status;
            exitCalled = true;
            super.checkExit(status);
        }
        public void checkPermission(Permission perm) {
            if (perm.getName().startsWith("exitVM")) { // represents exitVM, exitVM.*
                throw new SecurityException("System.exit() disallowed during this unit test.");
            }
            
        }
    }
}
