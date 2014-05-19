/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

package dk.netarkivet.testutils;

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;

import java.util.logging.Logger;

/**
 * This class allows checking who's running the tests.
 */
public class TestUtils {
    private static final String RUN_AS_USER
            = "dk.netarkivet.testutils.runningAs";

    /**
     * Return true if we're running as the given user, or if no specific user is
     * given, or if we're running as user "ALL"
     *
     * @param user A unique indication of a user
     * @return True if the user given is the same as the one given in settings,
     *         or if we're running as "all users" (ALL or no setting).
     */
    public static boolean runningAs(String user) {
        Logger log = Logger.getLogger(TestUtils.class.getName());
        StackTraceElement callerSTE = new Throwable().getStackTrace()[1];
        String caller = callerSTE.getClassName() + "."
                        + callerSTE.getMethodName();
        String userSet;
        try {
            userSet = Settings.get(RUN_AS_USER);
        } catch (UnknownID e) {
            // Not found, so not set, so running as all.
            return true;
        }
        if (userSet == null) {
            return true;
        }
        if (userSet.equalsIgnoreCase(user) || userSet.equalsIgnoreCase("ALL")) {
            return true;
        }
        log.info("User " + user + " excluded " + caller + "()");
        return false;
    }
}
