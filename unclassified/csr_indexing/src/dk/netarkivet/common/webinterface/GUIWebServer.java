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
package dk.netarkivet.common.webinterface;

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
import dk.netarkivet.common.utils.StringUtils;

/**
 * A class representing an HttpServer. This class loads web applications as
 * given in settings.
 *
 */
public class GUIWebServer implements CleanupIF {
    /**
     * The unique instance of this class.
     */
    static GUIWebServer instance;
    /**
     * The Jetty server.
     */
    private Server server;
    /**
     * Logger for this class.
     */
    private Log log = LogFactory.getLog(getClass().getName());

    /**
     * Initialises a GUI Web Server and adds web applications.
     *
     * @throws IOFailure on trouble starting server.
     */
    protected GUIWebServer() {
        //Read and log settings.
        int port = Integer.parseInt(Settings.get(
                CommonSettings.HTTP_PORT_NUMBER));
        if (port < 1025 || port > 65535) {
            throw new IOFailure(
                    "Port must be in the range 1025-65535, not " + port);
        }
        //TODO Replace with just one setting. See feature request 1204
        String[] webApps = Settings.getAll(
                CommonSettings.SITESECTION_WEBAPPLICATION);
        String[] classes = Settings.getAll(CommonSettings.SITESECTION_CLASS);
        if (webApps.length != classes.length) {
            throw new IOFailure(
                    "Number of webapplications and number of classes defining "
                    + "the webapps do not match. "
                    + "Webapps: [" + StringUtils.conjoin(",", webApps) + "]. "
                    + "]. Classes: [" + StringUtils.conjoin(",", classes)
                    + "]");
        }

        log.info("Starting webserver. Port: " + port
                 + " deployment directories: '"
                 + StringUtils.conjoin(",", webApps)
                 + "' classes: '"
                 + StringUtils.conjoin(",", classes) + "'");

        //Get a Jetty server.
        server = new Server(port);

        //Add web applications
        try {
            for (int i = 0; i < webApps.length;
                 i++) {
                addWebApplication(webApps[i]);
            }
        } catch (Exception e) {
            throw new IOFailure(
                    "Error deploying the webapplications", e);
        }

        //Add default handler, giving 404 page that lists web contexts, and
        //favicon.ico from Jetty
        server.addHandler(new DefaultHandler());
    }

    /**
     * Returns the unique instance of this class. If instance is new, starts a
     * GUI web server.
     *
     * @return the instance
     */
    public static synchronized GUIWebServer getInstance() {
        if (instance == null) {
            instance = new GUIWebServer();
            instance.startServer();
        }
        return instance;
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
        
        for (SiteSection section : SiteSection.getSections()) {
            if (webbase.equals("/" + section.getDirname())) {
                section.initialize();
                break;
            }
        }
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
                SiteSection.cleanup();
            } catch (Exception e) {
                throw new IOFailure("Error while stopping server", e);
            }
            log.info("HTTP server has been stopped.");
        }
        instance = null;
    }
}
