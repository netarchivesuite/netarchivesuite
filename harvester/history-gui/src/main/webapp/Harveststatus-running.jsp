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

<%@ page
	import="
    java.util.Date,
    java.util.List,
    java.util.Set,
    java.util.Map,
    java.text.SimpleDateFormat,
    dk.netarkivet.common.utils.I18n,
    dk.netarkivet.common.webinterface.HTMLUtils,
    dk.netarkivet.common.webinterface.SiteSection,
    dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo,
    dk.netarkivet.harvester.harvesting.monitor.HarvestMonitorServer,
    dk.netarkivet.harvester.webinterface.Constants,
    dk.netarkivet.harvester.webinterface.FindRunningJobQuery"
	pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"/>
<fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/>
<%!private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);%>

<%
    HarvestMonitorServer monitor = HarvestMonitorServer.getInstance();
    
    //list of information to be shown
    Map<String, Set<StartedJobInfo>> infos = monitor.getJobInfosByHarvestName();
    
    FindRunningJobQuery findJobQuery = new FindRunningJobQuery(request);
    Long[] jobIdsForDomain = findJobQuery.getRunningJobIds(); 
    
    HTMLUtils.setUTF8(request);
    HTMLUtils.generateHeader(pageContext, 10); // Autorefresh every 10 seconds
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

<table class="selection_table">
<%
		
		for (String harvestName : infos.keySet()) {
			
			String harvestDetailsLink = "Harveststatus-perhd.jsp?"
		           + Constants.HARVEST_PARAM + "="
		           + HTMLUtils.encode(harvestName);
		
%>

<tr class="spacerRowBig"><td colspan="12">&nbsp;</td></tr>
<tr><th colspan="12">
    <fmt:message key="table.running.jobs.harvestName"/>&nbsp;<a href=<%=harvestDetailsLink%>><%=harvestName %></a>
</th>
</tr> 
<tr class="spacerRowSmall"><td colspan="12">&nbsp;</td></tr>
<tr>
    <th class="harvestHeader" rowspan="2"><fmt:message key="table.running.jobs.jobId"/></th>
    <th class="harvestHeader" rowspan="2"><fmt:message key="table.running.jobs.host"/></th>
    <th class="harvestHeader" rowspan="2"><fmt:message key="table.running.jobs.progress"/></th>
    <th class="harvestHeader" rowspan="2"><fmt:message key="table.running.jobs.elapsedTime"/></th>
    <%-->th class="harvestHeader" colspan="5"><fmt:message key="table.running.jobs.queues"/></th--%>
    <th class="harvestHeader" colspan="4"><fmt:message key="table.running.jobs.queues"/></th>        
    <th class="harvestHeader" colspan="3"><fmt:message key="table.running.jobs.performance"/></th>        
    <th class="harvestHeader" rowspan="2"><fmt:message key="table.running.jobs.alerts"/></th>
</tr>
<tr>
    <th class="harvestHeader"><fmt:message key="table.running.jobs.queuedFiles"/></th>
    <th class="harvestHeader"><fmt:message key="table.running.jobs.totalQueues"/></th>
    <th class="harvestHeader"><fmt:message key="table.running.jobs.activeQueues"/></th>
    <%--th class="harvestHeader"><fmt:message key="table.running.jobs.retiredQueues"/></th--%>
    <th class="harvestHeader"><fmt:message key="table.running.jobs.exhaustedQueues"/></th>
    <th class="harvestHeader"><fmt:message key="table.running.jobs.currentProcessedDocsPerSec"/></th>
    <th class="harvestHeader"><fmt:message key="table.running.jobs.currentProcessedKBPerSec"/></th>
    <th class="harvestHeader"><fmt:message key="table.running.jobs.toeThreads"/></th>
</tr>
<%

   int rowcount = 0;
   for (StartedJobInfo info : infos.get(harvestName)) {
	   long jobId = info.getJobId();
	   
	   String jobDetailsLink = "Harveststatus-jobdetails.jsp?"
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
        <td align="right"><%=info.getProgress()%></td>
        <td align="right"><%=info.getElapsedTime()%></td>
        <td align="right"><%=info.getQueuedFilesCount()%></td>
        <td align="right"><%=info.getTotalQueuesCount()%></td>
        <td align="right"><%=info.getActiveQueuesCount()%></td>
        <%-->td align="right"><%=info.getRetiredQueuesCount()%></td--%>
        <td align="right"><%=info.getExhaustedQueuesCount()%></td>
        <td align="right"><%=info.getCurrentProcessedDocsPerSec() + " (" + info.getProcessedDocsPerSec() + ")"%></td>
        <td align="right"><%=info.getCurrentProcessedKBPerSec() + " (" + info.getProcessedKBPerSec() + ")"%></td>
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
    <img src="redbullet.png" alt="<%=I18N.getString(request.getLocale(), "table.running.jobs.status.crawlerPaused")%>"/>
    </fmt:param>
    <fmt:param>
    <img src="greybullet.png" alt="<%=I18N.getString(request.getLocale(), "table.running.jobs.status.crawlFinished")%>"/>
    </fmt:param>
</fmt:message>
<br/><br/>
<form method="get" name="findJobForDomainForm" action="Harveststatus-running.jsp">

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
<% } %>
<%
	}
%>

<% 
 HTMLUtils.generateFooter(out);
%>
