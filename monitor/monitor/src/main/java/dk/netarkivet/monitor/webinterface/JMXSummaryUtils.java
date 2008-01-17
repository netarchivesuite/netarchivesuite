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

package dk.netarkivet.monitor.webinterface;

import javax.management.MalformedObjectNameException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;

/**
 * Various utility methods and classes for the JMX Monitor page.
 */
public class JMXSummaryUtils {
    
    /** JMX properties used by Monitor-JMXsummary.jsp */
    public static final String JMXLocationProperty = "location";
    public static final String JMXHostnameProperty = "hostname";
    public static final String JMXHttpportProperty = "httpport";
    public static final String JMXApplicationnameProperty = "applicationname";
    public static final String JMXIndexProperty = "index";
    /** JMX properties, which can set to star. */   
    public static final String[] STARRABLE_PARAMETERS = new String[]{
        JMXLocationProperty,
        JMXHostnameProperty, 
        JMXHttpportProperty,
        JMXApplicationnameProperty,
        JMXIndexProperty};
    
    private static final String LOGGING_MBEAN_NAME_PREFIX =
            "dk.netarkivet.common.logging:";

    /** Internationalisation object. */
    private static final I18n I18N
            = new I18n(dk.netarkivet.monitor.Constants.TRANSLATIONS_BUNDLE);

    /* Instance of class Random used to generate a unique id for each div. */
    private static final Random random = new Random();

    /** Reduce the class name of an application to the essentials.
     *
     * @param applicationName The class name of the application
     * @return A reduced name suitable for user output.
     */
    public static String reduceApplicationName(String applicationName) {
        ArgumentNotValid.checkNotNull(applicationName, "applicationName");
        String[] split = applicationName.split("\\.");
        return split[split.length - 1];
    }

    /** Reduce a hostname to a more readable form.
     *
     * @param hostname A host name.
     * @return The same host name with all domain parts stripped off.
     */
    public static String reduceHostname(String hostname) {
        ArgumentNotValid.checkNotNull(hostname, "hostName");
        String[] split = hostname.split("\\.", 2);
        return split[0];
    }

    /** Generate HTML to show at the top of the table, containing a "show all"
     * link if the parameter is currently restricted.
     *
     * @param starredRequest A request to take parameters from.
     * @param parameter The parameter that, if not already unrestricted, should
     * be unrestricted in the "show all" link.
     * @param l the current locale
     * @return HTML to insert at the top of the JMX monitor table.
     */
    public static String generateShowAllLink(StarredRequest starredRequest,
                                             String parameter, Locale l) {
        ArgumentNotValid.checkNotNull(starredRequest, "starredRequest");
        ArgumentNotValid.checkNotNull(parameter, "parameter");
        if ("*".equals(starredRequest.getParameter(parameter))) {
            return "";
        } else {
            return "("
                    + generateLink(starredRequest, parameter, "*", 
                            I18N.getString(l, "showall"))
                    + ")";
        }
    }

    /** Generate an HTML link to the JMX summary page with one part of the
     * URL parameters set to a specific value.
     *
     * @param request A request to draw other parameter values from
     * @param setPart Which of the parameters to set.
     * @param setValue The value to set that parameter to.
     * @param linkText The HTML text that should go inside the link.  Remember
     * to escape HTML values if inserting a normal string.
     * @return A link to insert in the page, or an unlinked text, if setPart or
     * setValue is null, or an empty string if linkText is null.
     * @throws ArgumentNotValid if request is null
     */
    public static String generateLink(StarredRequest request, String setPart,
                                      String setValue, String linkText) {
        ArgumentNotValid.checkNotNull(request, "request");
        if (linkText == null) {
            return "";
        }
        if (setPart == null || setValue == null) {
            return linkText;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<a href=\"/Status/Monitor-JMXsummary.jsp?");
        boolean isFirst = true;
        for (String queryPart : STARRABLE_PARAMETERS) {
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append("&amp;");
            }
            builder.append(queryPart);
            builder.append("=");
            if (queryPart.equals(setPart)) {
                builder.append(HTMLUtils.encode(setValue));
            } else {
                builder.append(HTMLUtils.encode(
                        request.getParameter(queryPart)));
            }
        }
        builder.append("\">");
        builder.append(linkText);
        builder.append("</a>");
        return builder.toString();
    }

    /** Get status entries from JMX based on a request and some parameters.
     *
     * @param parameters The parameters to query JMX for.
     * @param request A request possibly containing values for some of the
     * parameters.
     * @param context the current JSP context
     * @return Status entries for the MBeans that match the parameters.
     * 
     * @throws ArgumentNotValid if the query is invalid (typically caused by
     * invalid parameters).
     * @throws ForwardedToErrorPage if unable to create JMX-query
     */
    public static List<StatusEntry> queryJMXFromRequest(
            String[] parameters,
            StarredRequest request,
            PageContext context) {
        ArgumentNotValid.checkNotNull(parameters, "parameters");
        ArgumentNotValid.checkNotNull(request, "request");
        ArgumentNotValid.checkNotNull(context, "context");
        String query = null;
        try {
            query = createJMXQuery(parameters, request);
            return JMXStatusEntry.queryJMX(query);
        } catch (MalformedObjectNameException e) {
            if (query != null) {
                HTMLUtils.forwardWithErrorMessage(context, I18N, e, 
                        "errmsg;error.in.querying.jmx.with.query.0", 
                query);
                throw new ForwardedToErrorPage(
                        "Error in querying JMX with query '" + query + "'.", e);
            } else {
                HTMLUtils.forwardWithErrorMessage(context, I18N, e, 
                        "errmsg;error.in.building.jmxquery");
                throw new ForwardedToErrorPage("Error building JMX query", e );
            }
        }
    }

    /** Build a JMX query string (ObjectName) from a request and a list
     * of parameters to query for.  This string is always a property
     * pattern (wildcarded), even if all the values we define in the names
     * are specified.
     * @param parameters The parameters to query for.  These should make
     * up the parts of the unique identification of an MBean.
     * @param starredRequest A request containing current values for the given
     * parameters.
     * @return A query, wildcarded for those parameters that are
     * * or missing in starredRequest.
     */
    private static String createJMXQuery(String[] parameters,
                                         StarredRequest starredRequest) {
        StringBuilder query = new StringBuilder(LOGGING_MBEAN_NAME_PREFIX + "*");
        for (String queryPart : parameters) {
            if (!"*".equals(starredRequest.getParameter(queryPart))) {
                query.append(",");
                query.append(queryPart);
                query.append("=");
                String parameter = starredRequest.getParameter(queryPart);
                query.append(parameter);
            }
        }

        return query.toString();
    }

    /** Make an HTML fragment that shows a log message preformatted.
     * If the log message is longer than three lines, the rest are hidden
     * and replaced with an internationalized link "More..." that will show the rest.
     * @param logMessage The log message to present
     * @param l the current Locale
     * @return An HTML fragment as defined above.
     */
    public static String generateMessage(String logMessage, Locale l) {
        StringBuilder msg = new StringBuilder();
        BufferedReader sr = new BufferedReader(new StringReader(logMessage));
        msg.append("<pre>");
        int lineno = 0;
        String line;
        try {
            while (lineno < 5 && (line = sr.readLine()) != null) {
                msg.append(HTMLUtils.escapeHtmlValues(line));
                msg.append('\n');
                ++lineno;
            }
            msg.append("</pre>");
            if ((line = sr.readLine()) != null) {
                //We use a random number for generating a unique id for the div.
                int id = random.nextInt();
                msg.append("<a id=\"show");
                msg.append(id);
                msg.append("\" href=\"#\""
                           + " onclick=\"document.getElementById('log");
                msg.append(id);
                msg.append("').style.display='block';"
                           + "document.getElementById('show");
                msg.append(id);
                
                msg.append("').style.display='none';\">");
                msg.append(I18N.getString(l, "more.dot.dot.dot"));
                msg.append("</a>");

                msg.append("<div id=\"log");
                msg.append(id);
                msg.append("\" style=\"display:none\">");
                msg.append("<pre>");
                do  {
                    msg.append(HTMLUtils.escapeHtmlValues(line));
                    msg.append('\n');
                } while ((line = sr.readLine()) != null);
                msg.append("</pre>");
                msg.append("</div>");
            }
        } catch (IOException e) {
            //This should never happen, but if it does, just return the string
            //without all the fancy stuff.
            return "<pre>" + HTMLUtils.escapeHtmlValues(logMessage) + "</pre>";
        }
        return msg.toString();
    }

    /** This class encapsulates a HttpServletRequest, making non-existing
     * parameters appear as "*" for wildcard (or "0" for the index parameter).
     */
    public static class StarredRequest {
        HttpServletRequest req;

        public StarredRequest(HttpServletRequest req) {
            ArgumentNotValid.checkNotNull(req, "HttpServletRequest req");
            this.req = req;
        }

        /** Gets a parameter from the original request, except if the
         * parameter is unset, return "*" (except the "index" parameter
         * returns "0").
         * @param paramName a parameter
         * @return The parameter or "*" or "0"; never null.
         */
        public String getParameter(String paramName) {
            ArgumentNotValid.checkNotNull(paramName, "paramName");
            String value = req.getParameter(paramName);
            if (value == null || value.length() == 0) {
                if (JMXIndexProperty.equals(paramName)) {
                    return "0";
                } else {
                    return "*";
                }
            } else {
                return value;
            }
        }
    }

}
