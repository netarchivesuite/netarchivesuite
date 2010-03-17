/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

package dk.netarkivet.testutils;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.NetarkivetMessage;

/**
 * Assertions on JMS/Netarkivet messages
 *
 */

public class MessageAsserts {
    /** Assert that a message is ok, and if not, print out the error message.
     *
     * @param s A string explaining what was expected to happen, e.g.
     * "Get message on existing file should reply ok"
     * @param msg The message to check the status of
     */
    public static void assertMessageOk(String s, NetarkivetMessage msg) {
        if (!msg.isOk()) {
            TestCase.fail(s + ": " + msg.getErrMsg());
        }
    }
}
