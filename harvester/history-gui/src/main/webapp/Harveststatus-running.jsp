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
--%>

<%--
This page displays a list of running jobs.
--%>

<%@ page
        import="
	            dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor,
                java.util.List,
                java.util.Map,
                java.util.Collections,
                dk.netarkivet.common.utils.I18n,
                dk.netarkivet.common.webinterface.HTMLUtils,
                dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo,
                dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO,
                dk.netarkivet.harvester.webinterface.Constants,
                dk.netarkivet.harvester.webinterface.FindRunningJobQuery,
                dk.netarkivet.common.utils.StringUtils,
                dk.netarkivet.common.utils.TableSort,
                dk.netarkivet.harvester.webinterface.HarvestStatusRunningTablesSort,
                dk.netarkivet.heritrix3.monitor.HistoryServlet"
        pageEncoding="UTF-8" %>

<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"/>
<fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/>
<%!private static final I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);%>

<%
    // Sort data for table(?)
    HarvestStatusRunningTablesSort tbs = (HarvestStatusRunningTablesSort)session.getAttribute("TablesSortData");
    if (tbs == null) {
        tbs = new HarvestStatusRunningTablesSort();
        session.setAttribute("TablesSortData", tbs);
    }

    String sortedColumn = request.getParameter(Constants.COLUMN_PARAM);
    String sortedHarvest = request.getParameter(Constants.HARVEST_PARAM);

    if (sortedColumn != null && sortedHarvest != null) {
        tbs.sortByHarvestName(sortedHarvest, Integer.parseInt(sortedColumn));
    }

    // Get list of information to be shown, i.e. most recent record for every job, partitioned by harvest def. name
    Map<String, List<StartedJobInfo>> infos = RunningJobsInfoDAO.getInstance().getMostRecentByHarvestName();

    // Count number of running jobs
    int jobCount = 0;
    for (List<StartedJobInfo> jobList : infos.values()) {
        jobCount += jobList.size();
    }

    // Get domain name that user has searched for (if any, otherwise null)
    String searchedDomainName = request.getParameter(FindRunningJobQuery.UI_FIELD.DOMAIN_NAME.name());
	String searchedDomainValue = "";
    if (searchedDomainName != null){
        searchedDomainValue = searchedDomainName; 
    }
    // Try finding the runningsjobs in the database matching the searchDomainName
    FindRunningJobQuery findJobQuery = new FindRunningJobQuery(request);
    // We don't need to retrieve this, because we use the findJobQuery.found(jobId) method instead
    //Long[] jobIdsForDomain = findJobQuery.getRunningJobIds();
    
    // Find out the filtering method used cachedLogs or database (using jobIdsForDomain)
    String filteringMethod = Settings.get(HarvesterSettings.RUNNINGJOBS_FILTERING_METHOD);
    boolean useCachedLogsFiltering = true;
    if (filteringMethod.equalsIgnoreCase("database")) {
        useCachedLogsFiltering = false; 
    }
    
    HTMLUtils.setUTF8(request);
    HTMLUtils.generateHeader(
            pageContext,
            HarvestMonitor.getAutoRefreshDelay());  // Auto-refresh every x seconds
%>

<%-- Make header of page --%>
<h3 class="page_heading"><fmt:message key="pagetitle;all.jobs.running"/></h3>

<%
	if (infos.size() == 0) {
%>
        <fmt:message key="table.job.no.jobs"/>
<%
	} else {  // Make table with found jobs
%>

<%-- Show number of running jobs --%>
<fmt:message key="running.jobs.nbrunning">
    <fmt:param value="<%=jobCount%>"/>
</fmt:message>


<%-- Input field for search --%>
<form method="get" name="findJobForDomainForm" action="Harveststatus-running.jsp">
    <input type="hidden" name="searchDone" value="1"/>

    <fmt:message key="running.jobs.finder.inputGroup">
        <fmt:param>
            <input type="text"
                   name="<%=FindRunningJobQuery.UI_FIELD.DOMAIN_NAME.name()%>"
                   size="30"
                   value="<%=searchedDomainValue%>"/>
        </fmt:param>
    </fmt:message>

    <input type="submit"
           name="search"
           value="<fmt:message key="running.jobs.finder.submit"/>"/>
</form>

<table class="selection_table">
    <%
        for (String harvestName : infos.keySet()) {
            String harvestDetailsLink = "Harveststatus-perhd.jsp?"
                    + Constants.HARVEST_PARAM + "="
                    + HTMLUtils.encode(harvestName);

            //Handling of which arrow to show
            String incSortPic = "&uarr;"; // html entity for UPWARDS ARROW
            String descSortPic = "&darr;"; // html entity for DOWNWARDS ARROW
            String noSortPic = ""; // NO ARROW
            String tabArrow[] = new String[10];
            for (int i = 0; i < 10; i++) {
                tabArrow[i] = noSortPic;
            }
            String arrow = noSortPic;
            HarvestStatusRunningTablesSort.ColumnId cid = tbs.getSortedColumnIdentByHarvestName(harvestName);
            if (cid != HarvestStatusRunningTablesSort.ColumnId.NONE) {
                TableSort.SortOrder order = tbs.getSortOrderByHarvestName(harvestName);
                if (order == TableSort.SortOrder.INCR) {
                    arrow = incSortPic;
                }
                if (order == TableSort.SortOrder.DESC) {
                    arrow = descSortPic;
                }
                tabArrow[cid.ordinal()] = arrow;
            }

            String sortBaseLink = "Harveststatus-running.jsp?"
                    + Constants.HARVEST_PARAM + "="
                    + HTMLUtils.encode(harvestName)
                    + "&"
                    + Constants.COLUMN_PARAM + "=" ;
            String sortLink;
    %>

    <tr class="spacerRowBig"><td colspan="13">&nbsp;</td></tr>

    <%-- Headline for each harvest definition --%>
    <tr>
        <th colspan="13">
            <fmt:message key="table.running.jobs.harvestName"/>
            &nbsp;
            <a href="<%=harvestDetailsLink%>"><%=harvestName%></a>
        </th>
    </tr>

    <tr class="spacerRowSmall"><td colspan="13">&nbsp;</td></tr>

    <%-- Topmost row of headers for each column of the table --%>
    <tr>
        <th class="harvestHeader" rowspan="2">
            <% sortLink=sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.ID.hashCode(); %>
            <a href="<%=sortLink %>">
                <fmt:message key="table.running.jobs.jobId"/>
                <%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.ID.ordinal()]%>
            </a>
        </th>
        <th class="harvestHeader" rowspan="2">
            <% sortLink=sortBaseLink
                    + HarvestStatusRunningTablesSort.ColumnId.HOST.hashCode(); %>
            <a href="<%=sortLink %>">
                <fmt:message key="table.running.jobs.host"/>
                <%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.HOST.ordinal()]%>
            </a>
        </th>
        <th class="harvestHeader" rowspan="2">
            <% sortLink=sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.PROGRESS.hashCode(); %>
            <a href="<%=sortLink %>">
                <fmt:message key="table.running.jobs.progress"/>
                <%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.PROGRESS.ordinal()]%>
            </a>
        </th>

        <th class="harvestHeader" rowspan="2">
            <% sortLink = sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.ELAPSED.hashCode(); %>
            <a href="<%=sortLink %>">
                <fmt:message key="table.running.jobs.elapsedTime"/>
                <%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.ELAPSED.ordinal()]%>
            </a>
        </th>
        <th class="harvestHeader" colspan="5"><fmt:message key="table.running.jobs.queues"/></th>
        <th class="harvestHeader" colspan="3"><fmt:message key="table.running.jobs.performance"/></th>
        <th class="harvestHeader" rowspan="2"><fmt:message key="table.running.jobs.alerts"/></th>
    </tr>

    <%-- Sub-headers for the top-headers that span multiple columns --%>
    <tr>
        <th class="harvestHeader" >
            <% sortLink = sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.QFILES.hashCode(); %>
            <a href="<%=sortLink %>">
                <fmt:message key="table.running.jobs.queuedFiles"/>
                <%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.QFILES.ordinal()]%>
            </a>
        </th>
        <th class="harvestHeader" >
            <% sortLink = sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.TOTALQ.hashCode(); %>
            <a href="<%=sortLink %>">
                <fmt:message key="table.running.jobs.totalQueues"/>
                <%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.TOTALQ.ordinal()]%>
            </a>
        </th>
        <th class="harvestHeader" >
            <% sortLink = sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ.hashCode(); %>
            <a href="<%=sortLink %>">
                <fmt:message key="table.running.jobs.activeQueues"/>
                <%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ.ordinal()]%>
            </a>
        </th>
        <th class="harvestHeader">
            <% sortLink = sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.RETIREDQ.hashCode(); %>
            <a href="<%=sortLink %>">
                <fmt:message key="table.running.jobs.retiredQueues"/>
                <%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.RETIREDQ.ordinal()]%>
            </a>
        </th>
        <th class="harvestHeader" >
            <% sortLink = sortBaseLink + HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ.hashCode(); %>
            <a href="<%=sortLink %>">
                <fmt:message key="table.running.jobs.exhaustedQueues"/>
                <%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ.ordinal()]%>
            </a>
        </th>
        <th class="harvestHeader"><fmt:message key="table.running.jobs.currentProcessedDocsPerSec"/></th>
        <th class="harvestHeader"><fmt:message key="table.running.jobs.currentProcessedKBPerSec"/></th>
        <th class="harvestHeader"><fmt:message key="table.running.jobs.toeThreads"/></th>
    </tr>

    <%-- Prepare data for rows --%>
    <%
        int rowCount = 0;

        // Get list
        List<StartedJobInfo> infoList = infos.get(harvestName);

        // Sort List
        HarvestStatusRunningTablesSort.ColumnId cidSort = tbs.getSortedColumnIdentByHarvestName(harvestName);

        if (cidSort != HarvestStatusRunningTablesSort.ColumnId.NONE) {
            for (StartedJobInfo info : infoList) {
                if (cidSort == HarvestStatusRunningTablesSort.ColumnId.ID) {
                    info.chooseCompareCriteria(StartedJobInfo.Criteria.JOBID);
                }
                if (cidSort == HarvestStatusRunningTablesSort.ColumnId.HOST) {
                    info.chooseCompareCriteria(StartedJobInfo.Criteria.HOST);
                }
                if (cidSort == HarvestStatusRunningTablesSort.ColumnId.ELAPSED) {
                    info.chooseCompareCriteria(StartedJobInfo.Criteria.ELAPSED);
                }
                if (cidSort == HarvestStatusRunningTablesSort.ColumnId.PROGRESS) {
                    info.chooseCompareCriteria(StartedJobInfo.Criteria.PROGRESS);
                }
                if (cidSort == HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ) {
                    info.chooseCompareCriteria(StartedJobInfo.Criteria.EXHAUSTEDQ);
                }
                if (cidSort == HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ) {
                    info.chooseCompareCriteria(StartedJobInfo.Criteria.ACTIVEQ);
                }
                if (cidSort == HarvestStatusRunningTablesSort.ColumnId.TOTALQ) {
                    info.chooseCompareCriteria(StartedJobInfo.Criteria.TOTALQ);
                }
                if (cidSort == HarvestStatusRunningTablesSort.ColumnId.QFILES) {
                    info.chooseCompareCriteria(StartedJobInfo.Criteria.QFILES);
                }
            }

            TableSort.SortOrder order = tbs.getSortOrderByHarvestName(harvestName);

            if (order == TableSort.SortOrder.INCR) {
                Collections.sort(infoList);
            }
            if (order == TableSort.SortOrder.DESC) {
                Collections.sort(infoList, Collections.reverseOrder());
            }
        }

        // Iterate through the jobs to be listed
        for (StartedJobInfo info : infoList) {
            long jobId = info.getJobId();

            if (searchedDomainName != null && !searchedDomainName.isEmpty()) {
             // Something's been searched for, so let's see if this job should be skipped according to the search...
                if (useCachedLogsFiltering){ 
                	if (HistoryServlet.environment != null
                        	&& !HistoryServlet.environment.jobHarvestsDomain(jobId, searchedDomainName, null)) {
                    	// Current job doesn't harvest searched domain, so don't show it. Continue from the next job.
                    	continue;
                	}
                } else { // Look for jobId in list of jobIds matching the given domainsearch
                    if (!findJobQuery.found(jobId)) {
                        continue;
                    }
                }
            }
    %>

    <%-- Generate the rows of data --%>
    <tr class="<%=HTMLUtils.getRowClass(rowCount++)%>">
        <td><a href="history/job/<%=jobId%>/"><%=jobId%></a></td>
        <td class="crawlerHost">
            &nbsp;
            <%
                String altStatus = "?";
                String bullet = "?";
                switch (info.getStatus()) {
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
            &nbsp;
            <a href="<%=info.getHostUrl()%>" target="_blank"><%=info.getHostName()%></a>
        </td>
        <td align="right"><%=StringUtils.formatPercentage(info.getProgress())%></td>
        <td align="right"><%=info.getElapsedTime()%></td>
        <td align="right"><%=info.getQueuedFilesCount()%></td>
        <td align="right"><%=info.getTotalQueuesCount()%></td>
        <td align="right"><%=info.getActiveQueuesCount()%></td>
        <td align="right"><%=info.getInactiveQueuesCount()%></td>
        <td align="right"><%=info.getExhaustedQueuesCount()%></td>
        <td align="right">
            <%= StringUtils.formatNumber(info.getCurrentProcessedDocsPerSec())
                    + " (" + StringUtils.formatNumber(info.getProcessedDocsPerSec())
                    + ")" %>
        </td>
        <td align="right">
            <%= StringUtils.formatNumber(info.getCurrentProcessedKBPerSec())
                    + " (" + StringUtils.formatNumber(info.getProcessedKBPerSec())
                    + ")" %>
        </td>
        <td align="right"><%=info.getActiveToeCount()%></td>
        <td align="right"><%=info.getAlertsCount()%></td>
    </tr>
    <%
            }
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

<% } %>
<%
 HTMLUtils.generateFooter(out);
%>
