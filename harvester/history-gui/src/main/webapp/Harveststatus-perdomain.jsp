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

--%><%-- This page handles showing harvest status and history for domains.
With no parameters, it gives an input box for searching that feeds back into
itself.
With parameter domainName, it performs a search.  Name can be a glob pattern
(using ? and * only) or a single domain.  If domains are found, they are
displayed, if no domains are found a message is shown.
--%><%@ page import="javax.servlet.RequestDispatcher,
                 java.util.List,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.DomainDAO,
                 dk.netarkivet.harvester.datamodel.DomainHarvestInfo,
                 dk.netarkivet.harvester.webinterface.Constants"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"
/><%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    String domainName = request.getParameter(Constants.DOMAIN_SEARCH_PARAM);
    if (domainName != null && domainName.length() > 0) {
        //Domain name parameter given
        domainName = domainName.trim();
        if (domainName.contains("?") || domainName.contains("*")) {
            //Wildcard search
            List<String> matchingDomains = DomainDAO.getInstance().getDomains(
                    domainName);
            if (matchingDomains.isEmpty()) {//No matching domains
                //Wildcard search with no matches
                HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                        "errormsg;no.matching.domains.for.0", domainName);
                return;
            } else {
                //Wildcard search with matches, display them
                HTMLUtils.generateHeader(pageContext);
                %><h3 class="page_heading">
                <fmt:message key="searching.for.0.gave.1.hits">
                    <fmt:param value="<%=domainName%>"/>
                    <fmt:param value="<%=matchingDomains.size()%>"/>
                </fmt:message>
                </h3>
                <% for (String domainS : matchingDomains) {
                    String encodedDomain = HTMLUtils.encode(domainS);
                    %>
                    <a href="Harveststatus-perdomain.jsp?<%=
                      Constants.DOMAIN_SEARCH_PARAM%>=<%=
                      HTMLUtils.escapeHtmlValues(encodedDomain)%>"><%=
                      HTMLUtils.escapeHtmlValues(domainS)%>
                    </a><br/>
<%
                }
            }
        } else if (DomainDAO.getInstance().exists(domainName)) {
            //Specified and found domain name
            HTMLUtils.generateHeader(pageContext);
            %><h3 class="page_heading">
            <fmt:message key="harvest.history.for.0">
                <fmt:param><a href="/HarvestDefinition/Definitions-edit-domain.jsp?<%=
                    Constants.DOMAIN_PARAM%>=<%=HTMLUtils.encode(domainName)%>"><%=
                    HTMLUtils.escapeHtmlValues(domainName)%></a></fmt:param>
                </fmt:message>
            </h3>
            <% List<DomainHarvestInfo> hiList
                    = DomainDAO.getInstance().getDomainHarvestInfo(domainName);
            if (hiList == null || hiList.size() == 0) {// No history
            %><p><fmt:message key="domain.0.was.never.harvested">
                  <fmt:param value="<%=domainName%>"/>
            </fmt:message></p><%
            } else {//print history
%>
            <table class="selection_table">
            <tr>
                <th><fmt:message key="table.job.harvestname"/></th>
                <th><fmt:message key="table.job.harvestnumber"/></th>
                <th><fmt:message key="table.job.jobid"/></th>
                <th><fmt:message key="table.job.domainconfigurations.configuration"/></th>
                <th><fmt:message key="table.job.starttime"/></th>
                <th><fmt:message key="table.job.stoptime"/></th>
                <th><fmt:message key="table.job.domainconfigurations.bytesharvested"/></th>
                <th><fmt:message key="table.job.domainconfigurations.documentsharvested"/></th>
                <th><fmt:message key="table.job.domainconfigurations.stopreason"/></th>
            </tr>
<%
                int rowCount = 0;
                for (DomainHarvestInfo hi : hiList) {
                    String harvestLink = "Harveststatus-perhd.jsp?"
                            + Constants.HARVEST_PARAM + "="
                            + HTMLUtils.encode(hi.getHarvestName());
                    String jobLink = "Harveststatus-jobdetails.jsp?jobID="
                            + hi.getJobID();
                    %>
                    <tr class="<%=HTMLUtils.getRowClass(rowCount++)%>">
                    	<!-- td for the harvestname -->
                        <td><a href="<%=HTMLUtils.escapeHtmlValues(harvestLink)%>">
                            <%=HTMLUtils.escapeHtmlValues(hi.getHarvestName())%>
                        </a></td>
                        <!-- td for the harvestnumber -->
                        <td><%=dk.netarkivet.harvester.webinterface
                                .HarvestStatus
                                .makeHarvestRunLink(hi.getHarvestID(),
                                                    hi.getHarvestNum())%>
                        </td>
                        <!-- td for the Jobid -->
                        <td><%
                            if (hi.getJobID() == 0) {
                                %>-<%
                            } else {
                                %><a href="<%=
                                    HTMLUtils.escapeHtmlValues(jobLink)%>">
                                    <%=hi.getJobID()%>
                                </a><%
                            }
                        %>
                        </td>
                        <td><%=HTMLUtils.escapeHtmlValues(hi.getConfigName())%></td>
                        <td><fmt:formatDate type="both" value="<%=hi.getStartDate()%>"/></td>
                        <td><fmt:formatDate type="both" value="<%=hi.getEndDate()%>"/></td>
                        <td><fmt:formatNumber value="<%=hi.getBytesDownloaded()%>"/></td>
                        <td><fmt:formatNumber value="<%=hi.getDocsDownloaded()%>"/></td>
                        <td><%
                            if (hi.getStopReason() == null) {
                                %>-<%
                            } else {
                               %><%=hi.getStopReason().getLocalizedString(response.getLocale())%><%
                            }
                        %>
                        </td>
                    </tr>
                    <%
                }
            }
            %>
            </table>
            <%
        } else {
            //Specified domain name, but not found
            String message = I18N.getString(response.getLocale(),
                                            "errormsg;unknown.domain.0",
                                            domainName);
            request.setAttribute("message", message);
            RequestDispatcher rd = pageContext.getServletContext()
                    .getRequestDispatcher("/message.jsp");
            rd.forward(request, response);
            return;
        }
    } else {
        //No search or domain name given, so show the search formular to the user
        HTMLUtils.generateHeader(pageContext);
        %>
        <h3 class="page_heading">
            <fmt:message key="find.all.jobs.for.this.domain"/>
        </h3>
        <form method="get" action="Harveststatus-perdomain.jsp">
        <table>
            <tr>
                <td><fmt:message key="prompt;enter.name.of.domain"/></td>
                <td><span id="focusElement">
                    <input name="<%=Constants.DOMAIN_SEARCH_PARAM%>" 
                        size="<%=Constants.DOMAIN_NAME_FIELD_SIZE %>"/>
                    </span>
                </td>
                <td><input type="submit" value="<fmt:message key="search"/>"/></td>
            </tr>
            <tr>
                <td colspan="2"><fmt:message key="may.use.wildcards"/></td>
            </tr>
        </table>
        </form>
<%
    }
    HTMLUtils.generateFooter(out);
%>