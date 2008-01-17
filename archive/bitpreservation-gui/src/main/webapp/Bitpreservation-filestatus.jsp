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

--%><%--
This page provides information about the state of the bitarchive at every known Location.
This page is the entrypoint to correct missing or corrupt data in the bitarchives.
There are no parameters.
--%><%@ page import="dk.netarkivet.archive.arcrepository.bitpreservation.WorkFiles,
         dk.netarkivet.archive.webinterface.BitpreserveFileStatus,
         dk.netarkivet.archive.webinterface.Constants,
         dk.netarkivet.common.distribute.arcrepository.Location,
         dk.netarkivet.common.utils.I18n,
         dk.netarkivet.common.webinterface.HTMLUtils"
    pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE);

%><%
    HTMLUtils.setUTF8(request);
    BitpreserveFileStatus.processUpdateRequest(request, pageContext);
    HTMLUtils.generateHeader(pageContext);

%>
<h3 class="page_heading"><fmt:message key="pagetitle;filestatus"/></h3>

<h4><fmt:message key="bitarchive.state"/></h4>

<%
    //
    // For each known bitarchive in the system, print out statistics about missing files
    //
    for (Location location : Location.getKnown()) {
        String locationParam = Constants.BITARCHIVE_NAME_PARAM
                + "=" + HTMLUtils.encodeAndEscapeHTML(location.getName());
%>
    	<fmt:message key="filestatus.for"/>&nbsp;<b><%=HTMLUtils.escapeHtmlValues(location.getName())%></b>
   		<br>
    	<fmt:message key="number.of.files"/>&nbsp;<%=BitpreserveFileStatus.getBACountFiles(location)%>
    	<br>
    	<fmt:message key="missing.files"/>&nbsp;<%=BitpreserveFileStatus.getBACountMissingFiles(location)%>
    	<% if (BitpreserveFileStatus.getBACountMissingFiles(location) > 0) { %>
          &nbsp;<a href="<%= Constants.FILESTATUS_MISSING_PAGE + "?"
              + locationParam %>">
          <fmt:message key="show.missing.files"/></a>
        <% } %>
    	<br>
        <fmt:message key="last.update.at.0"><fmt:param><fmt:formatDate type="both" value="<%=WorkFiles.getLastUpdate(location, WorkFiles.FILES_ON_BA)%>"/></fmt:param></fmt:message>
        <br>
        <a href="<%= Constants.FILESTATUS_PAGE + "?"
                + Constants.FIND_MISSING_FILES_PARAM + "=1&amp;"
                + locationParam %>">
            <fmt:message key="update"/></a>
        <br><br>
    <%
    } // end for

    //
    //For each known bitarchive in the system, print out statistics about corrupt files (files with wrong checksums)
    //

    for (Location location : Location.getKnown()) {
        String locationParam = Constants.BITARCHIVE_NAME_PARAM
                + "=" + HTMLUtils.encodeAndEscapeHTML(location.getName());
 	%>	<fmt:message key="checksum.status.for"/>
 		<b><%=HTMLUtils.escapeHtmlValues(location.getName())%></b><br>
        <fmt:message key="number.of.files.with.error"/>&nbsp;<%=BitpreserveFileStatus.getCountWrongFiles(location)%>
        <% if (BitpreserveFileStatus.getCountWrongFiles(location) > 0) { %>
             &nbsp;<a href="<%= Constants.FILESTATUS_CHECKSUM_PAGE + "?"
                + locationParam %>"><fmt:message key="show.files.with.error"/></a>
         <% } %>
        <br><fmt:message key="last.update.at.0"> <fmt:param><fmt:formatDate
        	type="both" value="<%=WorkFiles.getLastUpdate(location, WorkFiles.WRONG_FILES)%>"/></fmt:param></fmt:message>
   		<br>
        <a href="<%= Constants.FILESTATUS_PAGE + "?"
        + Constants.CHECKSUM_PARAM + "=1&amp;" + locationParam%>">
        <fmt:message key="update"/></a>
        <br><br>
        <%
    } // end for

    %>
<%
    HTMLUtils.generateFooter(out);
%>