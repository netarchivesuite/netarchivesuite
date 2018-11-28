/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
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

import java.io.File;

import javax.servlet.ServletException;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.scan.Constants;
import org.apache.tomcat.util.scan.StandardJarScanFilter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;

/**
 * A class representing an HttpServer. This class loads web applications as given in settings.
 */
public class GUIWebServer implements CleanupIF {
    /**
     * The unique instance of this class.
     */
    private static GUIWebServer instance;
    /**
     * The Tomcat server.
     */
    private Tomcat server;
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(GUIWebServer.class);

    /** The lower limit of acceptable HTTP port-numbers. */
    private static final int HTTP_PORT_NUMBER_LOWER_LIMIT = 1025;

    /** The upper limit of acceptable HTTP port-numbers. */
    private static final int HTTP_PORT_NUMBER_UPPER_LIMIT = 65535;

    /**
     * Initialises a GUI Web Server and adds web applications.
     *
     * @throws IOFailure on trouble starting server.
     */
    public GUIWebServer() {
        // Read and log settings.

        int port = Integer.parseInt(Settings.get(CommonSettings.HTTP_PORT_NUMBER));
        if (port < HTTP_PORT_NUMBER_LOWER_LIMIT || port > HTTP_PORT_NUMBER_UPPER_LIMIT) {
            throw new IOFailure("Port must be in the range [" + HTTP_PORT_NUMBER_LOWER_LIMIT + ", "
                    + HTTP_PORT_NUMBER_UPPER_LIMIT + "], not " + port);
        }
        // TODO Replace with just one setting. See issue NAS-1687
        String[] webApps = Settings.getAll(CommonSettings.SITESECTION_WEBAPPLICATION);
        String[] classes = Settings.getAll(CommonSettings.SITESECTION_CLASS);
        if (webApps.length != classes.length) {
            throw new IOFailure("Number of webapplications and number of classes defining "
                    + "the webapps do not match. " + "Webapps: [" + StringUtils.conjoin(",", webApps) + "]. "
                    + "]. Classes: [" + StringUtils.conjoin(",", classes) + "]");
        }

        log.info("Starting webserver. Port: " + port + " deployment directories: '" + StringUtils.conjoin(",", webApps)
                + "' classes: '" + StringUtils.conjoin(",", classes) + "'");

        // Get a tomcat server.
        server = new Tomcat();


        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");

        // Use directory in commontempdir for cache
        final File tempDir = FileUtils.getTempDir();
        log.debug("GUI using tempdir " + tempDir.getAbsolutePath());
        File basedir = tempDir.getAbsoluteFile().getParentFile();
        if(tempDir.isAbsolute()) {
        	basedir = new File("");
        }
        log.debug("GUI using basedir " + basedir.getAbsolutePath());
        server.setBaseDir(basedir.getAbsolutePath());

        //File webapps = new File(basedir, "/webapps");
        File webapps = tempDir;
        if (webapps.exists()) {
            FileUtils.removeRecursively(webapps);
            log.info("Deleted existing tempdir '" + webapps.getAbsolutePath() + "'");
        }

        webapps.mkdirs();
        server.getHost().setAppBase(webapps.getAbsolutePath());
        server.getHost().setAutoDeploy(true);
        ((StandardHost) server.getHost()).setUnpackWARs(true);
        //set the port on which tomcat should run
        server.setPort(port);
        boolean taglibsScanningDisabled = false;

        if (System.getProperty(Constants.SKIP_JARS_PROPERTY) == null) {
            log.info("Scanning for taglibs is disabled as " + Constants.SKIP_JARS_PROPERTY + " is unset."); // Only log this once for all contexts
            taglibsScanningDisabled = true;
        }

        //add webapps to tomcat
        for (String webapp: webApps) {
            // Construct webbase from the name of the webapp.
            // (1) If the webapp is webpages/History, the webbase is /History
            // (2) If the webapp is webpages/History.war, the webbase is /History
            String webappFilename = new File(webapp).getName();
            String webbase = "/" + webappFilename;
            final String warSuffix = ".war";
            if (webappFilename.toLowerCase().endsWith(warSuffix)) {
                webbase = "/" + webappFilename.substring(0, webappFilename.length() - warSuffix.length());
            }

            for (SiteSection section : SiteSection.getSections()) {
                if (webbase.equals("/" + section.getDirname())) {
                    section.initialize();
                    break;
                }
            }
            try {
                //add the jar file to tomcat
                final File warfileFile = new File(basedir.getAbsolutePath(), webapp);
                if (!warfileFile.exists() || !warfileFile.isFile()) {
                    throw new IOFailure("Could not find expected file " + warfileFile.getAbsolutePath());
                }
                String warfile = warfileFile.getAbsolutePath();
                log.info("Deploying webapp with context {} at docbase {}.", webbase, warfile);
                StandardContext ctx = (StandardContext) server.addWebapp(webbase, warfile);
                if (taglibsScanningDisabled) {
                    StandardJarScanFilter jarScanFilter = (StandardJarScanFilter) ctx.getJarScanner().getJarScanFilter();
                    // Disable scanning for taglibs
                    jarScanFilter.setTldSkip("*");
                }
                if (webapp.equals(webApps[0])) {
                    //Re-add the 1st context as also the root context
                    StandardContext rootCtx = (StandardContext) server.addWebapp("/", warfile);
                    if (taglibsScanningDisabled) {
                        StandardJarScanFilter jarScanFilter = (StandardJarScanFilter) rootCtx.getJarScanner().getJarScanFilter();
                        // Disable scanning for taglibs
                        jarScanFilter.setTldSkip("*");
                    }
                }
            }
            catch (ServletException e) {
                log.error("Unable to add webapp " + webapp, e);
            }
        }
    }


    /**
     * Returns the unique instance of this class. If instance is new, starts a GUI web server.
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
     * Starts a Tomcat server.
     *
     * @throws IOFailure if the server for any reason cannot be started.
     */
    public void startServer() {
        // start the server.
        try {
            server.start();

        } catch (Exception e) {
            cleanup();
            log.warn("Could not start GUI", e);
        }
    }

    /**
     * Closes the GUI webserver, and nullifies this instance.
     */
    public void cleanup() {

        try {
            server.stop();
            server.destroy();
            SiteSection.cleanup();
            log.info("GUI webserver has been stopped.");
        } catch (Exception e) {
            //throw new IOFailure("Error while stopping server", e);
        	log.warn("Error while stopping server, Trying to ignore it", e);
        }
        

        resetInstance();
    }

    /** resetClassInstance. */
    private static synchronized void resetInstance() {
        instance = null;
    }
}
