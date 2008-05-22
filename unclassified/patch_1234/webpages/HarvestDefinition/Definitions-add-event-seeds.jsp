�<%--
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
This page is used to add a (potenitially) large number of seeds to an event harvest.
Parameters:
harvestName:
          the harvest to add the seeds to, must be name of a known harvest
update:
          if null, the page just displays a form for input. If not null, the backing
          method  EventHarvest.addConfigurations is called to process the seeds to be added
seeds:
          A whitespace-separated list of seed urls to be added
orderTemplate:
          The name of the order template to use with these seeds

This page has major side effects in that it will:
1) Create any unknown domains present in the seedlist
2) Create for every seedlist a configuration and seedlist formed from the
name of the harvest and the orderTemplate and add that configuration to the
harvest.
--%><%@ page import="java.util.Iterator,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.PartialHarvest,
                 dk.netarkivet.harvester.datamodel.TemplateDAO,
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.webinterface.EventHarvest"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    String harvestName = request.getParameter(Constants.HARVEST_PARAM);
    if (harvestName == null) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg.missing.parameter",
                Constants.HARVEST_PARAM);
        return;
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
    if (request.getParameter(Constants.UPDATE_PARAM) != null
            && request.getParameter(Constants.UPDATE_PARAM).length() > 0) {
        try {
            EventHarvest.addConfigurations(pageContext, I18N, harvest);
        } catch (ForwardedToErrorPage e) {
            HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                    "errormsg.error.while.adding.seeds", e);
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

<form action="Definitions-add-event-seeds.jsp" method="post">
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
                <textarea name="<%= Constants.SEEDS_PARAM %>"
                          rows="20" cols="60"></textarea>
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
        <%--<tr>
            <td>Maximal load rate(default 60 fetches/min): </td>
            <td><input name="maxRate" size="4" /></td>
        </tr>
        <tr>
            <td>Max objects fetched per domain (default 2000): </td>
            <td><input name="maxObjects" size="4" /></td>
        </tr>--%>
        <tr>
            <td colspan="2"><input type="submit"
                                   value="<fmt:message key="insert"/>"/></td>
        </tr>
    </table>
</form>
<%
    HTMLUtils.generateFooter(out);
%>