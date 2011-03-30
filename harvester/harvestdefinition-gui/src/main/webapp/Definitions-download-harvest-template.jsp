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

--%><%@ page import="org.dom4j.Document,
                 org.dom4j.io.XMLWriter,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.TemplateDAO, 
                 dk.netarkivet.harvester.datamodel.JobDAO,
                 dk.netarkivet.harvester.datamodel.Job,
                 dk.netarkivet.harvester.webinterface.Constants"
          pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    
    String requestedContentType = request.getParameter("requestedContentType");
    if (requestedContentType == null) {
        	requestedContentType = "text/xml";
    }
   
    try {		    
          HTMLUtils.forwardOnEmptyParameter(pageContext,
          "order_xml_to_download");
    } catch (ForwardedToErrorPage e) {
        return;
    }
    
    String orderXmlToShow = request.getParameter("order_xml_to_download");
    String filename = orderXmlToShow;
    TemplateDAO dao = TemplateDAO.getInstance();
    if (!dao.exists(orderXmlToShow)) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
               "errormsg;harvest.template.0.does.not.exist", orderXmlToShow);
        return;
    }
    Document doc = dao.read(orderXmlToShow).getTemplate();
    
    // Write the template to the page.
    	
    response.setHeader("Content-Type", requestedContentType);
    if (requestedContentType.startsWith("binary")) {
        response.setHeader("Content-Disposition", "Attachment; filename="
                + filename + Constants.XML_EXTENSION);

    }
    XMLWriter writer = new XMLWriter(out);
    writer.write(doc);
%>