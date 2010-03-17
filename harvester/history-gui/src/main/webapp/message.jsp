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
This is a utility page used to display a message to the user. The message to
be displayed is taken from the request attribute "message". The message
is not escaped and may therefore contain active html content such as
hyperlinks and formatting.
--%><%@ page import="dk.netarkivet.common.Constants,
                     dk.netarkivet.common.utils.I18n,
                     dk.netarkivet.common.webinterface.HTMLUtils"%>
<%@ page pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"
/><%! private static final I18n I18N = new dk.netarkivet.common.utils.I18n(
        Constants.TRANSLATIONS_BUNDLE);
%><%
    String title = I18N.getString(response.getLocale(),
            "pagetitle.message");
    HTMLUtils.generateHeader(title, pageContext);
%>

<h3><fmt:message key="pagetitle.message"/></h3>

<div id="message">
    <% out.print(request.getAttribute("message")); %>
</div>

<%
    HTMLUtils.generateFooter(out);
%>