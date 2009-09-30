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
This page provides user functionality to download and upload order templates.
Upload can either replace an existing template or add a new one. There are
no parameters.
--%><%@ page import="java.util.ArrayList,
                 java.util.Iterator,
                 java.util.List,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.datamodel.TemplateDAO"
          pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    HTMLUtils.generateHeader(
            pageContext);
    TemplateDAO dao = TemplateDAO.getInstance();
    List<String> templateList = new ArrayList<String>();
    Iterator<String> templates = dao.getAll();
    while (templates.hasNext()) {
        templateList.add(templates.next());
    }

%>

<h3 class="page_heading"><fmt:message key="pagetitle;edit.harvest.templates"/></h3>

<form method="post" action="Definitions-download-harvest-template.jsp">
    <h4><fmt:message key="download"/></h4><fmt:message key="harvestdefinition.templates.select"/><br />
    <select name="order_xml_to_download">
<%
    for (String template : templateList) {
        out.println("<option value=\"" + HTMLUtils.escapeHtmlValues(template)
                    + "\">" + HTMLUtils.escapeHtmlValues(template)
                            + "</option>");
    }
%>
    </select>
    <select name="requestedContentType">
<%
     String[] contentTypes = { "text/plain", "text/xml",
             "binary/octet-stream" };
    String[] contentDescriptions = {
    	I18N.getString(response.getLocale(), "harvestdefinition.templates.show.as.text"),
    	I18N.getString(response.getLocale(), "harvestdefinition.templates.show.as.xml"),
        I18N.getString(response.getLocale(), "harvestdefinition.templates.save.to.disk")
        };
     for (int i = 0;
          i < Math.min(contentTypes.length, contentDescriptions.length); i++) {
         out.println("<option value=\""
                     + HTMLUtils.escapeHtmlValues(contentTypes[i])
                     + "\">"
                     + HTMLUtils.escapeHtmlValues(contentDescriptions[i])
                     + "</option>");
      }
%>
  </select>
    <input type="submit" name="download" value=<fmt:message key="harvestdefinition.templates.retrieve"/> />
</form>
<br />
<hr/>
<h4><fmt:message key="upload"/></h4>

<form method="post" action="Definitions-upload-harvest-template.jsp"
      enctype="multipart/form-data">
    <fmt:message key="harvestdefinition.templates.upload.to.replace"/><br />
    <select name="order_xml_to_replace">
<%
    for (String template : templateList) {
        out.println("<option value=\"" + HTMLUtils.escapeHtmlValues(template)
                    + "\">" + HTMLUtils.escapeHtmlValues(template)
                    + "</option>");
     }
%>
    </select>
    <input type="file" name="upload_file" size="<%=Constants.UPLOAD_FILE_FIELD_WIDTH%>" /><br/>
    <input type="submit" name="upload"
           value="<fmt:message key="harvestdefinition.templates.upload.replace"/>"/>
</form>
<br/>

<form method="post" action="Definitions-upload-harvest-template.jsp"
      enctype="multipart/form-data">
    <fmt:message key="harvestdefinition.templates.upload.to.create"/><br />
 <fmt:message key="harvestdefinition.templates.upload.template.name"/> <input name="order_xml_to_upload" size="<%=Constants.TEMPLATE_NAME_WIDTH %>" value="">
<fmt:message key="prompt;harvestdefinition.templates.upload.select.file"/><input type="file" size="<%=Constants.UPLOAD_FILE_FIELD_WIDTH%>" name="upload_file"/><br/>
<input type="submit" name="upload"
       value="<fmt:message key="harvestdefinition.templates.upload.create"/>"/>
</form>
<%
    HTMLUtils.generateFooter(out);
%>