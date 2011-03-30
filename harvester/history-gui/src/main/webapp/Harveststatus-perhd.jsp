<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark

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

This page shows historical information pertaining to a single harvest, through
all its runs.

Parameters:

harvestname (Constants.HARVEST_PARAM): The name of the harvest that will be
   displayed.

--%>
<?xml version="1.0" encoding="utf-8"?>
<%@ page import="java.util.Date,
                 java.util.List,
                 dk.netarkivet.common.CommonSettings,
                 dk.netarkivet.common.utils.Settings,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.HarvestRunInfo,
                 dk.netarkivet.harvester.datamodel.JobStatus,
                 dk.netarkivet.harvester.datamodel.SparseFullHarvest,
                 dk.netarkivet.harvester.datamodel.SparsePartialHarvest,
                 dk.netarkivet.harvester.webinterface.HarvestStatus, 
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.webinterface.HarvestStatusQuery"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%>
<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page"
       basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    String harvestName = request.getParameter(Constants.HARVEST_PARAM);
    HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
    String heading;
    Date nextDate;
    long hdoid;
    SparsePartialHarvest partialhd = null;
    SparseFullHarvest fullhd = hddao.getSparseFullHarvest(harvestName);
    if (fullhd != null) {
        String harvestLink;
        if (SiteSection.isDeployed(Constants.DEFINITIONS_SITESECTION_DIRNAME)) {
            harvestLink = "<a href=\"" + HTMLUtils.escapeHtmlValues(
                    "/HarvestDefinition/Definitions-edit-snapshot-harvest.jsp?"
                            + Constants.HARVEST_PARAM + "="
                            + HTMLUtils.encode(harvestName))
                    + "\">"
                    + HTMLUtils.escapeHtmlValues(harvestName)
                    + "</a>";
        } else {
            harvestLink = HTMLUtils.escapeHtmlValues(harvestName);
        }
        heading = I18N.getString(response.getLocale(),
                "pagetitle;full.harvest.0.history", harvestLink);
        nextDate = null;
        hdoid = fullhd.getOid();
    } else {
        partialhd = hddao.getSparsePartialHarvest(harvestName);
        if (partialhd == null) {
            HTMLUtils.forwardWithErrorMessage(
                    pageContext, I18N, "errormsg;harvest.0.does.not.exist",
                    harvestName);
            return;
        }
        String harvestLink;
        if (SiteSection.isDeployed(Constants.DEFINITIONS_SITESECTION_DIRNAME)) {
            harvestLink = "<a href=\"" + HTMLUtils.escapeHtmlValues(
                    "/HarvestDefinition/Definitions-edit-selective-harvest.jsp?"
                            + Constants.HARVEST_PARAM + "="
                            + HTMLUtils.encode(harvestName))
                    + "\">"
                    + HTMLUtils.escapeHtmlValues(harvestName)
                    + "</a>";
        } else {
            harvestLink = HTMLUtils.escapeHtmlValues(harvestName);
        }
        heading = I18N.getString(response.getLocale(),
                "pagetitle;partial.harvest.0.history", harvestLink);
        nextDate = partialhd.getNextDate();
        hdoid = partialhd.getOid();
    }
    List<HarvestRunInfo> hrList = hddao.getHarvestRunInfo(hdoid);
    HTMLUtils.generateHeader(pageContext);
%>

<script type="text/javascript" src="navigate.js"></script>


<%
    String startPage=request.getParameter("START_PAGE_INDEX");

    if(startPage == null){
        startPage="1";
    }
    long pageSize = Long.parseLong(Settings.get(
            CommonSettings.HARVEST_STATUS_DFT_PAGE_SIZE));

    String startPagePost=request.getParameter("START_PAGE_INDEX");

    if(startPagePost == null){
        startPagePost="1";
    }

    String searchParam=request.getParameter(Constants.HARVEST_PARAM);
    String searchParamHidden = searchParam.replace(" ","+");  
%>

               
<form method="post" name="filtersForm" action="Harveststatus-perhd.jsp">
<input type="hidden" 
       name="START_PAGE_INDEX"
       value="<%=startPagePost%>"/>
</form>    

<%
    long totalResultsCount = hrList.size();
    long actualPageSize = (pageSize == 0 ?
        totalResultsCount : pageSize);

    long startPageIndex = Long.parseLong(startPage);
    long startIndex = 0;
    long endIndex = 0;
    
    if (totalResultsCount > 0) {
        startIndex = ((startPageIndex - 1) * actualPageSize);
        endIndex = Math.min(startIndex + actualPageSize , totalResultsCount);
    }

    boolean prevLinkActive = false;
    if (pageSize != 0
            && totalResultsCount > 0
            && startIndex > 1) {
        prevLinkActive = true;
        
        
    }
    
    boolean nextLinkActive = false;
    if (pageSize != 0
            && totalResultsCount > 0
            && endIndex < totalResultsCount) {
        nextLinkActive = true;
    }
%>     

              <h3 class="page_heading">
                <fmt:message key="searching.for.0.gave.1.hits">
                    <fmt:param value="<%=searchParam%>"/>
                    <fmt:param value="<%=hrList.size()%>"/>
                </fmt:message>
                </h3>
                
                <fmt:message key="status.results.displayed">
                    <fmt:param><%=totalResultsCount%></fmt:param>
                    <fmt:param><%=startIndex+1%></fmt:param>
                    <fmt:param><%=endIndex%></fmt:param>
                </fmt:message>
     
  
  
      
                <p style="text-align: right">
                    <fmt:message key="status.results.displayed.pagination">
                        <fmt:param>
                        <%
                        if (prevLinkActive) {
                        %>
                            <a href="javascript:previousPage('<%=Constants.HARVEST_PARAM%>','<%=searchParamHidden%>');">
                                <fmt:message key="status.results.displayed.prevPage"/>
                            </a>
                        <%
                        } else {
                        %>
                            <fmt:message key="status.results.displayed.prevPage"/>
                        <%
                        }
                        %>
                        </fmt:param>
                        <fmt:param>
                        <%
                        if (nextLinkActive) {
                        %>
                            <a href="javascript:nextPage('<%=Constants.HARVEST_PARAM%>','<%=searchParamHidden%>');">
                                <fmt:message key="status.results.displayed.nextPage"/>
                            </a>
                        <%
                        } else {
                        %>
                            <fmt:message key="status.results.displayed.nextPage"/>
                        <%
                        }
                        %>
                        </fmt:param>       
                    </fmt:message>
                </p>               
     
     
<h3 class="page_heading"><%=heading%></h3>
<%
    if (partialhd != null) {
        if (nextDate == null) {
%>
            <h4><fmt:message key="harvest.0.will.never.run">
                <fmt:param value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>
            </fmt:message>
            </h4>
<%
        } else {
            if (partialhd.isActive()) {
%>
            <h4><fmt:message key="harvest.0.will.next.run.1">
                <fmt:param value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>
                <fmt:param value="<%=nextDate%>"/>
            </fmt:message>
            </h4>
<%
            } else {
%>
            <h4><fmt:message key="inactive.harvest.0.would.run.next.1">
                <fmt:param value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>
                <fmt:param value="<%=nextDate%>"/>
            </fmt:message>
            </h4>
<%
            }
        }
    }
    if (hrList.size() > 0) {
%>
<table class="selection_table">
    <tr>
        <th>
            <fmt:message key="table.job.harvestnumber"/>
        </th>
        <th>
            <fmt:message key="table.job.starttime"/>
        </th>
        <th>
            <fmt:message key="table.job.stoptime"/>
        </th>
        <th>
            <fmt:message key="table.job.domainconfigurations.bytesharvested"/>
        </th>
        <th>
            <fmt:message
                    key="table.job.domainconfigurations.documentsharvested"/>
        </th>
        <th>
            <fmt:message key="table.job.total.number.of.jobs"/>
        </th>
        <th>
            <fmt:message key="table.job.number.of.failed.jobs"/>
        </th>
        <th>
            <fmt:message key="table.job.number.of.resubmitted.jobs"/>
        </th>
    </tr>
    <%
        int rowcount = 0;
        List<HarvestRunInfo> matchingSubList=hrList.
        subList((int)startIndex,(int)endIndex);

        for (HarvestRunInfo hri : matchingSubList) {
    %>
    <tr class="<%=HTMLUtils.getRowClass(rowcount++)%>">
        <td><%=dk.netarkivet.harvester.webinterface.HarvestStatus
                .makeHarvestRunLink(hri.getHarvestID(), hri.getRunNr())%>
        </td>
        <td>
            <fmt:formatDate type="both" value="<%=hri.getStartDate()%>"/>
        </td>
        <td>
            <fmt:formatDate type="both" value="<%=hri.getEndDate()%>"/>
        </td>
        <td>
            <fmt:formatNumber type="number"
                              value="<%=hri.getBytesHarvested()%>"/>
        </td>
        <td>
            <fmt:formatNumber type="number"
                              value="<%=hri.getDocsHarvested()%>"/>
        </td>
        <td>
            <fmt:formatNumber type="number"
                              value="<%=hri.getJobCount()%>"/>
            <a href="Harveststatus-perharvestrun.jsp?<%=
            Constants.HARVEST_ID_PARAM%>=<%=hri.getHarvestID()%>&amp;<%=
            Constants.HARVEST_NUM_PARAM%>=<%=hri.getRunNr()%>&amp;<%=
            Constants.JOBSTATUS_PARAM%>=<%=HarvestStatusQuery.JOBSTATUS_ALL%>">
                <fmt:message key="show.jobs"/>
            </a>
        </td>
        <td><%=hri.getJobCount(JobStatus.FAILED)%>
        </td>
        <td><%=hri.getJobCount(JobStatus.RESUBMITTED)%>
        </td>
    </tr>
    <%
        }
    %>
</table>
<%
    } else {
        %>
            <fmt:message key="harvestdefinition.0.has.never.run">
                <fmt:param value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>
            </fmt:message>
        <%
    }
    HTMLUtils.generateFooter(out);
%>
