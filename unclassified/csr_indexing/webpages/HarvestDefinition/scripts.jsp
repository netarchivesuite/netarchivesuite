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
                dk.netarkivet.harvester.Constants"
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
    var minutes = 0;
    var hours = 0;
    var day_of_week = 2;
    var day_of_month = 1;

    function saveScheduleValues(form) {
        if (form.frequency_minutes != null) {
            minutes = form.frequency_minutes.value;
        }
        if (form.frequency_hours != null) {
            hours = form.frequency_hours.value;
        }
        if (form.frequency_day != null) {
            day_of_week = form.frequency_day.value;
        }
        if (form.frequency_date != null) {
            day_of_month = form.frequency_date.value;
        }
    }

    function restoreScheduleValues(form) {
        if (form.frequency_minutes != null) {
            form.frequency_minutes.value = minutes;
        }
        if (form.frequency_hours != null) {
            form.frequency_hours.value = hours;
        }
        if (form.frequency_day != null) {
            form.frequency_day.value = day_of_week;
        }
        if (form.frequency_date != null) {
            form.frequency_date.value = day_of_month;
        }
    }

    function check(s) {
        document.getElementById(s).checked=true;
    }

    /**
    * Attach calendars to the two input fields for the edit-calendar page
    * @param The time format for the calendar.
    */
    function addCalendars(timeFormat){
        Calendar.setup({
            inputField     :    "<%=dk.netarkivet.harvester.webinterface
                                      .Constants.HOW_OFTEN_FIELD%>",      
                                               // id of the input field
            ifFormat       :    timeFormat,    // format of the input field
            showsTime      :    true,          // will display a time selector
            singleClick    :    true,          // single-click mode
            step           :    1,             // show all years in drop-down
                   // boxes (instead of every other year as default)
            firstDay       :    1              // first day is monday
        });

        Calendar.setup({
            inputField     :    "<%=dk.netarkivet.harvester.webinterface
                                      .Constants.END_TIME_FIELD%>",
                                               // id of the input field
            ifFormat       :    timeFormat,    // format of the input field
            showsTime      :    true,          // will display a time selector
            singleClick    :    true,          // single-click mode
            step           :    1,             // show all years in drop-down
                   // boxes (instead of every other year as default)
            firstDay       :    1              // first day is monday
        });
    }

    /**
    * Attach the calendar to the nextdate field on the edit-harvestdefinition
    * page
    */
    function setupNextdateCalendar(){
        Calendar.setup({
            inputField     :    "<%=dk.netarkivet.harvester.webinterface
                                      .Constants.NEXTDATE_PARAM%>",
                                               // id of the input field
            ifFormat       :    "<fmt:message key="jscalendar.timeformat"/>",
                                               // format of the input field
            showsTime      :    true,          // will display a time selector
            singleClick    :    true,          // single-click mode
            step           :    1,             // show all years in drop-down
                   // boxes (instead of every other year as default)
            firstDay       :    1              // first day is monday
        });
    }

    function updateTime(select) {
        saveScheduleValues(select.form);

        spanElement = document.getElementById('puttimehere');

        var freq_type;
        for(i=0; i<select.options.length; i++) {
            if (select.options[i].selected == true) {
                freq_type = select.options[i].value;
            }
            //alert(freq_type);
        }
        if (freq_type == 'hours') {
            spanElement.innerHTML = '<fmt:message key="on.the.0th.minute"><fmt:param><input type=\"text\" size=\"2\" name=\"frequency_minutes\" id=\"frequency_minutes\" onchange=\"check(\'atime\');\"/></fmt:param></fmt:message>';
        } else if (freq_type == 'days') {
            spanElement.innerHTML = '<fmt:message key="at.0.1"><fmt:param><input type=\"text\" size=\"2\" name=\"frequency_hours\" id=\"frequency_hours\" onchange=\"check(\'atime\');\"/></fmt:param><fmt:param><input type=\"text\" size=\"2\" name=\"frequency_minutes\" id=\"frequency_minutes\" onchange=\"check(\'atime\');\"/></fmt:param></fmt:message>';
        } else if (freq_type == 'weeks') {
            spanElement.innerHTML = '<fmt:message key="at.0.1.every.2"><fmt:param><input type=\"text\" size=\"2\" name=\"frequency_hours\" id=\"frequency_hours\" onchange=\"check(\'atime\');\"/></fmt:param><fmt:param><input type=\"text\" size=\"2\" name=\"frequency_minutes\" id=\"frequency_minutes\" onchange=\"check(\'atime\');\"/></fmt:param><fmt:param><select name="frequency_day" id="frequency_day" onchange="check(\'atime\');"><option value=\"2\"><fmt:message key="monday"/></option><option value=\"3\"><fmt:message key="tuesday"/></option><option value=\"4\"><fmt:message key="wednesday"/></option><option value=\"5\"><fmt:message key="thursday"/></option><option value=\"6\"><fmt:message key="friday"/></option><option value=\"7\"><fmt:message key="saturday"/></option><option value=\"1\"><fmt:message key="sunday"/></option></select></fmt:param></fmt:message>';
        } else if (freq_type == 'months') {
            spanElement.innerHTML = '<fmt:message key="at.0.1.on.2"><fmt:param><input type=\"text\" size=\"2\" name=\"frequency_hours\" id=\"frequency_hours\" onchange=\"check(\'atime\');\"/></fmt:param><fmt:param><input type=\"text\" size=\"2\" name=\"frequency_minutes\" id=\"frequency_minutes\" onchange=\"check(\'atime\');\"/></fmt:param><fmt:param><input type="text" size="2" name="frequency_date" id="frequency_date" onchange="check(\'atime\');\"></fmt:param></fmt:message>';
        }
        restoreScheduleValues(select.form);
    }
</script>