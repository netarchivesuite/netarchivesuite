<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark

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
--%>

<%--
This page displays details about a running job.
--%>

<%@ page
    import="
    dk.netarkivet.common.utils.I18n,
    dk.netarkivet.common.exceptions.ForwardedToErrorPage,
    dk.netarkivet.harvester.webinterface.Constants,
    dk.netarkivet.common.webinterface.HTMLUtils,
    dk.netarkivet.common.exceptions.UnknownID,
    dk.netarkivet.harvester.datamodel.JobDAO,
    dk.netarkivet.harvester.datamodel.Job,
    dk.netarkivet.harvester.harvesting.frontier.FrontierReportLine,
    dk.netarkivet.harvester.harvesting.frontier.InMemoryFrontierReport,
    dk.netarkivet.common.utils.StringUtils,
    dk.netarkivet.harvester.harvesting.monitor.HarvestMonitorServer,
    dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo,
    dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
    dk.netarkivet.harvester.webinterface.ExportFrontierReportCsvQuery"
    pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"/>
<fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/>
<%!private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);%>

<%
    long jobID;
    try {
        jobID = HTMLUtils.parseAndCheckInteger(pageContext,
                Constants.JOB_PARAM, 1, Integer.MAX_VALUE);
    } catch (ForwardedToErrorPage e) {
        return;
    }

    HarvestMonitorServer.getInstance().setChartLocale(
            jobID, response.getLocale());

    Job job;
    try {
        job = JobDAO.getInstance().read(jobID);
    } catch (UnknownID e) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;job.unknown.id.0", jobID);
        return;
    }

    String harvestName;
    String harvestUrl= "";

    final Long harvestID = job.getOrigHarvestDefinitionID();
    try {
        harvestName = HarvestDefinitionDAO.getInstance().getHarvestName(
                harvestID);
        if (HarvestDefinitionDAO.getInstance().isSnapshot(harvestID)) {
            harvestUrl +=
                "/HarvestDefinition/Definitions-edit-snapshot-harvest.jsp?"
                        + Constants.HARVEST_PARAM + "="
                        + HTMLUtils.encode(harvestName);
        } else {
            harvestUrl =
                "/HarvestDefinition/Definitions-edit-selective-harvest.jsp?"
                + Constants.HARVEST_PARAM + "="
                + HTMLUtils.encode(harvestName);
        }
    } catch (UnknownID e) {
        // If no harvestdefinition is known with ID=harvestID
        // Set harvestName = an internationalized version of
        // "Unknown harvest" + harvestID
        harvestName = I18N.getString(response.getLocale(),
                "unknown.harvest.0", harvestID);
    }
    String harvestLink = HTMLUtils.escapeHtmlValues(harvestName);
    harvestLink =
        "<a href=\"" + HTMLUtils.escapeHtmlValues(harvestUrl)
        + "\">" + HTMLUtils.escapeHtmlValues(harvestName) + "</a>";

    StartedJobInfo latest =
        HarvestMonitorServer.getMostRecentRunningJobInfo(jobID);

    StartedJobInfo[] history =
        HarvestMonitorServer.getMostRecentRunningJobInfos(jobID);

    if (history.length == 0) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errorMsg;running.job.details.noStartedJobInfo", jobID);
        return;
    }

    HTMLUtils.setUTF8(request);
    HTMLUtils.generateHeader(
            pageContext,
            HarvestMonitorServer.getAutoRefreshDelay()); // Autorefresh every x seconds

    InMemoryFrontierReport frontierReport =
        HarvestMonitorServer.getInstance().getFrontierReport(jobID);

    InMemoryFrontierReport retiredQueues =
        HarvestMonitorServer.getInstance().getFrontierRetiredQueues(jobID);

    InMemoryFrontierReport exhaustedQueues =
        HarvestMonitorServer.getInstance().getFrontierExhaustedQueues(jobID);

%>

<%--Make header of page--%>
<h3 class="page_heading">
<fmt:message key="running.job.details.title">
    <fmt:param><%= jobID %></fmt:param>
</fmt:message>
</h3>

<table>

    <tr class="spacerRowBig"><td>&nbsp;</td></tr>
    <tr><th colspan="2">
        <fmt:message key="table.running.jobs.harvestName"/>
        &nbsp;<%= harvestLink %>
    </th></tr>
    <tr>
	    <td colspan="2"><ul>
	       <!-- Link to Heritrix console -->
	       <li>
	           <fmt:message key="running.job.details.heritrixConsoleLink">
	               <fmt:param>
                       <a href="<%=latest.getHostUrl()%>" target="_blank">
                       <fmt:message key="running.job.details.display"/>
                       </a>
                   </fmt:param>
	               <fmt:param>
	                   <a href="<%=latest.getHostUrl()%>" target="_blank">
	                   <%=latest.getHostName()%>
	                   </a>
	               </fmt:param>
	           </fmt:message>
	       </li>
	       <!-- Link to job definition page -->
	       <li>
	           <fmt:message key="running.job.details.jobDefinitionLink">
	                <fmt:param>
	                    <a href="Harveststatus-jobdetails.jsp?jobID=<%= jobID %>">
	                    <fmt:message key="running.job.details.display"/>
	                    </a>
	                </fmt:param>
	           </fmt:message>
           </li>

	       <% if (frontierReport.getSize() > 0 )  { %>
	       <!-- Link to frontier section within the page -->
	       <li>
           <fmt:message key="running.job.details.heritrixFrontierLinks">

                <fmt:param>
                    <a href="#frontierReport">
                        <fmt:message key="running.job.details.display"/>
                    </a>
                </fmt:param>

                <fmt:param>
                    <a href="./Harveststatus-frontier-csvexport.jsp?<%= ExportFrontierReportCsvQuery.UI_FIELD.JOB_ID.name() %>=<%= jobID %>">
                        <fmt:message key="running.job.details.export"/>
                    </a>
                </fmt:param>

                <fmt:param>
	            <%= StringUtils.formatDate(
	                    frontierReport.getTimestamp(), "yyyy/MM/dd HH:mm") %>
	            </fmt:param>

           </fmt:message>
           </li>
           <% } %>

           <% if (exhaustedQueues.getSize() > 0 )  { %>
           <!-- Link to frontier section within the page -->
           <li>
           <a href="#frontierReportExhausted">
               <fmt:message key="running.job.details.exhaustedQueuesLink"/>
           </a>
           </li>
           <% } %>

           <% if (retiredQueues.getSize() > 0 )  { %>
           <!-- Link to frontier section within the page -->
           <li>
           <a href="#frontierReportRetired">
               <fmt:message key="running.job.details.retiredQueuesLink"/>
           </a>
           </li>
           <% } %>

	    </ul></td>
    </tr>
    <tr class="spacerRowBig"><td>&nbsp;</td></tr>

    <tr>
        <!-- Charts -->
        <td valign="top">
        <img src="<%= HarvestMonitorServer.getInstance().getChartFilePath(jobID) %>" alt="history_<%=jobID %>"/>
        </td>

        <!-- History table -->
        <td width="55%" valign="top">
           <table>
               <tr>
                   <th><fmt:message key="running.job.details.table.date"/></th>
                   <th><fmt:message key="table.running.jobs.elapsedTime"/></th>
                   <th><fmt:message key="table.running.jobs.progress"/></th>
                   <th><fmt:message key="table.running.jobs.queuedFiles"/></th>
               </tr>
               <%
                   for (int i = 0; i < history.length; i++) {
                       StartedJobInfo sji = history[i];
               %>
               <tr class="<%= HTMLUtils.getRowClass(i) %>">
                   <td><%= StringUtils.formatDate(sji.getTimestamp().getTime(), "MM/dd HH:mm") %></td>
                   <td><%= StringUtils.formatDuration(sji.getElapsedSeconds()) %></td>
                   <td align="right"><%= StringUtils.formatPercentage(sji.getProgress()) %></td>
                   <td align="right"><%= sji.getQueuedFilesCount() %></td>
               </tr>
               <%
                   }
               %>
           </table>
        </td>
    </tr>

    <tr class="spacerRowBig"><td colspan="2">&nbsp;</td></tr>
    <tr class="spacerRowBig"><td colspan="2">&nbsp;</td></tr>
    <tr class="spacerRowBig"><td colspan="2">&nbsp;</td></tr>

<%

    if (frontierReport.getSize() > 0) {

%>

    <tr><th colspan="2">
        <a id="frontierReport" name="frontierReport"/> <!-- Anchor  -->
        <fmt:message key="running.job.details.frontier.title.TopTotalEnqueuesFilter">
            <fmt:param>
            <%= StringUtils.formatDate(
                    frontierReport.getTimestamp(), "yyyy/MM/dd HH:mm") %>
            </fmt:param>
        </fmt:message>
    </th></tr>
    <tr class="spacerRowBig"><td colspan="2">&nbsp;</td></tr>
    <tr>
	    <td colspan="2">
	    <table class="selection_table">
               <tr>
                   <th><fmt:message key="running.job.details.frontier.queueName"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalEnqueues"/></th>
                   <th><fmt:message key="running.job.details.frontier.currentSize"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalSpent"/></th>
                   <th><fmt:message key="running.job.details.frontier.lastQueuedUri"/></th>
               </tr>
               <%
                   for (FrontierReportLine l : frontierReport.getLines()) {
               %>
               <tr>
                <td><%= l.getDomainName() %></td>
                <td align="right">
                    <fmt:formatNumber type="number" value="<%= l.getTotalEnqueues() %>"/></td>
                <td align="right">
                    <fmt:formatNumber type="number" value="<%= l.getCurrentSize() %>"/></td>
                <td align="right"><fmt:formatNumber type="number" value="<%= l.getTotalSpend() %>"/></td>
                <td><a href="<%= l.getLastQueuedUri() %>" target="_blank">
                <%= StringUtils.makeEllipsis(l.getLastQueuedUri(), 30) %></a></td>
               </tr>
               <%  } %>
         </table>
	    </td>
    </tr>



<% } %>

    <tr class="spacerRowBig"><td colspan="2">&nbsp;</td></tr>

<%

    if (exhaustedQueues.getSize() > 0) {

%>

    <tr><th colspan="2">
        <a id="frontierReportExhausted" name="frontierReportExhausted">
        <fmt:message key="running.job.details.frontier.title.ExhaustedQueuesFilter">
            <fmt:param>
            <%= StringUtils.formatDate(
                    exhaustedQueues.getTimestamp(), "yyyy/MM/dd HH:mm") %>
            </fmt:param>
        </fmt:message>
        </a>
    </th></tr>
    <tr class="spacerRowBig"><td colspan="2">&nbsp;</td></tr>
    <tr>
        <td colspan="2">
        <table class="selection_table">
               <tr>
                   <th><fmt:message key="running.job.details.frontier.queueName"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalEnqueues"/></th>
                   <th><fmt:message key="running.job.details.frontier.currentSize"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalSpent"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalBudget"/></th>
               </tr>
               <%
                   for (FrontierReportLine l : exhaustedQueues.getLines()) {
               %>
               <tr>
                <td><%= l.getDomainName() %></td>
                <td align="right">
                    <fmt:formatNumber type="number" value="<%= l.getTotalEnqueues() %>"/></td>
                <td align="right">
                    <fmt:formatNumber type="number" value="<%= l.getCurrentSize() %>"/></td>
                <td align="right"><fmt:formatNumber type="number" value="<%= l.getTotalSpend() %>"/></td>
                <td align="right"><fmt:formatNumber type="number" value="<%= l.getTotalBudget() %>"/></td>
               </tr>
               <%  } %>
         </table>
        </td>
    </tr>



<% } %>

    <tr class="spacerRowBig"><td colspan="2">&nbsp;</td></tr>

<%

    if (retiredQueues.getSize() > 0) {

%>

    <tr><th colspan="2">
        <a id="frontierReportRetired" name="frontierReportRetired">
        <fmt:message key="running.job.details.frontier.title.RetiredQueuesFilter">
            <fmt:param>
            <%= StringUtils.formatDate(
                    retiredQueues.getTimestamp(), "yyyy/MM/dd HH:mm") %>
            </fmt:param>
        </fmt:message>
        </a>
    </th></tr>
    <tr class="spacerRowBig"><td colspan="2">&nbsp;</td></tr>
    <tr>
        <td colspan="2">
        <table class="selection_table">
               <tr>
                   <th><fmt:message key="running.job.details.frontier.queueName"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalEnqueues"/></th>
                   <th><fmt:message key="running.job.details.frontier.currentSize"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalSpent"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalBudget"/></th>
               </tr>
               <%
                   for (FrontierReportLine l : retiredQueues.getLines()) {
               %>
               <tr>
                <td><%= l.getDomainName() %></td>
                <td align="right">
                    <fmt:formatNumber type="number" value="<%= l.getTotalEnqueues() %>"/></td>
                <td align="right">
                    <fmt:formatNumber type="number" value="<%= l.getCurrentSize() %>"/></td>
                <td align="right"><fmt:formatNumber type="number" value="<%= l.getTotalSpend() %>"/></td>
                <td align="right"><fmt:formatNumber type="number" value="<%= l.getTotalBudget() %>"/></td>
               </tr>
               <%  } %>
         </table>
        </td>
    </tr>



<% } %>

    <tr class="spacerRowBig"><td colspan="2">&nbsp;</td></tr>

</table>

<%
 HTMLUtils.generateFooter(out);
%>
