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
This page is used to edit a new or pre-existing domain configuration.
Parameters:

name:
    the domain-name in which this configuration lives. Must be present and
    must correspond to a known domain.
update:
    If present and not empty, process the posted parameters. Otherwise just
    display forms for input.
edition:
    The edition of the domain this config was read from.
configName:
    The name of the configuration to update. If this is not present, an input
    field is displayed where the name of the new configuration can be entered.
    If update is present, this field must be non-empty.
order_xml:
    The name of the template to use in this configuration. If update is present
    this must be present and must refer to a known template
urlListList:
    May be present multiple times. If update is present, must be at least one
    occurrence. All occurrences must be names of known seedlists for the
    relevant domain.
maxRate:
    Request rate for configuration
maxObjects:
    Object limit for configuration
maxBytes:
    Byte limit for configuration
passwordList:
    Currently ignored


--%><%@ page import="java.text.NumberFormat,
                 java.util.HashSet,
                 java.util.Iterator,
                 java.util.Locale,
                 java.util.Set,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.Domain,
                 dk.netarkivet.harvester.datamodel.DomainConfiguration,
                 dk.netarkivet.harvester.datamodel.DomainDAO,
                 dk.netarkivet.harvester.datamodel.SeedList,
                 dk.netarkivet.harvester.datamodel.TemplateDAO,
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.webinterface.DomainConfigurationDefinition"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    // update is set to non-null when the page posts data to itself. When
    // null, no updating is done and the form content is just displayed
    try {
        HTMLUtils.forwardOnEmptyParameter(pageContext, Constants.DOMAIN_PARAM);
        DomainConfigurationDefinition.processRequest(pageContext, I18N);
    } catch (ForwardedToErrorPage e) {
        return;
    }
    String domainName = request.getParameter(Constants.DOMAIN_PARAM);
    DomainDAO ddao = DomainDAO.getInstance();
    if (!ddao.exists(domainName)) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;unknown.domain.0", domainName);
        return;
    }
    Domain domain = ddao.read(domainName);
    // If we have updated the configuration, go back to the main domain page
    if (request.getParameter(Constants.UPDATE_PARAM) != null) {
        response.sendRedirect("Definitions-edit-domain.jsp?"
                + Constants.DOMAIN_PARAM + "="
                + HTMLUtils.encode(domainName));
        return;
    }
    String configName = request.getParameter(Constants.CONFIG_NAME_PARAM);
    DomainConfiguration dc = null;
    if (configName != null) {
        if (!domain.hasConfiguration(configName)) {
            HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                    "errormsg;unknown.configuration.0", configName);
            return;
        }
        dc = domain.getConfiguration(configName);
    }
    HTMLUtils.generateHeader(pageContext);
%>

<%--
Display all the form information for this domain
--%>

<h3 class="page_heading">
    <%=HTMLUtils.escapeHtmlValues(domainName)%>
</h3>

<form method="post" action="./Definitions-edit-domain-config.jsp">

<input type="hidden" name="<%=Constants.DOMAIN_PARAM%>"
       value="<%=HTMLUtils.escapeHtmlValues(domainName)%>"/>
<input type="hidden" name="<%=Constants.UPDATE_PARAM%>" value="1"/>
<input type="hidden" name="<%=Constants.EDITION_PARAM%>"
       value="<%= domain.getEdition() %>"/>

<div id="configuration">

<%-- table for selecting/editing configurations --%>
<div class="edit_box">
<table cellspacing="2" class="selection_table">
<th colspan="2">
    <fmt:message key="harvestdefinition.config.edit.editConfig"/>
</th>
<%
    // Prefill values for editing an existing configuration. Cannot
    // change the name of an existing configuration
    String nameString = "";
    String currentTemplate = "";
    String load = "";
    String maxObjects = "";
    String maxBytes = "";
    if (dc != null) {
        nameString = "value=\"" + HTMLUtils.escapeHtmlValues(configName)
                     + "\" readonly=\"readonly\"";
        load = "value=\"" + dc.getMaxRequestRate() + "\"";
        maxObjects = "value=\"" +
                     HTMLUtils.localiseLong(dc.getMaxObjects(), pageContext)
                     + "\"";
        maxBytes = "value=\""
                   + HTMLUtils.localiseLong(dc.getMaxBytes(), pageContext) 
                   + "\"";
        currentTemplate = dc.getOrderXmlName();
    }
%>
<tr>
    <td>
        <%-- First column is a two-column table of input fields --%>
        <table>
            <tr>
                <td><fmt:message key="prompt;name"/> </td>
                <td><span id="focusElement">
                        <input name="<%=Constants.CONFIG_NAME_PARAM%>" size="50"
                            <%=nameString%>/>
                    </span>
                </td>
            </tr>
            <tr>
                <td><fmt:message key="prompt;harvest.template"/> </td>
                <td><select name="<%=Constants.ORDER_XML_NAME_PARAM%>">
                    <%
                        Iterator<String> templates =
                                TemplateDAO.getInstance().getAll();
                        while (templates.hasNext()) {
                            String selected = "";
                            String template = templates.next();
                            if (currentTemplate.equals(template)) {
                                selected = "selected = \"selected\"";
                            }
                            out.println("<option value=\"" +
                                        HTMLUtils.escapeHtmlValues(template)
                                        + "\"" + selected + ">" +
                                        HTMLUtils.escapeHtmlValues(template)
                                        + "</option>");
                        }
                    %>
                </select>
                </td>
            </tr>
            <input name="<%=Constants.MAX_RATE_PARAM%>" type="hidden" <%=load%> />
            <tr>
                <td><fmt:message key="prompt;maximum.number.of.objects"/> </td>
                <td><input name="<%=Constants.MAX_OBJECTS_PARAM%>" size="20" <%=maxObjects%> /></td>
            </tr>
            <tr>
                <td><fmt:message key="prompt;maximum.number.of.bytes"/> </td>
                <td>
                    <input name="<%=Constants.MAX_BYTES_PARAM%>" size="20" <%=maxBytes%> />
                </td>
            </tr>
            <tr>
                <td colspan="2"><fmt:message key="prompt;comments"/> </td>
            </tr>
            <tr>
                <td colspan="2">
                    <textarea name="<%=Constants.COMMENTS_PARAM%>" rows="5" cols="42"><%=
                         HTMLUtils.escapeHtmlValues(dc != null?dc.getComments():"")
                    %></textarea>
                </td>
            </tr>
        </table>
    </td>
    <%-- Second element is also a two-column table containing mulitple-selects for the url-lists and passwords --%>
    <td>
        <table>
            <tr>
                <td><fmt:message key="prompt;seed.list"/> <br/>
                    <select name="<%=Constants.URLLIST_LIST_PARAM%>" multiple="multiple" size="8">
                    <%-- list of url list options --%>
                    <%
                        Iterator<SeedList> allSeedListsIt
                                = domain.getAllSeedLists();
                        //A set to contain all seedlists for this domain
                        Set<SeedList> allSeedLists = new HashSet<SeedList>();
                        //A set to contain all seedlists currently in this
                        //configuration
                        Set<SeedList> actualSeedLists = new HashSet<SeedList>();
                        while (allSeedListsIt.hasNext()) {
                            allSeedLists.add(allSeedListsIt.next());
                        }
                        if (dc != null) {
                            Iterator<SeedList> actualSeedListsIt
                                    = dc.getSeedLists();
                            while (actualSeedListsIt.hasNext()) {
                                actualSeedLists.add(actualSeedListsIt.next());
                            }
                        } else  {
                        	// When creating a new domain configuration:
                        	// If only one seed to choose from, preselect this
                        	// one by adding the one seedList to the list 
                        	// of actual seeds.
                        	if (allSeedLists.size() == 1) { 
   								actualSeedLists.addAll(allSeedLists);
   							}
                        }
   						
   						//Display multi-select showing all seedlists with
                        //actual seedlists pre-selected
                        for (SeedList sl : allSeedLists) {
                            String selected = "";
                            if (actualSeedLists.contains(sl)) {
                                selected = "selected=\"selected\"";
                            }
                            out.print("<option value=\""
                                      + HTMLUtils.escapeHtmlValues(sl.getName())
                                      + "\" " + selected + ">"
                                      + HTMLUtils.escapeHtmlValues(sl.getName())
                                      + "</option>\n");
                        }
                    %>
                </select></td>
            </tr>
        </table>
    </td>
</tr>
</table>
</div>
</div>
<br/>
<input type="submit" value="<fmt:message key="save"/>"/>
</form>

<%
    HTMLUtils.generateFooter(out);
%>