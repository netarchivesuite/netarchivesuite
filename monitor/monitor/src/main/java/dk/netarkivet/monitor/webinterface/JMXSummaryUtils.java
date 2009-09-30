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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
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
 * and a bunch of JMX properties used by Monitor-JMXsummary.jsp. 
 */
public class JMXSummaryUtils {
    /** JMX property for the physical location.*/
    public static final String JMXPhysLocationProperty = 
        dk.netarkivet.common.management.Constants.PRIORITY_KEY_LOCATION;
    /** JMX property for the machine name.*/
    public static final String JMXMachineNameProperty = 
        dk.netarkivet.common.management.Constants.PRIORITY_KEY_MACHINE;
    /** JMX property for the application name.*/
    public static final String JMXApplicationNameProperty = 
        dk.netarkivet.common.management.Constants.PRIORITY_KEY_APPLICATIONNAME;
    /** JMX property for the application instance id.*/
    public static final String JMXApplicationInstIdProperty = 
        dk.netarkivet.common.management.Constants
            .PRIORITY_KEY_APPLICATIONINSTANCEID;
    /** JMX property for the HTTP port. */
    public static final String JMXHttpportProperty = 
        dk.netarkivet.common.management.Constants.PRIORITY_KEY_HTTP_PORT;
    /** JMX property for the harvest priority*/
    public static final String JMXHarvestPriorityProperty = 
        dk.netarkivet.common.management.Constants.PRIORITY_KEY_PRIORITY;
    /** JMX property for the replica name.*/
    public static final String JMXArchiveReplicaNameProperty = 
        dk.netarkivet.common.management.Constants.PRIORITY_KEY_REPLICANAME;
    /** JMX property for the index. */
    public static final String JMXIndexProperty = 
        dk.netarkivet.common.management.Constants.PRIORITY_KEY_INDEX;
    
    /** JMX properties, which can set to star. */   
    public static final String[] STARRABLE_PARAMETERS = new String[]{
        JMXPhysLocationProperty,
        JMXMachineNameProperty, 
        JMXApplicationNameProperty,
        JMXApplicationInstIdProperty,
        JMXHttpportProperty,
        JMXHarvestPriorityProperty,
        JMXArchiveReplicaNameProperty,
        JMXIndexProperty};

    /** The log MBean name prefix.*/
    private static final String LOGGING_MBEAN_NAME_PREFIX =
            "dk.netarkivet.common.logging:";

    /** Internationalisation object. */
    private static final I18n I18N
            = new I18n(dk.netarkivet.monitor.Constants.TRANSLATIONS_BUNDLE);
    
    /** The character for show all.*/
    private static final String CHARACTER_SHOW_ALL = "*";
    /** The character for don't show column.*/
    private static final String CHARACTER_NOT_COLUMN = "-";
    /** The character for only seeing the first row of the log.*/
    private static final String CHARACTER_FIRST_ROW = "0";
    /** Status/Monitor-JMXsummary.jsp. */
    private static final String STATUS_MONITOR_JMXSUMMARY = 
        "Status/Monitor-JMXsummary.jsp";
    

    /** Instance of class Random used to generate a unique id for each div. */
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
    
    /**
     * Creates the show links for showing columns again.
     * 
     * Goes through all parameters to check if their column is active.
     * If a column is not active, the link to showing a specific column again
     * is generated.
     * 
     * @param starredRequest A request to take parameters from.
     * @param l For retrieving the correct words form the current language.
     * @return The link to show the parameter again.
     */
    public static String generateShowColumn(StarredRequest starredRequest, 
            Locale l) {
        ArgumentNotValid.checkNotNull(starredRequest, "starredRequest");
        
        StringBuilder res = new StringBuilder();
        
        for(String parameter : STARRABLE_PARAMETERS) {
            if(CHARACTER_NOT_COLUMN.equals(starredRequest
                    .getParameter(parameter))) {
                // generate the link, but use the parameter applied to the 
                // table field value.
                res.append(generateLink(starredRequest, parameter, 
                        CHARACTER_SHOW_ALL, I18N.getString(l, 
                        "tablefield;" + parameter)));
                res.append(",");
            }
        }
        
        // If any content, then remove last ',' and put 'show' in front
        if(res.length() > 0) {
            res.deleteCharAt(res.length() - 1);
            res.insert(0, I18N.getString(l, "show") + ": ");
        }
        
        return res.toString();
    }

    /** Generate HTML to show at the top of the table, containing a "show all"
     * link if the parameter is currently restricted.
     * This function is only used by JMXIndexProperty field, 
     * the other properties uses generateShowLing instead.
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
        if (CHARACTER_SHOW_ALL.equals(starredRequest.getParameter(parameter))) {
            return "";
        } else {
            return "("
                    + generateLink(starredRequest, parameter, 
                            CHARACTER_SHOW_ALL, 
                            I18N.getString(l, "showall"))
                    + ")";
        }
    }
    
    /** Generate HTML to show at the top of the table, containing a "show all"
     * and a "off" links if the parameter is currently restricted.
     *
     * @param starredRequest A request to take parameters from.
     * @param parameter The parameter that, if not already unrestricted, should
     * be unrestricted in the "show all".
     * @param l the current locale
     * @return HTML to insert at the top of the JMX monitor table.
     */
    public static String generateShowLink(StarredRequest starredRequest,
                                             String parameter, Locale l) {
        ArgumentNotValid.checkNotNull(starredRequest, "starredRequest");
        ArgumentNotValid.checkNotNull(parameter, "parameter");
        if (CHARACTER_SHOW_ALL.equals(starredRequest.getParameter(parameter))) {
            return "("
                   + generateLink(starredRequest, parameter, 
                           CHARACTER_NOT_COLUMN, 
                           I18N.getString(l, "hide"))
                   + ")";
        } else {
            return "("
                   + generateLink(starredRequest, parameter, 
                           CHARACTER_SHOW_ALL,
                     I18N.getString(l, "showall"))
                   + ", "
                   + generateLink(starredRequest, parameter,
                           CHARACTER_NOT_COLUMN, 
                           I18N.getString(l, "hide"))
                   + ")";
        }
    }
    
    /**
     * Tests if a parameter in the request is "-" (thus off). 
     * 
     * @param starredRequest A request to take parameters from.
     * @param parameter The parameter that should be tested.
     * @return Whether the parameter is set to "-".
     */
    public static boolean showColumn(StarredRequest starredRequest, 
            String parameter) {
        ArgumentNotValid.checkNotNull(starredRequest, 
                "StarredRequest starredRequest");
        ArgumentNotValid.checkNotNullOrEmpty(parameter, "String parameter");
        if (CHARACTER_NOT_COLUMN.equals(starredRequest
                .getParameter(parameter))) {
            return false;
        }
        return true;
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
        builder.append("<a href=\"/" + STATUS_MONITOR_JMXSUMMARY + "?");
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
                      "errormsg;error.in.querying.jmx.with.query.0", query);
                throw new ForwardedToErrorPage(
                        "Error in querying JMX with query '" + query + "'.", e);
            } else {
                HTMLUtils.forwardWithErrorMessage(context, I18N, e,
                                         "errormsg;error.in.building.jmxquery");
                throw new ForwardedToErrorPage("Error building JMX query", e);
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
            if (!CHARACTER_SHOW_ALL.equals(starredRequest
                    .getParameter(queryPart)) && !CHARACTER_NOT_COLUMN.equals(
                    starredRequest.getParameter(queryPart))) {
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
        logMessage = HTMLUtils.escapeHtmlValues(logMessage);
        // All strings starting with "http:" or "https:" are replaced with 
        // a proper HTML Anchor 
        logMessage = logMessage.replaceAll("(https?://[^ \\t\\n\"]*)",
                "<a href=\"$1\">$1</a>");
        BufferedReader sr = new BufferedReader(new StringReader(logMessage));
        msg.append("<pre>");
        int lineno = 0;
        String line;
        try {
            while (lineno < 5 && (line = sr.readLine()) != null) {
                msg.append(line);
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
         * parameter is unset, return the following.
         * "index" = "0".
         * "applicationInstanceId" = "-".
         * "location" = "-".
         * "http-port" = "-".
         * Default = "*".
         * 
         * @param paramName The parameter.
         * @return The parameter or "*", "0" or "-"; never null.
         */
        public String getParameter(String paramName) {
            ArgumentNotValid.checkNotNull(paramName, "paramName");
            String value = req.getParameter(paramName);
            if (value == null || value.length() == 0) {
                if (JMXIndexProperty.equals(paramName)) {
                    return CHARACTER_FIRST_ROW;
                } else if (JMXPhysLocationProperty.equals(paramName)) {
                    return CHARACTER_NOT_COLUMN;
                } else if (JMXApplicationInstIdProperty.equals(paramName)) {
                    return CHARACTER_NOT_COLUMN;
                } else if (JMXHttpportProperty.equals(paramName)) {
                    return CHARACTER_NOT_COLUMN;
                } else {
                    return CHARACTER_SHOW_ALL;
                }
            } else {
                return value;
            }
        }
    }
}
