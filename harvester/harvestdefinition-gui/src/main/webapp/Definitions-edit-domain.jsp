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
This page is the main page used for editing information on internet
domains.
Parameters:
name: required. The name of the domain to be updated
update: if non-empty, the method DomainDefinition.processRequest is called
        to update the domain information in the database. Otherwise this
        page provides access to forms for editing the domain information.
        If the update parameter is non-empty, additional parameters are
        required as follows:
        default: Which configuration is the default
        crawlertraps: String (possibly empty) of crawlertraps for this domain
        comments: Comments (possibly empty) on this domain
        alias: If non-empty, domain that this domain is an alias of.
--%><%@page import="dk.netarkivet.harvester.datamodel.extendedfield.ExtendableEntity"%>
<%@ page import="java.util.Date,
                 java.util.Iterator,
                 java.util.List,
                 java.util.Locale,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.utils.StringUtils,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.datamodel.AliasInfo,
                 dk.netarkivet.harvester.datamodel.Domain,
                 dk.netarkivet.harvester.datamodel.DomainConfiguration,
                 dk.netarkivet.harvester.datamodel.DomainDAO,
                 dk.netarkivet.harvester.datamodel.SeedList, 
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.webinterface.DomainDefinition,
                 dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%>
<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    // update is set to non-null when the page posts data to itself. When
    // null, no updating is done and the form content is just displayed
    String update = request.getParameter(Constants.UPDATE_PARAM);

    if (update != null) {
        // Call the backing method DomainDefinition.processRequest
        try {
            DomainDefinition.processRequest(pageContext, I18N);
        } catch (ForwardedToErrorPage e) {
            return; // Forwarded inside processRequest
        }
    }

    String domainName = request.getParameter(Constants.DOMAIN_PARAM);
    if (domainName == null || domainName.trim().length() == 0) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;no.domain.name");
        return;
    }
    DomainDAO ddao = DomainDAO.getInstance();
    domainName = domainName.trim();
    Domain domain = ddao.read(domainName);
    AliasInfo info = domain.getAliasInfo();
    String alias = "";
    if (info != null) {
        alias = info.getAliasOf();
    }
    List<AliasInfo> aliasesOfThisDomain = ddao.getAliases(domainName);
    int aliasesCountdown = aliasesOfThisDomain.size();
    String[] aliasLinks = new String[aliasesCountdown];
    int pointer = 0;
    String urlPrefix = "<a href=\"Definitions-edit-domain.jsp?"
            + Constants.DOMAIN_PARAM + "=";
    for (AliasInfo a : aliasesOfThisDomain) {
        String domainAsString = a.getDomain();
        aliasLinks[pointer] = new String(
                urlPrefix + HTMLUtils.encode(domainAsString) + "\">"
                        + domainAsString + "</a>");
        pointer = pointer + 1;
    }
    String aliasesString = "&nbsp;" + StringUtils.conjoin(
    	",&nbsp;", aliasLinks);

    // Include JS files for the calendar

    String lang = HTMLUtils.getLocale(request);
    if (lang.length() >= 2) {
        lang = lang.substring(0, 2);
    }

    HTMLUtils.generateHeader(
            pageContext,
            "./jscalendar/calendar.js",
            "./jscalendar/lang/calendar-" + lang + ".js",
            "./jscalendar/calendar-setup.js");%>

<jsp:include page="calendar-scripts.jsp"/>
    
<%--
Display all the form information for this domain
--%>

<form method="post" action="Definitions-edit-domain.jsp">
<input type="hidden" name="<%=Constants.UPDATE_PARAM%>" value="1"/>

<h3 class="page_heading">
    <fmt:message key="pagetitle;edit.domain"/>
</h3>

<div id="configuration">
    <br/>
    <input type="hidden" name="<%= Constants.EDITION_PARAM%>"
           value="<%= domain.getEdition() %>"/>
    <fmt:message key="domain.name"/>:&nbsp; <input type="text"
                             name="<%= Constants.DOMAIN_PARAM%>"
                             size="<%=Constants.DOMAIN_NAME_FIELD_SIZE%>"
                             value="<%=HTMLUtils.escapeHtmlValues(domainName) %>"
                             readonly="readonly"/>
    <br/>
    <fmt:message key="prompt;comments"/><br/>
    <textarea name="<%=Constants.COMMENTS_PARAM%>" rows="5" cols="42"><%=
      HTMLUtils.escapeHtmlValues(domain.getComments())
    %></textarea>
    <br/>
    <% if (aliasesOfThisDomain.size() > 0) {%>
        <fmt:message key="prompt;domains.aliases.of.domain"/><%= aliasesString %><br/>
    <% } else { %>
        <fmt:message key="prompt;alias.of"/>
    <input type="text" name="<%=Constants.ALIAS_PARAM%>" 
    	size="<%=Constants.DOMAIN_NAME_FIELD_SIZE %>" value="<%= alias %>"/>
    <%    if (info != null) {
            Date aliasTimeoutDate =
                    new Date(info.getLastChange().getTime() +
                             dk.netarkivet.harvester.datamodel
                                     .Constants.ALIAS_TIMEOUT_IN_MILLISECONDS);
            Date now = new Date();
            if (aliasTimeoutDate.before(now)) { %>
                <%=HTMLUtils.parseDate(aliasTimeoutDate)%>
    <%        } else { %>
                <%=HTMLUtils.parseDate(aliasTimeoutDate)%>
    <%        } %>
            <br/><input type='radio' name='<%=Constants.RENEW_ALIAS_PARAM%>'
                        value='no' checked="checked"/> <fmt:message key="dont.renew.alias"/>
            <input type='radio' name='<%=Constants.RENEW_ALIAS_PARAM%>'
                   value='yes'/> <fmt:message key="renew.alias"/>
    <%    }
    }
    %>
    <br/>

    <%
    ExtendableEntity extendableEntity = domain;
    int extendedFieldType = ExtendedFieldTypes.DOMAIN;
    %>
    
    <%@ include file="extendedfields_element.jspf" %>
        
    <%
    Locale loc = HTMLUtils.getLocaleObject(pageContext);
    %>

    <%-- table for selecting/editing configurations --%>

  <%
    boolean showUnusedConfigurations = Boolean.parseBoolean(request.getParameter(Constants.SHOW_UNUSED_CONFIGURATIONS_PARAM));
    boolean showUnusedSeeds = Boolean.parseBoolean(request.getParameter(Constants.SHOW_UNUSED_SEEDS_PARAM));
    List<DomainConfiguration> usedDomainConfigurations = null;
    List<DomainConfiguration> shownDomainConfigurations;
    if (!showUnusedConfigurations || !showUnusedSeeds) {
      usedDomainConfigurations = DomainDefinition.getUsedConfiguration(domain, loc);
    }
  %>
  <a href="<%=DomainDefinition.createDomainUrlWithFlippedShowConfigurations(request)%>">
    <% if (showUnusedConfigurations) { %>
    <fmt:message key="domain.configurations.hideunused"/> <%
        shownDomainConfigurations = domain.getAllConfigurationsAsSortedList(loc);
    } else { %>
    <fmt:message key="domain.configurations.showunused"/>
    <%
        shownDomainConfigurations = usedDomainConfigurations;
      } %>
  </a>

    <table class="selection_table">
    	<tr>
        	<th colspan="2"><fmt:message key="configurations"/></th>
        	<th colspan="1"><fmt:message key="default"/></th>
        </tr>
        <%
            int rowcount = 0;
            for (DomainConfiguration conf: shownDomainConfigurations) {
                String rowClass = HTMLUtils.getRowClass(rowcount++);
                String confName = conf.getName();
                String checked = "";
                if (confName.equals(
                        domain.getDefaultConfiguration().getName())) {
                    checked = "checked=\"checked\"";
                        }
                String editUrl = "Definitions-edit-domain-config.jsp?"
                        + Constants.EDIT_CONFIG_PARAM + "1&"
                        + Constants.CONFIG_NAME_PARAM + "="
                        + HTMLUtils.encode(confName) + "&"
                        + Constants.DOMAIN_PARAM + "="
                        + HTMLUtils.encode(domain.getName());
                String seedlistNames = "[";
                for (Iterator<SeedList> i = conf.getSeedLists(); i.hasNext();) {
                    seedlistNames += i.next().getName();
                    if (i.hasNext()) {
                        seedlistNames += ", ";
                    }
                }
                seedlistNames += "]";
        %>
        <tr class="<%= rowClass %>">
        <td><strong><%= HTMLUtils.escapeHtmlValues(confName) %></strong>
            <em>(<%=HTMLUtils.escapeHtmlValues(conf.getOrderXmlName())%>,
                <%=HTMLUtils.escapeHtmlValues(seedlistNames)%>)</em></td>
        <td><a href="<%= HTMLUtils.escapeHtmlValues(editUrl) %>"><fmt:message key="edit"/></a></td>
        <td><input type="radio" name="<%=Constants.DEFAULT_PARAM%>"
                   value="<%= HTMLUtils.escapeHtmlValues(confName) %>" <%=checked%>/> </td>
        </tr>
<%            }
            String newConfUrl = "Definitions-edit-domain-config.jsp?"
                    + Constants.EDIT_CONFIG_PARAM + "=1&"
                    + Constants.DOMAIN_PARAM + "="
                    + HTMLUtils.encode(domain.getName());
%>
            <tr><td><a href="<%= HTMLUtils.escapeHtmlValues(newConfUrl) %>">
                <fmt:message key="new.configuration"/></a></td></tr>
    </table>
</div>
<br/><br/>

<div id="SeedLists">
  <%
    List<SeedList> shownSeedsLists;
  %>
   <a href="<%=DomainDefinition.createDomainUrlWithFlippedShowSeeds(request)%>">
    <% if (showUnusedSeeds) { %>
    <fmt:message key="domain.seeds.hideunused"/>
    <%
      shownSeedsLists = domain.getAllSeedListsAsSortedList(loc);
    } else { %>
    <fmt:message key="domain.seeds.showunused"/>
    <%
        shownSeedsLists = DomainDefinition.getSeedLists(usedDomainConfigurations);
      } %>
  </a>

    <table class="selection_table">
    <tr>
        <th colspan="2"><fmt:message key="seed.lists"/></th>
    </tr>
        <%
            rowcount = 0;
            for (SeedList sl : shownSeedsLists) {
                String rowClass = HTMLUtils.getRowClass(rowcount++);
                String listName = sl.getName();
                String editLink = "Definitions-edit-domain-seedlist.jsp?"
                        + Constants.EDIT_SEEDLIST_PARAM + "=1&"
                        + Constants.SEEDLIST_NAME_PARAM + "="
                        + HTMLUtils.encode(listName) + "&"
                        + Constants.DOMAIN_PARAM + "="
                        + HTMLUtils.encode(domainName);
                String seeds = StringUtils.conjoin("; ", sl.getSeeds());
                if (seeds.length() > 100) {
                    seeds = seeds.substring(0, 97) + "...";
                }
              %>
        <tr class="<%= rowClass %>">
            <td><strong><%= HTMLUtils.escapeHtmlValues(listName)%></strong>
                <em>(<%= HTMLUtils.escapeHtmlValues(seeds) %>)</em></td>
            <td><a href = "<%= HTMLUtils.escapeHtmlValues(editLink)%>">
                <fmt:message key="edit"/></a></td>
        </tr>
        <%
            }
            String newSeedLink = "Definitions-edit-domain-seedlist.jsp?"
                    + Constants.EDIT_SEEDLIST_PARAM + "=1&"
                    + Constants.DOMAIN_PARAM + "="
                    + HTMLUtils.encode(domainName);
        %>
        <tr><td><a href="<%=HTMLUtils.escapeHtmlValues(newSeedLink)%>">
            <fmt:message key="new.seed.list"/></a></td></tr>
    </table>
</div>
<br/><br/>

<div id="crawlertraps">
    <%
    %>
    <table class="selection_table">
        <tr>
            <th><fmt:message key="crawler.traps"/></th>
        </tr>
        <tr><td><a id="showCrawlertraps" href="" onclick="showHideCrawlertraps();return false;">
            <span id="showCrawlerTrap"><fmt:message key="show.crawler.traps"/></span>
            <span id="hideCrawlerTrap"><fmt:message key="hide.crawler.traps"/></span>
        </a></td></tr>
        <tr id="crawlertrapRow">
            <td>
                <textarea rows="<%=Constants.CRAWLERTRAPS_ROWS%>" cols="<%=Constants.CRAWLERTRAPS_COLUMNS%>"
                          name="<%=Constants.CRAWLERTRAPS_PARAM%>"><%=HTMLUtils.escapeHtmlValues(
                            StringUtils.conjoin("\n", domain.getCrawlerTraps()))
                    %></textarea>
            </td>
        </tr>
    </table>
</div>


<script type="text/javascript">
document.getElementById("crawlertrapRow").style.display = "none";
document.getElementById("hideCrawlerTrap").style.display = "none";
function showHideCrawlertraps() {
    if (document.getElementById("hideCrawlerTrap").style.display == "none") {
        // show crawler traps
        document.getElementById("crawlertrapRow").style.display = "block";
        document.getElementById("hideCrawlerTrap").style.display = "block";
        document.getElementById("showCrawlerTrap").style.display = "none";
    } else {
        // hide crawler traps
        document.getElementById("crawlertrapRow").style.display = "none";
        document.getElementById("hideCrawlerTrap").style.display = "none";
        document.getElementById("showCrawlerTrap").style.display = "block";
    }
}
</script>

<br/>
<br/>
<br/>
<input type="submit" value="<fmt:message key="save"/>"/>
</form>
<br/>
<br/>
<%
    if (SiteSection.isDeployed("History")) {
        String link = "/History/Harveststatus-perdomain.jsp?domainName="
                + HTMLUtils.encode(domainName);
%>
<a href="<%=HTMLUtils.escapeHtmlValues(link)%>">
    <fmt:message key="show.historical.harvest.information.for.0">
    <fmt:param value="<%=HTMLUtils.escapeHtmlValues(domainName)%>"></fmt:param>
    </fmt:message>
</a>
<%
    }
    HTMLUtils.generateFooter(out);
%>
