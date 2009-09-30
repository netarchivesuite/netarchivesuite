<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark

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
This page provides an interface for entering and updating information referring
to a particular selective harvest.

Parameters:
harvestname:
     the name of the harvest definition.  If this is not set, the user is
     allowed to enter a new name, otherwise a harvest definition of that
     name must exist and the name cannot be changed.

The following parameters are posted by the page to itself:

update:
     if defined, the harvest definition database will be updated from the
     submitted fields.
createnew:
     Set when creating new harvest, to indicate that rather than throwing a
     fit when no harvest exists called harvestname, it should be created.  This
     must only be set when update is set.
domainlist:
     A new line separated list of domains to be added for this
     harvest-definition.
     Domains are always added with their default configuration.
schedulename:
     The name of schedule to use
unknownDomains:
     A list of unrecognised domains. The user is presented with a dialogue from
     which these may be created
nextdate:
    If set, will change the next date to run to this date.
save:
     Set if we are finished editing this harvest definition, in which case we do
     any updates and redirect to the Definitions-selective-harvests.jsp page.  This may
     be unset even when update is set if we are adding configurations or
     seeds.
addDomains:
     Add the domains specified in unknowndomains. This has the sideeffect of
     first creating these domains. Note: ONLY set on adding after unknowndomains
     are found!
deleteconfig:
     If present, it must contain a domain name followed by a colon (:) and
     a configuration name on that domain. the given configuration from the
     given domain is deleted from the harvest.


DomainConfigurations are posted as pairs
<Constants.DOMAIN_IDENTIFIER>domainName=configurationName
    These configurations are added/included in the harvest definition

--%><%@ page import="java.util.ArrayList,
                 java.util.List,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.utils.StringUtils,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.DomainDAO,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.Schedule,
                 dk.netarkivet.harvester.datamodel.ScheduleDAO,
                 dk.netarkivet.harvester.datamodel.SparseDomain,
                 dk.netarkivet.harvester.datamodel.SparseDomainConfiguration,
                 dk.netarkivet.harvester.datamodel.SparsePartialHarvest,
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.webinterface.SelectiveHarvest"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);

    HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
    // Update all relevant HD data from request, some results are saved in
    // string buffers
    List<String> unknownDomains = new ArrayList<String>();
    List<String> illegalDomains = new ArrayList<String>();
    try {
        SelectiveHarvest.processRequest(pageContext, I18N,
                unknownDomains, illegalDomains);
    } catch (ForwardedToErrorPage e) {
        return;
    }

    //Redirect if we just saved the HD
    if (request.getParameter(Constants.SAVE_PARAM) != null) {
        response.sendRedirect("Definitions-selective-harvests.jsp");
        return;
    }

    // Preload default parameter values
    SparsePartialHarvest hdd = null;

    // Now, unless we are creating a new definition from scratch, we read in the
    // current values from the dao
    String harvestName = request.getParameter(Constants.HARVEST_PARAM);
    if (harvestName != null) {
        hdd = hddao.getSparsePartialHarvest(harvestName);
        if (hdd == null) {
            HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                    "errormsg;harvest.0.does.not.exist", harvestName);
            return;
        }
    }

    HTMLUtils.generateHeader(pageContext);
%>
<jsp:include page="scripts.jsp" flush="true"/>
<h3 class="page_heading"><fmt:message key="pagetitle;selective.harvest"/></h3>

<form method="post" action="Definitions-edit-selective-harvest.jsp">
<%
    // create hidden fields
    if (hdd == null) {
        // the createnew parameter indicates that the harvest is not expected
        // to exist, but should be created upon submit
%>
<input type="hidden" name="<%= Constants.CREATENEW_PARAM %>" value="1"/>
<%
    } else {
%>
<input type="hidden" name="<%= Constants.EDITION_PARAM %>" value="<%=
       hdd.getEdition() %>"/>
<%  } %>
<input type="hidden" name="<%= Constants.UPDATE_PARAM %>" value="1"/>

<h4><fmt:message key="prompt;harvest.name"/>
    <%
        // If we have doCreateNew && !doUpdate then we need to enter a
        // harvestName. Otherwise, resubmit it as a readonly field
        if (hdd != null) {
            out.print("<input type=\"text\" name=\"" + Constants.HARVEST_PARAM
                      + "\" value=\""
                      + HTMLUtils.escapeHtmlValues(harvestName)
                      + "\" readonly=\"readonly\" />  \n");
        } else {
            out.print("<span id=\"focusElement\"><input type=\"text\" name=\""
                      + Constants.HARVEST_PARAM + "\" size=\"20\"/></span>\n");
        }
    %>
</h4>

<fmt:message key="prompt;comments"/><br/>
<textarea rows="5" cols="42" name="<%= Constants.COMMENTS_PARAM %>"><%=
    HTMLUtils.escapeHtmlValues(hdd!=null?hdd.getComments():"")
%></textarea>
<br/><br/>
<fmt:message key="schedule"/>:
<select name="<%= Constants.SCHEDULE_PARAM %>" size="1">
    <%
        String scheduleName = (hdd != null?hdd.getScheduleName():"");

        for (Schedule sch : ScheduleDAO.getInstance()) {
            String selected = "";
            if (sch.getName().equals(scheduleName)) {
                selected = "selected=\"selected\"";
            }
    %>
    <option <%=selected%>>
        <%=HTMLUtils.escapeHtmlValues(sch.getName())%>
    </option>
    <%
        }
    %>
</select>
<%
if (hdd != null) {
%>
<br/>
<%
    if (hdd.getNextDate() == null) {
%>
<fmt:message key="harvest.0.will.never.run">
    <fmt:param value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>
</fmt:message>
<%
    } else if (hdd.isActive()) {
%>
<fmt:message key="harvest.0.will.next.run.1">
    <fmt:param value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>
    <fmt:param value="<%=hdd.getNextDate()%>"/>
</fmt:message>
<%
    } else {
%>
<fmt:message key="inactive.harvest.0.would.run.next.1">
    <fmt:param value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>
    <fmt:param value="<%=hdd.getNextDate()%>"/>
</fmt:message>
<%
    }
%>
<br/>
<fmt:message key="override.with.new.date"/>
<input name="<%=Constants.NEXTDATE_PARAM%>" size="25"
       id="<%=Constants.NEXTDATE_PARAM%>"/>
(<fmt:message key="harvestdefinition.schedule.edit.timeformatDescription"/>)
<script type="text/javascript">
setupNextdateCalendar();
</script>
<%
}
%>
<br/>
<br/>
<table class="selection_table" width="100%">
    <tr>
        <th width="65%"><fmt:message key="domain"/></th>
        <th width="25%"><fmt:message key="choose.configuration"/></th>
        <th width="10%"><fmt:message key="remove.from.list"/></th>
    </tr>
    <%
        // New definitions do not contain any domains
        if (hdd != null) {
            int rowcount = 0;
            Iterable<SparseDomainConfiguration> sparseDomainConfigurations
                    = hddao.getSparseDomainConfigurations(hdd.getOid());
            for (SparseDomainConfiguration dcc : sparseDomainConfigurations) {
                //Switch between grey and white every three lines

                String domainName = dcc.getDomainName();
                SparseDomain sd
                        = DomainDAO.getInstance().readSparse(domainName);

                String link = "Definitions-edit-domain.jsp?"
                        + Constants.DOMAIN_PARAM + "="
                        + HTMLUtils.encode(domainName);
    %>
    <tr class="<%= HTMLUtils.getRowClass(rowcount++) %>">
        <td width="65%">
            <a href="<%= HTMLUtils.escapeHtmlValues(link) %>">
                <%= HTMLUtils.escapeHtmlValues(domainName) %>
            </a>
        </td>
        <td width="25%">
            <select name="<%= Constants.DOMAIN_IDENTIFIER
            + HTMLUtils.escapeHtmlValues(domainName) %>" style="width: 100%">
                <%
                    String activeConfiguration = dcc.getConfigurationName();
                    for (String configurationName
                            : sd.getDomainConfigurationNames()) {
                %>
                <option <%= activeConfiguration.equals(configurationName)
                                ? " selected=\"selected\" " : "" %>>
                    <%= HTMLUtils.escapeHtmlValues(configurationName) %>
                </option>
                <%
                    }
                %>
            </select>
        </td>
        <td width="10%">
            <% // Need to pass both domain name and configuration name.
               // Since domains cannot contain ":", we use that for seperator.
                String deleteValue =
                    dcc.getDomainName() + ":" + dcc.getConfigurationName();
            %>
            <input type="submit"
                   onclick="var e = document.createElement('input'); e.type = 'hidden'; e.name = '<%=Constants.DELETECONFIG_PARAM%>'; e.value='<%=HTMLUtils.escapeJavascriptQuotes(HTMLUtils.escapeHtmlValues(deleteValue))%>'; this.form.appendChild(e); return true;"
                   value="<fmt:message key="remove"/>"/>
        </td>
    </tr>
    <%
            }
        }
    %>
</table>

<%
    if (unknownDomains.size() > 0) {
%>
<br/>
<table class="selection_table">
    <tr>
        <th colspan="2">
            <fmt:message key="unknown.domains.not.added"/>
        </th>
    </tr>
    <tr>
        <td>
            <textarea rows="<%= unknownDomains.size() %>" cols="30"
                name="<%=Constants.UNKNOWN_DOMAINS_PARAM%>"><%=
                HTMLUtils.escapeHtmlValues
                        (StringUtils.conjoin("\n", unknownDomains))
                %></textarea>
        </td>
        <td>
            <input type="submit" name="<%=Constants.ADDDOMAINS_PARAM%>"
                   value="<fmt:message key="create.and.add.to.harvest"/>"/>
        </td>
    </tr>
</table>
<%
    }
%>
<%
    if (illegalDomains.size() > 0) {
%>
<br/>
<table class="selection_table">
    <tr>
        <th>
            <fmt:message key="illegal.domains.not.addable"/>
        </th>
    </tr>
    <tr>
        <td>
            <textarea rows="<%= illegalDomains.size()%>" cols="30"><%=
                HTMLUtils.escapeHtmlValues(StringUtils.conjoin("\n", illegalDomains))
            %></textarea>
        </td>
    </tr>
</table>
<%
    }
%>

<br/>
<br/>
<h4><fmt:message key="prompt;enter.domains.to.add.here"/></h4><br/>
<textarea name="<%= Constants.DOMAINLIST_PARAM %>" rows="5" cols="42">
</textarea>

<br/>
<input type="submit" value="<fmt:message key="add.domains"/>"/>
<br/>
<br/>
<input type="submit" value="<fmt:message key="save"/>"
       name="<%=Constants.SAVE_PARAM%>"/>
</form>

<h4><fmt:message key="prompt;event.harvest"/> </h4>
<%
    if (hdd != null) {
        String seedLink = "Definitions-add-event-seeds.jsp?" + Constants.HARVEST_PARAM
                          + "=" + HTMLUtils.encode(harvestName);
    %>
      <a href="<%= HTMLUtils.escapeHtmlValues(seedLink) %>"><fmt:message key="add.seeds"/></a>
    <%
    } else {
    %>
      <fmt:message key="save.harvest.definition.first"/>
    <%
    }
    HTMLUtils.generateFooter(out);
%>