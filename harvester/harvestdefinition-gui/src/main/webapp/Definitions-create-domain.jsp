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
This page is used to create new domains in the system either by posting a list
of domain names or by ingesting a file of newline-separated domain names.
Parameters:

    domainlist: a whitespace-separated list of domains to be created. Names
    are validated and a list of links to the new domain definition pages is
    displayed.
--%><%@ page import="javax.servlet.RequestDispatcher,
                 java.util.Arrays,
                 java.util.Set,
                 java.util.HashSet,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.DomainDAO,
                 dk.netarkivet.harvester.webinterface.Constants,
                 dk.netarkivet.harvester.webinterface.DomainDefinition"
          pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    String domains = request.getParameter(Constants.DOMAINLIST_PARAM);
    if (domains != null) {
        String[] domainsList = domains.split("\\s+");
        Set<String> invalidDomainNames = new HashSet<String>(
                DomainDefinition.createDomains(domainsList));
               
        if (domainsList.length == 1
                && DomainDAO.getInstance().exists(domainsList[0])) {
            RequestDispatcher rd =
                    pageContext.getServletContext().
                            getRequestDispatcher(
                                    "/Definitions-edit-domain.jsp?"
                                            + Constants.DOMAIN_PARAM
                                            + "=" + HTMLUtils.encode(
                                            domainsList[0]));
            rd.forward(request, response);
            return;
        } else {
            StringBuilder message = new StringBuilder();
            Set<String> validDomains = new HashSet<String>(Arrays.asList(domainsList));
            validDomains.removeAll(invalidDomainNames);
            if (!validDomains.isEmpty()) {
            	message.append("<h4>");
            	message.append(I18N.getString(response.getLocale(),
                    "harvestdefinition.domains.created"));
            	message.append("</h4><br/>");
            
            	for (String domain : validDomains) {
                	if (DomainDAO.getInstance().exists(domain)) {
                    	message.append(DomainDefinition.makeDomainLink(domain));
                    	message.append("<br/>");
                	}
            	}
            }
            if (invalidDomainNames.size() > 0) {
                message.append("<br/>");
                message.append(I18N.getString(response.getLocale(),
                        "harvestdefinition.domains.notcreated"));
                message.append("<br/>");
                DomainDAO dao = DomainDAO.getInstance();
                for (String invalid : invalidDomainNames) {
                    if (dao.exists(invalid)) {
                        message.append(
                                DomainDefinition.makeDomainLink(invalid));
                    } else {
                        message.append(invalid);
                    }
                    message.append("<br/>");
                }
            }
            request.setAttribute("message", message.toString());
            RequestDispatcher rd = pageContext.getServletContext().
                    getRequestDispatcher("/message.jsp");
            rd.forward(request, response);
            return;
        }
    }
    HTMLUtils.generateHeader(
            pageContext);
    //We only reach this point if no domainlist was sent in the request
%>

<h3 class="page_heading"><fmt:message key="pagetitle;create.domain"/></h3>

<form method="post" action="Definitions-create-domain.jsp">
    <fmt:message key="harvestdefinition.domains.enter"/> <br />
    <span id="focusElement">
        <textarea cols="20" rows="10" name="<%=Constants.DOMAINLIST_PARAM%>"></textarea>
    </span><br />
    <input type="submit" value=<fmt:message key="harvestdefinition.domains.create"/> />
</form>
<br />
<fmt:message key="harvestdefinition.domains.import"/>
<form method="post" action="Definitions-ingest-domains.jsp"
      enctype="multipart/form-data">
    <input size="50" type="file" name="<%=Constants.DOMAINLIST_PARAM%>"
           value=<fmt:message key="harvestdefinition.domains.domainlist"/>/>
    <input type="submit" value="<fmt:message key="ingest"/>" />
</form>

<%
    HTMLUtils.generateFooter(out);
%>