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

--%><%--
This page shows selected parts of a crawllog for a given job, filtered by
either domain or regexp.

Note that the response language for the page is set using requested locale
of the client browser when fmt:setBundle is called. After that, fmt:format
and reponse.getLocale use this locale.

Parameters:
jobid - the id of the job to get the log for
regexp - the regular expression used to match the wanted extract of the crawl.log.
or
domain - the domain to get the log for
--%><%@ page import="java.io.File,
                 java.io.FileInputStream,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.FileUtils,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.utils.StreamUtils,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.viewerproxy.webinterface.Constants,
                 dk.netarkivet.viewerproxy.webinterface.Reporting"
         pageEncoding="UTF-8"
%>
<%@ page import="org.apache.commons.io.IOUtils" %>
<%@ page import="java.io.LineNumberReader" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="dk.netarkivet.common.utils.Settings" %>
<%@ page import="dk.netarkivet.harvester.HarvesterSettings" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.viewerproxy.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(
            dk.netarkivet.viewerproxy.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    String domain;
    String regexp;
    int jobid;
    File crawlLogExtract;
    try {
        HTMLUtils.forwardOnMissingParameter(pageContext, Constants.JOBID_PARAM);
        regexp = request.getParameter(Constants.REGEXP_PARAM);
        domain = request.getParameter(Constants.DOMAIN_PARAM);
        if ( (regexp == null || regexp.length() == 0) && (domain == null || domain.length() == 0) ) {
            pageContext.getRequest().setAttribute("message", "Must specify either 'domain' or 'regexp' " +
                    "parameter on request.");
            RequestDispatcher rd
                    = pageContext.getServletContext().getRequestDispatcher(
                    "/message.jsp");
            rd.forward(pageContext.getRequest(), pageContext.getResponse());
            return;
        }
        jobid = HTMLUtils.parseAndCheckInteger(pageContext, Constants.JOBID_PARAM, 1,
                Integer.MAX_VALUE);
        if (regexp != null && regexp.length() != 0 ) {
            crawlLogExtract = Reporting.getCrawlLoglinesMatchingRegexp(jobid, regexp);
        } else { // use 'domain' as the regular expression
        	//regexp = ".*" + domain.replaceAll("\\.", "\\\\.") + ".*";
           	regexp = ".*(https?:\\/\\/(www\\.)?|dns:|ftp:\\/\\/)([\\w_-]+\\.)?([\\w_-]+\\.)?([\\w_-]+\\.)?" 
            		+ domain.replaceAll("\\.", "\\\\.") +  "($|\\/|\\w|\\s).*";
        	crawlLogExtract = Reporting.getCrawlLoglinesMatchingRegexp(jobid, regexp);
        }
        LineNumberReader reader = new LineNumberReader(new FileReader(crawlLogExtract));
        reader.skip(Long.MAX_VALUE);
        int linesInFile = reader.getLineNumber();
        int maxLinesInBrowser =
                Settings.getInt(HarvesterSettings.MAX_CRAWLLOG_IN_BROWSER);
        if (linesInFile > maxLinesInBrowser) {
            response.setHeader("Content-Type", "binary/octet-stream");
            response.setHeader("Content-Disposition", "Attachment; filename=crawl_log_extract.txt");
            final ServletOutputStream outputStream = response.getOutputStream();
            IOUtils.copy(new FileInputStream(crawlLogExtract), outputStream);
            outputStream.flush();
            outputStream.close();
            FileUtils.remove(crawlLogExtract);
            return;
        }
    } catch (ForwardedToErrorPage e) {
        return;
    }
    HTMLUtils.generateHeader(pageContext);
%>
<h3><fmt:message key="pagetitle;qa.crawllog.lines.for.job.0.matching.regexp.1">
    <fmt:param value="<%=jobid%>"/>
    <fmt:param value="<%=regexp%>"/>
</fmt:message></h3>
<pre>
<%
    StreamUtils.copyInputStreamToJspWriter(new FileInputStream(crawlLogExtract), out);
    FileUtils.remove(crawlLogExtract);
%>
</pre>
<%
    HTMLUtils.generateFooter(out);
%>
