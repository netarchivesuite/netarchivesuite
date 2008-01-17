/*$Id$
* $Revision$
* $Date$
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
package dk.netarkivet.common.webinterface;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.harvester.datamodel.TestInfo;

/**
 * @version $Id$
 *          <p/>
 *          Tests running a web server.
 */
public class GUIWebServerTester extends TestCase {
    TestInfo info = new TestInfo();
    private GUIWebServer server;

    public void setUp() {
        Settings.set(Settings.SITESECTION_WEBAPPLICATION,
                     TestInfo.GUI_WEB_SERVER_JSP_DIRECTORY);
        Settings.set(Settings.SITESECTION_DEPLOYPATH,
                     TestInfo.GUI_WEB_SERVER_WEBBASE);
        Settings.set(Settings.HTTP_PORT_NUMBER,
                     Integer.toString(TestInfo.GUI_WEB_SERVER_PORT));
    }

    public void tearDown() {
        if (server != null) {
            server.cleanup();
        }
        Settings.reload();
    }

    public void testRunningServer() {
        server = new GUIWebServer();
        server.startServer();
        try {
            new Socket(InetAddress.getLocalHost(),
                       TestInfo.GUI_WEB_SERVER_PORT);
        } catch (IOException e) {
            fail("Port not in use after starting server!");
        }
    }

    public void testExpectedExceptionsWhenStartingServer() throws IOException {
        Settings.set(Settings.HTTP_PORT_NUMBER,
                     Long.toString(65536L));


        //arguments not in range
        try {
            server = new GUIWebServer();
            fail("IOFailure expected for port " + 65536);
        } catch (IOFailure e) {
            //Expected
        }

        Settings.set(Settings.HTTP_PORT_NUMBER,
                     Long.toString(-1L));

        try {
            server = new GUIWebServer();
            fail("IOFailure expected for port " + -1);
        } catch (IOFailure e) {
            //Expected
        }

        Settings.set(Settings.HTTP_PORT_NUMBER,
                     Long.toString(1023L));

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
        Settings.set(Settings.SITESECTION_WEBAPPLICATION,
                     "/not_found_bescause_it_doesnt_exist");
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
            fail("Port not in use after starting server!");
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
