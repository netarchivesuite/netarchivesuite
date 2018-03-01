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

--%><%-- This page handles searching for domains.  With no parameters, it
gives an input box for searching that feeds back into itself.
With parameter DOMAIN_QUERY_TYPE and DOMAIN_QUERY_STRING set, it performs a
search.  
The DOMAIN_QUERY_STRING can be a glob pattern
(using ? and * only) or a single domain.  If domains are found, they are
displayed, otherwise the message is given that there are no domains matching the query.

The search-system are now able to search in different fields of the 'domain' table
 - name (the only search-option before), 
 - comments, 
 - crawlertraps
 On the todo list are aliases, seeds. domainconfigurations, 

--%><%@page import="java.util.List,
				dk.netarkivet.common.CommonSettings,
				dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.utils.Settings, dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.webinterface.DomainDefinition,
                 dk.netarkivet.harvester.webinterface.DomainSearchType"
         pageEncoding="UTF-8"
%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/>

<%! private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
	
	String searchType = request.getParameter(Constants.DOMAIN_QUERY_TYPE_PARAM);
	String searchQuery =  request.getParameter(Constants.DOMAIN_QUERY_STRING_PARAM);
    
	if (searchType == null) { // The default is NAME searchS
	    searchType = Constants.NAME_DOMAIN_SEARCH;
	}
	
    if (searchQuery != null && searchQuery.trim().length() > 0) {
        // search field is not empty
        searchQuery = searchQuery.trim();
        List<String> matchingDomains = DomainDefinition.getDomains(
                pageContext, I18N, searchQuery, searchType);

            if (matchingDomains.isEmpty()) { // No matching domains
                HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                        "errormsg;no.matching.domains.for.query.0.when.searching.by.1", 
                        searchQuery, searchType);
                return;
            } else {
                // Include navigate.js 
                HTMLUtils.generateHeader(pageContext, "navigateWithTwoParams.js");
                %><h3 class="page_heading">
                <fmt:message key="searching.for.0.gave.1.hits">
                    <fmt:param value="<%=searchQuery%>"/>
                    <fmt:param value="<%=matchingDomains.size()%>"/>
                </fmt:message>
                </h3>

                <%
                 String startPage=request.getParameter(Constants.START_PAGE_PARAMETER);

                 if(startPage == null){
                     startPage="1";
                 }	                

                 long totalResultsCount = matchingDomains.size();
                 long pageSize = Long.parseLong(Settings.get(
                         CommonSettings.HARVEST_STATUS_DFT_PAGE_SIZE));
                 long actualPageSize = (pageSize == 0 ?
                         totalResultsCount : pageSize);

                 long startPageIndex = Long.parseLong(startPage);
                 long startIndex = 0;
                 long endIndex = 0;

                 if (totalResultsCount > 0) {
                     startIndex = ((startPageIndex - 1) * actualPageSize);
                     endIndex = Math.min(startIndex + actualPageSize ,
                             totalResultsCount);
                 }
                 boolean prevLinkActive = false;
                 if (pageSize != 0 && totalResultsCount > 0 && startIndex > 1) {
                     prevLinkActive = true;
                 }

                 boolean nextLinkActive = false;
                 if (pageSize != 0 && totalResultsCount > 0
                         && endIndex < totalResultsCount) {
                     nextLinkActive = true;
                 }
                 %>

                <fmt:message key="status.results.displayed">
                <fmt:param><%=totalResultsCount%></fmt:param>
                <fmt:param><%=startIndex+1%></fmt:param>
                <fmt:param><%=endIndex%></fmt:param>
                </fmt:message>
                <%
                String startPagePost=request.getParameter(Constants.START_PAGE_PARAMETER);

                if(startPagePost == null){
                    startPagePost="1";
                }

                String searchqueryParam=request.getParameter(Constants.DOMAIN_QUERY_STRING_PARAM);
                searchqueryParam = HTMLUtils.encode(searchqueryParam);
                %>

                <p style="text-align: right">
                    <fmt:message key="status.results.displayed.pagination">
                        <fmt:param>
                        <%
                        if (prevLinkActive) {
                        %>
                            <a href="javascript:previousPage('<%=Constants.DOMAIN_QUERY_STRING_PARAM%>','<%=searchqueryParam%>','<%=Constants.DOMAIN_QUERY_TYPE_PARAM%>','<%=searchType%>');">
                                <fmt:message
                                key="status.results.displayed.prevPage"/>
                            </a>
                        <%
                        } else {
                        %>
                            <fmt:message
                            key="status.results.displayed.prevPage"/>
                        <%
                        }
                        %>
                        </fmt:param>
                        <fmt:param>
                        <%
                        if (nextLinkActive) {
                        %>
                            <a href="javascript:nextPage('<%=Constants.DOMAIN_QUERY_STRING_PARAM%>','<%=searchqueryParam%>', '<%=Constants.DOMAIN_QUERY_TYPE_PARAM%>','<%=searchType%>');">
                                <fmt:message
                                key="status.results.displayed.nextPage"/>
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

                <form method="post" name="filtersForm" action="Definitions-find-domains.jsp">
                    <input type="hidden" name="START_PAGE_INDEX" value="<%=startPagePost%>"/>
                </form>


<%
  List<String> matchingDomainsSubList = matchingDomains.
          subList((int)startIndex,(int)endIndex);
  int rowCount = 0;
  if (matchingDomainsSubList.size() > 0) {
  %>
<table class="selection_table">
<tr>
  <th><fmt:message key="domain"/></th>
  <th><fmt:message key="harvestdefinition.linktext.historical"/></th>
</tr>

<%
                for (String domainS : matchingDomainsSubList) {
                    String encodedDomain = HTMLUtils.encode(domainS);
                    %>
<tr class="<%=HTMLUtils.getRowClass(rowCount++)%>">
  <td>
                   <a href="Definitions-edit-domain.jsp?<%=
                      Constants.DOMAIN_PARAM%>=<%=
                      HTMLUtils.escapeHtmlValues(encodedDomain)%>"><%=
                      HTMLUtils.escapeHtmlValues(domainS)%>
                    </a>

</td>
<%
  if (SiteSection.isDeployed("History")) {
    String historyLink = "/History/Harveststatus-perdomain.jsp?domainName="
            + HTMLUtils.encode(domainS);
%>
<td><a href="<%=HTMLUtils.escapeHtmlValues(historyLink)%>">
  <fmt:message key="harvestdefinition.linktext.historical"/></a>
</td>
<% } %>
</tr>

<%
                    }
%>
</table>
<%

                }
                HTMLUtils.generateFooter(out);
                return;
            }
                 
    }
      
  
    //Note: This point is only reached if no name was sent to the JSP-page
    HTMLUtils.generateHeader(pageContext);
%>
<h3 class="page_heading"><fmt:message key="pagetitle;find.domains"/></h3>


<form method="post" onclick="resetPagination();"
                                      action="Definitions-find-domains.jsp">
    <table>
        <tr>
            <td><fmt:message key="prompt;enter.domain.query"/></td>
            <td><span id="focusElement">
                <input name="<%=Constants.DOMAIN_QUERY_STRING_PARAM%>"
                  size="<%=Constants.DOMAIN_NAME_FIELD_SIZE %>" value=""/>
                </span>
            </td>
            <td><fmt:message key="may.use.wildcards"/></td>
        </tr>
        <tr>
            <!--  add selector for what kind of search to make -->
            <td><fmt:message key="search.domains.by"/></td>
            <td><select name="<%=Constants.DOMAIN_QUERY_TYPE_PARAM%>">
                    <%
                      for(DomainSearchType aSearchType: DomainSearchType.values()) {
                            String selected = "";

                            if (aSearchType.equals(DomainSearchType.NAME)) {
                                selected = "selected = \"selected\"";
                            }
                            %> <option value="<%=HTMLUtils.escapeHtmlValues(aSearchType.name())%>"<%=selected%>>
                                <fmt:message key="<%=HTMLUtils.escapeHtmlValues(aSearchType.getLocalizedKey())%>"/>
                               </option>
                            <%
                      }
                    %>
                </select>
                </td>
                <td>&nbsp;</td>
        </tr>
        <tr>
            <td colspan="3"><input type="submit" value="<fmt:message key="search"/>"/></td>
        </tr>
    </table>
</form>
<%
    HTMLUtils.generateFooter(out);
%>