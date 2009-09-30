<%--
File:       $Id: Harveststatus-show-harvest-template.jsp 84 2007-09-28 11:29:23Z svc $
Revision:   $Revision: 84 $
Author:     $Author: svc $
Date:       $Date: 2007-09-28 13:29:23 +0200 (fr, 28 sep 2007) $

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark

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

--%><%@ page import="org.dom4j.Document,
                 org.dom4j.io.XMLWriter,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.JobDAO,
                 dk.netarkivet.harvester.datamodel.Job,
                 dk.netarkivet.harvester.webinterface.Constants"
          pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    
    String requestedContentType = request.getParameter("requestedContentType");
    if (requestedContentType == null) {
        	requestedContentType = "text/xml";
    }
  
    // Insert check for missing JobID parameter
    
    String JOBID_PARAMETER = "JobID";
    try {		    
 		HTMLUtils.forwardOnEmptyParameter(pageContext, JOBID_PARAMETER);
    	} catch (ForwardedToErrorPage e) {
        	return;
    	}
    String jobIdAsString = request.getParameter(JOBID_PARAMETER);
    
    long jobId;
    	try {
    		jobId = Long.parseLong(jobIdAsString);
    	} catch (NumberFormatException e) {
    		HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;0.must.be.integer.not.1", JOBID_PARAMETER, 
                jobIdAsString);
        	return;
    	}
    // show a harvesttemplate for job with id=jobId
    	
    	JobDAO dao = JobDAO.getInstance();
        Document doc = null;
    	
    	if (dao.exists(jobId)) {
    		Job j = dao.read(jobId);
    		doc = j.getOrderXMLdoc();
    	} else {
    		HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;job.unknown.id.0", jobId);
        	return;
    	}
    	       
    // Write the template to the page.
    String filename = new String ("order-xml-for-job-" + jobId); 
    response.setHeader("Content-Type", requestedContentType);
    if (requestedContentType.startsWith("binary")) {
        response.setHeader("Content-Disposition", "Attachment; filename="
                + filename + Constants.XML_EXTENSION);

    }
    XMLWriter writer = new XMLWriter(out);
    writer.write(doc);
%>