<%@ page import="dk.netarkivet.common.utils.I18n,
dk.netarkivet.common.webinterface.HTMLUtils,
dk.netarkivet.harvester.webinterface.GlobalCrawlerTrapDefinition,
dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO,
java.util.List,
dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList,static dk.netarkivet.harvester.webinterface.Constants.*, dk.netarkivet.harvester.webinterface.TrapAction"
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
    for (GlobalCrawlerTrapList trapList: activeTrapLists) {
%>
<div class="traplist">
    <%@ include file="traplist-download-element.jspf" %>
    <%
        String activationAction = TrapActionEnum.DEACTIVATE.name();
        String activationKey = "crawlertrap.deactivate";
    %>
    <%@ include file="traplist_activation_element.jspf" %>
    <%
        String trapIdString = trapList.getId() + "";
        String trapName = trapList.getName();
        String trapDescription = trapList.getDescription();
    %>

    <%@ include file="traplist_createorupdate_element.jspf"%>
</div>
</div>
<%
        }
    }
%>

<h3 class="page_heading"><fmt:message key="crawlertrap.inactive.header"/></h3>
<%
    if (inactiveTrapLists.isEmpty()) {
%>
<fmt:message key="crawlertrap.noinactive"/>
<%
} else {
    for (GlobalCrawlerTrapList trapList: inactiveTrapLists) {
%>
<div class="traplist">
    <%@ include file="traplist-download-element.jspf" %>
    <%
        String activationAction = TrapActionEnum.ACTIVATE.name();
        String activationKey = "crawlertrap.activate";
    %>
    <%@ include file="traplist_activation_element.jspf" %>
    <%
        String trapIdString = trapList.getId() + "";
        String trapName = trapList.getName();
        String trapDescription = trapList.getDescription();
    %>
    <%@ include file="traplist_createorupdate_element.jspf"%>
</div>
<%
        }
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