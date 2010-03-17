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
This page is used to add a (potentially) large number of seeds to an event harvest.
Parameters:
harvestName:
          the harvest to add the seeds to, must be name of a known harvest
update:
          if null, the page just displays a form for input. If not null, the backing
          method  EventHarvest.addConfigurations is called to process the seeds to be added
seeds:
          A whitespace-separated list of seed urls to be added
usingFileMode:
		  if null, you enter the seeds in the designated textarea; otherwise, it will ask for a file
		  that contains the seeds.	         
orderTemplate:
          The name of the order template to use with these seeds

This page has major side effects in that it will:
1) Create any unknown domains present in the seedlist
2) Create for every seedlist a configuration and seedlist formed from the
name of the harvest and the orderTemplate and add that configuration to the
harvest.
--%><%@ page import="java.util.Iterator,
                 java.util.List,
                 java.io.File,
                 dk.netarkivet.common.utils.FileUtils,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.PartialHarvest,
                 dk.netarkivet.harvester.datamodel.TemplateDAO,
                 dk.netarkivet.harvester.webinterface.Constants,
                 org.apache.commons.fileupload.FileItemFactory,
                 org.apache.commons.fileupload.disk.DiskFileItemFactory,
                 org.apache.commons.fileupload.servlet.ServletFileUpload,
                 org.apache.commons.fileupload.FileItem,
                 dk.netarkivet.harvester.webinterface.EventHarvest"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    
    boolean isMultiPart = ServletFileUpload.isMultipartContent(request);
    String harvestName = null;
    String mode = null;
    String update = null;
    // These fields are necessary
    File seedsFile = File.createTempFile("seeds", ".txt", 
                    FileUtils.getTempDir());
    String maxbytesString = null;
    String maxobjectsString = null;
    String orderTemplateString = null;
    String maxrateString = null;
    String seedsFileName = "";
                    
    if (isMultiPart) {
    	// Create a factory for disk-based file items
    	FileItemFactory factory = new DiskFileItemFactory();
		// Create a new file upload handler
   		ServletFileUpload upload = new ServletFileUpload(factory);
        // As the parsing of the formdata has the sideeffect of removing the
        // formdata from the request(!), we have to extract all possible data
        // the first time around.
        List items = upload.parseRequest(request);
        for (Object o : items) {
        	FileItem item = (FileItem) o;
            String fieldName = item.getFieldName();
            if (fieldName.equals(Constants.HARVEST_PARAM)) {
            	harvestName = item.getString();
          	} else if (fieldName.equals(Constants.UPDATE_PARAM)) {
           		update = item.getString();
            } else if (fieldName.equals(Constants.MAX_BYTES_PARAM)) {
           		maxbytesString = item.getString();
            } else if (fieldName.equals(Constants.MAX_OBJECTS_PARAM)) {
          		maxobjectsString = item.getString();
            } else if (fieldName.equals(Constants.MAX_RATE_PARAM)) {
               	maxrateString = item.getString();             
            } else if (fieldName.equals(Constants.ORDER_TEMPLATE_PARAM)) {
             	orderTemplateString = item.getString();
            } else if (fieldName.equals(Constants.UPLOAD_FILE_PARAM)) {
              	item.write(seedsFile);
           		seedsFileName = item.getName();
            }
       	}
    } else {
    	harvestName = request.getParameter(Constants.HARVEST_PARAM);
    	mode = request.getParameter(Constants.FROM_FILE_PARAM);
    	update = request.getParameter(Constants.UPDATE_PARAM);
    }
    	
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
    
    PartialHarvest harvest = (PartialHarvest)
            HarvestDefinitionDAO.getInstance().
                    getHarvestDefinition(harvestName);
    if (harvest == null) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;harvest.0.does.not.exist",
                harvestName);
        return;
    }
    if (update != null && update.length() > 0) {
        try {
            if (!isMultiPart) {
			  	EventHarvest.addConfigurations(pageContext, I18N, harvest);
			} else {
				if (!seedsFileName.isEmpty()) { // File exists
					String seeds = FileUtils.readFile(seedsFile);			
					if (!seeds.isEmpty()) {
						EventHarvest.addConfigurationsFromSeedsFile(
							pageContext, I18N, harvest, seeds, maxbytesString, 
							maxobjectsString, maxrateString, orderTemplateString);
					}
				} else {
					HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                	"errormsg;no.seedsfile.was.uploaded");
        			return;
        		}
			}
        } catch (ForwardedToErrorPage e) {
            HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                    "errormsg;error.adding.seeds.to.0", harvestName, e);
            return;
        }
        response.sendRedirect("Definitions-edit-selective-harvest.jsp?"
                + Constants.HARVEST_PARAM + "="
                + HTMLUtils.encode(harvestName));
        return;
    }
    HTMLUtils.generateHeader(pageContext);
%>

<h2><fmt:message key="prompt;event.harvest"/>
    <%=HTMLUtils.escapeHtmlValues(harvestName)%>
</h2>

<%--
Here we print the comments field from the harvest definition as a service to
the user
--%>
<div class="show_comments">
    <%=HTMLUtils.escapeHtmlValues(harvest.getComments())%>
</div>

<form action="Definitions-add-event-seeds.jsp" 
<% if (usingFileMode) { %>enctype="multipart/form-data" <%} %> method="post">

    <input type="hidden" name="<%= Constants.UPDATE_PARAM %>" value="1"/>
    <input type="hidden" name="<%= Constants.HARVEST_PARAM %>"
           value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>
           
    <%--Setting of these variables is not currently supported in the system so we
     just use default values as placeholders for a future upgrade --%>
    <input type="hidden" name="<%= Constants.MAX_RATE_PARAM %>" value="-1"/>
    <input type="hidden" name="<%= Constants.MAX_OBJECTS_PARAM %>" value="-1"/>
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
            <td><fmt:message key="prompt;harvest.template"/></td>
            <td>
                <select name="<%= Constants.ORDER_TEMPLATE_PARAM %>">
                    <% Iterator<String> templates
                            = TemplateDAO.getInstance().getAll();
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
        <tr>
            <td colspan="2"><input type="submit"
                                   value="<fmt:message key="insert"/>"/></td>
        </tr>
    </table>
</form>
<%
    HTMLUtils.generateFooter(out);
%>