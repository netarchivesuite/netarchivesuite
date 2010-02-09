<%--
File:        $Id$
Revision:    $Revision$
Author:      $Author$
Date:        $Date$

 Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 --%><%@ page import="java.util.List,
dk.netarkivet.common.utils.I18n,
dk.netarkivet.common.webinterface.HTMLUtils,
dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList,
dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO,
dk.netarkivet.harvester.webinterface.TrapAction, dk.netarkivet.harvester.webinterface.TrapAction"
         pageEncoding="UTF-8"
        %><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
        %><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
        /><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><script type="text/javascript">
    function show_block(id) {
        document.getElementById(id).style.display="block";
    }
    function show_inline(id) {
        document.getElementById(id).style.display="inline";
    }
    function hide(id) {
        document.getElementById(id).style.display="none";
    }
</script><%
    HTMLUtils.setUTF8(request);
    // First do any request updates
    TrapAction.processRequest(pageContext, I18N);
    //Now display the results
    GlobalCrawlerTrapListDBDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
    List<GlobalCrawlerTrapList> activeTrapLists = dao.getAllActive();
    List<GlobalCrawlerTrapList> inactiveTrapLists = dao.getAllInActive();
    HTMLUtils.generateHeader(pageContext);
%><h3 class="page_heading"><fmt:message key="crawlertrap.active.header"/></h3>
<%
    if (activeTrapLists.isEmpty()) {
%>
<fmt:message key="crawlertrap.noactive"/>
<%
} else {
%>
<table>
    <%
        for (GlobalCrawlerTrapList trapList: activeTrapLists) {
    %>
    <div class="traplist">
        <tr>
            <%@ include file="traplist-download-element.jspf" %>
            <%
                String activationAction = TrapActionEnum.DEACTIVATE.name();
                String activationKey = "crawlertrap.deactivate";
            %>
            <td><%@ include file="traplist_activation_element.jspf" %></td>
            <%
                String trapIdString = trapList.getId() + "";
                String trapName = trapList.getName();
                String trapDescription = trapList.getDescription();
            %>

            <td><%@ include file="traplist_createorupdate_element.jspf"%></td>
            <td><%@include file="traplist_delete_element.jspf"%></td>
        </tr>
    </div>

    <%
        }
    %>
</table>
<%
}
%>

<h3 class="page_heading"><fmt:message key="crawlertrap.inactive.header"/></h3>
<%
    if (inactiveTrapLists.isEmpty()) {
%>
<fmt:message key="crawlertrap.noinactive"/>
<%
} else {
%>
<table>
    <%
        for (GlobalCrawlerTrapList trapList: inactiveTrapLists) {
    %>
    <div class="traplist">
        <tr>
            <%@ include file="traplist-download-element.jspf" %>
            <%
                String activationAction = TrapActionEnum.ACTIVATE.name();
                String activationKey = "crawlertrap.activate";
            %>
            <td><%@ include file="traplist_activation_element.jspf" %></td>
            <%
                String trapIdString = trapList.getId() + "";
                String trapName = trapList.getName();
                String trapDescription = trapList.getDescription();
            %>
            <td><%@ include file="traplist_createorupdate_element.jspf"%></td>
            <td><%@include file="traplist_delete_element.jspf"%></td>
        </tr>
    </div>
    <%
        }
    %>
</table>
<%
    }
%>

<%
    String trapIdString = null;
    String trapName = "";
    String trapDescription = "";
%>

<h3 class="page_heading"><fmt:message key="crawlertrap.createnew"/></h3>
<%@ include file="traplist_createorupdate_element.jspf" %>

<%
    HTMLUtils.generateFooter(out);
%>