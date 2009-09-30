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

--%><%--
This page is used to set up a snapshot harvest. The following parameters may be
passed to it:
update (Constants.UPDATE_PARAM):
    (null or single) If set, create the snapshot harvest and return
    to the snapshot listing page. If unset, show a page for editing a new
    snapshot definition
createnew (Constant.CREATENEW_PARAM):
    (null or single) If set, the name parameter is used to create a new
    snapshot harvest definition from the posted values.
edition (Constants.EDITION_PARAM):
    (null or single, int) If set, this is the edition that updates should be
    done against.  If somebody else modified the database between loading the
    page and submitting changes, this can be used to notice and give a proper
    error.
old_snapshot_name (Constants.OLDSNAPSHOT_PARAM):
    (null or single) If set, name of old snapshot to use as basis for this one.
    If null, start snapshot from scratch.
snapshot_object_limit (Constants.DOMAIN_LIMIT_PARAM):
    (null or single, int) Number of objects per domain to be harvested. Default
    value is Constants.DOMAIN_LIMIT_DEFAULT
snapshot_byte_limit (Constants.DOMAIN_BYTELIMIT_PARAM):
    (null or single, long) Number of bytes per domain to be harvested.  Default
     value is Constants.DEFAULT_MAX_BYTES
harvestName (Constants.HARVEST_SNAPSHOT_PARAM):
    (null or single) The name of the harvest to be created or modified
--%><%@ page import="java.text.NumberFormat,
                 java.util.Locale,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO,
                 dk.netarkivet.harvester.datamodel.SparseFullHarvest, dk.netarkivet.harvester.webinterface.Constants, dk.netarkivet.harvester.webinterface.SnapshotHarvestDefinition"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);

    try {
        SnapshotHarvestDefinition.processRequest(pageContext, I18N);
    } catch (ForwardedToErrorPage e) {
        return;
    }

    //Redirect if we just saved the HD
    if (request.getParameter(Constants.UPDATE_PARAM) != null) {
        response.sendRedirect("Definitions-snapshot-harvests.jsp");
        return;
    }

    // overwrite with existing harvestdefinition values
    String harvestName = request.getParameter(Constants.HARVEST_PARAM);
    SparseFullHarvest hd = null;
    HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
    if (harvestName != null) {
        hd = hddao.getSparseFullHarvest(harvestName);
        if (hd == null) {
            HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                    "errormsg;harvest.0.does.not.exist", harvestName);
            return;
        }
    }

    long objectLimit =
            dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS;
    Long oldHarvestOid = null;
    if (hd != null) {
        objectLimit = hd.getMaxCountObjects();
        oldHarvestOid = hd.getPreviousHarvestDefinitionOid();
    }
    HTMLUtils.generateHeader(pageContext);
    NumberFormat nf =
            NumberFormat.getInstance(HTMLUtils.getLocaleObject(pageContext));
%>
<h3 class="page_heading"><fmt:message key="pagetitle;snapshot.harvest"/></h3>

<form method="post" action="Definitions-edit-snapshot-harvest.jsp">
    <!-- create hidden fields -->
    <% if (hd == null) {
        // the createnew parameter is only set when no name was given %>
        <input type="hidden" name="<%=Constants.CREATENEW_PARAM%>" value="1" />
  <% } else { %>
    <input type="hidden" name="<%= Constants.EDITION_PARAM %>"
           value="<%= hd.getEdition() %>"/>
  <% } %>
    <input type="hidden" name="<%= Constants.UPDATE_PARAM %>" value="1"/>
    
    <table>
        <tr>
            <td><fmt:message key="prompt;harvest.name"/></td>
            <td>
          <% if (hd == null) { %>
              <span id="focusElement">
                <input type="text" name="<%= Constants.HARVEST_PARAM%>"
                       size="20"/>
              </span>
          <% } else { %>
                <input type="text" name="<%= Constants.HARVEST_PARAM %>"
                     value="<%=HTMLUtils.escapeHtmlValues(harvestName)%>"
                     readonly="readonly"/>
          <% } %>
            </td>
        </tr>

        <tr>
            <%
            long dftMaxObjects =
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS;
            %>
            <td><fmt:message key="prompt;max.objects.per.domain"/></td>
            <td><input 
                name="<%= Constants.DOMAIN_OBJECTLIMIT_PARAM %>"
                size="20" 
                value="<%= HTMLUtils.localiseLong(
                    (hd != null ? hd.getMaxCountObjects() : dftMaxObjects), 
                    pageContext)                
                   %>"/>
             </td>
        </tr>
        
        <tr>
            <%
            long dftMaxBytes =
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES;
            %>
            <td><fmt:message key="prompt;max.bytes.per.domain"/></td>
            <td><input 
                name="<%= Constants.DOMAIN_BYTELIMIT_PARAM %>"
                size="20" 
                value="<%= HTMLUtils.localiseLong(
                    (hd != null ? hd.getMaxBytes() : dftMaxBytes), 
                    pageContext)                
                   %>"/>
             </td>
        </tr>
    </table>

    <br/>
    <br/>

    <fmt:message key="prompt;comments"/><br/>
    <textarea rows="5" cols="42" name="comments"><%=
       HTMLUtils.escapeHtmlValues(hd != null?hd.getComments():"")
    %></textarea>
    <br/><br/>

    <table class="selection_table">
        <tr>
            <th colspan="2"><fmt:message key="prompt;harvest.only.unfinished.domains"/> </th>
        </tr>
        <tr class="row0">
            <td>
                <em><fmt:message key="none"/></em>
            </td>
            <td>
                <input type="radio" name="<%= Constants.OLDSNAPSHOT_PARAM %>"
                       <%=oldHarvestOid == null?" checked=\"checked\"":""%>
                        value=""/>
            </td>
        </tr>
        <%
            int rowcount = 0;
            for (SparseFullHarvest oldHarvest
                    : hddao.getAllSparseFullHarvestDefinitions()) {
                if (oldHarvest.getName().equals(harvestName)) {
                    continue;
                }
        %>
        <tr class="<%= HTMLUtils.getRowClass(rowcount++)%>">
            <td><%=HTMLUtils.escapeHtmlValues(oldHarvest.getName())%></td>
            <td><input 
                     type="radio" 
                     name="<%=Constants.OLDSNAPSHOT_PARAM%>"
                     value="<%=oldHarvest.getOid()%>"
                  <%=oldHarvest.getOid().equals(oldHarvestOid) ?
                     "checked=\"checked\"":""%>/>
            </td>
        </tr>
                    <%
            }
        %>
    </table>
    <br/>
    <input type="submit" value="<fmt:message key="save"/>">
</form>
<%
    HTMLUtils.generateFooter(out);
%>
