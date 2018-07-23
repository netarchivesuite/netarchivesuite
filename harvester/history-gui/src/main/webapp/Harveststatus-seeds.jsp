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

This page shows a sorted lists of all domains and seeds of a single harvest definition

Parameters:

harvestname (Constants.HARVEST_PARAM): The name of the harvest that will be
   displayed.
   

--%><%@ page import="java.util.Date, java.util.Collection,
                 java.util.List, java.util.Map, java.util.Set,
                 java.util.Iterator,  java.util.HashMap,
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

<%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
	final String scriptName = "Harveststatus-seeds.jsp";
	
    String harvestName = request.getParameter(Constants.HARVEST_PARAM);
    if (harvestName == null) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;missing.parameter.0",
                Constants.HARVEST_PARAM);
        return;
    }
    HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
    // Check that harvestName exists.
    // If not show an errormessage for the user.
    if (!hddao.exists(harvestName)) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "unknown.harvest.0",
                Constants.HARVEST_PARAM);
        return;
    }
    
    // Include navigate.js
    HTMLUtils.generateHeader(pageContext, "navigate.js");

	long domainCount = 0;
	long seedCount = 0;

	List<String> domainList = hddao.getListOfDomainsOfHarvestDefinition(
	        harvestName);

	domainCount = domainList.size();
	Map<String, List<String>> seedsMap = new HashMap<String, List<String>>();


	for (String domainname : domainList) {
    	List<String> seeds = hddao.getListOfSeedsOfDomainOfHarvestDefinition(
        	harvestName, domainname);
    	seedsMap.put(domainname, seeds);    
    	seedCount += seeds.size();
	}
	
    String startPage=request.getParameter(Constants.START_PAGE_PARAMETER);

    if(startPage == null){
        startPage="1";
    }

    long totalResultsCount = domainList.size();
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

String searchParam = request.getParameter(Constants.HARVEST_PARAM);
%>

<p style="text-align: right">
<fmt:message key="status.results.displayed.pagination">
    <fmt:param>
        <%
            if (prevLinkActive) {
                String link =
                        scriptName + "?"
                    	+ Constants.START_PAGE_PARAMETER 
                        + "=" + (startPageIndex - 1)
                        + "&" + Constants.HARVEST_PARAM + "="
                        + HTMLUtils.encode(searchParam);
        %>
        
        <a href="<%= link %>">
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
                String link =
                        scriptName + "?"
                        + Constants.START_PAGE_PARAMETER 
                        + "=" + (startPageIndex + 1)
                        + "&" + Constants.HARVEST_PARAM + "="
                        + HTMLUtils.encode(searchParam);
        %>
        <a href="<%= link %>">
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

<form method="post" name="filtersForm" action="<%=scriptName%>">
<input type="hidden"
       name="<%=Constants.START_PAGE_PARAMETER%>"
       value="<%=startPage%>"/>
</form>

<table class="selection_table" cols="6">

<%
List<String> matchingDomainsSubList = 
	domainList.subList((int)startIndex, (int)endIndex);

for (String domainname : matchingDomainsSubList) {
	List<String> seeds = seedsMap.get(domainname);	
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
