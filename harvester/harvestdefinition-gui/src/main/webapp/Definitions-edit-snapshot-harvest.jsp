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
This page is used to set up a snapshot harvest. The following parameters may be
passed to it:
update (Constants.UPDATE_PARAM):
    (null or single) If set, create the snapshot harvest and return
    to the snapshot listing page. If unset, show a page for editing a new
    snapshot definition
createnew (Constant.CREATENEW_PARAM):
    (null or single) If set, the name parameter is used to create a new
    snapshot harvest definition from the posted values.
edition (Constants.EDITION_PARAM):
    (null or single, int) If set, this is the edition that updates should be
    done against.  If somebody else modified the database between loading the
    page and submitting changes, this can be used to notice and give a proper
    error.
old_snapshot_name (Constants.OLDSNAPSHOT_PARAM):
    (null or single) If set, name of old snapshot to use as basis for this one.
    If null, start snapshot from scratch.
snapshot_object_limit (Constants.DOMAIN_LIMIT_PARAM):
    (null or single, int) Number of objects per domain to be harvested. Default
    value is Constants.DOMAIN_LIMIT_DEFAULT
snapshot_byte_limit (Constants.DOMAIN_BYTELIMIT_PARAM):
    (null or single, long) Number of bytes per domain to be harvested.  Default
     value is Constants.DEFAULT_MAX_BYTES
harvestName (Constants.HARVEST_SNAPSHOT_PARAM):
    (null or single) The name of the harvest to be created or modified
--%><%@ page import="java.text.NumberFormat,
                 java.util.List,
                 java.util.Locale,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.SparseFullHarvest, 
                 dk.netarkivet.harvester.webinterface.Constants, 
                 dk.netarkivet.harvester.webinterface.SnapshotHarvestDefinition,
                 dk.netarkivet.harvester.datamodel.eav.EAV,
                 dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType,
                 com.antiaction.raptor.dao.AttributeTypeBase,
                 com.antiaction.raptor.dao.AttributeBase"
         pageEncoding="UTF-8"
%>
<%@ page import="javax.inject.Provider" %>
<%@ page import="dk.netarkivet.harvester.datamodel.JobDAO" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    try {
        SnapshotHarvestDefinition snapshotHarvestDefinition = SnapshotHarvestDefinition.
                createSnapshotHarvestDefinitionWithDefaultDAOs();
        snapshotHarvestDefinition.processRequest(pageContext, I18N);
    } catch (ForwardedToErrorPage e) {
        return;
    }

    //Redirect if we just saved the HD
    if (request.getParameter(Constants.UPDATE_PARAM) != null) {
        response.sendRedirect("Definitions-snapshot-harvests.jsp");
        return;
    }

    // overwrite with existing harvestdefinition values
    String harvestName = request.getParameter(Constants.HARVEST_PARAM);
    SparseFullHarvest hd = null;
    if (harvestName != null) {
        hd = HarvestDefinitionDAO.getInstance().getSparseFullHarvest(harvestName);
        if (hd == null) {
            HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                    "errormsg;harvest.0.does.not.exist", harvestName);
            return;
        }
    }

    long objectLimit = dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS;
    Long oldHarvestOid = null;
    if (hd != null) { // We're editing an existing snapshot harvest
        objectLimit = hd.getMaxCountObjects();
        oldHarvestOid = hd.getPreviousHarvestDefinitionOid();
    }
    HTMLUtils.generateHeader(pageContext);
    NumberFormat nf = NumberFormat.getInstance(HTMLUtils.getLocaleObject(pageContext));
%>
<h3 class="page_heading"><fmt:message key="pagetitle;snapshot.harvest"/></h3>

<form method="post" action="Definitions-edit-snapshot-harvest.jsp">
    <!-- create hidden fields -->
    <% if (hd == null) {
        // the createnew parameter is only set when no name was given %>
        <input type="hidden" name="<%=Constants.CREATENEW_PARAM%>" value="1" />
  <% } else { %>
    <input type="hidden" name="<%= Constants.EDITION_PARAM %>"
           value="<%= hd.getEdition() %>"/>              
  <% } %>
    <input type="hidden" name="<%= Constants.UPDATE_PARAM %>" value="1"/>
    <input type="hidden" name="<%= Constants.HARVEST_OLD_PARAM %>" value="<%=HTMLUtils.escapeHtmlValues(harvestName) %>"/>
    
    <table>
        <tr>
            <td style="text-align:right;"><fmt:message key="prompt;harvest.name"/></td>
            <td>
          <% if (hd == null) { %>
              <span id="focusElement">
                <input type="text" name="<%= Constants.HARVEST_PARAM%>"
                       size="20"/>
              </span>
          <% } else { %>
                <input type="text" name="<%= Constants.HARVEST_PARAM %>"
                     value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>
          <% } %>
            </td>
        </tr>
        <tr>
            <%
            long dftMaxObjects =
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS;
            %>
            <td style="text-align:right;"><fmt:message key="prompt;max.objects.per.domain"/></td>
            <td><input 
                name="<%= Constants.DOMAIN_OBJECTLIMIT_PARAM %>"
                size="20" 
                value="<%= HTMLUtils.localiseLong(
                    (hd != null ? hd.getMaxCountObjects() : dftMaxObjects), 
                    pageContext)                
                   %>"/>
             </td>
        </tr>
        <tr>
            <%
            long dftMaxBytes =
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES;
            %>
            <td style="text-align:right;"><fmt:message key="prompt;max.bytes.per.domain"/></td>
            <td><input 
                name="<%= Constants.DOMAIN_BYTELIMIT_PARAM %>"
                size="20" 
                value="<%= HTMLUtils.localiseLong(
                    (hd != null ? hd.getMaxBytes() : dftMaxBytes), 
                    pageContext)                
                   %>"/>
             </td>
        </tr>
        <tr> <!-- edit area for max job running time -->
            <%
            long dftMaxJobRunningTime =
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_JOB_RUNNING_TIME;
            %>
            <td style="text-align:right;"><fmt:message key="prompt;max.seconds.per.crawljob"/></td>
            <td><input 
                name="<%= Constants.JOB_TIMELIMIT_PARAM %>"
                size="20" 
                value="<%= HTMLUtils.localiseLong(
                    (hd != null ? hd.getMaxJobRunningTime() : dftMaxJobRunningTime), 
                    pageContext)                
                   %>"/>
             </td>
        </tr>
<%
		if (hd == null) {
			EAV eav = EAV.getInstance();
			List<AttributeTypeBase> attributeTypes = eav.getAttributeTypes(EAV.SNAPSHOT_TREE_ID);
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
		} else {
			List<AttributeAndType> attributesAndTypes = hd.getAttributesAndTypes();
			AttributeAndType attributeAndType;
			for (int i=0; i<attributesAndTypes.size(); ++i) {
				attributeAndType = attributesAndTypes.get(i);
				Integer intVal = null;
				if (attributeAndType.attribute != null) {
					intVal = attributeAndType.attribute.getInteger();
				}
				if (intVal == null) {
					intVal = attributeAndType.attributeType.def_int;
				}
				%>
		        <tr> <!-- edit area for eav attribute -->
		            <td style="text-align:right;"><fmt:message key="<%= attributeAndType.attributeType.name %>"/></td>
		            <td> 
<%
					switch (attributeAndType.attributeType.viewtype) {
					case 1:
%>
	  	                <input type="text" id="<%= attributeAndType.attributeType.name %>" name="<%= attributeAndType.attributeType.name %>" value="<%= intVal %>">
<%
						break;
					case 5:
					case 6:
						if (intVal > 0) {
%>
		            <input type="checkbox" id="<%= attributeAndType.attributeType.name %>" name="<%= attributeAndType.attributeType.name %>" value="1" checked="1">
<%
						} else {
%>
		            <input type="checkbox" id="<%= attributeAndType.attributeType.name %>" name="<%= attributeAndType.attributeType.name %>">
<%
						}
						break;
					}
%>
		             </td>
		        </tr>
<%
			}
		}
%>
    </table>

    <br/>
    <br/>

    <fmt:message key="prompt;comments"/><br/>
    <textarea rows="5" cols="42" name="comments"><%=
       HTMLUtils.escapeHtmlValues(hd != null?hd.getComments():"")
    %></textarea>
    <br/><br/>
	<!-- select harvest to continue BEGINS -->
	<fmt:message key="prompt;harvest.only.unfinished.domains"/>
	<select name="<%= Constants.OLDSNAPSHOT_PARAM %>" size="1">
	<%
	String selectedPrevious="";
	String noneSelected="";
	if (oldHarvestOid == null) {
	    noneSelected="selected ";
	}
	%>
	<option <%=noneSelected%>value=''><fmt:message key="none"/></option>
	<% 
	for (SparseFullHarvest previousHarvest
            : HarvestDefinitionDAO.getInstance().getAllSparseFullHarvestDefinitions()) {
        if (previousHarvest.getName().equals(harvestName)) {
            continue;
        }
        if (oldHarvestOid != null && oldHarvestOid.equals(previousHarvest.getOid())) {
            selectedPrevious = "selected ";
        }
        %>
        <option <%=selectedPrevious%>value='<%=previousHarvest.getOid() %>'><%=HTMLUtils.escapeHtmlValues(previousHarvest.getName())%></option>
        <%
        // reset selectedPrevious value
        selectedPrevious="";
	}
	%>
    </select>
    <!--   select harvest to continue ENDS -->
    <br/>
    <input type="submit" value="<fmt:message key="save"/>">
</form>
<%
    HTMLUtils.generateFooter(out);
%>
