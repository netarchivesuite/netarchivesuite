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

import java.io.IOException;
import java.security.Permission;

import junit.framework.TestCase;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import org.xml.sax.SAXException;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.GUIWebServer;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Integritytests for the package dk.netarkivet.common.webinterface
 */
public class IntegrityTests extends TestCase {
    private SecurityManager m;
    private GUIWebServer gui;
    ReloadSettings rs = new ReloadSettings();

    public IntegrityTests(String s) {
            super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        Settings.set(CommonSettings.SITESECTION_WEBAPPLICATION, TestInfo.GUI_WEB_SERVER_JSP_DIRECTORY);
        //Settings.set(CommonSettings.SITESECTION_DEPLOYPATH, TestInfo.GUI_WEB_SERVER_WEBBASE);
        Settings.set(CommonSettings.SITESECTION_CLASS, TestSiteSection.class.getName());
        Settings.set(CommonSettings.HTTP_PORT_NUMBER, Integer.toString(TestInfo.GUI_WEB_SERVER_PORT));

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
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
    }

    public void tearDown() throws Exception {
        super.tearDown();
        System.setSecurityManager(m);
        if (gui != null) {
            gui.cleanup();
        }
        rs.tearDown();
    }

    public void testRun() throws IOException, SAXException {
        gui = GUIWebServer.getInstance();
        WebConversation conv = new WebConversation();
        conv.setExceptionsThrownOnErrorStatus(false);
        WebResponse resp = conv.getResponse("http://localhost:"
                + Integer.toString(TestInfo.GUI_WEB_SERVER_PORT)
                + "/" + TestInfo.GUI_WEB_SERVER_WEBBASE);
        assertTrue("Expected responsecode 200 for "
                + resp.getURL() + ", got " + resp.getResponseCode(), 
                resp.getResponseCode()==200);
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
