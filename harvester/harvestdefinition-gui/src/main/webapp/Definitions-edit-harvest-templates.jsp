<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2012 The Royal Danish Library, the Danish State and
University Library, the National Library of France and the Austrian
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
%>
<%@ page import="dk.netarkivet.harvester.datamodel.HeritrixTemplate" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    TemplateDAO dao = TemplateDAO.getInstance();
    String flipactive = request.getParameter(Constants.FLIPACTIVE_PARAM);
    if (flipactive != null)  {
        HeritrixTemplate heritrixTemplate = dao.read(flipactive);
        heritrixTemplate.setIsActive(!heritrixTemplate.isActive());
        dao.update(flipactive, heritrixTemplate);
        response.sendRedirect("Definitions-edit-harvest-templates.jsp");
        return;
    }


    HTMLUtils.generateHeader(
            pageContext);
    List<String> templateNameList = new ArrayList<String>();
    Iterator<String> templates = dao.getAll();
    while (templates.hasNext()) {
        templateNameList.add(templates.next());
    }
    class TemplateWithActivity{
        public String name;
        public boolean isActive;

        public TemplateWithActivity(String name, boolean isActive) {
            this.name = name;
            this.isActive = isActive;
        }
    }
    List<TemplateWithActivity> templateList = new ArrayList<TemplateWithActivity>();
    for (String templateName: templateNameList) {
        HeritrixTemplate heritrixTemplate = dao.read(templateName);
        templateList.add(new TemplateWithActivity(templateName, heritrixTemplate.isActive()));
    }
    //TODO replace with a more efficient DAO method that just returns the name/isActive pair
%>
<table>
    <%
        for (TemplateWithActivity templateWithActivity: templateList) {
    %>
    <tr>
        <td><%=templateWithActivity.name%></td>
        <td>
            <form method="post" action="Definitions-download-harvest-template.jsp">
                <input name="order_xml_to_download" value="<%=templateWithActivity.name%>" type="hidden"/>
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
        </td>
        <td>
            <form method="post" action="Definitions-upload-harvest-template.jsp"
                  enctype="multipart/form-data">
                <input name="order_xml_to_replace" value="<%=templateWithActivity.name%>" type="hidden"/>
                <input type="file" name="upload_file" size="<%=Constants.UPLOAD_FILE_FIELD_WIDTH%>" />
                <input type="submit" name="upload"
                       value="<fmt:message key="harvestdefinition.templates.upload.replace"/>"/>
            </form>
        </td>
        <td>
            <%
                String linkText;
                if (templateWithActivity.isActive) {
                    linkText= "Deactivate";
                } else {
                    linkText = "Activate";
                }
                String formId = templateWithActivity.name + "flip";
            %>
            <form
                    id="<%=formId%>"
                    action="Definitions-edit-harvest-templates.jsp"
                    method="post"
                    ><input
                    type="hidden"
                    name="flipactive"
                    value="<%=templateWithActivity.name%>"
                    /><a href="" onclick="document.getElementById('<%=formId%>').submit(); return false;"><%=linkText%></a>
            </form>
        </td>
    </tr>
    <%
        }
    %>
</table>


<h3 class="page_heading"><fmt:message key="pagetitle;edit.harvest.templates"/></h3>

<form method="post" action="Definitions-download-harvest-template.jsp">
    <h4><fmt:message key="download"/></h4><fmt:message key="harvestdefinition.templates.select"/><br />
    <select name="order_xml_to_download">
<%
    for (String template : templateNameList) {
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
    for (String template : templateNameList) {
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