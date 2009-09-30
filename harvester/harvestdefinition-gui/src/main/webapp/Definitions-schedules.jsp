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

--%><%--
This is a summary page displaying all known schedules. It takes no
parameters.
--%><%@ page import="dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.Constants,
                 dk.netarkivet.harvester.datamodel.Schedule,
                 dk.netarkivet.harvester.datamodel.ScheduleDAO"
    pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    HTMLUtils.generateHeader(
            pageContext);
%>

<h3 class="page_heading"><fmt:message key="pagetitle;schedules"/></h3>

<%
Iterable<Schedule> isch = ScheduleDAO.getInstance();
if (!isch.iterator().hasNext()) { %>
   <fmt:message key="harvestdefinition.schedules.no.schedules"/>
   <p>
<% } else { %>

<table class="selection_table" cols="2">
<th colspan="3"><fmt:message key="prompt;harvestdefinition.schedules.existing"/></th>

<%
    int rowcount = 0;
    for (Schedule sch : isch) {
%>
        <tr class="<%= HTMLUtils.getRowClass(rowcount++) %>">
            <td><%= HTMLUtils.escapeHtmlValues(sch.getName()) %></td>
            <td width="15%">
                <a href="Definitions-edit-schedule.jsp?<%=
                dk.netarkivet.harvester.webinterface.Constants.DOMAIN_PARAM%>=<%=
                 HTMLUtils.escapeHtmlValues(HTMLUtils.encode(sch.getName())) %>">
                <fmt:message key="edit"/></a></td>
        </tr>
    <%
    } //for each schedule
    %>
</table>
<%
} //else (if no schedule)
%>
<a href="Definitions-edit-schedule.jsp">
     <fmt:message key="harvestdefinition.schedule.create"/>
</a>
<% 
HTMLUtils.generateFooter(out);
%>