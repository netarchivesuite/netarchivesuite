/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2017 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.common.webinterface;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Tests running a web server, represented by the GUIWebServer() class.
 */
public class GUIWebServerTester {

    private GUIWebServer server;
    ReloadSettings rs = new ReloadSettings();
    public static final File BASEDIR = new File("tests/dk/netarkivet/harvester/scheduler/data/");
    public static final File ORIGINALS = new File(BASEDIR, "originals");
    public static final File WORKING = new File(BASEDIR, "working");

    @Before
    public void setUp() throws Exception {
        rs.setUp();
        Settings.set(CommonSettings.SITESECTION_WEBAPPLICATION,
                dk.netarkivet.common.webinterface.TestInfo.GUI_WEB_SERVER_JSP_DIRECTORY);
        Settings.set(CommonSettings.SITESECTION_CLASS,
                dk.netarkivet.common.webinterface.TestInfo.GUI_WEB_SERVER_SITESECTION_CLASS);
        Settings.set(CommonSettings.HTTP_PORT_NUMBER,
                Integer.toString(dk.netarkivet.common.webinterface.TestInfo.GUI_WEB_SERVER_PORT));

        FileUtils.removeRecursively(dk.netarkivet.common.webinterface.TestInfo.TEMPDIR);
        dk.netarkivet.common.webinterface.TestInfo.TEMPDIR.mkdirs();
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR,
                dk.netarkivet.common.webinterface.TestInfo.TEMPDIR.getAbsolutePath());
        // Use mockup JMS
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();

        // Configure test DB
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, "dk.netarkivet.common.distribute.TestRemoteFile");
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WORKING);
        Settings.set(CommonSettings.DB_BASE_URL, "jdbc:derby:" + WORKING.getCanonicalPath() + "/fullhddb");

    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.cleanup();
        }
        JMSConnectionMockupMQ.clearTestQueues();
        FileUtils.removeRecursively(dk.netarkivet.common.webinterface.TestInfo.TEMPDIR);
        FileUtils.removeRecursively(dk.netarkivet.common.webinterface.TestInfo.WORKING_DIR);
        rs.tearDown();
    }

    @Test
    public void testDummy() {
    	
    }

    @Test
    public void testRestWS() throws ServletException, LifecycleException, IOException {
        Tomcat server = new Tomcat();


        Context restContext = server.addWebapp("/", "/home/csr/projects/netarchivesuite/common/restapi/target/restapi.war");


       /* Wrapper servlet = restContext.createWrapper();
        servlet.setName( "jaxrs" );
        servlet.setServletClass(
                "org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet" );

        servlet.addInitParameter(
                "jaxrs.serviceClasses",
                "dk.netarkivet.common.api.Hello"
        );
        servlet.setLoadOnStartup( 1 );
        restContext.addChild( servlet );
        restContext.addServletMapping( "/rest*//**//**//**//*", "jaxrs" );    */

        final String absolutePath = createBaseDirectory().getAbsolutePath();
        server.setBaseDir(absolutePath);
        server.setPort(4242);
        //server.setBaseDir(".");
        server.getHost().setAppBase(absolutePath);
        //server.getServer().addLifecycleListener(new AprLifecycleListener());
        //server.addWebapp("/rest", "/home/csr/projects/netarchivesuite/common/restapi/target/restapi.war");
        server.start();
        server.getServer().await();
        Socket socket = new Socket(InetAddress.getLocalHost(), 4242);
        server.stop();
    }

    private static File createBaseDirectory() throws IOException {
           final File base = File.createTempFile( "tmp-", "", new File(".") );

           if( !base.delete() ) {
               throw new IOException( "Cannot (re)create base folder: " + base.getAbsolutePath()  );
           }

           if( !base.mkdir() ) {
               throw new IOException( "Cannot create base folder: " + base.getAbsolutePath()  );
           }

           return base;
       }

    @Test
    public void testRunningServer() throws IOException {
        Settings.set(CommonSettings.SITESECTION_WEBAPPLICATION,
                "");
        Settings.set(CommonSettings.SITESECTION_CLASS, "");
        Settings.set("settings.common.webinterface.restwar", "/home/csr/projects/netarchivesuite/common/restapi/target/restapi.war");
        server = GUIWebServer.getInstance();
        //server = new GUIWebServer();
        //server.startServer();
        Socket socket = new Socket(InetAddress.getLocalHost(),
                dk.netarkivet.common.webinterface.TestInfo.GUI_WEB_SERVER_PORT);
        server.cleanup();
    }


    public void testExpectedExceptionsWhenStartingServer() throws IOException {
        Settings.set(CommonSettings.HTTP_PORT_NUMBER, Long.toString(65536L));

        // arguments not in range
        try {
            server = new GUIWebServer();
            fail("IOFailure expected for port " + 65536);
        } catch (IOFailure e) {
            // Expected
        }

        Settings.set(CommonSettings.HTTP_PORT_NUMBER, Long.toString(-1L));

        try {
            server = new GUIWebServer();
            fail("IOFailure expected for port " + -1);
        } catch (IOFailure e) {
            // Expected
        }

        Settings.set(CommonSettings.HTTP_PORT_NUMBER, Long.toString(1023L));

        try {
            server = new GUIWebServer();
            fail("IOFailure expected for port " + 1023);
        } catch (IOFailure e) {
            // Expected
        }

        // Port already in use
        ServerSocket socket = new ServerSocket(dk.netarkivet.common.webinterface.TestInfo.GUI_WEB_SERVER_PORT);

        try {
            server = new GUIWebServer();
            server.startServer();
            fail("IOFailure expected when running on port already in use");
        } catch (IOFailure e) {
            // Expected
        }

        socket.close();
        socket = null;
    }

    public void testExpectedExceptionsWhenAddingContext() throws IOException {
        // wrong arguments when adding context
        Settings.set(CommonSettings.SITESECTION_WEBAPPLICATION, "/not_found_because_it_doesnt_exist");
        try {
            server = new GUIWebServer();
            server.startServer();
            fail("IOFailure expected when directory is not found");
        } catch (IOFailure e) {
            // Expected
        }
    }

    
    public void testStopServer() throws InterruptedException {
        server = new GUIWebServer();
        try {
        	server.startServer();
        } catch (Throwable e) {
        	server = null; // avoid attempt of cleanup
        	fail("Failed to start server: " + e);
        }
        try {
            new Socket(InetAddress.getLocalHost(), dk.netarkivet.common.webinterface.TestInfo.GUI_WEB_SERVER_PORT);
        } catch (IOException e) {
            fail("Port not in use after starting server due to error: " + e);
        }
        server.cleanup();
        try {
            new Socket(InetAddress.getLocalHost(), dk.netarkivet.common.webinterface.TestInfo.GUI_WEB_SERVER_PORT);
            fail("Port still in use after stopping server!");
        } catch (IOException e) {
            // Expected
        }
    }
}
