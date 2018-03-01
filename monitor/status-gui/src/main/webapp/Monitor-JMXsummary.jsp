<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2018 The Royal Danish Library,
the National Library of France and the Austrian
National Library.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
--%><%--
This page shows the status of all applications that were known to exist
when the GUI-application was started. That information is currently taken from
the 'deploy' element in the setting.xml assigned to the
dk.netarkivet.common.webinterface.GUIApplication.
But the actual reading is done in auxiliary class
dk.netarkivet.monitor.jmx.HostForwarding

If the application is down, this can be seen on this page. Furthermore,
the last 100 significant (log-level INFO and above) log-messages
for each application can be browsed here.

Warning: Any applications added to the system after starting the GUI-application
will not appear here.
--%>
<%@ page import="java.util.List,
                 java.util.Locale,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.utils.DomainUtils,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.monitor.Constants,
                 dk.netarkivet.monitor.webinterface.JMXSummaryUtils,
                 dk.netarkivet.monitor.webinterface.StatusEntry,
                 java.net.URLEncoder,
                 java.net.URLDecoder,
                 org.slf4j.LoggerFactory,
                 org.slf4j.Logger"
         pageEncoding="UTF-8"
        %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
        %>
<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
        /><fmt:setBundle scope="page"
                         basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    // Reload settings if changed
    HTMLUtils.generateHeader(pageContext);
    Locale currentLocale = response.getLocale();

    // remove application if parameter remove is set.
    String remove = request.getParameter(Constants.REMOVE);
    if(remove != null) {
        JMXSummaryUtils.StarredRequest starredRequest =
                new JMXSummaryUtils.StarredRequest(request);
        try {
            JMXSummaryUtils.unregisterJMXInstance(
                    JMXSummaryUtils.STARRABLE_PARAMETERS, starredRequest,
                    pageContext);
            StringBuilder builder = new StringBuilder("/");
            builder.append(JMXSummaryUtils.STATUS_MONITOR_JMXSUMMARY);
            builder.append("?");
            /**
             * oldquery is set in the link to remove an application from the summary view. It is used to
             * enable us to return to the previous view after removing an application.
             */
            String oldquery = starredRequest.getParameter("oldquery");
            if (oldquery != null) {
                builder.append(java.net.URLDecoder.decode(oldquery));
            }
            response.sendRedirect(builder.toString());
        } catch (ForwardedToErrorPage e) {
            return;
        }

    }
%>
<h3 class="page_heading">
    <fmt:message key="pagetitle;monitor.summary"/>
</h3>
<%
    JMXSummaryUtils.StarredRequest starredRequest =
            new JMXSummaryUtils.StarredRequest(request);
    List<StatusEntry> result;
    try {
        result = JMXSummaryUtils.queryJMXFromRequest(
                JMXSummaryUtils.STARRABLE_PARAMETERS, starredRequest,
                pageContext);
    } catch (ForwardedToErrorPage e) {
        return;
    }
%><%= JMXSummaryUtils.generateShowColumn(starredRequest, currentLocale) %>
<table id="system_state_table">
    <tr>
        <%
            if (JMXSummaryUtils.showColumn(starredRequest,
                                           JMXSummaryUtils.JMXPhysLocationProperty)) {
        %>
        <th><fmt:message
                key="tablefield;location"/> <%=JMXSummaryUtils.generateShowLink(
                starredRequest,
                JMXSummaryUtils.JMXPhysLocationProperty, currentLocale)%>
        </th>
        <%
            }
            if (JMXSummaryUtils.showColumn(starredRequest,
                                           JMXSummaryUtils.JMXMachineNameProperty)) {
        %>
        <th><fmt:message
                key="tablefield;machine"/> <%=JMXSummaryUtils.generateShowLink(
                starredRequest,
                JMXSummaryUtils.JMXMachineNameProperty, currentLocale)%>
        </th>
        <%
            }
            if (JMXSummaryUtils.showColumn(starredRequest,
                                           JMXSummaryUtils.JMXApplicationNameProperty)) {
        %>
        <th><fmt:message
                key="tablefield;applicationname"/> <%=JMXSummaryUtils.generateShowLink(
                starredRequest,
                JMXSummaryUtils.JMXApplicationNameProperty, currentLocale)%>
        </th>
        <%
            }
            if (JMXSummaryUtils.showColumn(starredRequest,
                                           JMXSummaryUtils.JMXApplicationInstIdProperty)) {
        %>
        <th><fmt:message
                key="tablefield;applicationinstanceid"/> <%=JMXSummaryUtils.generateShowLink(
                starredRequest,
                JMXSummaryUtils.JMXApplicationInstIdProperty, currentLocale)%>
        </th>
        <%
            }
            if (JMXSummaryUtils.showColumn(starredRequest,
                                           JMXSummaryUtils.JMXHttpportProperty)) {
        %>
        <th><fmt:message
                key="tablefield;httpport"/> <%=JMXSummaryUtils.generateShowLink(
                starredRequest,
                JMXSummaryUtils.JMXHttpportProperty, currentLocale)%>
        </th>
        <%
        	}
                    if (JMXSummaryUtils.showColumn(starredRequest,
                                                   JMXSummaryUtils.JMXHarvestChannelProperty)) {
        %>
        <th><fmt:message
                key="tablefield;channel"/> <%=JMXSummaryUtils.generateShowLink(
                starredRequest,
                JMXSummaryUtils.JMXHarvestChannelProperty, currentLocale)%>
        </th>
        <%
        	}
                    if (JMXSummaryUtils.showColumn(starredRequest,
                                                   JMXSummaryUtils.JMXArchiveReplicaNameProperty)) {
        %>
        <th><fmt:message
                key="tablefield;replicaname"/> <%=JMXSummaryUtils.generateShowLink(
                starredRequest,
                JMXSummaryUtils.JMXArchiveReplicaNameProperty, currentLocale)%>
        </th>
        <%
        	}
        %>
        <th><fmt:message
                key="tablefield;index"/> <%=JMXSummaryUtils.generateShowAllLink(
                starredRequest,
                JMXSummaryUtils.JMXIndexProperty, currentLocale)%>
        </th>
        <th><fmt:message key="tablefield;logmessage"/></th>
        <%
        	if (JMXSummaryUtils.showColumn(starredRequest,
                                                   JMXSummaryUtils.JMXRemoveApplication)) {
        %>
        <th><fmt:message
                key="tablefield;removeapplication"/> <%=JMXSummaryUtils.generateShowLink(
                starredRequest,
                JMXSummaryUtils.JMXRemoveApplication, currentLocale)%>
        </th>
        <%
        	}
        %>
    </tr>
    <%
    	for (StatusEntry entry : result) {
                if (entry.getLogMessage(response.getLocale()).trim().length() > 0) {
    %>
    <tr>
        <%
        	if (JMXSummaryUtils.showColumn(starredRequest,
                                               JMXSummaryUtils.JMXPhysLocationProperty)) {
        %>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                                            JMXSummaryUtils.JMXPhysLocationProperty,
                                            entry.getPhysicalLocation(),
                                            HTMLUtils.escapeHtmlValues(
                                                    entry.getPhysicalLocation()))%>
        </td>
        <%
        	}
                    if (JMXSummaryUtils.showColumn(starredRequest,
                                                   JMXSummaryUtils.JMXMachineNameProperty)) {
        %>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                                            JMXSummaryUtils.JMXMachineNameProperty,
                                            entry.getMachineName(),
                                            HTMLUtils.escapeHtmlValues
                                                    (DomainUtils.reduceHostname(
                                                            entry.getMachineName())))%>
        </td>
        <%
        	}
                    if (JMXSummaryUtils.showColumn(starredRequest,
                                                   JMXSummaryUtils.JMXApplicationNameProperty)) {
        %>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                                            JMXSummaryUtils.JMXApplicationNameProperty,
                                            entry.getApplicationName(),
                                            HTMLUtils.escapeHtmlValues
                                                    (JMXSummaryUtils.reduceApplicationName(
                                                            entry.getApplicationName())))%>
        </td>
        <%
        	}
                    if (JMXSummaryUtils.showColumn(starredRequest,
                                                   JMXSummaryUtils.JMXApplicationInstIdProperty)) {
        %>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                                            JMXSummaryUtils.JMXApplicationInstIdProperty,
                                            entry.getApplicationInstanceID(),
                                            HTMLUtils.escapeHtmlValues(
                                                    entry.getApplicationInstanceID()))%>
        </td>
        <%
        	}
                    if (JMXSummaryUtils.showColumn(starredRequest,
                                                   JMXSummaryUtils.JMXHttpportProperty)) {
        %>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                                            JMXSummaryUtils.JMXHttpportProperty,
                                            entry.getHTTPPort(),
                                            HTMLUtils.escapeHtmlValues(
                                                    entry.getHTTPPort()))%>
        </td>
        <%
        	}
                    if (JMXSummaryUtils.showColumn(starredRequest,
                                                   JMXSummaryUtils.JMXHarvestChannelProperty)) {
        %>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                                            JMXSummaryUtils.JMXHarvestChannelProperty,
                                            entry.getHarvestPriority(),
                                            HTMLUtils.escapeHtmlValues(
                                                    entry.getHarvestPriority()))%>
        </td>
        <%
            }
            if (JMXSummaryUtils.showColumn(starredRequest,
                                           JMXSummaryUtils.JMXArchiveReplicaNameProperty)) {
        %>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                                            JMXSummaryUtils.JMXArchiveReplicaNameProperty,
                                            entry.getArchiveReplicaName(),
                                            HTMLUtils.escapeHtmlValues(
                                                    entry.getArchiveReplicaName()))%>
        </td>
        <%
            }
        %>
        <td><%=JMXSummaryUtils.generateLink(starredRequest,
                                            JMXSummaryUtils.JMXIndexProperty,
                                            entry.getIndex(),
                                            HTMLUtils.escapeHtmlValues(
                                                    entry.getIndex()))%>
        </td>
        <td><%=JMXSummaryUtils.generateMessage(entry.getLogMessage(response
                .getLocale()), currentLocale)%>
        </td>
        <%
            if (JMXSummaryUtils.showColumn(starredRequest,
                                           JMXSummaryUtils.JMXRemoveApplication)) {
        %>
        <td><%="<form>"
               + "<input onClick=\"parent.location='/"+ JMXSummaryUtils.STATUS_MONITOR_JMXSUMMARY + "?"
               + Constants.REMOVE + "=" + Constants.REMOVE
               + "&machine=" + HTMLUtils.escapeHtmlValues(entry.getMachineName())
               + "&httpport=" + HTMLUtils.escapeHtmlValues(entry.getHTTPPort())
               + "&applicationinstanceid=" + HTMLUtils.escapeHtmlValues(entry.getApplicationInstanceID())
               + "&applicationname=" + HTMLUtils.escapeHtmlValues(entry.getApplicationName())
               + "&oldquery=" + java.net.URLEncoder.encode("" + request.getQueryString())
               + "'\" type=\"button\" value=\""%><fmt:message key="tablefield;removeapplication"/><%="\" />"
               + "</form>"
            %>
        </td>
        <%
            }
        %>
    </tr>
    <%
            }
        }
    %>
</table>
<%
    HTMLUtils.generateFooter(out);
%>
