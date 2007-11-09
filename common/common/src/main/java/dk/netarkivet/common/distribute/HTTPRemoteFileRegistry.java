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
package dk.netarkivet.common.distribute;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.AbstractHandler;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.CleanupHook;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * This is a registry for HTTP remote file, meant for serving registered files
 * to remote hosts.
 * The embedded webserver handling remote files for HTTPRemoteFile
 * point-to-point communication. Optimised to use direct transfer on local
 * machine.
 */
public class HTTPRemoteFileRegistry implements CleanupIF {
    /** The unique instance. */
    protected static HTTPRemoteFileRegistry instance;

    /** Protocol for URLs. */
    private static final String PROTOCOL = "http";

    /** Port number for generating URLs. Read from settings. */
    protected final int port;

    /** Name of this host. Used for generating URLs. */
    private final String localHostName;

    /** Files to serve. */
    private final Map<URL, FileInfo> registeredFiles;

    /** Instance to create random URLs. */
    private final Random random;

    /** Postfix to add to an URL to get cleanup URL.
     * We are not using query string, because it behaves strangely in
     * HttpServletRequest. */
    private static final String UNREGISTER_URL_POSTFIX = "/unregister";

    /** Logger for this class. */
    private final Log log = LogFactory.getLog(getClass());
    protected Server server;
    private CleanupHook cleanupHook;

    /** Initialise the registry. This includes registering an HTTP server for
     * getting the files from this machine.
     * @throws IOFailure if it cannot initalise.
     */
    protected HTTPRemoteFileRegistry() {
        port = Settings.getInt(Settings.HTTPREMOTEFILE_PORT_NUMBER);
        localHostName = SystemUtils.getLocalHostName();
        registeredFiles = Collections.synchronizedMap(
                new HashMap<URL, FileInfo>());
        random = new Random();
        startServer();
        cleanupHook = new CleanupHook(this);
        Runtime.getRuntime().addShutdownHook(cleanupHook);
    }

    /** Start the server, including a handler that responds with registered
     * files, removes registered files on request, and gives 404 otherwise.
     * @throws IOFailure if it cannot initalise.
     */
    protected void startServer() {
        server = new Server();
        SocketConnector connector = new SocketConnector();
        connector.setPort(port);
        server.addConnector(connector);
        server.addHandler(new HTTPRemoteFileRegistryHandler());
        try {
            server.start();
        } catch (Exception e) {
            throw new IOFailure("Cannot start HTTPRemoteFile registry", e);
        }
    }

    /**
     * Get the protocol part of URLs, that is HTTP
     * @return "http", the protocol.
     */
    protected String getProtocol() {
        return PROTOCOL;
    }

    /** Get the unique instance.
     * @return The unique instance.
     */
    public synchronized static HTTPRemoteFileRegistry getInstance() {
        if (instance == null) {
            instance = new HTTPRemoteFileRegistry();
        }
        return instance;
    }

    /**
     * Register a file for serving to an endpoint.
     * @param file The file to register.
     * @param deletable Whether it should be deleted on cleanup.
     * @return The URL it will be served as. It will be uniquely generated.
     * @throws ArgumentNotValid on null or unreadable file.
     * @throws IOFailure on any trouble registerring the file
     */
    public URL registerFile(File file, boolean deletable) {
        ArgumentNotValid.checkNotNull(file, "File file");
        if (!file.isFile() && file.canRead()) {
            throw new ArgumentNotValid("File '" + file
                    + "' is not a readable file");
        }
        String path;
        URL url;
        //ensure we get a random and unique URL.
        do {
            path = "/" + Integer.toHexString(random.nextInt());
            try {
                url = new URL(getProtocol(), localHostName, port, path);
            } catch (MalformedURLException e) {
                throw new IOFailure("Unable to create URL for file '" + file
                                    + "'." + " '" + getProtocol() + "', '"
                                    + localHostName + "', '" + port + "', '"
                                    + path + "''", e);
            }
        } while (registeredFiles.containsKey(url));
        registeredFiles.put(url, new FileInfo(file, deletable));
        log.debug("Registered file '" + file.getPath() + "' with URL '" + url
                  + "'");
        return url;
    }

    /** Get the url for cleaning up after a remote file registered under some
     * URL.
     * @return the cleanup url.
     */
    URL getCleanupUrl(URL url) throws MalformedURLException {
        return new URL(url.getProtocol(), url.getHost(), url.getPort(),
                  url.getPath() + UNREGISTER_URL_POSTFIX);
    }

    /** Open a connection to an URL in a registry.
     * @param url The URL to open connection to. */
    protected URLConnection openConnection(URL url) throws IOException {
        return url.openConnection();
    }

    /** Pair of information registered. */
    private class FileInfo {
        /** The file. */
        final File file;
        /** Whether it should be deleted on cleanup. */
        final boolean deletable;

        /** Initialise pair.
         *
         * @param file The file.
         * @param deletable Whether it should be deleted on cleanup.
         */
        FileInfo(File file, boolean deletable) {
            this.file = file;
            this.deletable = deletable;
        }
    }

    /** Stops the server and nulls the instance. */
    public void cleanup() {
        synchronized(HTTPRemoteFileRegistry.class) {
            try {
                server.stop();
            } catch (Exception e) {
                log.warn("Unable to stop HTTPRemoteFile registry");
            }
            try {
                Runtime.getRuntime().removeShutdownHook(cleanupHook);
            } catch (Exception e) {
                //ignore
            }
            instance = null;
        }
    }

    /** A handler for the registry.
     *
     * It has three ways to behave:
     * Serve registered files, return 404 on unknown files, and unregister
     * registered files, depending on the URL.
     */
    protected class HTTPRemoteFileRegistryHandler extends AbstractHandler {
        /**
         * A method for handling Jetty requests.
         *
         * @see AbstractHandler#handle(String, HttpServletRequest, HttpServletResponse, int)
         *
         * @param string Unused domain.
         * @param httpServletRequest request object.
         * @param httpServletResponse the response to write to.
         * @param i Unused state.
         * @throws IOException On trouble in communication.
         * @throws ServletException On servlet trouble.
         */
        public void handle(String string,
                           HttpServletRequest httpServletRequest,
                           HttpServletResponse httpServletResponse, int i)
                throws IOException, ServletException {
            // since this is a jetty handle method, we know it is a Jetty
            // request object.
            Request request = ((Request) httpServletRequest);
            String urlString = httpServletRequest.getRequestURL()
                    .toString();
            if (urlString.endsWith(UNREGISTER_URL_POSTFIX)) {
                URL url = new URL(urlString.substring(0,
                     urlString.length() - UNREGISTER_URL_POSTFIX.length()));
                FileInfo fileInfo = registeredFiles.remove(url);
                if (fileInfo != null && fileInfo.deletable
                    && fileInfo.file.exists()) {
                    FileUtils.remove(fileInfo.file);
                    log.debug("Removed file '" + fileInfo.file.getPath()
                              + "' with URL '" + url + "'");
                }
                httpServletResponse.setStatus(200);
                request.setHandled(true);
            } else {
                URL url = new URL(urlString);
                FileInfo fileInfo = registeredFiles.get(url);
                if (fileInfo != null) {
                    httpServletResponse.setStatus(200);
                    FileUtils.writeFileToStream(fileInfo.file,
                                    httpServletResponse.getOutputStream());
                    request.setHandled(true);
                } else {
                    httpServletResponse.sendError(404);
                }
                log.debug("Served file '" + fileInfo.file.getPath()
                          + "' with URL '" + url + "'");
            }
        }
    }
}
