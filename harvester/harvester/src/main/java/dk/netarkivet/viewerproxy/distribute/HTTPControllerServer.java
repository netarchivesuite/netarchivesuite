/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.viewerproxy.distribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.viewerproxy.CommandResolver;
import dk.netarkivet.viewerproxy.Controller;
import dk.netarkivet.viewerproxy.Request;
import dk.netarkivet.viewerproxy.Response;
import dk.netarkivet.viewerproxy.URIResolver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Wrapper for an URIResolver, which calls the controller methods on given
 * specific URLs, and forwards all others to the wrapped handler. This allows
 * you to access control methods by giving specific urls to this class.
 *
 */
public class HTTPControllerServer extends CommandResolver {
    /**
     * The controller to call methods on
     */
    private Controller c;
    /** Logger for this class. */
    private Log log = LogFactory.getLog(getClass().getName());

    /** Command for starting url collection */
    static final String START_COMMAND = "/startRecordingURIs";
    /** Command for stopping url collection */
    static final String STOP_COMMAND = "/stopRecordingURIs";
    /** Command for clearing collected urls */
    static final String CLEAR_COMMAND = "/clearRecordedURIs";
    /** Command for getting collected urls */
    static final String GET_RECORDED_URIS_COMMAND = "/getRecordedURIs";
    /** Command for changing index */
    static final String CHANGE_INDEX_COMMAND = "/changeIndex";
    /** Command for getting status */
    static final String GET_STATUS_COMMAND = "/getStatus";

    /** Parameter defining the url to return to after doing start, stop, clear,
     * or changeIndex */
    static final String RETURN_URL_PARAMETER = "returnURL";
    /** Parameter for ids of jobs to change index to. May be repeated. */
    static final String JOB_ID_PARAMETER = "jobID";
    /** Parameter for label of an index. */
    static final String INDEX_LABEL_PARAMETER ="label";
    /** Parameter for locale to generate status */
    static final String LOCALE_PARAMETER = "locale";

    /** Http header for location */
    private static final String LOCATION_HEADER = "Location";
    /** Http header for content type */
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    /** Http header value for content type text */
    private static final String TEXT_PLAIN_MIMETYPE = "text/plain; charset=UTF-8";

    /** Http response code for redirect */
    private static final int REDIRECT_RESPONSE_CODE = 303;
    /** Http response code for OK */
    private static final int OK_RESPONSE_CODE = 200;

    /**
     * Make a new HTTPControllerServer, which calls commands on the given
     * controller, and forwards all other requests to the given URIResolver.
     *
     * @param c  The controller which handles commands given in command URLs.
     * @param ur The URIResolver to handle all other uris.
     * @throws ArgumentNotValid if either argument is null.
     */
    public HTTPControllerServer(Controller c, URIResolver ur) {
        super(ur);
        ArgumentNotValid.checkNotNull(c, "Controller c");
        this.c = c;
    }

    /**
     * Handles parsing of the URL and delegating to relevant
     * methods.  The commands are of the form
     *   http://<<localhostname>>/<<command>>?<<param>>=<<value>>*
     * Known commands are the following:
     * start           - params: returnURL - effect: start url collection
     *                                               return to returnURL
     * stop            - params: returnURL - effect: stop url collection
     *                                               return to returnURL
     * clear           - params: returnURL - effect: clear url collection
     *                                               return to returnURL
     * getRecordedURIs - params: none      - effect: write url collection to
     *                                               response
     * changeIndex     - params: jobID*,   - effect: generate index for jobs,
     *                           returnURL           return to returnURL
     * getStatus       - params: locale    - effect: write status to response.
     *
     * @param request  The request to check
     * @param response The response to give command results to if it is a
     *                 command.  If the request is one of these commands, the
     *                 response code is set to 303 if page is redirected to
     *                 return url; 200 if command url returns data; otherwise
     *                 whatever is returned by the wrapped resolver
     * @return Whether this was a command URL
     */
    protected boolean executeCommand(Request request, Response response) {
        //If the url is for this host (potential command)
        if (isCommandHostRequest(request)) {
            log.debug("Executing command " + request.getURI());
            //get path
            String path = request.getURI().getPath();
            if (path.equals(START_COMMAND)) {
                doStartRecordingURIs(request, response);
                return true;
            }
            if (path.equals(STOP_COMMAND)) {
                doStopRecordingURIs(request, response);
                return true;
            }
            if (path.equals(CLEAR_COMMAND)) {
                doClearRecordedURIs(request, response);
                return true;
            }
            if (path.equals(GET_RECORDED_URIS_COMMAND)) {
                doGetRecordedURIs(request, response);
                return true;
            }
            if (path.equals(CHANGE_INDEX_COMMAND)) {
                doChangeIndex(request, response);
                return true;
            }
            if (path.equals(GET_STATUS_COMMAND)) {
                doGetStatus(request, response);
                return true;
            }
        }
        return false;
    }

    /** Check parameter map for exactly the parameter names given. If any are
     * missing , throws IOFailure naming the expected parameters.
     *
     * @param request The request to check parameters in
     * @param parameterNames The parameters to check for.
     * @throws IOFailure on missing parameters.
     */
    private void checkParameters(Request request,
                                 String... parameterNames) {
        for (String parameter : parameterNames) {
            if (!request.getParameterMap().containsKey(parameter)) {
                throw new IOFailure("Bad request: '" + request.getURI() + "':\n"
                                    + "Wrong parameters. Expected: "
                                    + StringUtils.conjoin(parameterNames, ","));
            }
        }
    }

    /** Helper method to handle start command.
     *
     * @param request The HTTP request we are working on
     * @param response Response to handle result
     */
    private void doStartRecordingURIs(Request request, Response response) {
        setReturnResponseFromParameter(response, request);
        c.startRecordingURIs();
    }

    /** Helper method to handle stop command.
     *
     * @param request The HTTP request we are working on
     * @param response Response to handle result
     */
    private void doStopRecordingURIs(Request request, Response response) {
        setReturnResponseFromParameter(response, request);
        c.stopRecordingURIs();
    }

    /** Helper method to handle clear command.
     *
     * @param request The HTTP request we are working on
     * @param response Response to handle result
     */
    private void doClearRecordedURIs( Request request, Response response) {
        setReturnResponseFromParameter(response, request);
        c.clearRecordedURIs();
    }

    /** Helper method to handle getRecordedURIs command.
     *
     * @param request The HTTP request we are working on
     * @param response Response to handle result
     */
    private void doGetRecordedURIs(Request request, Response response) {
        checkParameters(request);
        Set<URI> uris = c.getRecordedURIs();
        response.addHeaderField(CONTENT_TYPE_HEADER, TEXT_PLAIN_MIMETYPE);
        OutputStream os = response.getOutputStream();
        try {
            for (URI recordedUri : uris) {
                os.write(recordedUri.toString().getBytes());
                os.write('\n');
            }
        } catch (IOException e) {
            throw new IOFailure("Error trying to write missing "
                                + "uris to http response!", e);
        }
        response.setStatus(OK_RESPONSE_CODE);
    }

    /** Helper method to handle changeIndex command.
     *
     * @param request The HTTP request we are working on
     * @param response Response to handle result
     */
    private void doChangeIndex(Request request, Response response) {
        checkParameters(request, JOB_ID_PARAMETER);
        setReturnResponseFromParameter(response, request);
        String[] jobIDStrings = request.getParameterMap().get(JOB_ID_PARAMETER);
        Set<Long> jobIDs = new HashSet<Long>();
        for (String jobIDString : jobIDStrings) {
            try {
                jobIDs.add(Long.parseLong(jobIDString));
            } catch (NumberFormatException e) {
                log.debug(
                           "Ignoring illegal job ID in change index "
                           + "command for uri '"+ request.getURI()
                           + "'", e);
            }
        }
        String label = getParameter(request, INDEX_LABEL_PARAMETER);
        c.changeIndex(jobIDs, label);
    }

    /** Helper method to handle getStatus command.
     *
     * @param request The HTTP request we are working on
     * @param response Response to handle result
     */
    private void doGetStatus(Request request, Response response) {
        String localeString = getParameter(request, LOCALE_PARAMETER);
        response.addHeaderField(CONTENT_TYPE_HEADER, TEXT_PLAIN_MIMETYPE);
        OutputStream os = response.getOutputStream();
        try {
            os.write(c.getStatus(new Locale(localeString)).getBytes());
        } catch (IOException e) {
            throw new IOFailure("Error trying to write status "
                                + "to http response!", e);
        }
        response.setStatus(OK_RESPONSE_CODE);
    }

    /** Set up the appropriate headers and return code for doing a redirect
     * to the URL given by the returnURL parameter.
     *
     * @param response The response to set to be a redirect
     * @param request The request to read the returnURL parameter from
     */
    private void setReturnResponseFromParameter(Response response,
                                                Request request) {
        String returnURL = getParameter(request, RETURN_URL_PARAMETER);
        response.addHeaderField(LOCATION_HEADER, returnURL);
        response.setStatus(REDIRECT_RESPONSE_CODE);
    }

    /** Get a single parameter out of a request.
     *
     * @param request A request to look up parameters in.
     * @param parameterName The name of the parameter to look up.
     * @return The value of one instance of the parameter in the request.  If
     * more than one instance exists, an arbitrary one is picked.
     * @throws IOFailure if the parameter is not given.
     */
    private String getParameter(Request request, String parameterName) {
        checkParameters(request, parameterName);
        String localeString = request.getParameterMap().get(parameterName)[0];
        return localeString;
    }
}