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

--%><%@ page import="java.util.List,
                 dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservation,
                 dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservationFactory,
                 dk.netarkivet.archive.arcrepository.bitpreservation.FileBasedActiveBitPreservation,
                 dk.netarkivet.archive.arcrepository.bitpreservation.PreservationState,
                 dk.netarkivet.archive.arcrepository.bitpreservation.FilePreservationState,
                 dk.netarkivet.archive.webinterface.BitpreserveFileState,
                 dk.netarkivet.archive.webinterface.Constants,
                 dk.netarkivet.common.distribute.arcrepository.Replica,
		 	     dk.netarkivet.common.exceptions.ForwardedToErrorPage, 
		 	     dk.netarkivet.common.exceptions.IllegalState, 
		 	     dk.netarkivet.common.utils.I18n, 
		 	     dk.netarkivet.common.webinterface.HTMLUtils"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    // Process checksumrequest. Any errors found while processing the files are added to res.
    StringBuilder res = new StringBuilder();
    PreservationState fs;
    try {
        fs = BitpreserveFileState.processChecksumRequest(res, pageContext);
    } catch (ForwardedToErrorPage e) {
        return;
    }

    //Note: The parameter has already been checked to be valid in processChecksumRequest()
    String bitarchiveName
            = request.getParameter(Constants.BITARCHIVE_NAME_PARAM);
    Replica bitarchive = Replica.getReplicaFromName(bitarchiveName);

    Iterable<String> wrongFiles;
    try {
        ActiveBitPreservation bitPreservation
                = ActiveBitPreservationFactory.getInstance();
        wrongFiles = bitPreservation.getChangedFiles(bitarchive);
    } catch (IllegalState e) {
        HTMLUtils.forwardWithErrorMessage(pageContext,
                                          I18N,
                                          "errormsg;unable.to.get.files");
        return;
    }

    // Get the page title from its URL
    HTMLUtils.generateHeader(pageContext);
%>
<h3 class="page_heading"><fmt:message key="pagetitle;filestatus.checksum.errors"/></h3>
<%
    if (res.length() > 0) {
%>
<p>
    <%
        // Output result string from request processing
        out.print(res.toString());
    %>
</p>
<%
    }
%>

<%
    String filename = request.getParameter(Constants.FILENAME_PARAM);
    // If a filename is specified output current file information
    if (filename != null) {
        if (fs == null) {
           %>
              <fmt:message key="no.info.on.file.0">
                 <fmt:param value="<%=filename%>"/>
              </fmt:message>
           <%
        } else {
            %>
            <!-- Table for presenting checksums -->
            <fmt:message key="state.of.file"/><%=HTMLUtils.escapeHtmlValues(filename)%>
            <% 
            // Print information about the file
            BitpreserveFileState.printFileState(out, fs, response.getLocale());
            %>

            <!-- Form for taking action -->
            <form method="post" action="">
            <input type="hidden" 
                   value="<%=HTMLUtils.escapeHtmlValues(bitarchive.getName())%>" 
                   name="<%=Constants.BITARCHIVE_NAME_PARAM%>">
            <input type="hidden" 
                   value="<%=HTMLUtils.escapeHtmlValues(filename)%>" 
                   name="<%=Constants.FILENAME_PARAM%>">
            <%
            if (fs.isAdminCheckSumOk()) {
                List<String> checksum = fs.getReplicaChecksum(bitarchive);
                // Check that at most one checksum were returned from 
                // replica 'bitarchive' for this file, and that this checksum 
                // is NOT equal to the one stored in admin data.
                if (checksum.size() == 1 
                    && !checksum.get(0).equals(fs.getAdminChecksum())) {
                   // Remove file action
                    %>
                    <fmt:message key="insert.password"/>
                    <input type="password" 
                        name="<%=Constants.CREDENTIALS_PARAM%>">
                    <input type="hidden" 
                        value="<%=HTMLUtils.escapeHtmlValues(checksum.get(0))%>" 
                        name="<%=Constants.CHECKSUM_PARAM%>">
                    <input type="submit" 
                        value="<fmt:message key="replace.file.in.replica.0">
                        <fmt:param><%=bitarchive%></fmt:param></fmt:message>">
                    <%
                    // Either (1) no checksums or more than one checksum were returned from location 'bitarchive' for this file
                    // Or (2) the checksum returned is equal to the one stored in admin data
                } else {
                	// Check that case (2) is not true 
                    if (!(checksum.size() == 1 
                          && checksum.get(0).equals(fs.getAdminChecksum()))) {
                        %>
                        <fmt:message key="unable.to.correct"/>
                        <%
                    } else { //case (2): checksum is already correct and ok
                        //remove file from wrongFiles list
                    }
                }
            } else {
                  // Correct admin data action
            %>
                <input type="hidden" value="1" name="<%=Constants.FIX_ADMIN_CHECKSUM_PARAM%>" >
                <input type="submit" value="<fmt:message key="correct.admin.checksum"/>">
            <%
            }
            %>
            </form>
<%          
        } // if fs != null
    } // if filename != null
%>

<!-- List of files with checksum errors, if any  -->

<h4><fmt:message key="files.with.checksum.errors.in.0">
    <fmt:param><%=bitarchiveName%></fmt:param>
</fmt:message></h4>
<table>
<%
    int rowCount = 0;
    for (String fn : wrongFiles) {
%>
    <tr class="<%=HTMLUtils.getRowClass(rowCount)%>">
        <td>
            <%=fn%>
        </td>
        <td>
            <a href="./Bitpreservation-filestatus-checksum.jsp?<%=
            Constants.BITARCHIVE_NAME_PARAM%>=<%=
            HTMLUtils.encodeAndEscapeHTML(bitarchive.getName())%>&amp;<%=
            Constants.FILENAME_PARAM%>=<%=
            HTMLUtils.encodeAndEscapeHTML(fn)%>"><fmt:message key="info"/></a>
        </td>
    </tr>
<%
        rowCount++;
    } // end for
    if (rowCount == 0) {
    %>
        <fmt:message key="no.files.with.checksum.errors"/>
    <%
    }
%>
</table>
<%
    HTMLUtils.generateFooter(out);
%>