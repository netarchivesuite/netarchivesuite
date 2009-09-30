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
This page is used to ingest a list of domains posted to the server. It will
throw an exception if it does not receive a multipart request. Every item
(usually exactly one) in the request is treated as a new-line separated list
of domain names. The file is then processed by IngestDomainList.updateDomainInfo.
The page tries to prevent a user-agent timeout by sending regular information on
the progress of the ingestion.
--%><%@ page import="java.io.File,
                 java.util.List,
                 org.apache.commons.fileupload.DiskFileUpload,
                 org.apache.commons.fileupload.FileItem,
                 org.apache.commons.fileupload.FileUpload,
                 dk.netarkivet.common.utils.FileUtils,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.Constants,
                 dk.netarkivet.harvester.webinterface.DomainIngester"
         pageEncoding="UTF-8"
%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(Constants.TRANSLATIONS_BUNDLE);
%><%
    //This is the time interval between writing keep-alive output
    //back to the browser
    final long SLEEP_TIME = 10000;
    HTMLUtils.setUTF8(request);

    //A temporary file to use for the domain list
    final File ingestFile = File.createTempFile("ingest_list", "txt",
            FileUtils.getTempDir());
    boolean isMultiPart = FileUpload.isMultipartContent(request);
    if (!isMultiPart) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;domain.upload.not.multipart");
        return;
    }
    DiskFileUpload upload = new DiskFileUpload();

    //Read the multipart request to the temporary file on the server machine
    try {
        List items = upload.parseRequest(request);
        for (Object o : items) {
            FileItem item = (FileItem) o;
            if (!item.isFormField()) {
                item.write(ingestFile);
            }
        }
    } catch (Exception e) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                e, "errormsg;domain.upload.failed");
        return;
    }

    HTMLUtils.generateHeader(
            pageContext);
    long numberOfDomains = FileUtils.countLines(ingestFile);
%>
    <fmt:message key="ingesting.0.domains">
        <fmt:param value="<%=numberOfDomains%>"/></fmt:message>
    <br/>
    <%

    DomainIngester ingestThread = new DomainIngester(out, ingestFile,
    	HTMLUtils.getLocaleObject(pageContext));
    ingestThread.start();

    long totalTime = 0;
    while (!ingestThread.isDone()) {
        Thread.sleep(SLEEP_TIME);
        totalTime += SLEEP_TIME;
        if (!ingestThread.isDone()) {
            %>
<fmt:message key="ingesting.domains.0.seconds">
    <fmt:param value="<%= totalTime %>"/>
</fmt:message>
<br/>
<%
        }
        out.flush();
    }
    %>
<fmt:message key="ingesting.done"/><br/>
<%
    if (ingestThread.getException() != null) {
        %>
        <fmt:message key="errormsg;error.while.ingesting.0">
            <fmt:param value="<%=ingestThread.getException().getMessage()%>"/>
        </fmt:message>
        <%
    }

    HTMLUtils.generateFooter(out);
%>