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

This page shows a sorted lists of all domains and seeds of a single harvest definition

Parameters:

harvestname (Constants.HARVEST_PARAM): The name of the harvest that will be
   displayed.

--%><%@ page import="java.util.Date, java.util.Collection, 
                 java.util.List, java.util.Map, java.util.Set,
                 java.util.Iterator,
                 dk.netarkivet.common.CommonSettings,
                 dk.netarkivet.common.utils.Settings,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.webinterface.Constants"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page"
       basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/>

<script type="text/javascript" src="navigate.js"></script>

<%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    String harvestName = request.getParameter(Constants.HARVEST_PARAM);
    if (harvestName == null) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;missing.parameter.0",
                Constants.HARVEST_PARAM);
        return;
    }
    HTMLUtils.generateHeader(pageContext);
%>
<%
int domainCount = 0;
int seedCount = 0;

HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();

List<String> result = hddao.getListOfDomainsOfHarvestDefinition(harvestName);
domainCount += result.size();

for (String domainname : result) {
    List<String> seeds = hddao.getListOfSeedsOfDomainOfHarvestDefinition(
        harvestName, domainname);
    seedCount += seeds.size();
}
%>
<%
    String startPage=request.getParameter("START_PAGE_INDEX");

    if(startPage == null){
        startPage="1";
    }

    long totalResultsCount = result.size();
    long pageSize = Long.parseLong(Settings.get(
            CommonSettings.HARVEST_STATUS_DFT_PAGE_SIZE));  
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

<h3 class="page_heading"><fmt:message key="harveststatus.seeds.for.harvest.0">
    <fmt:param><%=HTMLUtils.escapeHtmlValues(harvestName)%></fmt:param></fmt:message>
</h3>

<fmt:message key="status.results.displayed">
<fmt:param><%=totalResultsCount%></fmt:param>
<fmt:param><%=startIndex+1%></fmt:param>
<fmt:param><%=endIndex%></fmt:param>
</fmt:message>


<%
String startPagePost=request.getParameter("START_PAGE_INDEX");

if(startPagePost == null){
    startPagePost="1";
}

String searchParam=request.getParameter(Constants.HARVEST_PARAM);
String searchParamHidden = searchParam.replace(" ","+");
searchParamHidden = HTMLUtils.encode(searchParamHidden);
%>

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


<form method="post" name="filtersForm" action="Harveststatus-seeds.jsp">
<input type="hidden" 
       name="START_PAGE_INDEX"
       value="<%=startPagePost%>"/>
</form>


<table class="selection_table" cols="6">

<%
List<String> matchingDomainsSubList=result.
subList((int)startIndex,(int)endIndex);

for (String domainname : matchingDomainsSubList) {
	List<String> seeds = hddao.getListOfSeedsOfDomainOfHarvestDefinition(
		harvestName, domainname);
//	seedCount += seeds.size();
	
	%>
	<tr>
	    <th colspan="2"><%=domainname%> (<%=seeds.size()%>
	    <fmt:message key="harveststatus.seeds.seeds"/>)</th>
	</tr>
	<%
	for (String seed : seeds) { 
	%>
	<tr>
	    <td width="10%">&nbsp;</td>
	    <td><%=seed%></td>
<!-- 
        <td><a href="http://yourwaybackmachine/wayback/*/<%=seed%>" target="_blank"><%=seed%></a></td>
-->        
	</tr>
	<%
	}

}
%>
</table>
<p>
<table><tr><td>
	<fmt:message key="harveststatus.seeds.total"/>: <%=domainCount%> 
	<fmt:message key="harveststatus.seeds.domains"/> / <%=seedCount%>
	<fmt:message key="harveststatus.seeds.seeds"/>
</td></tr></table>

  
<%

HTMLUtils.generateFooter(out);

%>
