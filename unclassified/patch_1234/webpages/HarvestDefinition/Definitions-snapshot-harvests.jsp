<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark

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
Summary page displaying all snapshot harvests in the system.
Parameter:
flipactive=<harvestDefinition>:
        If set, the given harvestDefinition is flipped from active to inactive
        or vice-versa.
--%><%@ page import="dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.SparseFullHarvest, dk.netarkivet.harvester.webinterface.Constants, dk.netarkivet.harvester.webinterface.SnapshotHarvestDefinition"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final String HISTORY_SITESECTION_DIRNAME = "History";
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    try {
        if (SnapshotHarvestDefinition.flipActive(pageContext, I18N)) {
            response.sendRedirect("Definitions-snapshot-harvests.jsp");
            return;
        }
    } catch (ForwardedToErrorPage e) {
        return;
    }
    HTMLUtils.generateHeader(pageContext);
%>

<h3 class="page_heading"><fmt:message key="pagetitle;snapshot.harvests"/></h3>
<%
HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
Iterable<SparseFullHarvest> ihd = dao.getAllSparseFullHarvestDefinitions();
if (!ihd.iterator().hasNext()) { %>
   <fmt:message key="harvestdefinition.snapshot.no.harvestdefinition"/>
   <p>
<% } else { %>
<table class="selection_table">
    <tr>
        <th><fmt:message key="harvestdefinition.snapshot.header.harvestdefinition"/></th>
        <th><fmt:message key="harvestdefinition.snapshot.header.maxbytes"/></th>
        <th><fmt:message key="harvestdefinition.snapshot.header.status"/></th>
        <th colspan="3"><fmt:message key="harvestdefinition.snapshot.header.commands"/></th>
    </tr>
    <%
        // Build the HTML page
        int rowCount = 0;
        boolean inclHistory = SiteSection.isDeployed(HISTORY_SITESECTION_DIRNAME);
        
        for (SparseFullHarvest hd : ihd) {
            String name = hd.getName();
            String editLink = "Definitions-edit-snapshot-harvest.jsp?"
                    + Constants.HARVEST_PARAM + "="
                    + HTMLUtils.encode(hd.getName());
            String historicLink = "/History/Harveststatus-perhd.jsp?"
                    + Constants.HARVEST_PARAM + "="
                    + HTMLUtils.encode(name);

            String isActive;
            String flipActiveText;
            if (hd.isActive()) {
                isActive = I18N.getString(response.getLocale(),
                        "active");
                flipActiveText = I18N.getString(response.getLocale(),
                        "deactivate");
            } else {
                isActive = I18N.getString(response.getLocale(),
                        "inactive");
                flipActiveText = I18N.getString(response.getLocale(),
                        "activate");
            }
            String flipactiveLink
                    = "<a href=\"\" onclick=\"document.getElementById('flipActiveForm"
                    + hd.getOid() + "').submit(); return false;\">"
                    + HTMLUtils.escapeHtmlValues(flipActiveText) + "</a>";
    %>
    <tr class="<%=HTMLUtils.getRowClass(rowCount++)%>">
        <td><%=HTMLUtils.escapeHtmlValues(name)%></td>
        <td width="15%"><%=hd.getMaxBytes()%></td>
        <td width="15%"><%=HTMLUtils.escapeHtmlValues(isActive)%></td>
        <td width="15%"><form 
                           id="flipActiveForm<%=hd.getOid()%>" 
                           action="Definitions-snapshot-harvests.jsp" 
                           method="post"
                        ><input 
                             type="hidden" 
                             name="<%=Constants.FLIPACTIVE_PARAM%>" 
                             value="<%=HTMLUtils.escapeHtmlValues(hd.getName())%>"
                          /><%=flipactiveLink%>
                        </form>
        </td>
        <td width="15%">
            <a href="<%=HTMLUtils.escapeHtmlValues(editLink)%>">
            	<fmt:message key="edit"/>
            </a>
        </td>
        <td width="10%">
            <% if (inclHistory)  { %>
                <a href="<%=HTMLUtils.escapeHtmlValues(historicLink)%>">
                    <fmt:message key="harvestdefinition.linktext.historical"/></a>
            <% } else { %>
                <fmt:message key="harvestdefinition.linktext.no.historical"/>
            <% } %>
        </td>
    </tr>
    <%
    } //for each harvest
    %>
</table>
<%
} //else (if no harvests)
%>
<a href="Definitions-edit-snapshot-harvest.jsp?<%=Constants.CREATENEW_PARAM%>=1">
          <fmt:message key="create.new.snapshot.harvestdefinition"/>
</a>
<%
HTMLUtils.generateFooter(out);
%>