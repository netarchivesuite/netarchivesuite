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
This page implements the changeIndex command.

Parameters:
jobID (one or more): Id of jobs to create an index for.
indexLabel: An arbitrary name that will be associated with the created index.
--%><%@page import="java.util.HashSet,
                    java.util.Set,
                    dk.netarkivet.common.utils.I18n,
                    dk.netarkivet.common.webinterface.HTMLUtils,
                    dk.netarkivet.viewerproxy.Constants,
                    dk.netarkivet.viewerproxy.Controller,
                    dk.netarkivet.viewerproxy.distribute.HTTPControllerClient,
                    dk.netarkivet.viewerproxy.webinterface.Parameters, dk.netarkivet.viewerproxy.webinterface.QASiteSection"
            pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    HTMLUtils.forwardOnMissingParameter(pageContext, Parameters.JOB_ID,
            Parameters.INDEX_LABEL);
    Controller c = new HTTPControllerClient(response, out,
            QASiteSection.createQAReturnURL(request));
    Set<Long> jobIDList = new HashSet<Long>();
    //Note: It's okay exceptions end up on the error page, but sending an empty
    //set or null to the viewerproxy will delay exceptions for too long.
    for (String jobIDString : request.getParameterValues(Parameters.JOB_ID)) {
        jobIDList.add(Long.parseLong(jobIDString));
    }
    c.changeIndex(jobIDList, request.getParameter(Parameters.INDEX_LABEL));
%>
