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
This page is used to add a (potentially) large number of seeds to an event harvest.
Previously, the processing was done on this page. The processing is now done from page 
'Definitions-edit-selective-harvest.jsp'.

Parameters sent to this page:
harvestName:
          the harvest to add the seeds to, must be name of a known harvest 
usingFileMode:
		  if null, you enter the seeds in the designated textarea; otherwise, it will ask for a file
		  that contains the seeds.	         
		  
--%><%@ page import="java.util.Iterator,
                 java.util.List,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.TemplateDAO,
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.datamodel.eav.EAV,
                 dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType,
                 com.antiaction.raptor.dao.AttributeTypeBase,
                 com.antiaction.raptor.dao.AttributeBase"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);%><%HTMLUtils.setUTF8(request);
    
    String harvestName = request.getParameter(Constants.HARVEST_PARAM);
    String mode = request.getParameter(Constants.FROM_FILE_PARAM);

    if (harvestName == null) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;missing.parameter.0",
                Constants.HARVEST_PARAM);
        return;
    }
    
    // if mode set to "1", we read the seeds from a file
    boolean usingFileMode = false;
    if (mode != null) {
        if (mode.equalsIgnoreCase("1")) {
        	 usingFileMode = true;
    	}
    }
    
    HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance(); 
    if (!hddao.exists(harvestName)) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;harvest.0.does.not.exist",
                harvestName);
        return;
    }
    // Should we test, that this is in fact a PartialHarvest?
    String harvestComments = hddao.getSparsePartialHarvest(harvestName).getComments();
    
    HTMLUtils.generateHeader(pageContext);%>

<h2><fmt:message key="prompt;event.harvest"/>
    <%=HTMLUtils.escapeHtmlValues(harvestName)%>
</h2>

<%--
Here we print the comments field from the harvest definition as a service to
the user
--%>
<div class="show_comments">
    <%=HTMLUtils.escapeHtmlValues(harvestComments)%>
</div>

 <form action="Definitions-edit-selective-harvest.jsp"

<% if (usingFileMode) { %>enctype="multipart/form-data" <%} %> method="post">

    <input type="hidden" name="<%= Constants.UPDATE_PARAM %>" value="1"/>
    <input type="hidden" name="<%= Constants.HARVEST_PARAM %>"
           value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>        
    <input type="hidden" name="<%= Constants.ADD_SEEDS_PARAM %>" value="1"/>
           
    <%--Setting of these variables is not currently supported in the system so we
     just use default values as placeholders for a future upgrade --%>
    <input type="hidden" name="<%= Constants.MAX_RATE_PARAM %>" value="-1"/>
    <table class="selection_table">
        <tr>
            <th colspan="2">
                <fmt:message key="prompt;enter.seeds"/>
            </th>
        </tr>
        <tr>
            <td colspan="2">
              <% if (!usingFileMode) { %>
                <textarea name="<%= Constants.SEEDS_PARAM %>"
                          rows="20" cols="60"></textarea>
              <% } else { %>
                <fmt:message key="prompt;harvestdefinition.templates.upload.select.file"/>
                <input type="file" size="<%=Constants.UPLOAD_FILE_FIELD_WIDTH%>" name="upload_file"/><br/>                
              <% } %>
            </td>
        </tr>
        <tr>
        	<td><fmt:message key="prompt;max.bytes.per.domain"/></td>
            <td><input type="text" name="<%= Constants.MAX_BYTES_PARAM %>"
                       value="<%= HTMLUtils.localiseLong(dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES, pageContext) %>"/>
            </td>
        </tr>
        <tr>
            <td><fmt:message key="prompt;max.objects.per.domain"/></td>
            <td><input type="text" name="<%= Constants.MAX_OBJECTS_PARAM %>"
                       value="<%= HTMLUtils.localiseLong(dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS, pageContext) %>"/>
            </td>
        </tr>
        
        <tr>
            <td><fmt:message key="prompt;harvest.template"/></td>
            <td>
                <select name="<%= Constants.ORDER_TEMPLATE_PARAM %>">
                    <% Iterator<String> templates
                            = TemplateDAO.getInstance().getAll(true);
                        while (templates.hasNext()) {
                            String template = templates.next();
                            out.println("<option value=\""
                                    + HTMLUtils.escapeHtmlValues(template)
                                    + "\">"
                                    + HTMLUtils.escapeHtmlValues(template)
                                    + "</option>");
                        }
                    %>
                </select>
            </td>
        </tr>
        <!-- ############################################################################ -->
        <!--  add html for optional attributes -->
        <!-- ############################################################################ -->
        <%
        
		EAV eav = EAV.getInstance();
		List<AttributeTypeBase> attributeTypes = eav.getAttributeTypes(EAV.DOMAIN_TREE_ID);
		AttributeTypeBase attributeType;
		for (int i=0; i<attributeTypes.size(); ++i) {
			attributeType = attributeTypes.get(i);
%>
	        <tr> <!-- edit area for eav attribute -->
	            <td style="text-align:right;"><fmt:message key="<%= attributeTypes.get(i).name %>"/></td>
	            <td> 
<%
			switch (attributeType.viewtype) {
			case 1:
%>
  	                <input type="text" id="<%= attributeType.name %>" name="<%= attributeType.name %>" value="<%= attributeType.def_int %>">
<%
				break;
			case 5:
			case 6:
				if (attributeType.def_int > 0) {
%>
		            <input type="checkbox" id="<%= attributeType.name %>" name="<%= attributeType.name %>" value="1" checked="1">
<%
				} else {
%>
		            <input type="checkbox" id="<%= attributeType.name %>" name="<%= attributeType.name %>">
<%
				}
				break;
			}
%>
	             </td>
	        </tr>
<%
		}
%>
        
        <!-- ############################################################################ -->
        <!-- END OF: adding html for optional attributes -->
        <!-- ############################################################################ -->
        
        <tr>
            <td colspan="2"><input type="submit"
                                   value="<fmt:message key="insert"/>"/></td>
        </tr>
    </table>
</form>
<%
    HTMLUtils.generateFooter(out);
%>
