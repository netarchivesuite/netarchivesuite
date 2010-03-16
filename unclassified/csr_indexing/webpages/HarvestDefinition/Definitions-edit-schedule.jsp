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
This page allows the user to edit details of a schedule.
Parameters:
  update: if set, we should update an existing schedule with new values.
  name: the name of the schedule
     * If name is unset and update is unset, the GUI can be used to create a
     * new schedule. It is an error condition to have name unset and
     * update set.
     *
     * edition: the edition of the schedule being edited. If not specified, the
     * name must not refer to an existing schedule.
     * frequency: castable to an integer > 0. Actually the period between
     * harvests in units of ...
     * timespan: allowable values days, hours, weeks, months
     *
     * harvestTime: allowable values, whenever, aTime
     *
     * If whenever is set then ignore remaining values in this group
     * frequency_hours: the hour time for harvesting, integer 0<=x<=23, must
     *                  be set if aTime is set and timespan is not 'hours'
     * frequency_minutes: the minute time for harvesting, integer 0<=x<=59, must
     *                    be set if aTime is set
     * frequency_day: the day of the week on which the harvest is to take place.
     *                Allowable values 1-7. Must be set if timespan is 'weeks'.
     * frequency_date: the date of the month on which harvests are to occur.
     *           Integer 1<=x<=31. Must be set if timespan is 'months'
     *
     * beginAt: allowable values "asSoonAsPossible", "beginning". Not null
     * firstHarvestTime: a date/time field in format DD/MM YYYY hh:mm. Must be
     *                   set if beginAt="beginning"
     *
     * continue: allowable values "forever", "toTime", "numberOfHarvests"
     * endHarvestTime: a date/time field in format DD/MM YYYY hh:mm. Must be
     *                   set if continue="beginning"
     * numberOfHarvests: int > 0. Must be set if continue="numberOfHarvests"
--%><%@ page import="java.text.SimpleDateFormat,
                 java.util.Date,
                 dk.netarkivet.common.exceptions.ForwardedToErrorPage,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.Constants,
                 dk.netarkivet.harvester.datamodel.DailyFrequency,
                 dk.netarkivet.harvester.datamodel.Frequency,
                 dk.netarkivet.harvester.datamodel.HourlyFrequency,
                 dk.netarkivet.harvester.datamodel.MonthlyFrequency,
                 dk.netarkivet.harvester.datamodel.RepeatingSchedule,
                 dk.netarkivet.harvester.datamodel.Schedule,
                 dk.netarkivet.harvester.datamodel.ScheduleDAO, dk.netarkivet.harvester.datamodel.TimedSchedule, dk.netarkivet.harvester.datamodel.WeeklyFrequency, dk.netarkivet.harvester.webinterface.ScheduleDefinition"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N
            = new I18n(Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);
    // update is set to non-null when the page posts data to itself. When
    // null, no updating is done and the form content is just displayed
    String update = request.getParameter("update");
    String name = request.getParameter("name");
    ScheduleDAO sdao = ScheduleDAO.getInstance();
    // if "name" is set then we are editing an existing schedule, otherwise this
    //  is a new schedule
    boolean newSchedule =
            name == null || name.length() == 0 || !sdao.exists(name);
    long edition = 0L;
    Frequency oldFrequency = null;
    Schedule oldSchedule = null;
    if (!newSchedule) {
        oldSchedule = ScheduleDAO.getInstance().read(name);
        edition = oldSchedule.getEdition();
    }
    if (update != null) {
        // unpack the parameters and call the backing method DomainDefinition.editDomain
        try {
            ScheduleDefinition.processRequest(pageContext, I18N);
        } catch (ForwardedToErrorPage e) {
            return;
        }
        // Forward to main schedule page
%>
<jsp:forward page="Definitions-schedules.jsp"/>
<%
    }
    HTMLUtils.generateHeader(
            pageContext);
%>
<jsp:include page="scripts.jsp" flush="true"/>
<%-- Presentation section --%>
<form method="post" action="Definitions-edit-schedule.jsp">
<%
    String value = "";
    if (!newSchedule) {
        value = "value=\"" + HTMLUtils.escapeHtmlValues(name)
                + "\" readonly=\"readonly\"";
%>
<input type="hidden" name="edition" value="<%=edition%>"/>
<%
    }
%>
<h3><fmt:message key="pagetitle;edit.schedule"/></h3>
<input name="update" value="1" type="hidden"/>
<table>
    <tr>
        <td><fmt:message key="prompt;schedule.name"/></td>
        <td><span id="focusElement">
        <input name="name" size="20" <%=value%> type="text"/>
        </span></td>
    </tr>
    <br/><br/>
</table>
<fmt:message key="prompt;comments"/><br/>
<%
    String comments = "";
    if (oldSchedule != null) {
        comments = oldSchedule.getComments();
    }
%>
<textarea name="comments" rows="5" cols="42"><%=
   HTMLUtils.escapeHtmlValues(comments)
%></textarea>
<br/><br/>
<table>
<tr>
    <th colspan="2"><fmt:message key="harvestdefinition.schedule.edit.activateAt"/></th>
</tr>
<tr>
    <%
        String freqString = "value=\"1\"";
        if (!newSchedule) {
            oldFrequency = oldSchedule.getFrequency();
            freqString = "value=\"" + oldFrequency.getNumUnits() + "\"";
        }
    %>
    <td><fmt:message key="harvestdefinition.schedule.edit.every"/></td>
    <td><input name="frequency" <%=freqString%>
               size="2"/>
        <select name="timespan" id="time_select"
                onchange="updateTime(this);">
            <%
                String sel = " selected=\"selected\" ";
            %>
            <option value="hours" <% if (oldFrequency != null) {
                out.print((oldFrequency instanceof HourlyFrequency) ? sel :
                          "");
            } %> ><fmt:message key="harvestdefinition.schedule.edit.hours"/>
            </option>
            <option value="days" <% if (oldFrequency != null) {
                out.print((oldFrequency instanceof DailyFrequency) ? sel :
                          "");
            } %> ><fmt:message key="harvestdefinition.schedule.edit.days"/>
            </option>
            <option value="weeks" <% if (oldFrequency != null) {
                out.print((oldFrequency instanceof WeeklyFrequency) ? sel :
                          "");
            } %> ><fmt:message key="harvestdefinition.schedule.edit.weeks"/>
            </option>
            <option value="months" <% if (oldFrequency != null) {
                out.print((oldFrequency instanceof MonthlyFrequency) ? sel :
                          "");
            } %> ><fmt:message key="harvestdefinition.schedule.edit.months"/>
            </option>
        </select>
    </td>
</tr>
<tr>
    <td><fmt:message key="prompt;time.of.day"/></td>
    <%
        String check = " checked=\"checked\"";
        String ch1 = "";
        String ch2 = "";
        if (newSchedule || oldSchedule.getFrequency().isAnytime()) {
            ch1 = check;
        } else {
            ch2 = check;
        }
    %>
    <td><input type="radio" name="harvestTime"
               value="whenever" <%=ch1%>/><fmt:message key="harvestdefinition.schedule.edit.anytime"/>
    </td>
    </tr>
    <tr><td></td>
    <td><input type="radio" name="harvestTime" value="aTime"
               id="atime" <%=ch2%> /><span id="puttimehere"/>
    </td>
    <%
        //Determine values with which to prepopulate the fields
        String min = "0";
        String hr = "0";
        String date = "1";
        int day = 1;
        if (!newSchedule && !oldFrequency.isAnytime()) {
            if (oldFrequency instanceof HourlyFrequency) {
                min = ((HourlyFrequency) oldFrequency).getMinute() + "";
            }
            if (oldFrequency instanceof DailyFrequency) {
                min = ((DailyFrequency) oldFrequency).getMinute() + "";
                hr = ((DailyFrequency) oldFrequency).getHour() + "";
            }
            if (oldFrequency instanceof WeeklyFrequency) {
                min = ((WeeklyFrequency) oldFrequency).getMinute() + "";
                hr = ((WeeklyFrequency) oldFrequency).getHour() + "";
                day = ((WeeklyFrequency) oldFrequency).getDayOfWeek();
            }
            if (oldFrequency instanceof MonthlyFrequency) {
                min = ((MonthlyFrequency) oldFrequency).getMinute() + "";
                hr = ((MonthlyFrequency) oldFrequency).getHour() + "";
                date = ((MonthlyFrequency) oldFrequency).getDayOfMonth()
                       + "";
            }
        }
    %>
    <script type="text/javascript">
        //Set the appropriate fields for minutes/hours/days/date
        updateTime(document.getElementById('time_select'));
        //Prepopulate those fields
        var minuteEl = document.getElementById('frequency_minutes');
        if (minuteEl != null) {
            minuteEl.value = "<%=min%>";
        }
        var hourEl = document.getElementById('frequency_hours');
        if (hourEl != null) {
            hourEl.value = "<%=hr%>";
        }
        var dateEl = document.getElementById('frequency_date');
        if (dateEl != null) {
            dateEl.value = "<%=date%>";
        }
        var dayEl = document.getElementById('frequency_day');
        if (dayEl != null) {
            var dayOptions = dayEl.childNodes;
            for (var i = 0; i < dayOptions.length; i++) {
                if (dayOptions[i].getAttribute("value")
                        == "<%=day%>") {
                    dayOptions[i].selected = "selected";
                }
            }
        }
    </script>
</tr>
</table>
<br/><br/>
<table>
    <tr>
        <th colspan="2"><fmt:message key="harvestdefinition.schedule.edit.startsAt"/></th>
    </tr>
    <tr>
        <% boolean asap = oldSchedule == null
                          || oldSchedule.getStartDate() == null; %>
        <td><input type="radio" name="beginAt" value="asSoonAsPossible"
                <% if (asap) {
                    out.print(check);
                } %> /></td>
        <td><fmt:message key="harvestdefinition.schedule.edit.asap"/></td>
    </tr>
    <tr>
        <td><input type="radio" name="beginAt" value="beginning" id="beginning"
                <% if (!asap) {
                    out.print(check);
                } %> /></td>
        <td><fmt:message key="harvestdefinition.schedule.edit.atHour"/><input name="firstHarvestTime" size="25" id="howOftenField"
                      onChange="check('beginning');"
                <%
                    SimpleDateFormat sdf = new SimpleDateFormat(
                            I18N.getString(response.getLocale(),
                                    "harvestdefinition.schedule.edit.timeformat"));
                    if (oldSchedule != null
                            && oldSchedule.getStartDate() != null) {
                        out.print("value=\"" + HTMLUtils.escapeHtmlValues(
                                sdf.format(oldSchedule.getStartDate())) + "\"");                    }
                %>
                /><span id="howOften">(<fmt:message key="harvestdefinition.schedule.edit.timeformatDescription"/>)</span></td>
    </tr>
</table>
<br/><br/>

<table>
    <tr>
        <th colspan="3"><fmt:message key="harvestdefinition.schedule.edit.continue"/></th>
    </tr>
    <%
        ch1 = "";
        ch2 = "";
        String ch3 = "";
        String endTime = "";
        String numHarv = "";
        if (oldSchedule == null) {
            ch1 = check;
        } else if (oldSchedule instanceof RepeatingSchedule) {
            ch3 = check;
            numHarv = " value=\""
                    + ((RepeatingSchedule) oldSchedule).getRepeats() + "\"";
        } else if (((TimedSchedule) oldSchedule).getEndDate() != null) {
            ch2 = check;
            Date endDate = ((TimedSchedule) oldSchedule).getEndDate();
            endTime = " value=\"" + HTMLUtils.escapeHtmlValues(
                    sdf.format(endDate)) + "\" ";
        } else {
            ch1 = check;
        }
    %>
    <tr>
        <td><input type="radio" name="continue" value="forever" <%=ch1%> /></td>
        <td colspan="2"><fmt:message key="harvestdefinition.schedule.edit.forever"/></td>
    </tr>
    <tr>
        <td><input type="radio" name="continue" value="toTime"
                   id='ending' <%=ch2%>/></td>
        <td><fmt:message key="harvestdefinition.schedule.edit.untilTime"/></td>
        <td><input name="endHarvestTime" size="25" id="endTimeField"
                   onChange="check('ending');"
                <%=endTime%>/><span id="endTime">(<fmt:message key="harvestdefinition.schedule.edit.timeformatDescription"/>)
            </span>
        </td>
    </tr>
    <tr>
        <td><input type="radio" name="continue" value="numberOfHarvests"
                   id="number" <%=ch3%>/></td>
        <td>
            <fmt:message key="until.0.harvests.performed">
            <fmt:param>
        </td><td><input name="numberOfHarvests" size="10" onchange="check('number');" <%=numHarv%> />
            </fmt:param>
            </fmt:message>
        </td>
    </tr>
</table>
<script type="text/javascript">
    addCalendars('<fmt:message key="jscalendar.timeformat"/>');
</script>
<br/>
<input type="submit" value="<fmt:message key="save"/>"/>
</form>
<%
    HTMLUtils.generateFooter(out);
%>