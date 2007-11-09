<%--
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
This page displays a list of all jobs.
Parameters:
resubmit - jobID of a job to resubmit.
--%><%@ page import="java.util.List,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.datamodel.JobDBDAO,
                 dk.netarkivet.harvester.datamodel.JobStatus,
                 dk.netarkivet.harvester.datamodel.JobStatusInfo,
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.webinterface.HarvestStatus"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    try {
        HarvestStatus.processRequest(pageContext, I18N);
    } catch (ForwardedToErrorPage e) {
        return;
    }
    //After a resubmit, forward to this page
    if (request.getParameter(Constants.JOB_RESUBMIT_PARAM) != null) {
        response.sendRedirect("Harveststatus-alljobs.jsp");
        return;
    }
    List<JobStatusInfo> jobStatusList = JobDBDAO.getInstance().getStatusInfo();
    HTMLUtils.generateHeader(pageContext);
%>
<h3 class="page_heading"><fmt:message key="pagetitle;jobstatus"/></h3>

<%
if (jobStatusList.isEmpty()) 
{ %>
   <fmt:message key="table.job.no.jobs"/>
<% }
else
{ %>
<table class="selection_table">
    <tr>
        <th><fmt:message key="table.job.jobid"/></th>
        <th><fmt:message key="table.job.harvestname"/></th>
        <th><fmt:message key="table.job.harvestnumber"/></th>
        <th><fmt:message key="table.job.jobstatus"/></th>
        <th><fmt:message key="table.job.harvesterror"/></th>
        <th><fmt:message key="table.job.uploaderror"/></th>
        <th><fmt:message key="table.job.number.of.domainconfigurations"/></th>
    </tr>
    <%
        int rowcount = 0;
        for (JobStatusInfo js : jobStatusList) {
            String detailsLink = "Harveststatus-jobdetails.jsp?"
                                 + Constants.JOB_PARAM + "=" + js.getJobID();
            String harvestLink = "Harveststatus-perhd.jsp?"
                                 + Constants.HARVEST_PARAM + "="
                                 + HTMLUtils.encode(js.getHarvestDefinition());
    %>
            <tr class="<%=HTMLUtils.getRowClass(rowcount++)%>">
                <td><a href="<%=detailsLink%>">
                    <%=js.getJobID()%>
                </a></td>
                <td><a href="<%=HTMLUtils.escapeHtmlValues(harvestLink)%>">
                    <%=HTMLUtils.escapeHtmlValues(js.getHarvestDefinition())%>
                </a></td>
                <td><%=HarvestStatus.makeHarvestRunLink(
                        js.getHarvestDefinitionID(), js.getHarvestNum())%>
                </td>
                <td><%=HTMLUtils.escapeHtmlValues(
                        js.getStatus().getLocalizedString(
                                response.getLocale()))%>
                </td>
                <td><%=HTMLUtils.escapeHtmlValues(
                       HTMLUtils.nullToHyphen(js.getHarvestErrors()))%>
                    <%if (js.getStatus() == JobStatus.FAILED
                            && js.getHarvestErrors() != null
                            && js.getHarvestErrors().length() > 0
                            && SiteSection.isDeployed
                                  (Constants.DEFINITIONS_SITESECTION_DIRNAME)) {
                        //Note: The form is only displayed if Definitions
                        //sitesection is deployed. Thus you cannot change any
                        //state using the history sitesection only.
                    %>&nbsp;<form class ="inlineform" method="post"
                                  action="Harveststatus-alljobs.jsp">
                                <input type="hidden"
                                       name="<%=Constants.JOB_RESUBMIT_PARAM%>"
                                       value="<%=js.getJobID()%>"/>
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
                <td><fmt:formatNumber value="<%=js.getConfigCount()%>"/>
                </td>
            </tr>
    <%
        }
    %>
</table>
<% }
    HTMLUtils.generateFooter(out);
%>