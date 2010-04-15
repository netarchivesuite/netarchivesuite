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
--%>

<%--
This page displays a list of all jobs.
Parameters:
resubmit - jobID of a job to resubmit.
--%>

<%@ page import="
    java.util.List, 
    java.util.Set,
    java.util.Date, 
    java.util.Iterator,
    java.text.SimpleDateFormat,    
    dk.netarkivet.common.exceptions.ForwardedToErrorPage,
    dk.netarkivet.common.utils.I18n,
    dk.netarkivet.common.webinterface.HTMLUtils,
    dk.netarkivet.common.webinterface.SiteSection,
    dk.netarkivet.harvester.datamodel.JobStatus,
    dk.netarkivet.harvester.datamodel.JobStatusInfo,
    dk.netarkivet.harvester.webinterface.Constants,
    dk.netarkivet.harvester.webinterface.HarvestStatus,
    dk.netarkivet.harvester.webinterface.HarvestStatusQuery,
    dk.netarkivet.harvester.datamodel.DomainDAO,
    dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
    dk.netarkivet.harvester.datamodel.HarvestDefinition"
	pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"/>
<fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/>
<%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%>

<%
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
    HTMLUtils.generateHeader(pageContext);
    
    HarvestStatusQuery query = new HarvestStatusQuery(request);
    
    //list of information to be shown
    HarvestStatus results = HarvestStatus.getjobStatusList(query); 
    List<JobStatusInfo> jobStatusList = results.getJobStatusInfo();
    Set<JobStatus> selectedStatuses = query.getSelectedJobStatusesAsSet();
    
    boolean generateResubmitForm = selectedStatuses.isEmpty() 
        || selectedStatuses.contains(JobStatus.FAILED);
%>

<jsp:include page="calendar.jsp" flush="true"/>

<script language="javascript" type="text/javascript" src="./editableSelectBox.js">
</script>

<script type="text/javascript">

function resetForm() {
	document.filtersForm.<%=HarvestStatusQuery.UI_FIELD.JOB_STATUS.name()%>.selectedIndex = 0;
	document.filtersForm.<%=HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name()%>.selectedIndex = 0;	
	document.filtersForm.<%=HarvestStatusQuery.UI_FIELD.HARVEST_NAME.name()%>.selectedIndex = 0;
	
	document.filtersForm.<%=HarvestStatusQuery.UI_FIELD.START_DATE.name()%>.value = "";
	document.filtersForm.<%=HarvestStatusQuery.UI_FIELD.END_DATE.name()%>.value = "";
	document.filtersForm.<%=HarvestStatusQuery.UI_FIELD.PAGE_SIZE.name()%>.value = "";	
	document.filtersForm.<%=HarvestStatusQuery.UI_FIELD.START_PAGE_INDEX.name()%>.value = "";
}

function previousPage() {
	document.filtersForm.<%=HarvestStatusQuery.UI_FIELD.START_PAGE_INDEX.name()%>.value = "<%=query.getStartPageIndex() - 1%>";
	document.filtersForm.submit();
}

function nextPage() {
    document.filtersForm.<%=HarvestStatusQuery.UI_FIELD.START_PAGE_INDEX.name()%>.value = "<%=query.getStartPageIndex() + 1%>";
    document.filtersForm.submit();
}

function resetPagination() {
	document.filtersForm.<%=HarvestStatusQuery.UI_FIELD.START_PAGE_INDEX.name()%>.value = "1";
}

</script>

<%--Make line with comboboxes with job status and order to be shown --%>
<form method="get" name="filtersForm" action="Harveststatus-alljobs.jsp">

<h4>
<fmt:message key="status.job.filters.group1">
<fmt:param>
<select multiple name="<%=HarvestStatusQuery.UI_FIELD.JOB_STATUS.name()%>" size="4">
    <%
    String selected = (selectedStatuses.isEmpty() ? "selected=\"selected\"" : "");
    %>
        <option <%=selected%>  value="<%=HarvestStatusQuery.JOBSTATUS_ALL%>">
            <fmt:message key="status.job.all"/>
        </option>
    <%
    for (JobStatus st : JobStatus.values()) {
    	selected = "";
    	if (selectedStatuses.contains(st)) {
            selected = "selected=\"selected\"";
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
  <select name="<%=HarvestStatusQuery.UI_FIELD.HARVEST_NAME%>"
          onClick="beginEditing(this);" onBlur="finishEditing();"
          style="width:200px;">
  
        <option <%= query.getHarvestName().isEmpty() ? "selected=\"selected\"" : "" %> 
             value="<%=HarvestStatusQuery.HARVEST_NAME_ALL %>">
             <fmt:message key="status.harvest.all"/>
        </option>
        <%
          boolean valueInOptions = query.getHarvestName().isEmpty();
          HarvestDefinitionDAO hdDao = HarvestDefinitionDAO.getInstance();
          Iterator<HarvestDefinition> allHarvests = hdDao.getAllHarvestDefinitions();
          while (allHarvests.hasNext()) {
              String hdName = allHarvests.next().getName();
              String selected = "";
              if (query.getHarvestName().equals(hdName)) {
            	  selected="selected=\"selected\"";
            	  valueInOptions |= true;
              }
               
        %>
        <option
             <%=selected%>
             value="<%=hdName%>">
             <%=hdName%>
        </option>
        <%
          }
          if (! valueInOptions) {
        %> 
        <option selected="selected"
             value="<%=query.getHarvestName()%>">
             <%=query.getHarvestName()%>
        </option>      
        <%
          }
        %>
        
  </select>
  
</fmt:param>
<fmt:param>
<input name="<%=HarvestStatusQuery.UI_FIELD.START_DATE%>"
       id="<%=HarvestStatusQuery.UI_FIELD.START_DATE%>" 
       size="10"
       value="<%=query.getStartDateAsString()%>"/>
<script type="text/javascript">
setupCalendar("<%=HarvestStatusQuery.UI_FIELD.START_DATE%>", "<%=HarvestStatusQuery.CALENDAR_UI_DATE_FORMAT%>");
</script>
</fmt:param>
<fmt:param>
<input name="<%=HarvestStatusQuery.UI_FIELD.END_DATE%>"
       id="<%=HarvestStatusQuery.UI_FIELD.END_DATE%>"
       size="10"
       value="<%=query.getEndDateAsString()%>"/>
<script type="text/javascript">
setupCalendar("<%=HarvestStatusQuery.UI_FIELD.END_DATE%>", "<%=HarvestStatusQuery.CALENDAR_UI_DATE_FORMAT%>");
</script>
</fmt:param>
</fmt:message>
<br/>
<fmt:message key="status.job.filters.group2">
<fmt:param>
<select name="<%=HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER%>" size="1">
    <%
    String selected = (query.isSortAscending() ? "selected=\"selected\"" : "");
    %>
        <option <%=selected%> value="<%=HarvestStatusQuery.SORT_ORDER.ASC.name()%>">
             <fmt:message key="sort.order.asc"/>
        </option>
    <%
    selected = (! query.isSortAscending() ? "selected=\"selected\"" : "");
    %>
        <option <%=selected%> value="<%=HarvestStatusQuery.SORT_ORDER.DESC.name()%>">
             <fmt:message key="sort.order.desc"/>
        </option>
</select>
</fmt:param>
<fmt:param>
    <%
      String pageSizeStr = "";
      if (query.getPageSize() != HarvestStatusQuery.PAGE_SIZE_NONE) {
    	  pageSizeStr = Long.toString(query.getPageSize());
      }
    %>
    <input type="text" 
           name="<%=HarvestStatusQuery.UI_FIELD.PAGE_SIZE%>" 
           size="5"
           value="<%=pageSizeStr%>"/>
</fmt:param>
</fmt:message>

<input type="hidden" 
       name="<%=HarvestStatusQuery.UI_FIELD.START_PAGE_INDEX%>"
       value="<%=query.getStartPageIndex()%>"/>
   
<input type="submit" name="upload" 
       onclick="resetPagination();"
       value="<fmt:message key="status.sort.order.job.show"/>"/>

<a href="javascript:resetForm();">
    <fmt:message key="status.sort.order.job.reset"/>
</a>

</h4>
</form>

<%
    long totalResultsCount = results.getFullResultsCount();
    
    long pageSize = query.getPageSize();    
    long actualPageSize = (pageSize == HarvestStatusQuery.PAGE_SIZE_NONE ?
    	totalResultsCount : pageSize);

    long startPageIndex = query.getStartPageIndex();
    long startIndex = 0;
    long endIndex = 0;
    
    if (totalResultsCount > 0) {
        startIndex = ((startPageIndex - 1) * actualPageSize) + 1;
	    endIndex = Math.min(startIndex + actualPageSize - 1, totalResultsCount);
    }
%>
<fmt:message key="status.results.displayed">
<fmt:param><%=totalResultsCount%></fmt:param>
<fmt:param><%=startIndex%></fmt:param>
<fmt:param><%=endIndex%></fmt:param>
</fmt:message>

<%
    boolean prevLinkActive = false;
    if (pageSize != HarvestStatusQuery.PAGE_SIZE_NONE
    		&& totalResultsCount > 0
    		&& startIndex > 1) {
    	prevLinkActive = true;
    }
    
    boolean nextLinkActive = false;
    if (pageSize != HarvestStatusQuery.PAGE_SIZE_NONE
    		&& totalResultsCount > 0
            && endIndex < totalResultsCount) {
        nextLinkActive = true;
    }

%>
<p style="text-align: right">
<fmt:message key="status.results.displayed.pagination">

    <fmt:param>
        <%
            if (prevLinkActive) {
        %>
	    <a href="javascript:previousPage();">
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
            if (nextLinkActive) {
        %>
        <a href="javascript:nextPage();">
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

<%--Make header of page--%>
<h3 class="page_heading"><fmt:message key="pagetitle;jobstatus"/></h3>

<%
if (jobStatusList.isEmpty()) { 
%>
    <fmt:message key="table.job.no.jobs"/>
<% 
} else { //Make table with found jobs
%>

<%

  if (generateResubmitForm) {
%>

<script type="text/javascript">

function resubmitToggleSelection() {
    var allChecked = document.getElementsByName("resubmitJobsForm")[0].resubmit_all.checked;
    <% 
    for (JobStatusInfo js : jobStatusList) {
    	if (js.getStatus().equals(JobStatus.FAILED)) {
    %>
    document.getElementsByName("resubmitJobsForm")[0].resubmit_<%=js.getJobID()%>.checked=allChecked;
    <%
        }
    }
    %>
}

function resubmitSelectedJobs() {
	var concatenatedIds = "";
	var selectedCount = 0;
	<% 
    for (JobStatusInfo js : jobStatusList) {
        if (js.getStatus().equals(JobStatus.FAILED)) {
    %>
    if (document.getElementsByName("resubmitJobsForm")[0].resubmit_<%=js.getJobID()%>.checked) {
        concatenatedIds = concatenatedIds + ";<%=js.getJobID()%>";
        selectedCount++;
    }
    <%
        }
    }
	 
    %>

    if (selectedCount <= 0) {
        alert("<%=HTMLUtils.escapeHtmlValues(I18N.getString(
                pageContext.getResponse().getLocale(),
                "errormsg;resubmit.jobs.selectionEmpty", ""))%>");
        return;
    }
    concatenatedIds = concatenatedIds.substring(1, concatenatedIds.length);
    document.getElementsByName("resubmitJobsForm")[0].<%=HarvestStatusQuery.UI_FIELD.RESUBMIT_JOB_IDS.name()%>.value=concatenatedIds;
    document.getElementsByName("resubmitJobsForm")[0].submit();
}

</script>

<form method="post" name="resubmitJobsForm" action="Harveststatus-alljobs.jsp">

    <input type="hidden" 
        name="<%=HarvestStatusQuery.UI_FIELD.RESUBMIT_JOB_IDS.name()%>" 
        value=""/>

<% } %>

<table class="selection_table">
    <tr>
        <th><fmt:message key="table.job.jobid"/></th>
        <th><fmt:message key="table.job.harvestname"/></th>
        <th><fmt:message key="table.job.harvestnumber"/></th>
        <th><fmt:message key="table.job.jobstatus"/></th>
        <th><fmt:message key="table.job.harvesterror"/></th>
        <th><fmt:message key="table.job.uploaderror"/></th>
        <th><fmt:message key="table.job.number.of.domainconfigurations"/></th>
        <% if (generateResubmitForm) { %>
            <th><input type="checkbox" name="resubmit_all" onchange="resubmitToggleSelection()"/></tr>
        <% } %>
    <%
        int rowcount = 0;
        String detailsPage = "Harveststatus-jobdetails.jsp";
        for (JobStatusInfo js : jobStatusList) {
            String detailsLink = detailsPage + "?"
                                 + Constants.JOB_PARAM + "=" + js.getJobID();
            String harvestLink = "Harveststatus-perhd.jsp?"
                                 + Constants.HARVEST_PARAM + "="
                                 + HTMLUtils.encode(js.getHarvestDefinition());
            String jobStatusTdContents = HTMLUtils.escapeHtmlValues(
                        js.getStatus().getLocalizedString(
                                response.getLocale()));
            // If the status of the job is RESUBMITTED (and the new job is known),
    		// add a link to the new job
    		// Note: this information was only available from release 3.8.0
    		// So for historical jobs generated with previous versions of 
    		// NetarchiveSuite this information is not available. 
    		if (js.getStatus().equals(JobStatus.RESUBMITTED)
    			&& js.getResubmittedAsJob() != null) { 
    	 		jobStatusTdContents += "<br/>(<a href=\"" + detailsPage + "?" 
    	 			+ Constants.JOB_PARAM + "=" + js.getResubmittedAsJob() + "\">"
    	 			+ "Job " + js.getResubmittedAsJob() + "</a>" + ")";
    		}
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
                <td>
                	<%=jobStatusTdContents%>
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
                
                <% if (generateResubmitForm) { %>
                <td>
                    <input type="checkbox" name="resubmit_<%=js.getJobID()%>"
                    <%= JobStatus.FAILED.equals(js.getStatus()) ? "" : "disabled" %>
	                />    
                
                <% } %>
                </td>
            </tr>
    <%
        }
    %>
</table>

<%

  if (generateResubmitForm) {
%>
	  <br/>
	  <br/>	  

	  <input type="button" name="go"
	         value="<fmt:message key="resubmit.jobs.submit"/>"
	         onclick="resubmitSelectedJobs()"/>

	  </form>
<%
  }
%>

<% }
    HTMLUtils.generateFooter(out);
%>