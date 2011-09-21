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

--%>
<%--
This is a summary page displaying all known schedules. It takes no
parameters.
--%><%@ page import="dk.netarkivet.common.utils.I18n,
               	dk.netarkivet.common.webinterface.HTMLUtils,
                dk.netarkivet.harvester.Constants,
                java.util.List,
                dk.netarkivet.harvester.datamodel.Schedule,
                dk.netarkivet.harvester.datamodel.ScheduleDAO,
                dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO,
				dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO,
				dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField,
				dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes,
				dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypeDAO,
				dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypeDBDAO,
				dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldType,
				dk.netarkivet.harvester.webinterface.ExtendedFieldDefinition"			
				pageEncoding="UTF-8" 
%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"/>
<fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/>
<%!
    private static final I18n I18N
            = new I18n(Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    HTMLUtils.generateHeader(
            pageContext);
%>

<h3 class="page_heading"><fmt:message key="pagetitle;extendedfields"/></h3>

<%
ExtendedFieldTypeDAO typedao = ExtendedFieldTypeDBDAO.getInstance();
List<ExtendedFieldType> typelist = typedao.getAll();
if (typelist.size() == 0) { %>
   <fmt:message key="extendedfields.no.extendedfieldtypes"/>
   <p/>
<% } else {
    for (ExtendedFieldType type : typelist) {
    	ExtendedFieldDAO extdao = ExtendedFieldDBDAO.getInstance();
    	List<ExtendedField> extlist = extdao.getAll(type.getExtendedFieldTypeID());
    	String name = type.getName();
   		%>
        <table class="selection_table" cols="2">
        <th colspan="3"><%=name%></th>
   		<%

        if (extlist.size() == 0) { %>
            <tr class="row0">
                <td colspan="3"><fmt:message key="extendedfields.no.extendedfields"/></td>
            </tr>
        <p>
        <% } else {
            for (ExtendedField field : extlist) {
                %>
                <tr class="row0">
                    <td><%= HTMLUtils.escapeHtmlValues(field.getName()) %></td>
                    <td width="15%">
                        <a href="Definitions-edit-extendedfield.jsp?<%=
                        ExtendedFieldDefinition.EXTF_ACTION
                        %>=<%=ExtendedFieldDefinition.EXTF_ACTION_READ%>&<%=
                        ExtendedFieldDefinition.EXTF_ID
                        %>=<%=field.getExtendedFieldID()%>&<%=
                        ExtendedFieldDefinition.EXTF_TYPE_ID
                        %>=<%=field.getExtendedFieldTypeID()%>">
                        <fmt:message key="edit"/></a></td>
                    <td width="15%">
                        <a href="Definitions-edit-extendedfield.jsp?<%=
                        ExtendedFieldDefinition.EXTF_ACTION
                        %>=<%=ExtendedFieldDefinition.EXTF_ACTION_DELETE%>&<%=
                        ExtendedFieldDefinition.EXTF_ID
                        %>=<%=field.getExtendedFieldID()%>&<%=
                            ExtendedFieldDefinition.EXTF_TYPE_ID
                            %>=<%=field.getExtendedFieldTypeID()%>">
                        <fmt:message key="extendedfields.delete"/></a></td>
                </tr>
                <%
            }
        }
   		%>
   		</table>
   		<%    		
    	
    	%>
        <a href="Definitions-edit-extendedfield.jsp?<%=
            ExtendedFieldDefinition.EXTF_ACTION
            %>=<%=ExtendedFieldDefinition.EXTF_ACTION_CREATE%>&<%=        	
        	ExtendedFieldDefinition.EXTF_TYPE_ID
        %>=<%=type.getExtendedFieldTypeID()%>">
        <fmt:message key="extendedfields.create.extendedfield"/></a>
        <p/>
        <%
    }
}

HTMLUtils.generateFooter(out);
%>