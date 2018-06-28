/*
 * #%L
 * Netarchivesuite - harvester
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
package dk.netarkivet.viewerproxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.server.handler.DefaultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * The WebProxy is the ONLY viewerproxy class that interfaces with the Jetty classes. This class packages all requests
 * up nicely as calls to uriResolver.lookup().
 * <p>
 * In particular, it handles the control of the Jetty server that the Proxy server builds on.
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class WebProxy extends DefaultHandler implements URIResolverHandler {
    /** The URI resolver which handles URI lookups. */
    private URIResolver uriResolver;
    /** Logger used for reporting. */
    private static final Logger log = LoggerFactory.getLogger(WebProxy.class);

    /** The actual web server that we're the link to. */
    private Server jettyServer;

    /** HTTP header. */
    private static final String CONTENT_TYPE_NAME = "Content-type";
    /** Content-type header value. */
    private static final String CONTENT_TYPE_VALUE = "text/html";
    /** Inserted before error response to browser. */
    private static final String HTML_HEADER = "<html><head><title>" + "Internal Server Error" + "</title><body>";
    /** Inserted after error response to browser. */
    private static final String HTML_FOOTER = "</body></html>";
    
    private final int portNo;

    /**
     * Initialises a new web proxy, which delegates lookups to the given uri resolver. The WebProxy will start listening
     * on port given in settings.
     *
     * @param uriResolver The uriResolver used to handle lookups in the proxy.
     * @throws IOFailure on trouble starting the proxy server.
     * @throws ArgumentNotValid on null uriResolver.
     */
    public WebProxy(URIResolver uriResolver) {
        setURIResolver(uriResolver);
        portNo = Settings.getInt(CommonSettings.HTTP_PORT_NUMBER);
        jettyServer = new Server(portNo);
        jettyServer.setHandler(this);
        log.info("Starting viewerproxy jetty on port {}", portNo);
        try {
            jettyServer.start();
        } catch (Exception e) {
            throw new IOFailure("Error while starting jetty server on port " + portNo, e);
        }
    }

    /**
     * Sets the current URIResolver.
     *
     * @param ur The resolver to handle lookups.
     * @throws ArgumentNotValid on null uriResolver.
     */
    public void setURIResolver(URIResolver ur) {
        ArgumentNotValid.checkNotNull(ur, "URIResolver ur");
        this.uriResolver = ur;
    }

    /**
     * Handle an HTTP request. Overrides default behaviour of Jetty. This will forward the URI and response to the
     * wrapped URI resolver. Note that the server will NOT force the return value to be the one returned by the uri
     * resolver, rather it will use the one the uri resolver has set in the response object.
     * <p>
     * Exceptions will generate an internal server error-page with the details.
     *
     * @param target URL or name for request. Not used
     * @param request The original request, including URL
     * @param response The object that receives the result
     */
    @Override
    /*
    public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) {
        HttpResponse netarkivetResponse = new HttpResponse(response);
        HttpRequest netarkivetRequest = new HttpRequest(request);
        try {
            // The following is a bad idea because it hides where the
            // failure actually happens in the code
            // Generate URI to enforce fail-early of illegal URIs
            // uri = new URI(request.getRequestURL().toString());
            uriResolver.lookup(netarkivetRequest, netarkivetResponse);
            ((org.eclipse.jetty.server.Request) request).setHandled(true);
        } catch (Exception e) {
            createErrorResponse(netarkivetRequest.getURI(), netarkivetResponse, e);
        }
    }
    */
    public void handle(String target, HttpServletRequest request,
    		HttpServletResponse response, int dispatch) {
    	HttpResponse netarkivetResponse = new HttpResponse(response);
    	HttpRequest netarkivetRequest = new HttpRequest(request);
    	try {
    		//The following is a bad idea because it hides where the
    		//failure actually happens in the code
    		//Generate URI to enforce fail-early of illegal URIs 
    		//uri = new URI(request.getRequestURL().toString());
    		uriResolver.lookup(netarkivetRequest, netarkivetResponse);
    		((org.mortbay.jetty.Request) request).setHandled(true);
    	} catch (Exception e) {
    		createErrorResponse(netarkivetRequest.getURI(),
    				netarkivetResponse, e);
    	}
    }
    

    /**
     * Generate an appropriate error response when a URI generates an exception. If this fails, it is logged, but
     * otherwise ignored.
     *
     * @param uri The URI attempted read that could not be found
     * @param response The Response object to write the error response into.
     * @param e the exception generated by the URI
     */
    private void createErrorResponse(URI uri, Response response, Throwable e) {
        try {
            // first write a header telling the browser to expect text/html
            response.addHeaderField(CONTENT_TYPE_NAME, CONTENT_TYPE_VALUE);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // Now flush an errorscreen to the browser
            OutputStream browserOut = response.getOutputStream();
            browserOut.write((HTML_HEADER + "Internal server error for: " + uri + "\n<pre>"
                    + ExceptionUtils.getStackTrace(e) + "</pre>" + HTML_FOOTER).getBytes());
            browserOut.flush();
            log.warn("Exception for : " + uri, e);
        } catch (Exception e1) {
            log.warn(
                    "Error writing error response to browser " + "for '" + uri + "' with exception "
                            + ExceptionUtils.getStackTrace(e) + ". Giving up!", e1);
        }
        // Do not close stream! That is left to the servlet.
    }

    /** Shut down this server. */
    public void kill() {
        try {
        	log.info("Shutting down viewerproxy jetty listening on port {}", portNo);
            jettyServer.stop();
            jettyServer.destroy();
        } catch (Exception ie) {
            log.warn("Error shutting down server", ie);
        }
    }

    /**
     * A wrapper around the Jetty HttpResponse, giving the simple Response interface used in our URIResolvers. Also
     * Collects and remembers status code for a response.
     */
    public static class HttpResponse implements Response {
        /** The Jetty http response object. */
        private HttpServletResponse hr;
        /** The HTTP status code. */
        private int status;

        /**
         * Constructs a new HttpResponse based on the given Jetty response.
         *
         * @param htResp A response object to wrap.
         */
        private HttpResponse(HttpServletResponse htResp) {
            hr = htResp;
        }

        /**
         * Getter for the data output stream.
         *
         * @return An open output stream.
         * @throws IOFailure if an outputstream can not be obtained (on invalidated response).
         */
        public OutputStream getOutputStream() {
            try {
                return hr.getOutputStream();
            } catch (IOException e) {
                throw new IOFailure("Outputstream not available", e);
            }
        }

        /**
         * Setter for the status code (e.g. 200, 404)
         *
         * @param statusCode An HTTP status code.
         */
        public void setStatus(int statusCode) {
            this.status = statusCode;
            hr.setStatus(statusCode);
        }

        /**
         * Set status code and explanatory text string describing the status.
         *
         * @param statusCode should be valid http status ie. 200, 404,
         * @param reason text string explaining status ie. OK, not found,
         */
        public void setStatus(int statusCode, String reason) {
        	log.debug("Calling setStatus with statusCode {} and reason {} using deprecated API", statusCode, reason);
            this.status = statusCode;
            // Note: This uses deprecated method.
            // We still use this, because in the proxying we need to set both
            // status, reason, and body, and this is the only possible way to do
            // this
            hr.setStatus(statusCode, reason);
        }

        /**
         * Add an HTTP header to the response.
         *
         * @param name Name of the header, e.g. Last-Modified-Date
         * @param value The value of the header
         */
        public void addHeaderField(String name, String value) {
            if (hr.isCommitted()) {
                log.warn("Writing a header {}:{} after the http response is committed.", name, value);
            }
            hr.addHeader(name, value);
        }

        /**
         * Get the HTTP status of this response.
         *
         * @return The HTTP status.
         */
        public int getStatus() {
            return status;
        }
    }

    /**
     * A wrapper around the Jetty HttpRequest, giving the simple Request interface used in our URIResolvers. Gives
     * access to URI and posted parameters.
     */
    public static class HttpRequest implements Request {
        /** The Jetty http response object. */
        private HttpServletRequest hr;

        /**
         * Constructs a new HttpRequest based on the given Jetty request.
         *
         * @param htReq A request object to wrap.
         */
        protected HttpRequest(HttpServletRequest htReq) {
            hr = htReq;
        }

        /**
         * Get the URI from this request. In contrast to javax.servlet.HttpServletResponse this includes the query
         * string.
         *
         * @return The URI from this request.
         * @throws IOFailure if the URI is invalid. This should never happen.
         */
        public URI getURI() {
            String uriString;
            if (hr.getQueryString() != null) {
                uriString = hr.getRequestURL().toString() + "?" + uriEncode(hr.getQueryString());
            } else {
                uriString = hr.getRequestURL().toString();
            }
            try {
                return new URI(uriString);
            } catch (URISyntaxException e) {
                throw new IOFailure("Could not construct URI from '" + uriString + "'", e);
            }
        }

        /**
         * We here replace what should be standard API functionality with an apparent kludge. We do this because the
         * constructor java.net.URI(String s) violates its own documentation. It should encode all "other" characters in the
         * query part of the URI. These "other" characters include curly brackets, but actually the escaping is never
         * done. Hence we do it here.
         *
         * @param s the String to be encoded
         * @return the encoded String
         */
        public static String uriEncode(String s) {
            return s.replaceAll("\\{", "%7B").replaceAll("\\}", "%7D");
        }

        /**
         * Get parameters from this request. Note that this method is invalidated when the request is replied to.
         *
         * @return The parameters from this request.
         */
        public Map<String, String[]> getParameterMap() {
            return (Map<String, String[]>) hr.getParameterMap();
        }
    }
}
