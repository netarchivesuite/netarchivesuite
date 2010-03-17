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
This page displays harvest details for one harvest definition run
--%><%@ page import="java.util.ArrayList,
                 java.util.List, java.util.Set, 
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.JobStatus,
                 dk.netarkivet.harvester.datamodel.JobStatusInfo,
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.webinterface.HarvestStatus"
         pageEncoding="UTF-8"
%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page"
                 basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);

    try {
        HarvestStatus.processRequest(pageContext, I18N);
    } catch (ForwardedToErrorPage e) {
        return;
    }

    // Look for optional parameters for search of jobs.
    // Moved up to make it possible to transfer paraemters when redirecting.
    HarvestStatus.DefaultedRequest dfltRequest =
        new HarvestStatus.DefaultedRequest(request);
    Set<Integer> selectedJobStatusCodes =
            HarvestStatus.getSelectedJobStatusCodes(dfltRequest);
    String selectedSortOrder = HarvestStatus.getSelectedSortOrder(dfltRequest);

    // After a resubmit, forward to this page.
    if (request.getParameter(Constants.JOB_RESUBMIT_PARAM) != null) {
        String jobsStatusCodes = "&";
        for (JobStatus st : JobStatus.values()) {
            if (selectedJobStatusCodes.contains(new Integer(st.ordinal()))) {
                jobsStatusCodes = jobsStatusCodes.concat(Constants.JOBSTATUS_PARAM + "=" +
                st.name() + "&");
            }
        }
        response.sendRedirect("Harveststatus-perharvestrun.jsp?"
                              + Constants.HARVEST_ID_PARAM + "="
                              + request.getParameter(Constants.HARVEST_ID_PARAM)
                              + "&" + Constants.HARVEST_NUM_PARAM + "="
                              + request.getParameter(Constants.HARVEST_NUM_PARAM)
                              + "&" + Constants.JOBIDORDER_PARAM + "="
                              + request.getParameter(Constants.JOBIDORDER_PARAM)
                              + jobsStatusCodes
                                );
        return;
    }
	// Local variables to hold the value of the HARVEST_ID_PARAM and the 
	// HARVEST_NUM_PARAM.
    long harvestID;
    long harvestNum;
    try {
        harvestID = HTMLUtils.parseAndCheckInteger(pageContext,
                                                   Constants.HARVEST_ID_PARAM,
                                                   0, Integer.MAX_VALUE);
        harvestNum = HTMLUtils.parseAndCheckInteger(pageContext,
                                                    Constants.HARVEST_NUM_PARAM,
                                                    0, Integer.MAX_VALUE);
    } catch (ForwardedToErrorPage e) {
        return;
    }
        
    


    // List of information to be shown.
    List<JobStatusInfo> jobStatusList = HarvestStatus.getjobStatusList(
                                            harvestID,
                                            harvestNum,
                                            selectedJobStatusCodes, 
                                            selectedSortOrder
                                        );
                                        
    final String harvestName
            = HarvestDefinitionDAO.getInstance().getHarvestName(harvestID);
    String harvestLink = "<a href=\"Harveststatus-perhd.jsp?"
                         + Constants.HARVEST_PARAM + "="
                         + HTMLUtils.encodeAndEscapeHTML(harvestName)
                         + "\">" + HTMLUtils.escapeHtmlValues(harvestName)
                         + "</a>";
    HTMLUtils.generateHeader(pageContext);
    String selected;
    List<String> selectedJobs = new ArrayList<String>();
%>


<%--Make line with comboboxes with job status and order to be shown. --%>
<form method="get" action="Harveststatus-perharvestrun.jsp">
<input type="hidden" name="<%=Constants.HARVEST_ID_PARAM%>"
       value="<%=request.getParameter(Constants.HARVEST_ID_PARAM)%>"/>
<input type="hidden" name="<%=Constants.HARVEST_NUM_PARAM%>"
       value="<%=request.getParameter(Constants.HARVEST_NUM_PARAM)%>"/>
<h4>

<fmt:message key="status.0.sort.order.1.job.choice">
<fmt:param>
<select multiple name="<%= Constants.JOBSTATUS_PARAM %>"
        size="<%= JobStatus.values().length %>">
    <%
    selected = (selectedJobStatusCodes.contains(new Integer(JobStatus.ALL_STATUS_CODE)))
               ? "selected=\"selected\"" : "";
    %>
        <option <%=selected%>  value="<%=HarvestStatus.JOBSTATUS_ALL%>">
             <fmt:message key="status.job.all"/>
        </option>
    <%
    for (JobStatus st : JobStatus.values()) {
        selected = "";
        if (selectedJobStatusCodes.contains(new Integer(st.ordinal()))) {
            selected = "selected=\"selected\"";
            selectedJobs.add(st.name());
        }
    %>
        <option <%=selected%> value="<%=st.name()%>">
            <%=HTMLUtils.escapeHtmlValues(
                  st.getLocalizedString(response.getLocale())
               )%>
        </option>
    <%
    }
    %>
</select>
</fmt:param>

<fmt:param>
<select name="<%= Constants.JOBIDORDER_PARAM %>" size="1">
    <%
    selected = (selectedSortOrder.equals(HarvestStatus.SORTORDER_ASCENDING))
               ? "selected=\"selected\""
               : "";
    %>
        <option <%=selected%> value="<%=HarvestStatus.SORTORDER_ASCENDING%>">
             <fmt:message key="sort.order.asc"/>
        </option>
    <%
    selected = (selectedSortOrder.equals(HarvestStatus.SORTORDER_DESCENDING))
               ? "selected=\"selected\""
               : "";
    %>
        <option <%=selected%> value="<%=HarvestStatus.SORTORDER_DESCENDING%>">
             <fmt:message key="sort.order.desc"/>
        </option>
</select>
</fmt:param>
</fmt:message>
<input type="submit" name="upload" 
       value="<fmt:message key="status.sort.order.job.show"/>"/>
</h4>
</form>

<h2 class="page_heading">
    <fmt:message key="pagetitle;status.for.harvest.0.run.1">
        <fmt:param value="<%=harvestLink%>"/>
        <fmt:param value="<%=harvestNum%>"/>
    </fmt:message>
</h2>

<h3 class="page_heading"><fmt:message key="pagetitle;jobstatus"/></h3>
<table class="selection_table">
    <tr>
        <th><fmt:message key="table.job.jobid"/></th>
        <th><fmt:message key="table.job.jobstatus"/></th>
        <th><fmt:message key="table.job.harvesterror"/></th>
        <th><fmt:message key="table.job.uploaderror"/></th>
        <th><fmt:message key="table.job.number.of.domainconfigurations"/></th>
    </tr>
    <%
        //contains jobs for QA if jobs DONE or FAILED
        List<Long> jobIDs = new ArrayList<Long>();
        int rowcount = 0;
        for (JobStatusInfo js : jobStatusList) {
            String detailsLink = "Harveststatus-jobdetails.jsp?"
                    + Constants.JOB_PARAM + "=" + js.getJobID();
            //if job DONE or FAILED add jobid
            if (js.getStatus() == JobStatus.DONE
                    || js.getStatus() == JobStatus.FAILED) {
                jobIDs.add(js.getJobID());
            }

    %>
            <tr class="<%=HTMLUtils.getRowClass(rowcount++)%>">
                <td><a href="<%=HTMLUtils.escapeHtmlValues(detailsLink)%>">
                    <%=js.getJobID()%></a>
                </td>
                <td><%=HTMLUtils.escapeHtmlValues(
                        js.getStatus().getLocalizedString(
                                response.getLocale()))%>
                </td>
                <td><%=HTMLUtils.escapeHtmlValues(
                       HTMLUtils.nullToHyphen(js.getHarvestErrors()))%>
                    <% if (js.getStatus().equals(JobStatus.FAILED)
                          && js.getHarvestErrors() != null
                          && js.getHarvestErrors().length() > 0
                          && SiteSection.isDeployed(
                            Constants.DEFINITIONS_SITESECTION_DIRNAME)) {
                        // Note: The form is only displayed if Definitions
                        // sitesection is deployed. Thus you cannot change any
                        // state using the history sitesection only.
                    %>&nbsp;<form class ="inlineform" method="post"
                                  action="Harveststatus-perharvestrun.jsp">
                                <input type="hidden"
                                       name="<%=Constants.JOB_RESUBMIT_PARAM%>"
                                       value="<%=js.getJobID()%>"/>
                                <input type="hidden"
                                       name="<%=Constants.HARVEST_ID_PARAM%>"
                                       value="<%=harvestID%>"/>
                                <input type="hidden"
                                       name="<%=Constants.HARVEST_NUM_PARAM%>"
                                       value="<%=harvestNum%>"/>
                                <input type="hidden"
                                       name="<%=Constants.JOBIDORDER_PARAM%>"
                                       value="<%=selectedSortOrder%>"/>
                                <%
                                // Add jobstatusname to param list.
                                for(String job: selectedJobs) {
                                    %>
                                    <input type="hidden"
                                           name="<%=Constants.JOBSTATUS_PARAM%>"
                                           value="<%=job%>"/>
                                    <%
                                }
                                %>
                                <input type="submit"
                                       value="<fmt:message key="button;restart"/>"/>
                            </form>
                    <%
                    }
                    %>
                </td>
                <td><%=HTMLUtils.escapeHtmlValues(
                        HTMLUtils.nullToHyphen(js.getUploadErrors()))%>
                </td>
                <td><fmt:formatNumber value="<%=js.getConfigCount()%>"/></td>
            </tr>
    <%
        }

    %>

</table>
<%-- Display index link for DONE or FAILED job ID's. --%>
<%-- Display the index link, only if QA webpages are deployed. --%>
<%
    if (!jobIDs.isEmpty() && SiteSection.isDeployed(Constants.QA_SITESECTION_DIRNAME)) { %>
<h3><fmt:message key="subtitle.job.qa.selection"/></h3>
<table class="selection_table">
    <tr>
        <td>
            <form action="/<%=Constants.QA_SITESECTION_DIRNAME%>/QA-changeIndex.jsp"
                  method="POST" name="QAform" id="QAform">
                <% for (Long jobID : jobIDs) { %>
                <input type="hidden" name="<%=Constants.JOB_PARAM%>"
                       value="<%=jobID%>"/>
                <% } %>
                <input type="hidden" name="<%=Constants.INDEXLABEL_PARAM%>"
                       value="<fmt:message key="harvest.0.run.1">
                    <fmt:param value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"/>
                    <fmt:param value="<%=harvestNum%>"/>
                </fmt:message>">
                <p><a href="/<%=Constants.QA_SITESECTION_DIRNAME%>/QA-changeIndex.jsp"
                      onclick="document.getElementById('QAform').submit();return false;">
                    <fmt:message key="select.jobs.for.qa.with.viewerproxy"/>
                </a></p>
            </form>
        </td>
    </tr>
    <tr>
        <td><fmt:message key="helptext;select.jobs.for.qa.with.viewerproxy"/>
        </td>
    </tr>
</table>
<%  }
    HTMLUtils.generateFooter(out);
%>
