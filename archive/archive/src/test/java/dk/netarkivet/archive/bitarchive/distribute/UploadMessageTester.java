/* $Id$
 * $Revision$
 * $Author$
 * $Date$
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
package dk.netarkivet.archive.bitarchive.distribute;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class UploadMessageTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();

    public UploadMessageTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() {
        rs.setUp();
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
    }

    public void tearDown() {
        rs.tearDown();
    }

    public void testInvalidArguments() {
        try {
            new UploadMessage(Channels.getTheBamon(),
                    Channels.getTheRepos(),
                    RemoteFileFactory.getInstance(null, true, false, true));
            fail("Should throw ArgumentNotValid on null file");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }
}