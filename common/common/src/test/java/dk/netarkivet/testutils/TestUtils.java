/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

package dk.netarkivet.testutils;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.DomainDAOTester;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAOTester;
import dk.netarkivet.harvester.datamodel.JobDAOTester;
import dk.netarkivet.harvester.datamodel.ScheduleDAOTester;
import dk.netarkivet.harvester.datamodel.TemplateDAOTester;

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

    public static void resetDAOs() {
        DomainDAOTester.resetDomainDAO();
        TemplateDAOTester.resetTemplateDAO();
        HarvestDefinitionDAOTester.resetDAO();
        ScheduleDAOTester.resetDAO();
        JobDAOTester.resetDAO();
    }

    /**
     * Convert inputStream to byte array.
     *
     * @param data       inputstream
     * @param dataLength length of inputstream
     * @return byte[] containing data in inputstream
     */
    public static byte[] inputStreamToBytes(InputStream data, int dataLength) {
        byte[] contents = new byte[dataLength];
        try {
            data.read(contents, 0, dataLength);
        } catch (IOException e) {
            throw new IOFailure("Unable to convert inputstream to byte array",
                                e);
        }
        return contents;
    }
}
