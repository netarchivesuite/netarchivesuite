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
--%><%@ page import="dk.netarkivet.archive.webinterface.BitpreserveFileState,
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
    BitpreserveFileState.processUpdateRequest(pageContext);
    HTMLUtils.generateHeader(pageContext);

%>
<h3 class="page_heading"><fmt:message key="pagetitle;filestatus"/></h3>

<h4><fmt:message key="bitarchive.state"/></h4>

<%
    // For each known bitarchive in the system, print out statistics about
    // missing files
    for (Location location : Location.getKnown()) {
        BitpreserveFileState.printMissingFileStateForLocation(out, location,
                                                          response.getLocale());
    }

    // For each known bitarchive in the system, print out statistics about 
    // corrupt files (files with wrong checksums)
    for (Location location : Location.getKnown()) {
        BitpreserveFileState.printChecksumErrorStateForLocation(out, location,
                                                          response.getLocale());
    }
    HTMLUtils.generateFooter(out);
%>