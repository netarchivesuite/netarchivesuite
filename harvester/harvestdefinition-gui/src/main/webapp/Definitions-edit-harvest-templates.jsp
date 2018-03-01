<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2018 The Royal Danish Library,
the National Library of France and the Austrian
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
<%@ page import="dk.netarkivet.common.utils.Settings" %>
<%@ page import="dk.netarkivet.harvester.HarvesterSettings" %>
<%@ page import="dk.netarkivet.common.CommonSettings" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    TemplateDAO dao = TemplateDAO.getInstance();
    boolean hideInactive =  Settings.getBoolean(CommonSettings.HIDE_INACTIVE_TEMPLATES);
    String flipactive = request.getParameter(Constants.FLIPACTIVE_PARAM);
    if (flipactive != null && !hideInactive)  {
        HeritrixTemplate heritrixTemplate = dao.read(flipactive);
        heritrixTemplate.setIsActive(!heritrixTemplate.isActive());   //ie just flip the current state
        dao.update(flipactive, heritrixTemplate);
        response.sendRedirect("Definitions-edit-harvest-templates.jsp");
        return;
    }

    class TemplateWithActivity{
        public String name;
        public boolean isActive;

        public TemplateWithActivity(String name, boolean isActive) {
            this.name = name;
            this.isActive = isActive;
        }
    }
    HTMLUtils.generateHeader(
            pageContext);
    List<TemplateWithActivity> templateList = new ArrayList<TemplateWithActivity>();
    Iterator<String> templates = dao.getAll(true);
    while (templates.hasNext()) {
        String name = templates.next();
        boolean isDefaultOrder = Settings.get(HarvesterSettings.DOMAIN_DEFAULT_ORDERXML).equals(name);
        if (isDefaultOrder) {
            templateList.add(0, new TemplateWithActivity(name, true));
        } else {
            templateList.add(new TemplateWithActivity(name, true));
        }
    }
    if (!hideInactive) {
        templates = dao.getAll(false);
        while (templates.hasNext()) {
            String name = templates.next();
            templateList.add(new TemplateWithActivity(name, false));
        }
    }
%>
<script type="text/javascript">
    window.onload = hideInactive
    function hideInactive() {
        var inactiveRows = document.getElementsByClassName("inactive");
        for (i = 0; i < inactiveRows.length; ++i) {
            inactiveRows[i].style.display = 'none';
        }
        document.getElementById('hide').style.display = 'none';
        document.getElementById('show').style.display = 'inline';
    }
    function showInactive() {
        var inactiveRows = document.getElementsByClassName("inactive");
        for (i = 0; i < inactiveRows.length; ++i) {
             inactiveRows[i].style.display = 'table-row';
        }
        document.getElementById('hide').style.display = 'inline';
        document.getElementById('show').style.display = 'none';
    }
</script>
<h3 class="page_heading"><fmt:message key="pagetitle;edit.harvest.templates"/></h3>
<% if (!hideInactive) { %>
<button id="hide" onclick="hideInactive();"><fmt:message key="harvestdefinition.templates.hide.inactive"/></button><button id="show" onclick="showInactive();"><fmt:message key="harvestdefinition.templates.show.inactive"/></button> </br>
<%}%>
<table id="templates">
    <%
        int rowNumber = 0;
        for (TemplateWithActivity templateWithActivity: templateList) {
            String rowClass;
            if (templateWithActivity.isActive) {
                rowClass = "active";
            } else {
                rowClass = "inactive";
            }
            if (rowNumber%6 < 3) {
                rowClass += " light";
            } else {
                rowClass += " dark";
            }
            rowNumber++;
    %>
    <tr class="<%=rowClass%>">
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
        <%
            if (!hideInactive) {
        %>
        <td>
            <%
                String linkText;
                if (templateWithActivity.isActive) {
                    linkText= I18N.getString(response.getLocale(), "deactivate");
                } else {
                    linkText = I18N.getString(response.getLocale(), "activate");
                }
                String formId = templateWithActivity.name + "flip";
                boolean isDefaultOrder = Settings.get(HarvesterSettings.DOMAIN_DEFAULT_ORDERXML).equals(templateWithActivity.name);
                if (isDefaultOrder) {
                    linkText = I18N.getString(response.getLocale(), "harvestdefinition.templates.default.template");
                }
            %>
            <form
                    id="<%=formId%>"
                    action="Definitions-edit-harvest-templates.jsp"
                    method="post"
                    ><input
                    type="hidden"
                    name="flipactive"
                    value="<%=templateWithActivity.name%>"
                    />
                <%
                    if (!isDefaultOrder) {
                %>
                <a href="" onclick="document.getElementById('<%=formId%>').submit(); return false;"><%=linkText%></a>
                <%
                    } else {
                %>
                <a href=""><%=linkText%></a>
                <%
                    }
                %>
            </form>
        </td>
        <%
            }
        %>
    </tr>
    <%
        }
    %>
</table>


<hr/>
<h4><fmt:message key="upload"/></h4>

<form method="post" action="Definitions-upload-harvest-template.jsp"
      enctype="multipart/form-data">
    <fmt:message key="harvestdefinition.templates.upload.to.create"/><br />
 <fmt:message key="harvestdefinition.templates.upload.template.name"/> <input name="order_xml_to_upload" size="<%=Constants.TEMPLATE_NAME_WIDTH %>" value="">
<fmt:message key="prompt;harvestdefinition.templates.upload.select.file"/><input type="file" size="<%=Constants.UPLOAD_FILE_FIELD_WIDTH%>" name="upload_file"/>
<input type="submit" name="upload"
       value="<fmt:message key="harvestdefinition.templates.upload.create"/>"/>
</form>
<%
    HTMLUtils.generateFooter(out);
%>