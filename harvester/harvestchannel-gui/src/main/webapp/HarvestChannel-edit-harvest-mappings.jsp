<%--
File:       $Id: HarvestChannel-edit-harvest-mappings.jsp 2254 2012-02-09 07:28:35Z mss $
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
<%@page import="dk.netarkivet.harvester.webinterface.CookieUtils"%>
<%@page import="dk.netarkivet.harvester.datamodel.SparseFullHarvest"%>
<%@page import="dk.netarkivet.harvester.webinterface.Constants"%>
<%@page import="dk.netarkivet.harvester.datamodel.HarvestChannel"%>
<%@page import="dk.netarkivet.harvester.datamodel.SparsePartialHarvest"%>
<%@page import="java.util.Iterator"%>
<%@page import="dk.netarkivet.harvester.datamodel.HarvestChannelDAO"%>
<%@page import="dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO"%>
<%@page import="dk.netarkivet.harvester.datamodel.HarvestDefinition"%>
<%@page import="dk.netarkivet.harvester.webinterface.HarvestChannelAction"%>
<%@page import="dk.netarkivet.common.utils.I18n"%>
<%@page import="dk.netarkivet.common.webinterface.HTMLUtils"%>
<%@page pageEncoding="UTF-8" %>

<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"/>
<fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%>

<%
    HTMLUtils.setUTF8(request);
    // First do any request updates
    HarvestChannelAction.processRequest(pageContext, I18N);
    HTMLUtils.generateHeader(pageContext, "jquery-1.10.2.min.js");
    
    HarvestDefinitionDAO harvestDao = HarvestDefinitionDAO.getInstance();
    HarvestChannelDAO channelDao = HarvestChannelDAO.getInstance();
%>

<script type="text/javascript">

var lastHarvestId = -1;
var lastLinkTdContents;
var lastChannel;

function onClickEditChannel(harvestId, isSnapshot) {
	lastHarvestId = harvestId
	lastLinkTdContents = $("#linkTd" + lastHarvestId).children()[0];
	lastChannel = $("#formTd" + lastHarvestId).text();
	$.post("mapHarvestToChannel",
	       {harvestId : harvestId, snapshot : isSnapshot},
	       onClickEditChannelCallback);
}

function onClickEditChannelCallback(result) {
	var editTd = $("#formTd" + lastHarvestId);
	editTd.empty(); 
	editTd.append(result);
	
	var linkTd = $("#linkTd" + lastHarvestId);
	linkTd.empty();
}

function onClickCancelEditChannel() {
    var editTd = $("#formTd" + lastHarvestId);
    editTd.empty();
    editTd.text(lastChannel);
    
    var linkTd = $("#linkTd" + lastHarvestId);
    linkTd.empty();
    linkTd.append(lastLinkTdContents);
}

</script>

<h3 class="page_heading"><fmt:message key="edit.harvest.channels.mapping.list"/></h3>

<% 

  boolean showInactiveHDs = Boolean.parseBoolean(
		  CookieUtils.getParameterValue(request, Constants.SHOW_INACTIVE_PARAM));
   String flipShowHideInactiveLink = "HarvestChannel-edit-harvest-mappings.jsp?" 
		    + Constants.SHOW_INACTIVE_PARAM
		    + "=" + !showInactiveHDs;
   CookieUtils.setCookie(
		   response, 
		   Constants.SHOW_INACTIVE_PARAM, 
		   Boolean.toString(showInactiveHDs));
%>
   <a href="<%=HTMLUtils.escapeHtmlValues(flipShowHideInactiveLink)%>">
<% if (showInactiveHDs) { %>
        <fmt:message key="harvestdefinition.selective.hide.inactive"/>
<% } else { %>
        <fmt:message key="harvestdefinition.selective.show.inactive"/>
<% } %>
   </a>

<table class="selection_table" cols="5">
    <tr>
        <th width="30%"><fmt:message key="harvestdefinition.selective.header.harvestdefinition"/></th>
        <th width="15%"><fmt:message key="harvestdefinition.selective.header.nextrun"/></th>
        <th><fmt:message key="harvestdefinition.selective.header.status"/></th>
        <th width="20%"><fmt:message key="harvest.channel"/></th>
        <th><fmt:message key="edit.harvest.mappings.header.commands"/></th>
    </tr>
    
<%
    Iterator<SparsePartialHarvest> phIter = 
        harvestDao.getSparsePartialHarvestDefinitions(!showInactiveHDs).iterator();
    HarvestChannel defaultFocused = channelDao.getDefaultChannel(false);
    int rowCount = 0;
    while (phIter.hasNext()) {
    	SparsePartialHarvest ph = phIter.next();
    	HarvestChannel chan = channelDao.getChannelForHarvestDefinition(ph.getOid());  
    	if (chan == null) {
    		chan = defaultFocused;
    	}
    	String isActive = I18N.getString(response.getLocale(),
                ph.isActive() ? "active" : "inactive");
%>
    <tr class="<%=HTMLUtils.getRowClass(rowCount++)%>">
        <td><%=HTMLUtils.escapeHtmlValues(ph.getName())%></td>
        <td>
        <% // Only output the date, if the HarvestDefinition is active
        if (ph.isActive()) { %>
           <%= HTMLUtils.parseDate(ph.getNextDate())%>
        <% } else { out.print(Constants.NoNextDate); } %>
        </td>
        <td><%=HTMLUtils.escapeHtmlValues(isActive)%></td>
        <td id="formTd<%=ph.getOid()%>"><%=chan.getName()%></td>
        <td id="linkTd<%=ph.getOid()%>">
        <a href="#" onClick="onClickEditChannel(<%=ph.getOid()%>, false); return false">
        <fmt:message key="edit.harvest.mappings.edit.link"/>
        </a></td>
    </tr>
<%
    }
%>
    
</table>

<%
    HTMLUtils.generateFooter(out);
%>