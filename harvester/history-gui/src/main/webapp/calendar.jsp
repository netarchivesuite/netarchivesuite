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
This page contains javascript suppoting calendar handling.        
--%><%@ page import="dk.netarkivet.common.utils.I18n,
                dk.netarkivet.common.webinterface.HTMLUtils,
                dk.netarkivet.common.Constants"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(Constants.TRANSLATIONS_BUNDLE);
%>
<!-- main calendar program -->
<script type="text/javascript" src="./jscalendar/calendar.js"></script>
<!-- language for the calendar, taken from the language set by the user -->
<script type="text/javascript" src="./jscalendar/lang/calendar<%
String lang = HTMLUtils.getLocale(request);
if (lang.length() >= 2) {
    out.print("-" + lang.substring(0, 2));
}%>.js"></script>
<!-- the following script defines the Calendar.setup helper function,
which makes adding a calendar a matter of 1 or 2 lines of code. -->
<script type="text/javascript" src="./jscalendar/calendar-setup.js"></script>
<script type="text/javascript">
/**
 * Attach the calendar to the given field on the Harveststatus-alljobs
 * page
 */
 function setupCalendar(inputFieldId, dateFormat){
     Calendar.setup({
         inputField     :    inputFieldId,
                                            // id of the input field
         ifFormat       :    dateFormat,
                                            // format of the input field
         showsTime      :    false,          // will display a time selector
         singleClick    :    true,          // single-click mode
         step           :    1,             // show all years in drop-down
                // boxes (instead of every other year as default)
         firstDay       :    1              // first day is monday
     });
 }
</script>