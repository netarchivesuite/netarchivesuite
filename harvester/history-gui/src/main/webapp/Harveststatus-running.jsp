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
This page displays a list of running jobs.
--%>

<%@page import="dk.netarkivet.harvester.harvesting.monitor.HarvestMonitorServer"%>
<%@ page
	import="
    java.util.Date,
    java.util.List,
    java.util.Set,
    java.util.Map,
    java.util.TreeMap,
    java.util.Collections,
    java.text.SimpleDateFormat,
    dk.netarkivet.common.utils.I18n,
    dk.netarkivet.common.webinterface.HTMLUtils,
    dk.netarkivet.common.webinterface.SiteSection,
    dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo,
    dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO,
    dk.netarkivet.harvester.webinterface.Constants,
    dk.netarkivet.harvester.webinterface.FindRunningJobQuery,
    dk.netarkivet.common.utils.StringUtils,
    dk.netarkivet.harvester.webinterface.TableSort,
    dk.netarkivet.harvester.webinterface.HarvestStatusRunningTablesSort"
	pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"/>
<fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/>
<%!private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);%>

<%
    //
    HarvestStatusRunningTablesSort tbs=(HarvestStatusRunningTablesSort)
        session.getAttribute("TablesSortData");
    if(tbs==null){
        tbs = new HarvestStatusRunningTablesSort();
        session.setAttribute("TablesSortData",tbs);
    }

    String sortedColumn=request.getParameter(Constants.COLUMN_PARAM);
    String sortedHarvest=request.getParameter(Constants.HARVEST_PARAM);

    if( sortedColumn != null && sortedHarvest != null) {
        tbs.sortByHarvestName(sortedHarvest,Integer.parseInt(sortedColumn)) ;
    }

    //list of information to be shown
    Map<String, List<StartedJobInfo>> infos =
        RunningJobsInfoDAO.getInstance().getMostRecentByHarvestName();

    int jobCount = 0;
    for (List<StartedJobInfo> jobList : infos.values()) {
        jobCount += jobList.size();
    }

    FindRunningJobQuery findJobQuery = new FindRunningJobQuery(request);
    Long[] jobIdsForDomain = findJobQuery.getRunningJobIds();

    HTMLUtils.setUTF8(request);
    HTMLUtils.generateHeader(
            pageContext,
            HarvestMonitorServer.getAutoRefreshDelay()); // Autorefresh every x seconds
%>

<%--Make header of page--%>
<h3 class="page_heading"><fmt:message key="pagetitle;all.jobs.running"/></h3>

<%
	if (infos.size() == 0) {
%>
    <fmt:message key="table.job.no.jobs"/>
<%
	} else { //Make table with found jobs
%>
<fmt:message key="running.jobs.nbrunning">
     <fmt:param value="<%=jobCount%>"/>
</fmt:message>
<table class="selection_table">
<%

        for (String harvestName : infos.keySet()) {

			String harvestDetailsLink = "Harveststatus-perhd.jsp?"
		           + Constants.HARVEST_PARAM + "="
		           + HTMLUtils.encode(harvestName);



			//gestion des fleche de trie
			String incSortPic = "Up Arrow.png";
            String descSortPic = "Down Arrow.png";
            String noSortPic = "No Arrow.png";
            String tabArrow[] = new String[9];
            for( int i=0;i<9;i++) {
                tabArrow[i] =  noSortPic;
            }
            String arrow = noSortPic;
            HarvestStatusRunningTablesSort.ColumnId cid =
                tbs.getSortedColumnIdentByHarvestName(harvestName);
            if(cid != HarvestStatusRunningTablesSort.ColumnId.NONE){

                TableSort.SortOrder order = tbs.getSortOrderByHarvestName(harvestName);
                if( order == TableSort.SortOrder.INCR){
                    arrow = incSortPic;
                }
                if( order == TableSort.SortOrder.DESC){
                    arrow = descSortPic;
                }
                tabArrow[cid.ordinal()] = arrow;
            }

            String sortBaseLink="Harveststatus-running.jsp?"
                    + Constants.HARVEST_PARAM + "="
                    + HTMLUtils.encode(harvestName)
                    + "&"
                    +Constants.COLUMN_PARAM + "=" ;
            String sortLink;
            String columnId;
%>

<tr class="spacerRowBig"><td colspan="12">&nbsp;</td></tr>
<tr><th colspan="12">
    <fmt:message key="table.running.jobs.harvestName"/>&nbsp;<a href="<%=harvestDetailsLink%>"><%=harvestName %></a>
</th>
</tr>
<tr class="spacerRowSmall"><td colspan="12">&nbsp;</td></tr>
<tr>
    <th class="harvestHeader" rowspan="2">
    <% sortLink=sortBaseLink
    + HarvestStatusRunningTablesSort.ColumnId.ID.hashCode(); %>
        <a href="<%=sortLink %>">
            <fmt:message key="table.running.jobs.jobId"/>
            <img src="<%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.ID.ordinal()]%>" />
        </a>
    </th>
    <th class="harvestHeader" rowspan="2">
    <% sortLink=sortBaseLink
    + HarvestStatusRunningTablesSort.ColumnId.HOST.hashCode(); %>
        <a href="<%=sortLink %>">
            <fmt:message key="table.running.jobs.host"/>
            <img src="<%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.HOST.ordinal()]%>" />
        </a>
    </th>
    <th class="harvestHeader" rowspan="2">
    <% sortLink=sortBaseLink
    + HarvestStatusRunningTablesSort.ColumnId.PROGRESS.hashCode(); %>
        <a href="<%=sortLink %>">
            <fmt:message key="table.running.jobs.progress"/>
            <img src="<%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.PROGRESS.ordinal()]%>" />
         </a>
    </th>

    <th class="harvestHeader" rowspan="2">
    <% sortLink=sortBaseLink
    + HarvestStatusRunningTablesSort.ColumnId.ELAPSED.hashCode(); %>
        <a href="<%=sortLink %>">
            <fmt:message key="table.running.jobs.elapsedTime"/>
            <img src="<%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.ELAPSED.ordinal()]%>" />
        </a>
   </th>
    <%-->th class="harvestHeader" colspan="5"><fmt:message key="table.running.jobs.queues"/></th--%>
    <th class="harvestHeader" colspan="4"><fmt:message key="table.running.jobs.queues"/></th>
    <th class="harvestHeader" colspan="3"><fmt:message key="table.running.jobs.performance"/></th>
    <th class="harvestHeader" rowspan="2"><fmt:message key="table.running.jobs.alerts"/></th>
</tr>
<tr>
    <th class="harvestHeader" >
    <% sortLink=sortBaseLink
    + HarvestStatusRunningTablesSort.ColumnId.QFILES.hashCode(); %>
        <a href="<%=sortLink %>">
            <fmt:message key="table.running.jobs.queuedFiles"/>
            <img src="<%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.QFILES.ordinal()]%>" />
        </a>
    </th>
    <th class="harvestHeader" >
    <% sortLink=sortBaseLink
    + HarvestStatusRunningTablesSort.ColumnId.TOTALQ.hashCode(); %>
        <a href="<%=sortLink %>">
            <fmt:message key="table.running.jobs.totalQueues"/>
            <img src="<%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.TOTALQ.ordinal()]%>" />
        </a>
    </th>
    <th class="harvestHeader" >
    <% sortLink=sortBaseLink
    + HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ.hashCode(); %>
        <a href="<%=sortLink %>">
            <fmt:message key="table.running.jobs.activeQueues"/>
            <img src="<%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ.ordinal()]%>" />
        </a>
    </th>
    <%--th class="harvestHeader"><fmt:message key="table.running.jobs.retiredQueues"/></th--%>
    <th class="harvestHeader" >
    <% sortLink=sortBaseLink
    + HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ.hashCode(); %>
        <a href="<%=sortLink %>">
            <fmt:message key="table.running.jobs.exhaustedQueues"/>
            <img src="<%=tabArrow[HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ.ordinal()]%>" />
        </a>
    </th>
    <th class="harvestHeader"><fmt:message key="table.running.jobs.currentProcessedDocsPerSec"/></th>
    <th class="harvestHeader"><fmt:message key="table.running.jobs.currentProcessedKBPerSec"/></th>
    <th class="harvestHeader"><fmt:message key="table.running.jobs.toeThreads"/></th>
</tr>
<%

   int rowcount = 0;

   //recup list
   List<StartedJobInfo> infoList = infos.get(harvestName);

   //trie de la List
   HarvestStatusRunningTablesSort.ColumnId cidSort=
       tbs.getSortedColumnIdentByHarvestName(harvestName);

   if(cidSort != HarvestStatusRunningTablesSort.ColumnId.NONE){

       for (StartedJobInfo info : infoList) {
           if(cidSort == HarvestStatusRunningTablesSort.ColumnId.ID){
               info.chooseCompareCriteria(StartedJobInfo.Criteria.JOBID);
           }
           if(cidSort == HarvestStatusRunningTablesSort.ColumnId.HOST){
               info.chooseCompareCriteria(StartedJobInfo.Criteria.HOST);
           }
           if(cidSort == HarvestStatusRunningTablesSort.ColumnId.ELAPSED){
               info.chooseCompareCriteria(StartedJobInfo.Criteria.ELAPSED);
           }
           if(cidSort == HarvestStatusRunningTablesSort.ColumnId.PROGRESS){
               info.chooseCompareCriteria(StartedJobInfo.Criteria.PROGRESS);
           }
           if(cidSort == HarvestStatusRunningTablesSort.ColumnId.EXHAUSTEDQ){
               info.chooseCompareCriteria(StartedJobInfo.Criteria.EXHAUSTEDQ);
           }
           if(cidSort == HarvestStatusRunningTablesSort.ColumnId.ACTIVEQ){
               info.chooseCompareCriteria(StartedJobInfo.Criteria.ACTIVEQ);
           }
           if(cidSort == HarvestStatusRunningTablesSort.ColumnId.TOTALQ){
               info.chooseCompareCriteria(StartedJobInfo.Criteria.TOTALQ);
           }
           if(cidSort == HarvestStatusRunningTablesSort.ColumnId.QFILES){
               info.chooseCompareCriteria(StartedJobInfo.Criteria.QFILES);
           }
       }

       TableSort.SortOrder order = tbs.getSortOrderByHarvestName(harvestName);

       if( order == TableSort.SortOrder.INCR){
           Collections.sort(infoList);
       }
       if( order == TableSort.SortOrder.DESC){
           Collections.sort(infoList, Collections.reverseOrder());
       }
   }

   for (StartedJobInfo info : infoList) {
	   long jobId = info.getJobId();

	   String jobDetailsLink = "Harveststatus-running-jobdetails.jsp?"
	   + Constants.JOB_PARAM + "=" + jobId;

%>
   <tr class="<%=HTMLUtils.getRowClass(rowcount++)%>">
        <td><a href="<%=jobDetailsLink%>"><%=jobId%></a></td>
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
        <%-->td align="right"><%=info.getRetiredQueuesCount()%></td--%>
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
<form method="get" name="findJobForDomainForm" action="Harveststatus-running.jsp">

<input type="hidden" name="searchDone" value="1"/>

<fmt:message key="running.jobs.finder.inputGroup">

<fmt:param>
<input type="text"
           name="<%=FindRunningJobQuery.UI_FIELD.DOMAIN_NAME.name()%>"
           size="30"
           value=""/>
</fmt:param>
</fmt:message>

<input type="submit" name="search"
       value="<fmt:message key="running.jobs.finder.submit"/>"/>

</form>

<% if (jobIdsForDomain.length > 0) { %>
<br/>
<table class="selection_table_small">
<tr>
    <th><fmt:message key="running.jobs.finder.table.jobId"/></th>
</tr>
<% for (long jobId : jobIdsForDomain) {
    String jobDetailsLink = "Harveststatus-jobdetails.jsp?"
       + Constants.JOB_PARAM + "=" + jobId;
%>
<tr><td><a href="<%=jobDetailsLink%>"><%=jobId%></a></td></tr>
<% } %>
</table>
<% } else {

    //after using the search button "searchDone" !=null
    String searchDone = request.getParameter("searchDone");
    if (searchDone != null) { %>
    	 <fmt:message key="table.job.no.jobs"/>

<% } %>
<% } %>
<% } %>
<%
 HTMLUtils.generateFooter(out);
%>
