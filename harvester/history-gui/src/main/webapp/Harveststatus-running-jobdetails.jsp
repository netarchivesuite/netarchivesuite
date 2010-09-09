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

<table class="selection_table">

    <tr class="spacerRowBig"><td>&nbsp;</td></tr>
    <tr><th colspan=2">
        <fmt:message key="table.running.jobs.harvestName"/>
        &nbsp;<%= harvestLink %>
    </th></tr>
    <tr>
	    <td colspan=2"><ul>
	       <!-- Link to Heritrix console -->
	       <li>
	       <a href="<%=history[0].getHostUrl()%>" target="_blank">
	           <fmt:message key="running.job.details.heritrixConsoleLink">
	           <fmt:param><%=history[0].getHostName()%></fmt:param>
	           </fmt:message>
	       </a>
	       </li>
	       <!-- Link to job definition page -->
	       <li>
           <a href="Harveststatus-jobdetails.jsp?jobID=<%= jobID %>">
               <fmt:message key="running.job.details.jobDefinitionLink"/>
           </a>
           </li>

	       <% if (frontierReport.getSize() > 0 )  { %>
	       <!-- Link to frontier section within the page -->
	       <li>
           <a href="#frontierReport">
               <fmt:message key="running.job.details.heritrixFrontierLink"/>
           </a>
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
	    <td width="60%">
	       <b><fmt:message key="running.job.details.chartsLabel"/></b>
	    </td>

	    <!-- History table -->
	    <td>
	       <b><fmt:message key="running.job.details.historyTableLabel"/></b>
	    </td>
    </tr>

    <tr class="spacerRowSmall">
        <td colspan="2">&nbsp;</td>
    </tr>

    <tr>
        <!-- Charts -->
        <td>
        <img src="<%= HarvestMonitorServer.getInstance().getChartFilePath(jobID) %>" alt="history_<%=jobID %>"/>
        </td>

        <!-- History table -->
        <td width="55%">
           <table class="selection_table">
               <tr>
                   <th><fmt:message key="running.job.details.table.date"/></th>
                   <th><fmt:message key="table.running.jobs.elapsedTime"/></th>
                   <th><fmt:message key="table.running.jobs.progress"/></th>
                   <th><fmt:message key="table.running.jobs.queuedFiles"/></th>
                   <th><fmt:message key="running.job.details.table.status"/></th>
               </tr>
               <%
                   for (StartedJobInfo sji : history) {
               %>
               <tr>
                   <td><%= StringUtils.formatDate(sji.getTimestamp().getTime(), "yyyy/MM/dd HH:mm") %></td>
                   <td><%= StringUtils.formatDuration(sji.getElapsedSeconds()) %></td>
                   <td><%= StringUtils.formatPercentage(sji.getProgress()) %></td>
                   <td><%= sji.getQueuedFilesCount() %></td>
                   <td>
                        &nbsp;
			            <%
			                String altStatus = "?";
			                String bullet = "?";
			                switch (sji.getStatus()) {
			                    case PRE_CRAWL:
			                        altStatus = "table.running.jobs.status.preCrawl";
			                        bullet = "bluebullet.png";
			                        break;
			                    case CRAWLER_ACTIVE:
			                        altStatus = "table.running.jobs.status.crawlerRunning";
			                        bullet = "greenbullet.png";
			                        break;
			                    case CRAWLER_PAUSING:
			                        altStatus = "table.running.jobs.status.crawlerPausing";
			                        bullet = "yellowbullet.png";
			                        break;
			                    case CRAWLER_PAUSED:
			                        altStatus = "table.running.jobs.status.crawlerPaused";
			                        bullet = "redbullet.png";
			                        break;
			                    case CRAWLING_FINISHED:
			                        altStatus = "table.running.jobs.status.crawlFinished";
			                        bullet = "greybullet.png";
			                        break;

			                }
			            %>
                        <img src="<%=bullet%>" alt="<%=I18N.getString(request.getLocale(), altStatus)%>"/>
                   </td>
               </tr>
               <%
                   }
               %>
           </table>
           <br/><br/>
			&nbsp;
			<fmt:message key="table.running.jobs.legend">
			    <fmt:param>
			    <img src="bluebullet.png" alt="<%=I18N.getString(request.getLocale(), "table.running.jobs.status.preCrawl")%>"/>
			    </fmt:param>
			    <fmt:param>
			    <img src="greenbullet.png" alt="<%=I18N.getString(request.getLocale(), "table.running.jobs.status.crawlerRunning")%>"/>
			    </fmt:param>
			    <fmt:param>
			    <img src="yellowbullet.png" alt="<%=I18N.getString(request.getLocale(), "table.running.jobs.status.crawlerPausing")%>"/>
			    </fmt:param>
			    <fmt:param>
			    <img src="redbullet.png" alt="<%=I18N.getString(request.getLocale(), "table.running.jobs.status.crawlerPaused")%>"/>
			    </fmt:param>
			    <fmt:param>
			    <img src="greybullet.png" alt="<%=I18N.getString(request.getLocale(), "table.running.jobs.status.crawlFinished")%>"/>
			    </fmt:param>
			</fmt:message>
			<br/><br/>
        </td>
    </tr>

    <tr class="spacerRowBig"><td colspan=2">&nbsp;</td></tr>
    <tr class="spacerRowBig"><td colspan=2">&nbsp;</td></tr>
    <tr class="spacerRowBig"><td colspan=2">&nbsp;</td></tr>

<%

    if (frontierReport.getSize() > 0) {

%>

    <tr><th colspan=2">
        <a id="frontierReport" name="frontierReport">
        <fmt:message key="running.job.details.frontier.title.TopTotalEnqueuesFilter">
            <fmt:param>
            <%= StringUtils.formatDate(
                    frontierReport.getTimestamp(), "yyyy/MM/dd HH:mm") %>
            </fmt:param>
        </fmt:message>
        </a>
        &nbsp;
        <form method="post"
              action="./Harveststatus-frontier-csvexport.jsp">
            <input type="hidden"
                id="<%= ExportFrontierReportCsvQuery.UI_FIELD.JOB_ID.name() %>"
                name="<%= ExportFrontierReportCsvQuery.UI_FIELD.JOB_ID.name() %>"
                value ="<%= jobID %>"/>
            <input type="submit" value="<fmt:message key="running.job.details.frontier.exportAsCsv"/>"/>
        </form>
    </th></tr>
    <tr class="spacerRowBig"><td colspan=2">&nbsp;</td></tr>
    <tr>
	    <td colspan=2">
	    <table class="selection_table">
               <tr>
                   <th><fmt:message key="running.job.details.frontier.queueName"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalEnqueues"/></th>
                   <th><fmt:message key="running.job.details.frontier.currentSize"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalSpent"/></th>
                   <th><fmt:message key="running.job.details.frontier.totalBudget"/></th>
                   <th><fmt:message key="running.job.details.frontier.lastPeekUri"/></th>
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
                <td align="right"><fmt:formatNumber type="number" value="<%= l.getTotalBudget() %>"/></td>
                <td><a href="<%= l.getLastPeekUri() %>" target="_blank">
                    <%= StringUtils.makeEllipsis(l.getLastPeekUri(), 25) %></a></td>
                <td><a href="<%= l.getLastQueuedUri() %>" target="_blank">
                <%= StringUtils.makeEllipsis(l.getLastQueuedUri(), 25) %></a></td>
               </tr>
               <%  } %>
         </table>
	    </td>
    </tr>



<% } %>

    <tr class="spacerRowBig"><td colspan=2">&nbsp;</td></tr>

<%

    if (exhaustedQueues.getSize() > 0) {

%>

    <tr><th colspan=2">
        <a id="frontierReportExhausted" name="frontierReportExhausted">
        <fmt:message key="running.job.details.frontier.title.ExhaustedQueuesFilter">
            <fmt:param>
            <%= StringUtils.formatDate(
                    exhaustedQueues.getTimestamp(), "yyyy/MM/dd HH:mm") %>
            </fmt:param>
        </fmt:message>
        </a>
    </th></tr>
    <tr class="spacerRowBig"><td colspan=2">&nbsp;</td></tr>
    <tr>
        <td colspan=2">
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

    <tr class="spacerRowBig"><td colspan=2">&nbsp;</td></tr>

<%

    if (retiredQueues.getSize() > 0) {

%>

    <tr><th colspan=2">
        <a id="frontierReportRetired" name="frontierReportRetired">
        <fmt:message key="running.job.details.frontier.title.RetiredQueuesFilter">
            <fmt:param>
            <%= StringUtils.formatDate(
                    retiredQueues.getTimestamp(), "yyyy/MM/dd HH:mm") %>
            </fmt:param>
        </fmt:message>
        </a>
    </th></tr>
    <tr class="spacerRowBig"><td colspan=2">&nbsp;</td></tr>
    <tr>
        <td colspan=2">
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

    <tr class="spacerRowBig"><td colspan=2">&nbsp;</td></tr>

</table>

<%
 HTMLUtils.generateFooter(out);
%>
