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

--%><%@ page import="
dk.netarkivet.archive.arcrepository.bitpreservation.FileBasedActiveBitPreservation,
dk.netarkivet.archive.arcrepository.bitpreservation.FilePreservationStatus,
dk.netarkivet.archive.webinterface.BitpreserveFileStatus,
dk.netarkivet.archive.webinterface.Constants,
dk.netarkivet.common.distribute.arcrepository.Location,
dk.netarkivet.common.exceptions.ForwardedToErrorPage, dk.netarkivet.common.exceptions.IllegalState, dk.netarkivet.common.utils.I18n, dk.netarkivet.common.webinterface.HTMLUtils"
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
    java.util.Map<String, FilePreservationStatus> fileInfo;
    try {
        fileInfo = BitpreserveFileStatus.processMissingRequest(pageContext,
                                                               res);
    } catch (dk.netarkivet.common.exceptions.ForwardedToErrorPage e) {
        return;
    }

    //Note: The parameter is checked to be legal in processMissingRequest()
    String bitarchiveName =
            request.getParameter(Constants.BITARCHIVE_NAME_PARAM);
    Location bitarchive = Location.get(bitarchiveName);

    // Make a list of files to make status for:
    Iterable<String> missingFiles;
    try {
        missingFiles = FileBasedActiveBitPreservation.getInstance().getMissingFiles(bitarchive);
    } catch (IllegalState e) {

        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                                          "errormsg;unable.to.get.files");
        throw new ForwardedToErrorPage(e.getMessage(), e);
    }
    int numberOfMissingFiles
            = (int) FileBasedActiveBitPreservation.getInstance().getNumberOfMissingFiles(bitarchive);

    // Get the page title from its URL
    HTMLUtils.generateHeader(pageContext);
%>
<script type="text/javascript" language="javascript">
    /** Toggles the status of all checkboxes with a given class */
    function toggleCheckboxes(command) {
        var toggler = document.getElementById("toggle" + command);
        if (toggler.checked) {
            var setOn = true;
        } else {
            var setOn = false;
        }
        var elements = document.getElementsByName(command);
        var maxToggle = document.getElementById("toggleAmount" + command).value;
        if (maxToggle <= 0) {
            maxToggle = elements.length;
        }
        for (var i = 0; i < elements.length && i < maxToggle; i++) {
            elements[i].checked = setOn;
        }
    }
</script>
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

    // Table and form header
    %>
    <form action="" method="post">
        <input type="hidden" name="bitarchive" value="<%=bitarchive.getName()%>"/>
        <table>
    <%

    // How many files can be uploaded with ADD_COMMAND
    int uploadableFiles = 0;
    // How many files can be set failed with SET_FAILED_COMMAND
    int failableFiles = 0;

    // For all files
    int rowCount = 0;
    for (String filename : missingFiles) {
        //Print a row for the file with info
        BitpreserveFileStatus.printFileName(out, filename, rowCount, response.getLocale());
        // If info was requested, output it
        if (fileInfo.containsKey(filename)) {
            %>
            <tr><td>
                <%
            FilePreservationStatus fs = fileInfo.get(filename);
            if (fs == null) {
                %>
                <fmt:message key="no.info.on.file.0">
                      <fmt:param value="<%=filename%>"/>
                </fmt:message>
                <%
            } else {
                // Print information about the file
                BitpreserveFileStatus.printFileStatus(out, fs, response.getLocale());
                // If the file is indeed missing
                if (fs.getBitarchiveChecksum(bitarchive).isEmpty()) {
                    //TODO: It should not be the job of the webpage to
                    //decide which actions are available.
                    if (!fs.isAdminDataOk()) {
                        // If this contradicts admindata, give opportunity
                        // to correct it.
                        out.println((BitpreserveFileStatus.makeCheckbox(
                                BitpreserveFileStatus.SET_FAILED_COMMAND,
                                bitarchive.getName(), filename)));
                        %><fmt:message key="mark.as.failed.upload"/><%
                        failableFiles++;
                    } else {
                        // Else give opportunity to reupload the file.
                        out.println(BitpreserveFileStatus.makeCheckbox(
                                BitpreserveFileStatus.ADD_COMMAND,
                                bitarchive.getName(), filename));
                        %><fmt:message key="add.to.archive"/><%
                        uploadableFiles++;
                    }
                } // if (fs.getBitarchiveChecksum(bitarchive).isEmpty())
            } // if (fs != null)
            %>
            </td></tr>
            <%
        } // if (fileInfo.containsKey(filename))
        rowCount++;
    } // for (String filename : missingFiles)

    // Table and form footer%>

        </table>
        <input type="submit" value="<fmt:message key="execute"/>"/>
    </form>
    <br/>
<%
    //convenience checkboxes to toggle multiple
    BitpreserveFileStatus.printToggleCheckboxes(out, response.getLocale(),
                                                numberOfMissingFiles,
                                                failableFiles,
                                                uploadableFiles);
    HTMLUtils.generateFooter(out);
%>