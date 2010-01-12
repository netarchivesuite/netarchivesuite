<%@ page import="dk.netarkivet.common.utils.I18n,
dk.netarkivet.common.webinterface.HTMLUtils,
dk.netarkivet.harvester.webinterface.GlobalCrawlerTrapDefinition,
dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO,
java.util.List,
dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList,static dk.netarkivet.harvester.webinterface.Constants.*"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    // First do any request updates
    GlobalCrawlerTrapDefinition.processRequest(pageContext, I18N);
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
        String activationAction = TRAP_INACTIVATE;
        String activationKey = "crawlertrap.deactivate";
    %>
    <%@ include file="traplist_activation_element.jspf" %>
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
        String activationAction = TRAP_ACTIVATE;
        String activationKey = "crawlertrap.activate";
    %>
    <%@ include file="traplist_activation_element.jspf" %>
</div>
<%
        }
    }
%>


<h3 class="page_heading"><fmt:message key="crawlertrap.createnew"/></h3>
<form method="post" action="./Definitions-edit-global-crawler-traps.jsp"
      enctype="multipart/form-data">
    <input type="hidden" name="<%=TRAP_ACTION%>" value="<%=TRAP_CREATE%>"/>
    <input name="<%=TRAP_NAME%>"/><br/>
    <input type="radio" name="<%=TRAP_IS_ACTIVE%>" value="true" checked="checked"/>
    <input type="radio" name="<%=TRAP_IS_ACTIVE%>" value="false"/> <br/>
    <input type="text" name="<%=TRAP_DESCRIPTION%>" /><br/>
    <input type="file" name="<%=TRAP_FILENAME%>" size="<%=UPLOAD_FILE_FIELD_WIDTH%>"/>
    <input type="submit" name="upload" value="****" />
</form>

<%
    HTMLUtils.generateFooter(out);
%>