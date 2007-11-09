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

--%><%@ page import="java.util.HashMap,
         java.util.List,
         java.util.Map,
         dk.netarkivet.archive.arcrepository.bitpreservation.FilePreservationStatus,
         dk.netarkivet.archive.webinterface.BitpreserveFileStatus,
         dk.netarkivet.archive.webinterface.Constants,
         dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState,
         dk.netarkivet.common.distribute.arcrepository.Location,
         dk.netarkivet.common.exceptions.ForwardedToErrorPage,
         dk.netarkivet.common.utils.I18n,
         dk.netarkivet.common.webinterface.HTMLUtils"
     pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);

    StringBuilder res = new StringBuilder();
    Map params = new HashMap(request.getParameterMap());

    Map<String, FilePreservationStatus> fileInfo = null;
    try {
        fileInfo = BitpreserveFileStatus.processMissingRequest(
                pageContext, res, params
        );
    } catch (ForwardedToErrorPage e) {
        return;
    }

    HTMLUtils.forwardOnMissingParameter(pageContext,
            Constants.BITARCHIVE_NAME_PARAM);
    String bitarchiveName =
            request.getParameter(Constants.BITARCHIVE_NAME_PARAM);

    if (!Location.isKnownLocation(bitarchiveName)) {
        HTMLUtils.forwardOnIllegalParameter(
                pageContext,
                Constants.BITARCHIVE_NAME_PARAM,
                Location.getKnownNames()
        );
        return;
    }
    Location bitarchive = Location.get(bitarchiveName);

    // Get the page title from its URL
    HTMLUtils.generateHeader(
            pageContext);
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
<h3 class="page_heading"><fmt:message key="pagetitle;filestatus.files.missing"/></h3>
<p>
<%
// Output result string from request processing
if (res.length() > 0) {
    out.print(res.toString());
}
%>
</p>

<h4><fmt:message key="missing.files.for.0"><fmt:param><%=HTMLUtils.escapeHtmlValues(bitarchive.getName())%></fmt:param></fmt:message></h4>
<form action="" method="post">
    <input type="hidden" name="bitarchive" value="<%= bitarchive.getName() %>">
    <table>
<%
    // Make a list of files to make status for:
    List<String> missingFiles =
            BitpreserveFileStatus.getMissingFilesList(bitarchive, pageContext);
    // How many files can be uploaded with ADD_COMMAND
    int uploadableFiles = 0;
    // How many files can be set failed with SET_FAILED_COMMAND
    int failableFiles = 0;
    int rowCount = 0;
    for (String filename : missingFiles) { %>
    	<tr class="<%=HTMLUtils.getRowClass(rowCount)%>">
    	<%=HTMLUtils.makeTableElement(filename)%>
        <td><%=BitpreserveFileStatus.makeCheckbox(BitpreserveFileStatus.GET_INFO_COMMAND, filename)%>
        <fmt:message key="get.info"/></td></tr>
        <%
        rowCount++;
        // If info was requested, output it
        if (fileInfo.containsKey(filename)) {
            FilePreservationStatus fs = fileInfo.get(filename);
            if (fs == null) {
         %><fmt:message key="no.info.on.file.{0}">
              <fmt:param value="<%=filename%>"/>
           </fmt:message><%
            } else {
                %>
    		    <tr><td><fmt:message key="status"/>
                <table>
        	    <tr><td>&nbsp;</td><td><fmt:message key="state"/></td><td><fmt:message key="checksum"/></td></tr>
        	    <tr><td><fmt:message key="admin.data"/></td><td>-</td>
                <%= HTMLUtils.makeTableElement(fs.getAdminChecksum()) %></tr>
                <%
                for (Location l : Location.getKnown()) {
                    final String baLocation = l.getName();
                    List<String> csum = fs.getBitarchiveChecksum(l);
                    %><tr><%= HTMLUtils.makeTableElement(baLocation)
                     + HTMLUtils.makeTableElement(fs.getAdminBitarchiveState(l))
                     + HTMLUtils.makeTableElement(
                      HTMLUtils.presentChecksum(csum, response.getLocale()))%>
                <%
                    if (csum.size() == 0 &&
                        fs.getAdminBitarchiveStoreState(bitarchive) !=
                                BitArchiveStoreState.UPLOAD_FAILED) {
                        %><td>
                        <%=(BitpreserveFileStatus.makeCheckbox
                              (BitpreserveFileStatus.SET_FAILED_COMMAND,
                                   baLocation, filename))%>
                        <fmt:message key="mark.as.failed.upload"/></td>
                        <%
                        failableFiles++;
                    }
                    %></tr><%
                }
                %></table></td></tr>
                <% if (fs.isAdminDataOk()
                       && fs.getBitarchiveChecksum(bitarchive).isEmpty())  {
                    %>
                    <tr><td><%=BitpreserveFileStatus.makeCheckbox
                        (BitpreserveFileStatus.ADD_COMMAND, bitarchive.getName(), filename)%>
                    <fmt:message key="add.to.archive"/>
                    </td></tr>
                    <%
                    uploadableFiles++;
            	} // if (fs.isAdminDataOk()
        	} // if (fs != null)
        } // if (fileInfo.containsKey(filename))
    } // for (String filename : missingFiles)
    %>
</table>
<input type="submit" value="<fmt:message key="execute"/>"/>
</form>
<br/><input type="checkbox" id="toggle<%= BitpreserveFileStatus.GET_INFO_COMMAND %>"
            onclick="toggleCheckboxes('<%= BitpreserveFileStatus.GET_INFO_COMMAND %>')"/>
                  <fmt:message key="change.infobox.for.0.files"><fmt:param>
                  	  <input id="toggleAmount<%= BitpreserveFileStatus.GET_INFO_COMMAND%>"
                                             value="<%= Math.min(missingFiles.size(),
                                             			Constants.MAX_TOGGLE_AMOUNT)%>">
                        </fmt:param></fmt:message><br/> <%
if (failableFiles > 0) {
        %><br/><input type="checkbox"
                      id="toggle<%= BitpreserveFileStatus.SET_FAILED_COMMAND %>"
                      onclick="toggleCheckboxes('<%= BitpreserveFileStatus.SET_FAILED_COMMAND %>')"/>
         <fmt:message key="change"/><input id="toggleAmount<%= BitpreserveFileStatus.SET_FAILED_COMMAND%>"
                      value="<%= Math.min(failableFiles, Constants.MAX_TOGGLE_AMOUNT)%>">
               <fmt:message key="failed"/><br/><%
}
if (uploadableFiles > 0) {
        %><br/><input type="checkbox" id="toggle<%= BitpreserveFileStatus.ADD_COMMAND %>"
                      onclick="toggleCheckboxes('<%= BitpreserveFileStatus.ADD_COMMAND %>')"/>
                  <fmt:message key="change"/> <input id="toggleAmount<%= BitpreserveFileStatus.ADD_COMMAND%>"
                               value="<%= Math.min(uploadableFiles, Constants.MAX_TOGGLE_AMOUNT)%>">
                       <fmt:message key="may.be.added"/><br/><%
}
%>
<%
    HTMLUtils.generateFooter(out);
%>