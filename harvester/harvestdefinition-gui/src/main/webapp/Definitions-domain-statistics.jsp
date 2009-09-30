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

--%><%-- This page lists statistics of all domains:
 Number of domains registered total plus how many are registered within
 each TLD. --%><%@ page import="
        java.util.List,
        dk.netarkivet.common.utils.I18n,
        dk.netarkivet.common.webinterface.HTMLUtils,
        dk.netarkivet.harvester.Constants,
        dk.netarkivet.harvester.datamodel.DomainDAO,
        dk.netarkivet.harvester.datamodel.TLDInfo"
    pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    // Get the page title from its URL
    HTMLUtils.generateHeader(
            pageContext);
%>

<h3><fmt:message key="pagetitle;domain.statistics"/></h3>

<%
    DomainDAO dao = DomainDAO.getInstance();
    int domainCount = dao.getCountDomains();
    %><h2><fmt:message key="prompt;number.of.registered.domains"/> <%=domainCount%></h2>
    <%
    List<TLDInfo> tldList = dao.getTLDs();
    if (tldList.size() > 0) {
    %>
    <table><tr><th><fmt:message key="top.level.domain"/></th><th><fmt:message key="number.of.subdomains"/></th></tr>
    <%
        int rowCount = 0;
        for (TLDInfo tld : tldList) {
            String rowClass = HTMLUtils.getRowClass(rowCount++);

            String count;
            String domain;
            if (tld.isIP()) {
                domain = HTMLUtils.escapeHtmlValues(I18N.getString(response.getLocale(),
                        "ip.addresses"));
                count = HTMLUtils.localiseLong(tld.getCount(), pageContext);
            } else {
                domain = HTMLUtils.escapeHtmlValues(tld.getName());
                count = "<a href=\"/HarvestDefinition/Definitions-find-domains.jsp?"
                        + dk.netarkivet.harvester.webinterface.Constants.DOMAIN_PARAM
                        + "=*."
                        + HTMLUtils.escapeHtmlValues(HTMLUtils.encode(tld.getName()))
                        + "\">" + HTMLUtils.localiseLong(tld.getCount(), pageContext)
                        + "</a>";
            }
        %>
        <tr class="<%= rowClass %>"><td><%= domain %></td><td><%= count %></td></tr>
        <%
        }
    %></table><%
     }
    HTMLUtils.generateFooter(out);
%>
