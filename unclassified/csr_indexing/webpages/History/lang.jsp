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
This is a page for changing language. The requested locale is posted in the
parameter 'locale', and a human readable name in 'name', but locale will be used
if name is not sent.
--%><%@ page
        import="dk.netarkivet.common.Constants,
                dk.netarkivet.common.utils.I18n,
                dk.netarkivet.common.webinterface.HTMLUtils"
        pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><%!
    private static final I18n I18N = new I18n(
            Constants.TRANSLATIONS_BUNDLE);
%><%
    String locale = request.getParameter("locale");
    String name = request.getParameter("name");
    if (locale == null || locale.equals("")) {
        request.setAttribute("message",
                HTMLUtils.escapeHtmlValues(I18N.getString(
                        response.getLocale(),
                        "errormsg.locale.nolocale")));
        RequestDispatcher rd
                = pageContext.getServletContext().getRequestDispatcher(
                "/message.jsp");
        rd.forward(request, response);
    }
    if (name == null || name.equals("")) {
        name = locale;
    }
    Cookie cookie = new Cookie("locale", locale);
    cookie.setPath("/");
    //Keep the cookie for a year
    cookie.setMaxAge(365 * 24 * 60 * 60);
    response.addCookie(cookie);
%><fmt:setLocale value="<%=locale%>" scope="page"
/><fmt:setBundle basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%
    String title = I18N.getString(response.getLocale(),
            "pagetitle.language");
    HTMLUtils.generateHeader(title, pageContext);
%>
<h3><fmt:message key="pagetitle.language"/></h3>

<fmt:message key="text.language.description">
    <fmt:param value="<%=name%>"/>
</fmt:message>
<%
    HTMLUtils.generateFooter(out);
%>