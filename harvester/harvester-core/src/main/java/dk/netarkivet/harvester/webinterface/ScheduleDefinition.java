/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.webinterface;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.DailyFrequency;
import dk.netarkivet.harvester.datamodel.Frequency;
import dk.netarkivet.harvester.datamodel.HourlyFrequency;
import dk.netarkivet.harvester.datamodel.MinuteFrequency;
import dk.netarkivet.harvester.datamodel.MonthlyFrequency;
import dk.netarkivet.harvester.datamodel.Schedule;
import dk.netarkivet.harvester.datamodel.ScheduleDAO;
import dk.netarkivet.harvester.datamodel.WeeklyFrequency;

/**
 * Contains utility methods for creating and editing schedule definitions for harvests.
 */
public final class ScheduleDefinition {

    /*
     * Parameters for the Definitions-edit-schedule.jsp page. They should be self explanatory.
     */
    /** BeginAt parameter. */
    static final String BEGIN_AT_PARAMETER = "beginAt";
    /** FirsrHarvestTime parameter. */
    static final String FIRST_HARVEST_TIME_PARAMETER = "firstHarvestTime";
    /** Continue parameter. */
    static final String CONTINUE_PARAMETER = "continue";
    /** EndHarvestTime parameter. */
    static final String END_HARVEST_TIME_PARAMETER = "endHarvestTime";
    /** Timespan parameter. */
    static final String TIMESPAN_PARAMETER = "timespan";
    /** HarvestTime parameter. */
    static final String HARVEST_TIME_PARAMETER = "harvestTime";
    /** Frequency in minutes parameter. */
    static final String FREQUENCY_MINUTES_PARAMETER = "frequency_minutes";
    /** Frequency in hours parameter. */
    static final String FREQUENCY_HOURS_PARAMETER = "frequency_hours";
    /** Frequency defined by day parameter. */
    static final String FREQUENCY_DAY_PARAMETER = "frequency_day";
    /** Frequency defined by date parameter. */
    static final String FREQUENCY_DATE_PARAMETER = "frequency_date";
    /** Comments parameter. */
    static final String COMMENTS_PARAMETERS = "comments";
    /** Edition parameter. */
    static final String EDITION_PARAMETER = "edition";
    /** Number of harvests parameter. */
    static final String NUMBER_OF_HARVESTS_PARAMETER = "numberOfHarvests";
    /** name parameter. */
    static final String NAME_PARAMETER = "name";
    /** update parameter. */
    static final String UPDATE_PARAMETER = "update";
    /** Frequency parameter. */
    static final String FREQUENCY_PARAMETER = "frequency";

    /**
     * Private constructor. No instances.
     */
    private ScheduleDefinition() {

    }

    /**
     * Processes the request parameters for the page Definitions-edit-schedule.jsp The parameters are first checked for
     * validity. If they are not acceptable, an exception is thrown, otherwise the parameters are passed on to the
     * methods editScheduleDefinition() which edits or creates the relevant schedule.
     * <p>
     * update: if set, execute this method name: the name of the schedule If name is unset and update is unset, the GUI
     * can be used to create a new schedule. It is an error condition to have name unset and update set.
     * <p>
     * edition: the edition of the schedule being edited. If not specified, the name must not refer to an existing
     * schedule. frequency: castable to an integer > 0. Actually the period between harvests in units of ... timespan:
     * allowable values dage, timer, uger, m\u00e5neder
     * <p>
     * harvestTime: allowable values, whenever, aTime
     * <p>
     * If whenever is set then ignore remaining values in this group frequency_hours: the hour time for harvesting,
     * integer 0<=x<=23, must be set if aTime is set and timespan is not "hours" frequency_minutes: the minute time for
     * harvesting, integer 0<=x<=59, must be set if aTime is set and timespan is not "hours" frequency_day: the day of
     * the week on which the harvest is to take place. Allowable values 1-7. Must be set if timespan is set to "weeks".
     * frequency_date: the date of the month on which harvests are to occur. Integer 1<=x<=31. Must be set if timespan
     * is "months"
     * <p>
     * beginAt: allowable values "asSoonAsPossible", "beginning". Not null firstHarvestTime: a date/time field in format
     * DD/MM YYYY hh:mm. Must be set if beginAt="beginning"
     * <p>
     * continue: allowable values "forever", "toTime", "numberOfHarvests" endHarvestTime: a date/time field in format
     * DD/MM YYYY hh:mm. Must be set if continue="beginning" numberOfHarvests: int > 0. Must be set if
     * continue="numberOfHarvests"
     *
     * @param context Context of web request
     * @param i18n I18N information
     */
    public static void processRequest(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ServletRequest request = context.getRequest();
        String update = request.getParameter(UPDATE_PARAMETER);
        if (update == null) {
            return;
        }

        HTMLUtils.forwardOnEmptyParameter(context, NAME_PARAMETER);

        String name = request.getParameter(NAME_PARAMETER).trim();

        long edition = HTMLUtils.parseOptionalLong(context, EDITION_PARAMETER, -1L);
        if (ScheduleDAO.getInstance().exists(name)) {
            if (ScheduleDAO.getInstance().read(name).getEdition() != edition) {
                HTMLUtils.forwardWithRawErrorMessage(
                        context,
                        i18n,
                        "errormsg;schedule.has.changed.0.retry.1",
                        "<br/><a href=\"Definitions-edit-schedule.jsp?name="
                                + HTMLUtils.escapeHtmlValues(HTMLUtils.encode(name)) + "\">", "</a>");
                throw new ForwardedToErrorPage("Schedule '" + name + "' has changed");
            }
        } else {
            // Creating new schedule, always edition 0
            edition = 0;
        }

        Frequency freq = getFrequency(context, i18n);
        Date startDate = getStartDate(context, i18n);
        Date endDate = getEndDate(context, i18n);
        String continueS = request.getParameter(CONTINUE_PARAMETER);
        int repeats = HTMLUtils.parseOptionalLong(context, NUMBER_OF_HARVESTS_PARAMETER, 0L).intValue();

        String comments = request.getParameter(COMMENTS_PARAMETERS);
        updateSchedule(freq, startDate, continueS, endDate, repeats, name, edition, comments);
    }

    /**
     * Update or create the schedule in persistent storage.
     *
     * @param freq The frequency of the schedule
     * @param startDate The start date of the schedule
     * @param continueS The continue-mode of the schedule
     * @param endDate The end date for the schedule (if any)
     * @param repeats How many time should the schedule be repeated
     * @param name The name of the schedule
     * @param edition The edition of the schedule
     * @param comments Any comments associated with the schedule
     */
    private static void updateSchedule(Frequency freq, Date startDate, String continueS, Date endDate, int repeats,
            String name, long edition, String comments) {
        ScheduleDAO sdao = ScheduleDAO.getInstance();
        Schedule schedule;

        if (comments == null) {
            comments = "";
        }
        if (continueS.equals(NUMBER_OF_HARVESTS_PARAMETER)) {
            schedule = Schedule.getInstance(startDate, repeats, freq, name, comments);
        } else {
            schedule = Schedule.getInstance(startDate, endDate, freq, name, comments);
        }

        if (sdao.exists(name)) {
            schedule.setEdition(edition);
            sdao.update(schedule);
        } else {
            sdao.create(schedule);
        }
    }

    /**
     * If the beginAt parameter is set then this returns the first time at which the harvest is to be run. Otherwise it
     * returns null.
     *
     * @param context Web context of the request
     * @param i18n I18N information
     * @return the first time to run the harvest
     */
    private static Date getStartDate(PageContext context, I18n i18n) {
        Date startDate = null;
        HTMLUtils.forwardOnIllegalParameter(context, BEGIN_AT_PARAMETER, "asSoonAsPossible", "beginning");
        String beginAt = context.getRequest().getParameter(BEGIN_AT_PARAMETER);
        if (beginAt.equals("beginning")) {
            String firstHarvestTime = context.getRequest().getParameter(FIRST_HARVEST_TIME_PARAMETER);
            try {
                startDate = (new SimpleDateFormat(I18n.getString(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE,
                        context.getResponse().getLocale(), "harvestdefinition.schedule.edit.timeformat")))
                        .parse(firstHarvestTime);
            } catch (ParseException e) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;illegal.time.value.0", firstHarvestTime);
                throw new ForwardedToErrorPage("Illegal start time format. '" + firstHarvestTime + "'", e);
            }
        }
        return startDate;
    }

    /**
     * If the toTime parameter is set then this returns the last time at which the harvest is to be run. Otherwise it
     * returns null.
     *
     * @param context Web context of the request
     * @param i18n I18N information
     * @return the last time to run the harvest
     */
    private static Date getEndDate(PageContext context, I18n i18n) {
        Date endDate = null;
        HTMLUtils.forwardOnIllegalParameter(context, CONTINUE_PARAMETER, "forever", "toTime", "numberOfHarvests");
        String continueS = context.getRequest().getParameter(CONTINUE_PARAMETER);
        if (continueS.equals("toTime")) {
            String endHarvestTime = context.getRequest().getParameter(END_HARVEST_TIME_PARAMETER);
            try {
                endDate = (new SimpleDateFormat(I18n.getString(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE,
                        context.getResponse().getLocale(), "harvestdefinition.schedule.edit.timeformat")))
                        .parse(endHarvestTime);
            } catch (ParseException e) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;illegal.time.value.0", endHarvestTime, e);
                throw new ForwardedToErrorPage("Illegal end time format. '" + endHarvestTime + "'", e);
            }
        }
        return endDate;
    }

    /**
     * Returns a frequency object specifying whether the harvest is to be minute, hourly, daily, weekly, or monthly and
     * how often it is to be run.
     *
     * @param context Web context of the request
     * @param i18n I18N information
     * @return the Frequency for the harvest
     */
    private static Frequency getFrequency(PageContext context, I18n i18n) {
        int frequency = HTMLUtils.parseAndCheckInteger(context, FREQUENCY_PARAMETER, 1, Integer.MAX_VALUE);
        HTMLUtils.forwardOnEmptyParameter(context, TIMESPAN_PARAMETER, HARVEST_TIME_PARAMETER);
        HTMLUtils.forwardOnIllegalParameter(context, TIMESPAN_PARAMETER, "days", "hours", "weeks", "months", "minutes");
        HTMLUtils.forwardOnIllegalParameter(context, HARVEST_TIME_PARAMETER, "whenever", "aTime");
        ServletRequest request = context.getRequest();
        String timespan = request.getParameter(TIMESPAN_PARAMETER);
        String harvestTime = request.getParameter(HARVEST_TIME_PARAMETER);
        if (harvestTime.equals("whenever")) {
            if (timespan.equals("hours")) {
                return new HourlyFrequency(frequency);
            } else if (timespan.equals("days")) {
                return new DailyFrequency(frequency);
            } else if (timespan.equals("weeks")) {
                return new WeeklyFrequency(frequency);
            } else if (timespan.equals("minutes")) {
                return new MinuteFrequency(frequency);
            } else {
                return new MonthlyFrequency(frequency);
            }
        }
        int frequencyMinutes;
        int frequencyHours = 0;
        int frequencyDay = 0;
        int frequencyDate = 0;
        frequencyMinutes = HTMLUtils.parseAndCheckInteger(context, FREQUENCY_MINUTES_PARAMETER, 0, 59);
        if (!timespan.equals("hours")) {
            frequencyHours = HTMLUtils.parseAndCheckInteger(context, FREQUENCY_HOURS_PARAMETER, 0, 23);
        }
        if (timespan.equals("weeks")) {
            frequencyDay = HTMLUtils.parseAndCheckInteger(context, FREQUENCY_DAY_PARAMETER, 1, 7);
        }
        if (timespan.equals("months")) {
            frequencyDate = HTMLUtils.parseAndCheckInteger(context, FREQUENCY_DATE_PARAMETER, 1, 31);
        }
        if (timespan.equals("hours")) {
            return new HourlyFrequency(frequency, frequencyMinutes);
        } else if (timespan.equals("days")) {
            return new DailyFrequency(frequency, frequencyHours, frequencyMinutes);
        } else if (timespan.equals("weeks")) {
            return new WeeklyFrequency(frequency, frequencyDay, frequencyHours, frequencyMinutes);
        } else if (timespan.equals("minutes")) {
            return new MinuteFrequency(frequency);
        } else {
            return new MonthlyFrequency(frequency, frequencyDate, frequencyHours, frequencyMinutes);
        }
    }
}
