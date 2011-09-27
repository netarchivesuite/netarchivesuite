/*$Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.common.webinterface;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.TestCase;
import dk.netarkivet.TestUtils;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Tests running a web server, represented by the GUIWebServer() class.
 */
public class GUIWebServerTester extends TestCase {

    private GUIWebServer server;
    ReloadSettings rs = new ReloadSettings();
    public static final File BASEDIR =
        new File("tests/dk/netarkivet/harvester/scheduler/data/");
    public static final File ORIGINALS = new File(BASEDIR, "originals");
    public static final File WORKING = new File(BASEDIR, "working");

    /**
     * @param sTestName
     */
    public GUIWebServerTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws Exception {
        rs.setUp();
        Settings.set(CommonSettings.SITESECTION_WEBAPPLICATION, 
                TestInfo.GUI_WEB_SERVER_JSP_DIRECTORY);
        Settings.set(CommonSettings.SITESECTION_CLASS, 
                TestInfo.GUI_WEB_SERVER_SITESECTION_CLASS);
        Settings.set(CommonSettings.HTTP_PORT_NUMBER, 
                Integer.toString(TestInfo.GUI_WEB_SERVER_PORT));

        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestInfo.TEMPDIR.mkdirs();
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, 
                TestInfo.TEMPDIR.getAbsolutePath()); 
        // Use mockup JMS
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        
        // Configure test DB
        TestUtils.resetDAOs();
        Settings.set(CommonSettings.REMOTE_FILE_CLASS,
        "dk.netarkivet.common.distribute.TestRemoteFile");
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WORKING);
        Settings.set(CommonSettings.DB_BASE_URL, "jdbc:derby:"
                + WORKING.getCanonicalPath() + "/fullhddb");
        DatabaseTestUtils.getHDDB(new File(BASEDIR, "fullhddb.jar"),
                "fullhddb", WORKING);

    }

    public void tearDown() throws Exception {
        if (server != null) {
            server.cleanup();
        }
        JMSConnectionMockupMQ.clearTestQueues();
        DatabaseTestUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rs.tearDown();
    }

    public void testRunningServer() {
        server = new GUIWebServer();
        server.startServer();
        try {
            new Socket(InetAddress.getLocalHost(),
                    TestInfo.GUI_WEB_SERVER_PORT);
        } catch (IOException e) {
            fail("Port not in use after starting server due to error: " + e);
        }
    }

    public void testExpectedExceptionsWhenStartingServer() throws IOException {
        Settings.set(CommonSettings.HTTP_PORT_NUMBER, Long.toString(65536L));

        //arguments not in range
        try {
            server = new GUIWebServer();
            fail("IOFailure expected for port " + 65536);
        } catch (IOFailure e) {
            //Expected
        }

        Settings.set(CommonSettings.HTTP_PORT_NUMBER, Long.toString(-1L));

        try {
            server = new GUIWebServer();
            fail("IOFailure expected for port " + -1);
        } catch (IOFailure e) {
            //Expected
        }

        Settings.set(CommonSettings.HTTP_PORT_NUMBER, Long.toString(1023L));

        try {
            server = new GUIWebServer();
            fail("IOFailure expected for port " + 1023);
        } catch (IOFailure e) {
            //Expected
        }

        //Port already in use
        ServerSocket socket = new ServerSocket(TestInfo.GUI_WEB_SERVER_PORT);

        try {
            server = new GUIWebServer();
            server.startServer();
            fail("IOFailure expected when running on port already in use");
        } catch (IOFailure e) {
            //Expected
        }

        socket.close();
        socket = null;
    }

    public void testExpectedExceptionsWhenAddingContext() throws IOException {
        //wrong arguments when adding context
        Settings.set(CommonSettings.SITESECTION_WEBAPPLICATION,
        "/not_found_because_it_doesnt_exist");
        try {
            server = new GUIWebServer();
            server.startServer();
            fail("IOFailure expected when directory is not found");
        } catch (IOFailure e) {
            //Expected
        }
    }

    public void testStopServer() throws InterruptedException {
        server = new GUIWebServer();
        server.startServer();
        try {
            new Socket(InetAddress.getLocalHost(),
                    TestInfo.GUI_WEB_SERVER_PORT);
        } catch (IOException e) {
            fail("Port not in use after starting server due to error: " + e);
        }
        server.cleanup();
        try {
            new Socket(InetAddress.getLocalHost(),
                    TestInfo.GUI_WEB_SERVER_PORT);
            fail("Port still in use after stopping server!");
        } catch (IOException e) {
            //Expected
        }
    }
}
