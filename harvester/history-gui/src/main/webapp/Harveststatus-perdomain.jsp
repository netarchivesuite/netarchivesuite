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

--%><%-- This page handles showing harvest status and history for domains.
With no parameters, it gives an input box for searching that feeds back into
itself.
With parameter domainName, it performs a search.  Name can be a glob pattern
(using ? and * only) or a single domain.  If domains are found, they are
displayed, if no domains are found a message is shown.
--%>
<%@ page import="
                 java.util.List,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.DomainHarvestInfo,
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.webinterface.HarvestHistoryTableHelper"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"
/>

<%
  String domainName = request.getParameter(Constants.DOMAIN_SEARCH_PARAM);
  HarvestHistoryTableHelper helper = new HarvestHistoryTableHelper(
          domainName,
          request.getParameter(Constants.SORT_FIELD_PARAM),
          request.getParameter(Constants.SORT_ORDER_PARAM),
          request.getParameter(Constants.START_PAGE_PARAMETER)
  );

  HTMLUtils.setUTF8(request);
  HTMLUtils.generateHeader(pageContext, "navigateWithThreeParameters.js");
%>
  <!--
  This hidden form is triggered by the next-page / previous-page links but is not actually submitted. Rather the
  hidden fields are set as url parameters by the javascript.
  -->
<form method="post" name="filtersForm" action="Harveststatus-perdomain.jsp">
  <input type="hidden"
         name="<%= Constants.START_PAGE_PARAMETER%>"
         value="<%=helper.getPageIndex()%>"/>
</form>

<fmt:message key="status.results.displayed">
  <fmt:param><%=helper.getNumberOfResults()%></fmt:param>
  <fmt:param><%=helper.getStartIndex()+1%></fmt:param>
  <fmt:param><%=helper.getEndIndex()%></fmt:param>
</fmt:message>
<p style="text-align: right">
  <fmt:message key="status.results.displayed.pagination">
    <fmt:param>
      <%
        if (helper.isPreviousPageAvailable()) {
      %>
      <a
              href="javascript:previousPage(<%=helper.generateParameterStringForPaging()%>);">
        <fmt:message key="status.results.displayed.prevPage"/>
      </a>
      <%
      } else {
      %>
      <fmt:message key="status.results.displayed.prevPage"/>
      <%
        }
      %>
    </fmt:param>
    <fmt:param>
      <%
        if (helper.isNextPageAvailable()) {
      %>
      <a
              href="javascript:nextPage(<%=helper.generateParameterStringForPaging()%>);">
        <fmt:message key="status.results.displayed.nextPage"/>
      </a>
      <%
      } else {
      %>
      <fmt:message key="status.results.displayed.nextPage"/>
      <%
        }
      %>
    </fmt:param>
  </fmt:message>
</p>

<h3 class="page_heading">
  <fmt:message key="harvest.history.for.0">
    <fmt:param><a href="/HarvestDefinition/Definitions-edit-domain.jsp?<%=
                    Constants.DOMAIN_PARAM%>=<%=HTMLUtils.encode(domainName)%>"><%=
    HTMLUtils.escapeHtmlValues(domainName)%></a></fmt:param>
  </fmt:message>
</h3>
<%
  if (helper.getNumberOfResults() == 0) { // No history
%><p><fmt:message key="domain.0.was.never.harvested">
  <fmt:param value="<%=domainName%>"/>
</fmt:message></p><%
} else {
%>
<table class="selection_table">
  <%
  String sortBaseLink="Harveststatus-perdomain.jsp?"
        + Constants.DOMAIN_SEARCH_PARAM + "=" + HTMLUtils.encode(domainName) + "&";
  %>
  <tr>
  <th class="harvestHeader">
    <a href="<%=sortBaseLink + Constants.SORT_ORDER_PARAM + "=" +
         helper.getOrderAfterClick(HarvestHistoryTableHelper.HARVEST_NAME_FIELD) + "&" +
       Constants.SORT_FIELD_PARAM + "=" +
         HarvestHistoryTableHelper.HARVEST_NAME_FIELD %>">
      <fmt:message key="table.job.harvestname"/><%=helper.getOrderArrow(HarvestHistoryTableHelper.HARVEST_NAME_FIELD)%>
    </a>
  </th>
  <th class="harvestHeader">
    <a href="<%=sortBaseLink + Constants.SORT_ORDER_PARAM + "=" +
         helper.getOrderAfterClick(HarvestHistoryTableHelper.HARVEST_NUMBER_FIELD) + "&" +
       Constants.SORT_FIELD_PARAM + "=" +HarvestHistoryTableHelper.HARVEST_NUMBER_FIELD %>">
      <fmt:message key="table.job.harvestnumber"/>
      <%=helper.getOrderArrow(HarvestHistoryTableHelper.HARVEST_NUMBER_FIELD)%>
    </a>
  </th>
  <th class="harvestHeader">
    <a href="<%=sortBaseLink + Constants.SORT_ORDER_PARAM + "=" +
         helper.getOrderAfterClick(HarvestHistoryTableHelper.JOB_ID_FIELD) + "&" +
       Constants.SORT_FIELD_PARAM + "=" +HarvestHistoryTableHelper.JOB_ID_FIELD %>">
      <fmt:message key="table.job.jobid"/>
      <%=helper.getOrderArrow(HarvestHistoryTableHelper.JOB_ID_FIELD)%>
    </a>
  </th>

  <th class="harvestHeader">
    <a href="<%=sortBaseLink + Constants.SORT_ORDER_PARAM + "=" +
         helper.getOrderAfterClick(HarvestHistoryTableHelper.CONFIGURATION_NAME_FIELD) + "&" +
       Constants.SORT_FIELD_PARAM + "=" +HarvestHistoryTableHelper.CONFIGURATION_NAME_FIELD %>">
      <fmt:message key="table.job.domainconfigurations.configuration"/>
      <%=helper.getOrderArrow(HarvestHistoryTableHelper.CONFIGURATION_NAME_FIELD)%>
    </a>
  </th>

    <th class="harvestHeader">
      <a href="<%=sortBaseLink + Constants.SORT_ORDER_PARAM + "=" +
         helper.getOrderAfterClick(HarvestHistoryTableHelper.START_TIME_FIELD) + "&" +
       Constants.SORT_FIELD_PARAM + "=" +HarvestHistoryTableHelper.START_TIME_FIELD %>">
        <fmt:message key="table.job.starttime"/>
        <%=helper.getOrderArrow(HarvestHistoryTableHelper.START_TIME_FIELD)%>
      </a>
    </th>

    <th class="harvestHeader">
      <a href="<%=sortBaseLink + Constants.SORT_ORDER_PARAM + "=" +
         helper.getOrderAfterClick(HarvestHistoryTableHelper.STOP_TIME_FIELD) + "&" +
       Constants.SORT_FIELD_PARAM + "=" +HarvestHistoryTableHelper.STOP_TIME_FIELD %>">
        <fmt:message key="table.job.stoptime"/>
        <%=helper.getOrderArrow(HarvestHistoryTableHelper.STOP_TIME_FIELD)%>
      </a>
    </th>

    <th class="harvestHeader">
      <a href="<%=sortBaseLink + Constants.SORT_ORDER_PARAM + "=" +
         helper.getOrderAfterClick(HarvestHistoryTableHelper.BYTES_HARVESTED_FIELD) + "&" +
       Constants.SORT_FIELD_PARAM + "=" +HarvestHistoryTableHelper.BYTES_HARVESTED_FIELD %>">
        <fmt:message key="table.job.domainconfigurations.bytesharvested"/>
        <%=helper.getOrderArrow(HarvestHistoryTableHelper.BYTES_HARVESTED_FIELD)%>
      </a>
    </th>

    <th class="harvestHeader">
      <a href="<%=sortBaseLink + Constants.SORT_ORDER_PARAM + "=" +
         helper.getOrderAfterClick(HarvestHistoryTableHelper.DOCUMENTS_HARVESTED_FIELD) + "&" +
       Constants.SORT_FIELD_PARAM + "=" +HarvestHistoryTableHelper.DOCUMENTS_HARVESTED_FIELD %>">
        <fmt:message key="table.job.domainconfigurations.documentsharvested"/>
        <%=helper.getOrderArrow(HarvestHistoryTableHelper.DOCUMENTS_HARVESTED_FIELD)%>
      </a>
    </th>

    <th class="harvestHeader">
      <a href="<%=sortBaseLink + Constants.SORT_ORDER_PARAM + "=" +
         helper.getOrderAfterClick(HarvestHistoryTableHelper.STOPPED_DUE_TO_FIELD) + "&" +
       Constants.SORT_FIELD_PARAM + "=" +HarvestHistoryTableHelper.STOPPED_DUE_TO_FIELD %>">
        <fmt:message key="table.job.domainconfigurations.stopreason"/>
        <%=helper.getOrderArrow(HarvestHistoryTableHelper.STOPPED_DUE_TO_FIELD)%>
      </a>
    </th>
  </tr>
 <%
   int rowCount = 0;
   for (DomainHarvestInfo hi :
           helper.listCurrentPageHarvestHistory()) {
     String harvestLink = "Harveststatus-perhd.jsp?"
             + Constants.HARVEST_PARAM + "="
             + HTMLUtils.encode(hi.getHarvestName());
     String jobLink = "Harveststatus-jobdetails.jsp?jobID="
             + hi.getJobID();
 %>
  <tr class="<%=HTMLUtils.getRowClass(rowCount++)%>">
    <td><a href="<%=HTMLUtils.escapeHtmlValues(harvestLink)%>">
      <%=HTMLUtils.escapeHtmlValues(hi.getHarvestName())%>
    </a></td>
    <td><%=dk.netarkivet.harvester.webinterface
            .HarvestStatus
            .makeHarvestRunLink(hi.getHarvestID(), hi.getHarvestNum())%>
    </td>
    <td><%
      if (hi.getJobID() == 0) {
    %>-<%
    } else {
    %><a href="<%=HTMLUtils.escapeHtmlValues(jobLink)%>">
      <%=hi.getJobID()%>
    </a><%
      }
    %>
    </td>
    <td><%=HTMLUtils.escapeHtmlValues(hi.getConfigName())%>
    </td>
    <td><%=HTMLUtils.parseDate(hi.getStartDate())%></td>
    <td><%=HTMLUtils.parseDate(hi.getEndDate())%></td>
    <td><fmt:formatNumber value="<%=hi.getBytesDownloaded()%>"/></td>
    <td><fmt:formatNumber value="<%=hi.getDocsDownloaded()%>"/></td>
    <td><%
      if (hi.getStopReason() == null) {
    %>-<%
    } else {
    %>
      <%=hi.getStopReason().getLocalizedString(response.getLocale())%>
      <%
      }
    %>
    </td>
  </tr>
  <%
      }
    }
  %>
</table>
<%
  HTMLUtils.generateFooter(out);
%>