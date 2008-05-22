<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark

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

--%><%@page import="dk.netarkivet.common.utils.I18n,
                    dk.netarkivet.common.webinterface.HTMLUtils,
                    dk.netarkivet.common.webinterface.SiteSection, dk.netarkivet.viewerproxy.Constants"
            pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    // Get the page title from its URL
    HTMLUtils.generateHeader(pageContext);
%>
<h3 class="page_heading"><fmt:message key="pagetitle;qa.status"/></h3>
<p><fmt:message key="current.viewerproxy.status"/></p>
<iframe src="QA-getStatus.jsp" width="600"></iframe>
<p><fmt:message key="helptext;qa.statusbox.explanation"/></p>

<h2><fmt:message key="collect.missing.urls"/></h2>
<p>
<table><tr>
<td><form action="QA-startRecordingURIs.jsp" method="post"><input type="submit" value="<fmt:message key="start"/>"/></form></td>
<td><form action="QA-stopRecordingURIs.jsp" method="post"><input type="submit" value="<fmt:message key="stop"/>"/></form></td>
<td><form action="QA-clearRecordedURIs.jsp" method="post"><input type="submit" value="<fmt:message key="clear"/>"/></form></td>
<td><form action="QA-getRecordedURIs.jsp" method="post"><input type="submit" value="<fmt:message key="show"/>"/></form></td>
</tr></table></p>

<h2><fmt:message key="browsing.jobs.in.viewerproxy"/></h2>
<%
    if (SiteSection.isDeployed("HarvestDefinition")) {
%>
<p><fmt:message key="use.these.pages.for.viewerproxy.job.selection"/>
    <br/><a href="/HarvestDefinition/Definitions-selective-harvests.jsp"><fmt:message key="selective.harvest.history"/></a>
    <br/><a href="/HarvestDefinition/Definitions-snapshot-harvests.jsp"><fmt:message key="snapshot.harvest.history"/></a>
</p>
<%
    }
    HTMLUtils.generateFooter(out);
%>