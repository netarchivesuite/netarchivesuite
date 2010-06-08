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

--%><%@page import="org.dom4j.io.HTMLWriter,
                    java.io.File,
                    dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                    dk.netarkivet.common.utils.I18n,
                    dk.netarkivet.common.utils.FileUtils,
                    dk.netarkivet.common.webinterface.HTMLUtils,
                    dk.netarkivet.common.webinterface.SiteSection, 
                    dk.netarkivet.common.webinterface.BatchGUI,
                    dk.netarkivet.viewerproxy.Constants"
            pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N 
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
%><%
    try {
        HTMLUtils.forwardOnEmptyParameter(pageContext, "filename");
    } catch (ForwardedToErrorPage e) {
        return;
    }
%><%    
    String filename = request.getParameter("filename");
    File resultFile = new File(BatchGUI.getBatchDir(), filename);
%><%    
    // validate the file
    if(resultFile == null || !resultFile.exists() || !resultFile.isFile()) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N, 
            "errormsg;batch.result.file.0.is.missing", filename);
        return;
    }
%><%    
    if(resultFile == null || !resultFile.isFile()) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N, 
                "errormsg;batch.result.file.0.is.not.a.file", filename);
        return;
    }
%><%
    response.setHeader("Content-type", "text/plain");
    response.setHeader("Content-Disposition", "Attachment; filename=" 
            + filename);
%><%    
    out.write(FileUtils.readFile(resultFile));
%>