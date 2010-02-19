/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.wayback;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.webapp.WebAppContext;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
/**
 * Experimental code to launch wayback with Jetty.
 * This assumes the presence of the ROOT.war file in the netarchivesuite root dir.
 * Generated using the ant script "wayback.build.xml" with its "warfile" target
 * This script assumes that wayback-1.4.2 has been downloaded and the directory "examples/wayback" copied to "conf"
 * 
 * This launcher currently fails with an internal Jetty error: Path must not be null
 * 
 * 19-Feb-2010 11:28:27 dk.netarkivet.common.webinterface.WaybackWebServer addWebApplication
INFO: The web application '/home/svc/workspace/netarchivesuite/ROOT.war' is now deployed at '/ROOT'
19-Feb-2010 11:28:30 org.slf4j.impl.JCLLoggerAdapter warn
WARNING: failed RequestFilter: java.lang.IllegalArgumentException: Path must not be null
19-Feb-2010 11:28:30 org.slf4j.impl.JCLLoggerAdapter error
SEVERE: Failed startup of context org.mortbay.jetty.webapp.WebAppContext@11410e5{/ROOT,/home/svc/workspace/netarchivesuit
e/ROOT.war}
java.lang.IllegalArgumentException: Path must not be null
        at org.springframework.util.Assert.notNull(Assert.java:112)
        at org.springframework.core.io.FileSystemResource.<init>(FileSystemResource.java:59)
        at org.archive.wayback.webapp.RequestMapper.<init>(RequestMapper.java:75)
        at org.archive.wayback.webapp.RequestFilter.init(RequestFilter.java:59)
        at org.mortbay.jetty.servlet.FilterHolder.doStart(FilterHolder.java:97)
        at org.mortbay.component.AbstractLifeCycle.start(AbstractLifeCycle.java:50)
        at org.mortbay.jetty.servlet.ServletHandler.initialize(ServletHandler.java:662)
        at org.mortbay.jetty.servlet.Context.startContext(Context.java:140)
        at org.mortbay.jetty.webapp.WebAppContext.startContext(WebAppContext.java:1250)
        at org.mortbay.jetty.handler.ContextHandler.doStart(ContextHandler.java:517)
        at org.mortbay.jetty.webapp.WebAppContext.doStart(WebAppContext.java:467)
        at org.mortbay.component.AbstractLifeCycle.start(AbstractLifeCycle.java:50)
        at org.mortbay.jetty.handler.HandlerCollection.doStart(HandlerCollection.java:152)
        at org.mortbay.component.AbstractLifeCycle.start(AbstractLifeCycle.java:50)
        at org.mortbay.jetty.handler.HandlerWrapper.doStart(HandlerWrapper.java:130)
        at org.mortbay.jetty.Server.doStart(Server.java:224)
        at org.mortbay.component.AbstractLifeCycle.start(AbstractLifeCycle.java:50)
        at dk.netarkivet.common.webinterface.WaybackWebServer.startServer(WaybackWebServer.java:178)
        at dk.netarkivet.common.webinterface.WaybackWebServer.getInstance(WaybackWebServer.java:66)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
        at java.lang.reflect.Method.invoke(Method.java:597)
        at dk.netarkivet.common.utils.ApplicationUtils.startApp(ApplicationUtils.java:168)
        at dk.netarkivet.common.webinterface.WaybackApplication.main(WaybackApplication.java:38)
19-Feb-2010 11:28:30 dk.netarkivet.common.utils.ApplicationUtils startApp
FINEST: Factory method invoked.
19-Feb-2010 11:28:30 dk.netarkivet.common.utils.ApplicationUtils startApp
FINEST: Adding shutdown hook for dk.netarkivet.common.webinterface.WaybackWebServer
19-Feb-2010 11:28:30 dk.netarkivet.common.utils.ApplicationUtils startApp
FINEST: Added shutdown hook for dk.netarkivet.common.webinterface.WaybackWebServer

 *
 */
public class WaybackWebServer implements CleanupIF {

    /**
     * The unique instance of this class.
     */
    static WaybackWebServer instance;
    /**
     * The Jetty server.
     */
    private Server server;
    /**
     * Logger for this class.
     */
    private Log log = LogFactory.getLog(getClass().getName());
    
    /**
     * Returns the unique instance of this class. If instance is new, starts a
     * Wayback web server.
     *
     * @return the instance
     */
    public static synchronized WaybackWebServer getInstance() {
        if (instance == null) {
            instance = new WaybackWebServer();
            instance.startServer();
        }
        return instance;
    }
    
    /**
     * Initialises a Wayback Web Server and adds the wayback application.
     * Reads the port in the CommonSettings.HTTP_PORT_NUMBER. 
     * TODO change to CommonSettings.WAYBACK_PORT_NUMBER
     * @throws IOFailure on trouble starting server.
     */
    protected WaybackWebServer() {
        //Read and log settings.
        int port = Integer.parseInt(Settings.get(
                CommonSettings.HTTP_PORT_NUMBER));
        if (port < 1025 || port > 65535) {
            throw new IOFailure(
                    "Port must be in the range 1025-65535, not " + port);
        }
        
        //TODO Replace with just one setting. See feature request 1204
//        String[] webApps = Settings.getAll(
//                CommonSettings.SITESECTION_WEBAPPLICATION);
//        String[] classes = Settings.getAll(CommonSettings.SITESECTION_CLASS);
//        if (webApps.length != classes.length) {
//            throw new IOFailure(
//                    "Number of webapplications and number of classes defining "
//                    + "the webapps do not match. "
//                    + "Webapps: [" + StringUtils.conjoin(",", webApps) + "]. "
//                    + "]. Classes: [" + StringUtils.conjoin(",", classes)
//                    + "]");
//        }
//
        String webapp = new String("ROOT.war");
        
        log.info("Starting Wayback webserver. Port: " + port    
               + " deployment directory: '" + webapp + "'");

        //Get a Jetty server.
        server = new Server(port);

        //Add wayback application
        try {
          addWebApplication(
                  new String("ROOT.war"));
            
        } catch (Exception e) {
            throw new IOFailure(
                    "Error deploying the webapplications", e);
        }

        //Add default handler, giving 404 page that lists web contexts, and
        //favicon.ico from Jetty
        server.addHandler(new DefaultHandler());
    }
    
    /**
     * Adds a directory with jsp files on the given basepath of the web server.
     * Note: This must be done BEFORE starting the server.
     * The webbase is deduced from the name of the webapp.
     *
     * @param webapp  a directory with jsp files or a war file.
     * @throws IOFailure        if directory is not found.
     * @throws ArgumentNotValid if either argument is null or empty or if
     *                          webbase doesn't start with '/'.
     * @throws PermissionDenied if the server is already running.
     */
    private void addWebApplication(String webapp)
            throws IOFailure, ArgumentNotValid, PermissionDenied {
        
        if (!new File(webapp).exists()) {
            throw new IOFailure(
                    "Web application '" + webapp + "' not found");
        }
        
        // Construct webbase from the name of the webapp.
        // (1) If the webapp is webpages/History, the webbase is /History
        // (2) If the webapp is webpages/History.war, the webbase is /History
        String webappFilename = new File(webapp).getName();
        String webbase = "/" + webappFilename;
        if (webappFilename.toLowerCase().endsWith(".war")) {
            webbase = "/" + webappFilename.substring(0, webappFilename.length() - 4);
        }
        System.out.println("webapp = " + webapp);
        System.out.println("webbase = " + webbase);
        
//        for (SiteSection section : SiteSection.getSections()) {
//            if (webbase.equals("/" + section.getDirname())) {
//                section.initialize();
//                break;
//            }
//        }
        WebAppContext webApplication = new WebAppContext(webapp, webbase);
        //Do not have a limit on the form size allowed
        webApplication.setMaxFormContentSize(-1);
        //Use directory in commontempdir for cache
        webApplication.setTempDirectory(
                new File(FileUtils.getTempDir(), webbase));
        server.addHandler(webApplication);
        log.info("The web application '" + webapp + "' is now deployed at '"
                 + webbase + "'");
    }
    
    /**
     * Starts the jsp web server.
     *
     * @throws IOFailure if the server for any reason cannot be started.
     */
    public void startServer() {
        //start the server.
        try {
            server.start();
        } catch (Exception e) {
            log.warn("Could not start GUI", e);
            if (server.isStarted()) {
                cleanup();
            }
            throw new IOFailure("Could not start GUI", e);
        }
    }
    
    /**
     * Closes the GUI webserver, and nullifies this instance.
     */
    public void cleanup() {
        if (server.isStarted()) {
            try {
                server.stop();
                server.destroy();
                
            } catch (Exception e) {
                throw new IOFailure("Error while stopping server", e);
            }
            log.info("HTTP server has been stopped.");
        }
        instance = null;
    }

}
