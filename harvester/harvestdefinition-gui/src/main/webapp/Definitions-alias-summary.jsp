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
This page shows a list of all existing aliases in the system, sorted by
expiry date.

While we could have a way to renew a multitude of aliases all at once, we
believe they should be checked before renewed.
--%><%@ page import="
        java.util.List,
        dk.netarkivet.common.utils.I18n,
        dk.netarkivet.common.webinterface.HTMLUtils,
        dk.netarkivet.harvester.Constants,
        dk.netarkivet.harvester.datamodel.AliasInfo,
        dk.netarkivet.harvester.datamodel.DomainDAO,
        dk.netarkivet.harvester.webinterface.DomainDefinition"
    pageEncoding="UTF-8"
%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    HTMLUtils.generateHeader(
            pageContext);
%>

<h3><fmt:message key="pagetitle;existing.aliases.overview"/></h3>

<%
    List<AliasInfo> aliases = DomainDAO.getInstance().getAllAliases();

    if (aliases.size() > 0) {
        int rowCount = 0;
        // Only show expired table if any aliases have expired.
        if (aliases.get(0).isExpired()) {
            // write header for expired aliases
            %><h2><fmt:message key="prompt;expired.aliases"/></h2>
            <table><tr><th><fmt:message key="domain"/></th>
                <th><fmt:message key="prompt;alias.of"/></th>
                <th><fmt:message key="expired"/></th></tr>
            <%
                for (AliasInfo alias : aliases) {
                    if (!alias.isExpired()) {
                        break;
                    }
                    %>
                <tr class="<%= HTMLUtils.getRowClass(rowCount++) %>">
                    <td><%=DomainDefinition.makeDomainLink(alias.getDomain())%></td>
                    <td><%=DomainDefinition.makeDomainLink(alias.getAliasOf())%></td>
                    <td><fmt:formatDate type="both" value="<%= alias.getExpirationDate() %>"/></td>
                    </tr>
                <%
                }
            %></table><%
        }
        // Only show non-expired table if any domains are non-expired.
        if (!aliases.get(aliases.size() - 1).isExpired()) {
            // Write header for unexpired aliases
            %><h2><fmt:message key="prompt;existing.aliases"/></h2>
              <table><tr><th><fmt:message key="domain"/></th>
                  <th><fmt:message key="prompt;alias.of"/></th><th><fmt:message key="expires"/></th></tr>
            <%
                rowCount = 0;
                for (AliasInfo alias : aliases) {
                    if (alias.isExpired()) {
                        continue; // Skip expired aliases for this table
                    }
            %>
        <tr class="<%= HTMLUtils.getRowClass(rowCount++) %>">
            <td><%=DomainDefinition.makeDomainLink(alias.getDomain())%></td>
            <td><%=DomainDefinition.makeDomainLink(alias.getAliasOf())%></td>
            <td><fmt:formatDate type="both" value="<%= alias.getExpirationDate() %>"/></td>
            </tr>
        <%
                }
            %></table><%
        }
    } else {
        %><fmt:message key="no.aliases.defined"/><%
    }
    HTMLUtils.generateFooter(out);
%>
