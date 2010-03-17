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
This page handles editing a seedlist.
Parameters:
  name: Name of the domain that owns this seedlist.  Must be non-null.
  update: If set, this is a form posting to update the seedlist
  urlListName: The name of the seedlist.
  editUrlList: If non-null, indicates that we'll start editing an existing list
  seedlist: The list of URLs that make up this seedlist, one per line
  edition: Optimistic locking version number
  comments: Any user-comments on this seedlist
--%><%@ page import="dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.Domain,
                 dk.netarkivet.harvester.datamodel.DomainDAO,
                 dk.netarkivet.harvester.datamodel.SeedList,
                 dk.netarkivet.harvester.webinterface.Constants, dk.netarkivet.harvester.webinterface.DomainSeedsDefinition"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    try {
        HTMLUtils.forwardOnEmptyParameter(pageContext,
                Constants.DOMAIN_PARAM);
        DomainSeedsDefinition.processRequest(pageContext, I18N);
    } catch (ForwardedToErrorPage e) {
        return;
    }
    String domainName =
            request.getParameter(Constants.DOMAIN_PARAM);
    DomainDAO ddao = DomainDAO.getInstance();
    if (!ddao.exists(domainName)) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;unknown.domain.0", domainName);
        return;
    }
    Domain domain = ddao.read(domainName);

    SeedList sl = null;
    String seedListName =
            request.getParameter(Constants.URLLIST_NAME_PARAM);
    if (request.getParameter(Constants.EDIT_URLLIST_PARAM) != null
            && seedListName != null) {
        if (!domain.hasSeedList(seedListName)) {
            HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                    "errormsg;unknown.seedlist.0.for.domain.1",
                    seedListName, domainName);
            return;
        }
        sl = domain.getSeedList(seedListName);
    }

    // update is set to non-null when the page posts data to itself. In that
    // case, when we are done we go over to the main domain edit page.
    if (request.getParameter(Constants.UPDATE_PARAM) != null) {
        response.sendRedirect("Definitions-edit-domain.jsp?"
                + Constants.DOMAIN_PARAM + "="
                + HTMLUtils.encode(domainName));
        return;
    }

    HTMLUtils.generateHeader(
            pageContext);
%>
<%--
Display all the form information for this domain
--%>
<form method="post" action="./Definitions-edit-domain-seedlist.jsp">
    <input type="hidden" name="<%= Constants.UPDATE_PARAM %>" value="1"/>

    <h3 class="page_heading"><%= HTMLUtils.escapeHtmlValues(domainName) %>
    </h3>
    <input type="hidden" name="<%= Constants.EDITION_PARAM%>"
           value="<%=domain.getEdition()%>"/>
    <input type="hidden" name="<%= Constants.DOMAIN_PARAM %>"
          value="<%=HTMLUtils.escapeHtmlValues(domainName)%>"/>

    <div id="SeedLists">
        <div class="edit_box">
            <table class="selection_table">
                <th colspan="2"><fmt:message key="harvestdefinition.seedlist.editSeedlist"/></th>
                <tr><td><fmt:message key="prompt;name"/></td>
                    <%
                        String listname = "";
                        if (sl != null) {
                            listname = "value=\"" + HTMLUtils.escapeHtmlValues(sl.getName())
                                    + "\" readonly =\"readonly\"";
                        }
                    %>
                <td><span id="focusElement">
                    <input size="25" name="<%= Constants.URLLIST_NAME_PARAM %>" <%=listname%>/>
                </span></td></tr>
                <tr>
                    <td><fmt:message key="prompt;harvestdefinition.seedlist.edit.seeds"/></td>
                    <td>
                        <textarea rows="8" cols="80" name="<%= Constants.SEED_LIST_PARAMETER %>"><%=
                            HTMLUtils.escapeHtmlValues(sl != null? sl.getSeedsAsString() : "")
                        %></textarea>
                    </td>
                </tr>
                <tr><td colspan="2"><fmt:message key="prompt;comments"/></td></tr><tr><td colspan="2">
                    <textarea name="<%= Constants.COMMENTS_PARAM%>" rows="5" cols="42"><%=
                       HTMLUtils.escapeHtmlValues(sl != null?sl.getComments():"")
                    %></textarea>
                </td></tr>
            </table>
        </div>
    </div>
    <br/>
    <input type="submit" value="<fmt:message key="save"/>"/>
</form>
<%
    HTMLUtils.generateFooter(out);
%>
