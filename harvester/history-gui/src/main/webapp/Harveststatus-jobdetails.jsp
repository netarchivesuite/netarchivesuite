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
This page shows details about a single job.


Note that the response language for the page is set using requested locale
of the client browser when fmt:setBundle is called. After that, fmt:format
and reposne.getLocale use this locale.
--%><%@ page import="java.util.Map,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.exceptions.UnknownID,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.common.webinterface.SiteSection,
                 dk.netarkivet.harvester.datamodel.DomainDAO,
                 dk.netarkivet.harvester.datamodel.HarvestDefinition,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.HarvestInfo,
                 dk.netarkivet.harvester.datamodel.Job,
                 dk.netarkivet.harvester.datamodel.JobDAO,
                 dk.netarkivet.harvester.webinterface.Constants"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    long jobID;
    try {
        jobID = HTMLUtils.parseAndCheckInteger(pageContext,
                Constants.JOB_PARAM, 1, Integer.MAX_VALUE);
    } catch (ForwardedToErrorPage e) {
        return;
    }
    Job job;
    try {
        job = JobDAO.getInstance().read(jobID);
    } catch (UnknownID e) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;job.unknown.id.0", jobID);
        return;
    }
    String harvestname;
    String harvesturl= "/HarvestDefinition/Definitions-selective-harvests.jsp";
    boolean definitionsSitesectionDeployed = SiteSection.isDeployed(
                    Constants.DEFINITIONS_SITESECTION_DIRNAME);
    try {
        HarvestDefinition hd = HarvestDefinitionDAO.getInstance().read(
                job.getOrigHarvestDefinitionID());
        harvestname = hd.getName();
        // define harvesturl, only if we have deployed the Definitions sitesection
        if (definitionsSitesectionDeployed) {                    
        	if (hd.isSnapShot()) {
            	harvesturl =
                    "/HarvestDefinition/Definitions-edit-snapshot-harvest.jsp?"
                            + Constants.HARVEST_PARAM + "="
                            + HTMLUtils.encode(harvestname);
        	} else {
            	harvesturl = "/HarvestDefinition/Definitions-edit-selective-harvest.jsp?"
                    + Constants.HARVEST_PARAM + "="
                    + HTMLUtils.encode(harvestname);
        	}
        }
    } catch (UnknownID e) {
        harvestname = I18N.getString(response.getLocale(),
                "unknown.harvest.0", job.getOrigHarvestDefinitionID());
    }
    String tdContents = HTMLUtils.escapeHtmlValues(harvestname);
    if (definitionsSitesectionDeployed) {
    	tdContents = "<a href=\"" + HTMLUtils.escapeHtmlValues(harvesturl)
    		+ "\">" + HTMLUtils.escapeHtmlValues(harvestname) + "</a>";
    }
    HTMLUtils.generateHeader(pageContext);
%>
<h3 class="page_heading"><fmt:message key="pagetitle;details.for.job.0">
    <fmt:param value="<%=jobID%>"/>
</fmt:message></h3>
<table class="selection_table">
    <tr>
        <th><fmt:message key="table.job.jobid"/></th>
        <th><fmt:message key="table.job.type"/></th>
        <th><fmt:message key="table.job.harvestname"/></th>
        <th><fmt:message key="table.job.harvestnumber"/></th>
        <th><fmt:message key="table.job.starttime"/></th>
        <th><fmt:message key="table.job.stoptime"/></th>
        <th><fmt:message key="table.job.jobstatus"/></th>
        <th><fmt:message key="table.job.harvesterror"/></th>
        <th><fmt:message key="table.job.uploaderror"/></th>
        <th><fmt:message key="table.job.objectlimit"/></th>
        <th><fmt:message key="table.job.bytelimit"/></th>
    </tr>
    <tr>
        <td><%=job.getJobID()%></td>
        <td><%=job.getPriority().getLocalizedString(response.getLocale())%>
        </td>
        <td><%=tdContents %></td>
        <td><%=dk.netarkivet.harvester.webinterface.HarvestStatus
                .makeHarvestRunLink(job.getOrigHarvestDefinitionID(),
                                    job.getHarvestNum())%>
        </td>
        <td><fmt:formatDate type="both" value="<%=job.getActualStart()%>"/></td>
        <td><fmt:formatDate type="both" value="<%=job.getActualStop()%>"/></td>
        <td><%=job.getStatus().getLocalizedString(response.getLocale())%></td>
        <td><a href="#harvesterror">
            <%=HTMLUtils.escapeHtmlValues(job.getHarvestErrors())%>
        </a></td>
        <td><a href="#uploaderror">
            <%=HTMLUtils.escapeHtmlValues(job.getUploadErrors())%>
        </a></td>
        <td><fmt:formatNumber type="number"
                              value="<%=job.getMaxObjectsPerDomain()%>"/></td>
        <td><fmt:formatNumber type="number"
                              value="<%=job.getMaxBytesPerDomain()%>"/></td>
    </tr>
</table>
<%-- display the searchFilter if QA webpages are deployed--%>
<%
    if (SiteSection.isDeployed(Constants.QA_SITESECTION_DIRNAME)) { %>
<h3><fmt:message key="subtitle.job.qa.selection"/></h3>
<table class="selection_table">
    <tr>
        <td>
            <p><a href="/<%=Constants.QA_SITESECTION_DIRNAME
            %>/QA-changeIndex.jsp?<%=Constants.JOB_PARAM%>=<%=
            job.getJobID()%>&<%=Constants.INDEXLABEL_PARAM%>=<%=
            HTMLUtils.escapeHtmlValues(HTMLUtils.encode(I18N.getString(
            response.getLocale(), "job.0", job.getJobID())))%>">
                <fmt:message key="select.job.for.qa.with.viewerproxy"/>
            </a></p>
        </td>
    </tr>
    <tr>
        <td><fmt:message key="helptext;select.job.for.qa.with.viewerproxy"/>
        </td>
    </tr>
</table>
<% } %>
<h3><fmt:message key="subtitle.job.domainconfigurations"/></h3>
<table class="selection_table">
    <tr>
        <th><fmt:message key="table.job.domainconfigurations.domain"/></th>
        <th><fmt:message key="table.job.domainconfigurations.configuration"/></th>
        <th><fmt:message key="table.job.domainconfigurations.bytesharvested"/></th>
        <th><fmt:message key="table.job.domainconfigurations.documentsharvested"/></th>
        <th><fmt:message key="table.job.domainconfigurations.stopreason"/></th>
    </tr>
    <%
        DomainDAO ddao = DomainDAO.getInstance();
        int rowcount = 0;
        for (Map.Entry<String, String> conf :
                job.getDomainConfigurationMap().entrySet()) {
            String domainLink;
            String configLink;
            String domainName = conf.getKey();
            String configName = conf.getValue();
            if (SiteSection.isDeployed(
                    Constants.DEFINITIONS_SITESECTION_DIRNAME)) {
                domainLink =
                        "<a href=\"/HarvestDefinition/Definitions-edit-domain.jsp?"
                                + Constants.DOMAIN_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(domainName) + "\">"
                                + HTMLUtils.escapeHtmlValues(domainName) + "</a>";
                configLink =
                        "<a href=\"/HarvestDefinition/Definitions-edit-domain-config.jsp?"
  						        + Constants.DOMAIN_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(domainName) + "&amp;"
                                + Constants.CONFIG_NAME_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(configName) + "&amp;"
                                + Constants.EDIT_CONFIG_PARAM + "=1\">"
                                + HTMLUtils.escapeHtmlValues(configName)  + "</a>";
            } else {
                domainLink = HTMLUtils.escapeHtmlValues(domainName);
                configLink = HTMLUtils.escapeHtmlValues(configName);
            }
            HarvestInfo hi = ddao.getDomainJobInfo(job, domainName,
                    configName);
    %>
    <tr class="<%=HTMLUtils.getRowClass(rowcount++)%>">
        <td><%=domainLink%></td>
        <td><%=configLink%></td>
            <% if (hi == null) { %>
              <td>-</td><td>-</td><td>-</td>
            <% } else { %>
			  <td><fmt:formatNumber type="number"
                              value="<%=hi.getSizeDataRetrieved()%>"/></td>
			  <td><fmt:formatNumber type="number"
                              value="<%=hi.getCountObjectRetrieved()%>"/></td>
			  <td><%=hi.getStopReason().getLocalizedString(response.getLocale())%></td>
			  </tr>
		    <%
                }
            }
    %>
</table>
<h3><fmt:message key="subtitle.job.seedlist"/></h3>

<p>
    <%
        for (String seed : job.getSeedList().split("\n")) {
            String url;
            if (!seed.matches(Constants.NO_PROTOCOL_REGEXP)) {
                url = "http://" + seed;
            } else {
                url = seed;
            }
    %>
    <a target="viewerproxy"
       href="<%=HTMLUtils.escapeHtmlValues(url)%>"><%=
        HTMLUtils.escapeHtmlValues(seed)%>
    </a>
    <br/>
    <%
        }
    %>
</p>

<h3><fmt:message key="subtitle.job.harvesttemplate">
    <fmt:param value="<%=HTMLUtils.escapeHtmlValues((job.getOrderXMLName()))%>"/>
</fmt:message></h3>
<%
	// make link to harvest template for job
	String link = "/History/Harveststatus-download-job-harvest-template.jsp?"
	 + "JobID=" + job.getJobID();
%>
<a href="<%=link %>"><fmt:message key="show.job.0.harvesttemplate">
<fmt:param value="<%=job.getJobID()%>"/>
</fmt:message></a>


<%
    if (job.getUploadErrors() != null
        && job.getUploadErrors().length() != 0) {
%>
<a id="uploaderror"></a>

<h3><fmt:message key="subtitle.job.uploaderror.details"/></h3>
<pre><%=HTMLUtils.escapeHtmlValues(job.getUploadErrorDetails())%></pre>
<%
    }
    if (job.getHarvestErrors() != null
        && job.getHarvestErrors().length() != 0) {
%>
<a id="harvesterror"></a>

<h3><fmt:message key="subtitle.job.harvesterror.details"/></h3>
<pre><%=HTMLUtils.escapeHtmlValues(job.getHarvestErrorDetails())%></pre>
<%
    }
    HTMLUtils.generateFooter(out);
%>
