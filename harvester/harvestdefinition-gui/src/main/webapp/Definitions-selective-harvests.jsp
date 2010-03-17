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
This page is a summary page displaying a list of all selective harvests known to
the system.
Parameters:
flipactive=<definitionName>
If set, the harvest-definition with the given name is changed from active to
inactive or vice-versa.
--%><%@ page import="dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.datamodel.HarvestDefinition,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.SparsePartialHarvest,
                 dk.netarkivet.harvester.webinterface.Constants"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);

    private static final String COMMANDWIDTH="12%";

%><%
    HTMLUtils.setUTF8(request);
    HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
    String flipactive = request.getParameter(Constants.FLIPACTIVE_PARAM);
    // Change activation if requested
    if (flipactive != null) {
        HarvestDefinition hd = dao.getHarvestDefinition(flipactive);
        if (hd == null) {
            HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                    "errormsg;harvestdefinition.0.does.not.exist",
                    flipactive);
            return;
        } else {
        	// disallow going to active mode, if no domainconfigurations 
        	// associated with this harvestdefinition
        	if (!hd.getActive()) {
        		if (!hd.getDomainConfigurations().hasNext()) {
					HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                    	"errormsg;harvestdefinition.0.cannot.be.activated;"
                    	+ "no.domains.selected.for.harvesting", hd.getName());
            	return;
            	}
        	}
	        hd.setActive(!hd.getActive());
            HarvestDefinitionDAO.getInstance().update(hd);
            response.sendRedirect("Definitions-selective-harvests.jsp");
            return;
        }
    }
    HTMLUtils.generateHeader(pageContext);
%>

<h3 class="page_heading"><fmt:message key="pagetitle;selective.harvests"/></h3>
<%
Iterable<SparsePartialHarvest> isph = dao.getAllSparsePartialHarvestDefinitions();
if (!isph.iterator().hasNext()) { %>
   <fmt:message key="harvestdefinition.selective.no.harvestdefinition"/>
   <p>
<% } else { %>
  <table class="selection_table" cols="6">
    <tr>
        <th><fmt:message key="harvestdefinition.selective.header.harvestdefinition"/></th>
        <th><fmt:message key="harvestdefinition.selective.header.numberofruns"/></th>
        <th><fmt:message key="harvestdefinition.selective.header.nextrun"/></th>
        <th><fmt:message key="harvestdefinition.selective.header.status"/></th>
        <th colspan="4"><fmt:message key="harvestdefinition.selective.header.commands"/></th>
    </tr>

    <%
        // Build the HTML page
        int rowCount = 0;
        boolean inclHistory = SiteSection.isDeployed(Constants.HISTORY_SITESECTION_DIRNAME);

        for (SparsePartialHarvest sph : isph) {
            String name = sph.getName();
            String editLink = "Definitions-edit-selective-harvest.jsp?"
                    + Constants.HARVEST_PARAM + "="
                    + HTMLUtils.encode(name);
            String historicLink = "/History/Harveststatus-perhd.jsp?"
                    + Constants.HARVEST_PARAM + "="
                    + HTMLUtils.encode(name);
            String seedsLink = "/History/Harveststatus-seeds.jsp?"
                + Constants.HARVEST_PARAM + "="
                + HTMLUtils.encode(name);

            String isActive;
            String flipActiveText;
            if (sph.isActive()) {
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
                    + sph.getOid() + "').submit(); return false;\">"
                    + HTMLUtils.escapeHtmlValues(flipActiveText)
                    + "</a>";
    %>
    <tr class="<%=HTMLUtils.getRowClass(rowCount++)%>">
        <td><%=HTMLUtils.escapeHtmlValues(name)%></td>
        <td><%=sph.getNumEvents()%></td>         
        <td>
        <% // Only output the date, if the HarvestDefinition is active
        if (sph.isActive()) { %>
           <fmt:formatDate type="both" value="<%=sph.getNextDate()%>"/>
        <% } else { out.print(Constants.NoNextDate); } %>
        </td>
        <td width="<%=COMMANDWIDTH%>"><%=HTMLUtils.escapeHtmlValues(isActive)%></td>
        <td width="<%=COMMANDWIDTH%>"><form 
                           id="flipActiveForm<%=sph.getOid()%>" 
                           action="Definitions-selective-harvests.jsp" 
                           method="post"
                        ><input 
                           type="hidden" 
                           name="<%=Constants.FLIPACTIVE_PARAM%>" 
                           value="<%=HTMLUtils.escapeHtmlValues(sph.getName())%>"
                         /><%=flipactiveLink%>
                        </form>
        </td>
        <td width="<%=COMMANDWIDTH%>">
            <a href="<%=HTMLUtils.escapeHtmlValues(editLink)%>">
                <fmt:message key="edit"/>
            </a>
        </td>
        <td width="<%=COMMANDWIDTH%>">
            <a href="<%=HTMLUtils.escapeHtmlValues(seedsLink)%>">
                <fmt:message key="harvestdefinition.linktext.seeds"/>
            </a>
        </td>
        <td width="<%=COMMANDWIDTH%>">
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
} //else (if no harvest)
%>
<a href="Definitions-edit-selective-harvest.jsp?createnew=1">
        <fmt:message key="create.new.selective.harvestdefinition"/>
</a>
<% 
HTMLUtils.generateFooter(out);
%>