<%--
File:	   $Id$
Revision:   $Revision$
Author:	 $Author$
Date:	   $Date$

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

--%>
<%--
This page generates links to see the harvest reports for a given job.

Note that the response language for the page is set using requested locale
of the client browser when fmt:setBundle is called. After that, fmt:format
and reponse.getLocale use this locale.

Parameters:
jobid - The id of the job to show reports for.
--%>
<%@ page
	import="java.util.List,
				 java.net.URL,
				 java.nio.file.Path,
				 java.nio.file.Paths,
				 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
				 dk.netarkivet.common.utils.I18n,
				 dk.netarkivet.common.utils.cdx.CDXRecord,
				 dk.netarkivet.common.webinterface.HTMLUtils,
				 dk.netarkivet.harvester.tools.GetDataResolver,
				 dk.netarkivet.harvester.webinterface.Constants,
				 dk.netarkivet.harvester.webinterface.Reporting"
	pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page" />
<fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>" />
<%!
	private static final I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%>
<%
	HTMLUtils.setUTF8(request);
	int jobid;
	List<CDXRecord> records;
	try {
		jobid = HTMLUtils.parseAndCheckInteger(pageContext, Constants.JOB_PARAM, 1, Integer.MAX_VALUE);
		records = Reporting.getMetadataCDXRecordsForJob(jobid);
	} catch (ForwardedToErrorPage e) {
		return;
	}
	HTMLUtils.generateHeader(pageContext);
%>
<h3>
	<fmt:message key="pagetitle;reports.for.job.1">
		<fmt:param value="<%=jobid%>" />
	</fmt:message>
</h3>
<p></p>
<%
	for (CDXRecord record : records) {
		String filename = GetDataResolver.getFilename(record.getURL());
%>
	<a href='/History/Harveststatus-download-report-template.jsp?<%=GetDataResolver.COMMAND_PARAMETER%>=<%=GetDataResolver.GET_RECORD_COMMAND%>&<%=GetDataResolver.ARCFILE_PARAMETER%>=<%=record.getArcfile()%>&<%=GetDataResolver.FILE_OFFSET_PARAMETER%>=<%=record.getOffset()%>&<%=GetDataResolver.FILENAME_PARAMETER%>=<%=filename%>'><%=record.getURL()%></a><br>
<%
	}
	HTMLUtils.generateFooter(out);
%>
