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
This page shows selected parts of a crawllog for a given job and domain.

Note that the response language for the page is set using requested locale
of the client browser when fmt:setBundle is called. After that, fmt:format
and reponse.getLocale use this locale.

Parameters:
domain - the domain to get the log for
jobid - the id of the job to get the log for
--%><%@ page import="java.io.File,
                 java.io.FileInputStream,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.FileUtils,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.utils.StreamUtils, dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.viewerproxy.webinterface.Constants,
                 dk.netarkivet.viewerproxy.webinterface.Reporting"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.viewerproxy.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.viewerproxy.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    String domain;
    int jobid;
    File crawlLogExtract;
    try {
        HTMLUtils.forwardOnMissingParameter(pageContext, Constants.DOMAIN_PARAM, Constants.JOBID_PARAM);
        domain = request.getParameter(Constants.DOMAIN_PARAM);
        jobid = HTMLUtils.parseAndCheckInteger(pageContext, Constants.JOBID_PARAM, 1,
                                               Integer.MAX_VALUE);
        crawlLogExtract = Reporting.getCrawlLogForDomainInJob(domain, jobid);
    } catch (ForwardedToErrorPage e) {
        return;
    }
    HTMLUtils.generateHeader(pageContext);
%>
<h3><fmt:message key="pagetitle;qa.crawllog.lines.for.domain.0.in.1">
    <fmt:param value="<%=domain%>"/>
    <fmt:param value="<%=jobid%>"/>
</fmt:message></h3>
<pre>
<%
    StreamUtils.copyInputStreamToJspWriter(new FileInputStream(crawlLogExtract), out);
    FileUtils.remove(crawlLogExtract);
%>
</pre>
<%
    HTMLUtils.generateFooter(out);
%>
