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

--%><%-- This page handles showing harvest status and history for domains.
With no parameters, it gives an input box for searching that feeds back into
itself.
With parameter domainName, it performs a search.  Name can be a glob pattern
(using ? and * only) or a single domain.  If domains are found, they are
displayed, if no domains are found a message is shown.
--%>
<%@ page import="dk.netarkivet.common.webinterface.HTMLUtils"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"
/>

<%
  HTMLUtils.setUTF8(request);
  HTMLUtils.generateHeader(pageContext);
%>

<h3 class="page_heading">Deprecated</h3>

Please find the domain harvest history by:
<ol>
  <li> Going to the
<a href="/HarvestDefinition/Definitions-find-domains.jsp">Find Domain(s)
</a> page.
    <li>Search for the domain.
    <li>click the 'History' link for the domain in the result list'.
  </ol>
<%
  HTMLUtils.generateFooter(out);
%>