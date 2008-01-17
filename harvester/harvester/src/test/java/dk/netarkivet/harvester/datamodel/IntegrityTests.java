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
package dk.netarkivet.harvester.datamodel;

import java.io.IOException;
import java.security.Permission;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import org.xml.sax.SAXException;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.webinterface.GUIWebServer;

/**
 * @version $Id$
 */
public class IntegrityTests extends DataModelTestCase {
    private SecurityManager m;
    private GUIWebServer gui;

    public IntegrityTests(String s) {
            super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        Settings.set(Settings.SITESECTION_WEBAPPLICATION,
                     TestInfo.GUI_WEB_SERVER_JSP_DIRECTORY);
        Settings.set(Settings.SITESECTION_DEPLOYPATH,
                     TestInfo.GUI_WEB_SERVER_WEBBASE);
        Settings.set(Settings.HTTP_PORT_NUMBER,
                     Integer.toString(TestInfo.GUI_WEB_SERVER_PORT));

        m = System.getSecurityManager();
        SecurityManager manager = new SecurityManager() {
            public void checkPermission(Permission perm) {
                if(perm.getName().equals("exitVM")) {
                    throw new SecurityException("Thou shalt not exit in a unit test");
                }
            }
        };
        System.setSecurityManager(manager);
        
        /** Do not send notification by email. Print them to STDOUT. */
        Settings.set(Settings.NOTIFICATIONS_CLASS,
                RememberNotifications.class.getName());
    }

    public void tearDown() throws Exception {
        super.tearDown();
        Settings.reload();
        System.setSecurityManager(m);
        if (gui != null) {
            gui.cleanup();
        }
    }

    public void testRun() throws IOException, SAXException {
        gui = GUIWebServer.getInstance();
        WebConversation conv = new WebConversation();
        conv.setExceptionsThrownOnErrorStatus(false);
        WebResponse resp = conv.getResponse("http://localhost:"+Integer.toString(TestInfo.GUI_WEB_SERVER_PORT)+"/" + TestInfo.GUI_WEB_SERVER_WEBBASE);
        assertTrue("Expected responsecode 200 for "+resp.getURL()+", got "+resp.getResponseCode(),resp.getResponseCode()==200);
    }

    public void testContextWorksStaticPages() throws IOException, SAXException {
        GUIWebServer server = GUIWebServer.getInstance();
        server.startServer();
        try {
            WebConversation conv = new WebConversation();
            conv.setExceptionsThrownOnErrorStatus(false);
            WebResponse resp = conv.getResponse(
                    "http://localhost:" + TestInfo.GUI_WEB_SERVER_PORT + "/" +
                    TestInfo.GUI_WEB_SERVER_WEBBASE +
                    "/index.html");
            assertTrue(
                    "Expected responsecode 200 for " + resp.getURL() + ", got " +
                    resp.getResponseCode(),
                    resp.getResponseCode() == 200);
            assertEquals("Expected title to be 'Test'. Got " + resp.getTitle(),
                    resp.getTitle(), "Test");

        } finally {
            server.cleanup();
        }
    }

    public void testContextWorksJspPages() throws IOException, SAXException {
        GUIWebServer server = GUIWebServer.getInstance();
        server.startServer();
        try {
            WebConversation conv = new WebConversation();
            conv.setExceptionsThrownOnErrorStatus(false);
            WebResponse resp = conv.getResponse(
                    "http://localhost:" + TestInfo.GUI_WEB_SERVER_PORT + "/" +
                    TestInfo.GUI_WEB_SERVER_WEBBASE +
                    "/index.jsp");
            assertTrue(
                    "Expected responsecode 200 for " + resp.getURL() + ", got " +
                    resp.getResponseCode(),
                    resp.getResponseCode() == 200);
            assertEquals("Expected title to be 'Test'. Got " + resp.getTitle(),
                    resp.getTitle(), "Test");

        } finally {
            server.cleanup();
        }
    }

}
