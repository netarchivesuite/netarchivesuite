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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.viewerproxy.CommandResolver;
import dk.netarkivet.viewerproxy.Constants;
import dk.netarkivet.viewerproxy.Controller;

/**
 * Client side communication with http controller server. This class works on a
 * specific response object, simply forwarding it to the given url. Thus an
 * instance of this class is a use-once-object.
 *
 * The class is supposed to be used in JSP pages in order to make sure that the
 * remote URI is requested through the browser, so communication with the
 * HTTPControllerServer is done to the one currently set as viewer proxy.
 *
 */
public class HTTPControllerClient implements Controller {
    /** The response we are working on */
    private HttpServletResponse response;
    /** The JspWriter that the page gave us. */
    private JspWriter out;
    /** The url to return to, for commands using this. */
    private String returnURL;

    private static final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);

    /** Make an HTTP controller client.
     * Commands are sent using redirect on the given http response object.
     * For commands with no output, the page is then forwarded to the response
     * url.
     *
     * @param response The response object to use for redirect.
     * @param out
     * @param returnURL The URL to return to afterwards if no output is given.
     * This must not be null if either startRecordingURIs, stopRecordingURIs,
     * changeIndex or clearRecordedURIs are called.
     */
    public HTTPControllerClient(HttpServletResponse response,
                                JspWriter out, String returnURL) {
        ArgumentNotValid.checkNotNull(response, "HttpServletResponse response");
        ArgumentNotValid.checkNotNull(out, "JspWriter out");

        this.response = response;
        this.out = out;
        this.returnURL = returnURL;
    }

    /** Start recording URIs and return to return URL. */
    public void startRecordingURIs() {
        redirectForSimpleCommand(HTTPControllerServer.START_COMMAND, true);
    }

    /** Stop recording URIs and return to return URL. */
    public void stopRecordingURIs() {
        String command = HTTPControllerServer.STOP_COMMAND;
        redirectForSimpleCommand(command, true);
    }

    /** Clear recorded URIs and return to return URL. */
    public void clearRecordedURIs() {
        redirectForSimpleCommand(HTTPControllerServer.CLEAR_COMMAND, true);
    }

    /** Perform the necessary redirection to execute a simple (parameterless)
     * command.
     *
     * @param command One of the three parameterless commands START_COMMAND,
     * @param useReturnURL Whether to append the returnURL parameter
     */
    private void redirectForSimpleCommand(String command, boolean useReturnURL) {
        try {
            String url = "http://"
                    + CommandResolver.VIEWERPROXY_COMMAND_NAME + command;
            if (useReturnURL) {
                ArgumentNotValid.checkNotNullOrEmpty(returnURL, "String returnURL");
                url += '?' + HTTPControllerServer.RETURN_URL_PARAMETER
                        + '=' + HTMLUtils.encode(returnURL);
            }
            response.sendRedirect(url);
        } catch (IOException e) {
            throw new IOFailure("Unable to redirect to controller server", e);
        }
    }

    /** Write recorded URIs to response.
     * NOTE! This does not respect the Controller! The URIs are *not*
     * returned!
     * @return null in all cases. The URIs are written in response by the
     * forwarded call instead.
     */
    public Set<URI> getRecordedURIs() {
        redirectForSimpleCommand(HTTPControllerServer
                .GET_RECORDED_URIS_COMMAND, false);
        return null;
    }

    /** Change current index to work on these jobs. Then return to returnURL.
     *
     * Since post data cannot be transferred through a regular redirect, we
     * instead build a page that uses javascript to immediately repost the
     * data to the url.
     *
     * @param jobList The list of jobs.
     * @param label An arbitrary label that will be used to indicate this index
     */
    public void changeIndex(Set<Long> jobList, String label) {
        ArgumentNotValid.checkNotNull(jobList, "Set jobList");
        ArgumentNotValid.checkNotNullOrEmpty(label, "label");
        ArgumentNotValid.checkNotNullOrEmpty(returnURL, "String returnURL");
        StringBuffer url
                = new StringBuffer("http://"
                                   + CommandResolver.VIEWERPROXY_COMMAND_NAME
                                   + HTTPControllerServer.CHANGE_INDEX_COMMAND);
        try {
            out.println("<html><head><title>");
            out.println(I18N.getString(response.getLocale(), "redirecting"));
            out.println("</title></head>");
            out.println("<body onload='document.getElementById(\"form\").submit();'>");
            out.println("<form action=\""
                        + HTMLUtils.escapeHtmlValues(url.toString())
                        + "\" method=\"POST\" id=\"form\">");
            out.println("<input type='hidden' name='"
            + HTTPControllerServer.RETURN_URL_PARAMETER
                    + "' value='" + HTMLUtils.escapeHtmlValues(returnURL)
                    + "'/>");
            for (Long jobId : jobList) {
                out.println("<input type='hidden' name='"
                        + HTTPControllerServer.JOB_ID_PARAMETER
                        + "' value='" + jobId + "'/>");
            }
            out.println("<input type='hidden' name='"
                        + HTTPControllerServer.INDEX_LABEL_PARAMETER
                        + "' value='" + HTMLUtils.escapeHtmlValues(label)
                        + "'/>");
            out.println("</form>");
            out.println("<p>");
            out.println(I18N.getString(response.getLocale(),
                                       "generating.index.0.for.jobs.1",
                                       HTMLUtils.escapeHtmlValues(label),
                                       StringUtils.conjoin(", ",jobList )));
            out.println("</body></html>");
        } catch (IOException e) {
            throw new IOFailure("Unable to redirect to controller server", e);
        }
    }

    /**
     * Write the current status of viewerproxy to response.
     * NOTE! This does not respect the Controller API! The URIs are *not*
     * returned!
     * @return null. The status is written in response by the forwarded call
     * instead.
     * @param locale The locale (da, en, ...) that the response should be
     * written using.
     */
    public String getStatus(Locale locale) {
        ArgumentNotValid.checkNotNull(locale, "locale");
        try {
            response.sendRedirect("http://"
                    + CommandResolver.VIEWERPROXY_COMMAND_NAME
                    + HTTPControllerServer.GET_STATUS_COMMAND
                    + "?" + HTTPControllerServer.LOCALE_PARAMETER
                    + "=" + HTMLUtils.encode(locale.toString()));
        } catch (IOException e) {
            throw new IOFailure("Unable to redirect to controller server", e);
        }
        return null;
    }
}
