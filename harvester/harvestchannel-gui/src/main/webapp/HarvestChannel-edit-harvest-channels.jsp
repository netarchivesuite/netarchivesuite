<%--
File:       $Id: HarvestChannel-edit-harvest-channels.jsp 2254 2012-02-09 07:28:35Z mss $
Revision:   $Revision: 2254 $
Author:     $Author: mss $
Date:       $Date: 2012-02-09 08:28:35 +0100 (Thu, 09 Feb 2012) $

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

--%>

<%--
This page provides user functionality to review, create and delete harvest channels.
Additionally it allows the user to map harvests to channels.
--%>

<%-- Java imports --%>
<%@page import="dk.netarkivet.harvester.datamodel.HarvestDefinition"%>
<%@page import="dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO"%>
<%@page import="dk.netarkivet.harvester.webinterface.HarvestChannelAction"%>
<%@page import="dk.netarkivet.harvester.datamodel.HarvestChannel"%>
<%@page import="java.util.Iterator"%>
<%@page import="dk.netarkivet.harvester.datamodel.HarvestChannelDAO"%>
<%@page import="dk.netarkivet.common.utils.I18n"%>
<%@page import="dk.netarkivet.common.webinterface.HTMLUtils"%>
<%@page pageEncoding="UTF-8" %>

<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"/>
<fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%>

<script type="text/javascript">
    function show_block(id) {
        document.getElementById(id).style.display="block";
    }
    function show_inline(id) {
        document.getElementById(id).style.display="inline";
    }
    function hide(id) {
        document.getElementById(id).style.display="none";
    }
</script>

<%
    HTMLUtils.setUTF8(request);
    // First do any request updates
    HarvestChannelAction.processRequest(pageContext, I18N);
    HTMLUtils.generateHeader(pageContext);
%>

<h3 class="page_heading"><fmt:message key="edit.harvest.channels.channel.list"/></h3>

<table class="selection_table" cols="3">
    <tr>
        <th><fmt:message key="harvest.channel.name"/></th>
        <th><fmt:message key="harvest.type"/></th>
        <th><fmt:message key="harvest.channel.comments"/></th>
    </tr>

<%
    HarvestChannelDAO dao = HarvestChannelDAO.getInstance();
    Iterator<HarvestChannel> chanIter = dao.getAll(true);
    int rowCount = 0;
    while (chanIter.hasNext()) {
    	HarvestChannel channel = chanIter.next();
    	String typeKey = (channel.isSnapshot() 
    			? "harvest.channel.type.broad"
    					: "harvest.channel.type.focused");
    	String channelName = channel.getName(); 
    	if (channel.isDefault()) {
    			channelName = "<b>" + channelName + "</b>";
    	}
    	
    	String channelComments = channel.getComments();
    	if (channel.isSnapshot()) {
			channelComments = HarvestChannel.getSnapshotDescription(pageContext);
		}
    	
%>

<tr class="<%=HTMLUtils.getRowClass(rowCount++)%>">
    <td><%= channelName %></td>
    <td><fmt:message key="<%= typeKey %>"/></td>
    <td><%=channelComments%></td>
</tr>

<%  } %>
</table>
<p><i><fmt:message key="edit.harvest.channels.table.legend"/></i></p>

<%
   String formId = "harvestChannelForm";
   String showLinkId = "showHarvestChannelForm";
   String hideLinkId = "hideHarvestChannelForm";
%>
<a id="<%= showLinkId %>" onclick="show_block('<%=formId%>');show_inline('<%=hideLinkId%>');hide('<%=showLinkId%>');"><fmt:message key="edit.harvest.channels.show.create.dialog"/> </a>
<a id="<%= hideLinkId %>" style="display:none;" onclick="hide('<%=formId%>');hide('<%=hideLinkId%>');show_inline('<%=showLinkId%>');"><fmt:message key="edit.harvest.channel.hide"/> </a>
<div id="<%= formId %>" style="display:none;">
    <form method="post" 
          action="./HarvestChannel-edit-harvest-channels.jsp">
          <input type="hidden" 
                 id="<%=HarvestChannelAction.ACTION%>" 
                 name="<%=HarvestChannelAction.ACTION%>" 
                 value="<%=HarvestChannelAction.ActionType.createHarvestChannel.name()%>"/>
          <table border="0">
            <tr>
                <td><fmt:message key="harvest.channel.name"/></td>
                <td><input id="<%=HarvestChannelAction.CHANNEL_NAME%>" name="<%=HarvestChannelAction.CHANNEL_NAME%>" value=""/></td>
            </tr>            
            <tr>
                <td><fmt:message key="harvest.channel.comments"/></td>
                <td><textarea cols="30" rows="4" id="<%=HarvestChannelAction.COMMENTS%>" name="<%=HarvestChannelAction.COMMENTS%>"></textarea></td>
            </tr>
            <tr>
                <td><input type="submit" id="create" name="create" value="<fmt:message key="edit.harvest.channels.create.dialog.submit"/>" /></td>
                <td>&nbsp;</td>
            </tr>
          </table>
    </form>
</div>

<%
    HTMLUtils.generateFooter(out);
%>
