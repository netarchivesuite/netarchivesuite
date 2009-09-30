<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

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

--%><%@ page import="
dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservation,
dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservationFactory,
dk.netarkivet.archive.arcrepository.bitpreservation.FileBasedActiveBitPreservation,
dk.netarkivet.archive.arcrepository.bitpreservation.FilePreservationState,
dk.netarkivet.archive.webinterface.BitpreserveFileState,
dk.netarkivet.archive.webinterface.Constants,
dk.netarkivet.common.distribute.arcrepository.Replica, dk.netarkivet.common.exceptions.IllegalState, dk.netarkivet.common.utils.I18n, dk.netarkivet.common.webinterface.HTMLUtils"
         pageEncoding="UTF-8"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=dk.netarkivet.common.webinterface.HTMLUtils.getLocale(request)%>"
                 scope="page"
/><fmt:setBundle scope="page"
                 basename="<%=dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);

    StringBuilder res = new StringBuilder();
    java.util.Map<String, FilePreservationState> fileInfo;
    try {
        fileInfo = BitpreserveFileState.processMissingRequest(pageContext,
                                                               res);
    } catch (dk.netarkivet.common.exceptions.ForwardedToErrorPage e) {
        return;
    }

    //Note: The parameter has already been checked to be valid in processMissingRequest()
    String bitarchiveName =
            request.getParameter(Constants.BITARCHIVE_NAME_PARAM);
    Replica bitarchive = Replica.getReplicaFromName(bitarchiveName);

    // Make a list of files to make state for:
    Iterable<String> missingFiles;
    ActiveBitPreservation activeBitPreservation
            = ActiveBitPreservationFactory.getInstance();
    try {
        missingFiles = activeBitPreservation.getMissingFiles(bitarchive);
    } catch (IllegalState e) {

        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                                          "errormsg;unable.to.get.files");
        return;
    }
    int numberOfMissingFiles
            = (int) activeBitPreservation.getNumberOfMissingFiles(bitarchive);

    // Get the page title from its URL
    HTMLUtils.generateHeader(pageContext);
%>
<h3 class="page_heading"><fmt:message
        key="pagetitle;filestatus.files.missing"/></h3>
<%
    if (res.length() > 0) {
        // Output result string from request processing
%>
        <p>
            <%=res%>
        </p>
<%
    }
%>

<h4><fmt:message key="missing.files.for.0">
    <fmt:param><%=HTMLUtils.escapeHtmlValues(bitarchiveName)%></fmt:param>
</fmt:message></h4>

<%
    // Generate the page for showing missing file info, and allowing taking
    // actions.

    //First check if there are any missing files left. 
    //There will be no files left when there are all reuploaded.
    if (!missingFiles.iterator().hasNext()) {
	    %>
	    <fmt:message key="no.more.missing.files"/>
	    <%
    } else {
	    // Table and form header
	    %>
	    <form action="" method="post">
	        <input type="hidden" name="bitarchive" value="<%=bitarchive.getName()%>"/>
	        <table>
	    <%
	    // Counter for number of files that can be uploaded with ADD_COMMAND.
	    // This is increased in the loop below.
	    int uploadableFiles = 0;
	
	    // For all files
	    int rowCount = 0;
	    %>
	    <fmt:message key="status"/>
	    <%
	    for (String filename : missingFiles) {
	        //Print a row for the file with info
	        BitpreserveFileState.printFileName(out, filename, rowCount, response.getLocale());
	        // If info for file exists, output it
	        if (fileInfo.containsKey(filename)) {
	            %>
	            <tr><td>
	                <%
	            FilePreservationState fs = fileInfo.get(filename);
	            if (fs == null) {
	                %>
	                <fmt:message key="no.info.on.file.0">
	                      <fmt:param value="<%=filename%>"/>
	                </fmt:message>
	                <%
	            } else {
	                // Print information about the file
	                BitpreserveFileState.printFileState(out, fs, response.getLocale());
	                // If the file is indeed missing
	                if (fs.getBitarchiveChecksum(bitarchive).isEmpty()) {
	                    // Give opportunity to reupload the file.
	                    %></td><td><%
	                    out.println(BitpreserveFileState.makeCheckbox(
	                            Constants.ADD_COMMAND,
	                            bitarchive.getName(), filename));
	                    %><fmt:message key="add.to.archive"/><%
	                    uploadableFiles++;
	                } // if (fs.getBitarchiveChecksum(bitarchive).isEmpty())
	            } // if (fs != null)
	            %>
	            </td></tr>
	            <%
	        } // if (fileInfo.containsKey(filename))
	        rowCount++;
	    } // for (String filename : missingFiles)
	
        // Table and form footer
        %>

        </table>
        <input type="submit" value="<fmt:message key="execute"/>"/>
        </form>
        <br/>
        <%
        //convenience checkboxes to toggle multiple action checkboxes with a
        //single click.
        BitpreserveFileState.printToggleCheckboxes(out, response.getLocale(),
                                                numberOfMissingFiles,
                                                uploadableFiles);
    } //missingFiles.iterator().hasNext()

    HTMLUtils.generateFooter(out);
%>