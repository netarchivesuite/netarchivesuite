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
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.webinterface.Constants"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page"
       basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
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
<h3 class="page_heading"><fmt:message key="harveststatus.seeds.for.harvest.0">
    <fmt:param><%=HTMLUtils.escapeHtmlValues(harvestName)%></fmt:param></fmt:message>
</h3>

<table class="selection_table" cols="6">
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
</p>
<table><tr><td>
	<fmt:message key="harveststatus.seeds.total"/>: <%=domainCount%> 
	<fmt:message key="harveststatus.seeds.domains"/> / <%=seedCount%>
	<fmt:message key="harveststatus.seeds.seeds"/>
</td></tr></table>

  
<%

HTMLUtils.generateFooter(out);

%>
