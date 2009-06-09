<%--

This page shows a sorted lists of all domains and seeds of a single harvest definition

Parameters:

harvestname (Constants.HARVEST_PARAM): The name of the harvest that will be
   displayed.

--%><%@ page import="java.util.Date,
                 java.util.List,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.HarvestRunInfo,
                 dk.netarkivet.harvester.datamodel.JobStatus,
                 dk.netarkivet.harvester.datamodel.SparseFullHarvest,
                 dk.netarkivet.harvester.datamodel.SparsePartialHarvest, dk.netarkivet.harvester.webinterface.Constants"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%>

<%@page import="java.util.Map"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Collection"%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page"
       basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%

    HTMLUtils.setUTF8(request);
    String harvestName = request.getParameter(Constants.HARVEST_PARAM);
    String heading = "Domain/Seeds for " + HTMLUtils.escapeHtmlValues(harvestName);
    HTMLUtils.generateHeader(pageContext);
    
    
%>
<h3 class="page_heading"><%=heading%></h3>

<table class="selection_table" cols="6">
<%
int domainCount = 0;
int seedCount = 0;

HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();

List<String> result = hddao.getListOfDomainsOfHarvestDefinition(harvestName);
domainCount+=result.size();

for (String domainname : result) {
	List<String> seeds = hddao.getListOfSeedsOfDomainOfHarvestDefinition(harvestName, domainname);
	seedCount+=seeds.size();
	
	%>
	<tr>
	    <th colspan="2"><%=domainname%> (<%=seeds.size()%> <fmt:message key="harveststatus.seeds.seeds"/>)</th>
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
<fmt:message key="harveststatus.seeds.total"/>: <%=domainCount%> <fmt:message key="harveststatus.seeds.domains"/> / <%=seedCount%> <fmt:message key="harveststatus.seeds.seeds"/>
</td></tr></table>

  
<%

HTMLUtils.generateFooter(out);

%>
