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
This is the error page for the GUI and is displayed whenever an uncaught
exception is thrown. The error message is presented unescaped.
--%>
<%@ page import="java.io.PrintWriter,
                 org.apache.commons.logging.Log,
                 org.apache.commons.logging.LogFactory,
                 dk.netarkivet.common.Constants,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils"
         isErrorPage="true"
         pageEncoding="UTF-8"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"
/><%!
    private static final I18n I18N = new I18n(
            Constants.TRANSLATIONS_BUNDLE);
    Log log = LogFactory.getLog(getClass().getName());
%><%
    String title = I18N.getString(response.getLocale(),
            "pagetitle.error");
    HTMLUtils.generateHeader(title, pageContext);
    log.warn("JSP page threw exception: " + exception, exception);
%>
<h3>
    <fmt:message key="pagetitle.error"/>
</h3>

<h4>
    <fmt:message key="text.exception.description"/>
</h4>

<%
    if (exception != null) {
%>
<div id="error_message">
    <pre><%=exception.getMessage()%></pre>
</div>
<a id="detailslink" href="#"
   onclick="document.getElementById('details').style.display='block';document.getElementById('detailslink').style.display='none';return false">
    <fmt:message key="link.details"/>
</a>

<div id="details" style="display:none">
        <pre>
        <%
            PrintWriter printWriter = new PrintWriter(out);
            exception.printStackTrace(printWriter);
            printWriter.flush();
        %>
        </pre>
</div>
<%
} else {
%>
<div id="error_message">
    <fmt:message key="text.noexception.description"/>
</div>
<%
    }
%>
</div>
<%
    HTMLUtils.generateFooter(out);
%>