/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.harvester.webinterface;

import javax.servlet.jsp.PageContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.datamodel.DailyFrequency;
import dk.netarkivet.harvester.datamodel.HourlyFrequency;
import dk.netarkivet.harvester.datamodel.MonthlyFrequency;
import dk.netarkivet.harvester.datamodel.RepeatingSchedule;
import dk.netarkivet.harvester.datamodel.Schedule;
import dk.netarkivet.harvester.datamodel.ScheduleDAO;
import dk.netarkivet.harvester.datamodel.TimedSchedule;
import dk.netarkivet.harvester.datamodel.WeeklyFrequency;

/**
 * csr forgot to comment this!
 *
 */

public class ScheduleDefinitionTester extends WebinterfaceTestCase {

    private static final String DATE_FORMAT = "dd/M yyyy HH:mm";
    private static ScheduleDAO sdao = null;

    public ScheduleDefinitionTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        Date start = format.parse("12/11 2006 22:30");
        Schedule schedule1 = new RepeatingSchedule(start, 10,
                new DailyFrequency(1, 23, 0), "schedule1", "");
        sdao = ScheduleDAO.getInstance();
        sdao.create(schedule1);
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * test that a schedule already in the dao can be updated.
     * @throws ParseException
     */
    public void testProcessRequestUpdateSchedule() throws ParseException {
        Long old_edition = ScheduleDAO.getInstance().read("schedule1").getEdition();
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put("name", new String[] {"schedule1"});
        parameterMap.put("edition", new String[] {""+old_edition});
        parameterMap.put("update", new String[] {"1"});
        parameterMap.put("frequency", new String[] {"1"});
        parameterMap.put("timespan", new String[] {"weeks"});
        parameterMap.put("harvestTime", new String[] {"whenever"});
        parameterMap.put("beginAt", new String[] {"beginning"});
        String startDateString = "06/09 2005 12:30";
        parameterMap.put("firstHarvestTime", new String[] {startDateString});
        parameterMap.put("continue", new String[] {"forever"});
        final TestServletRequest servletRequest = new TestServletRequest();;
        servletRequest.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(servletRequest);
        ScheduleDefinition.processRequest(pageContext, I18N);
        //
        // Check (some of the) changed values
        //
        TimedSchedule schedule_updated = (TimedSchedule) sdao.read("schedule1");
        assertTrue("Schedule should now be weekly",
                schedule_updated.getFrequency() instanceof WeeklyFrequency);
        assertEquals("", schedule_updated.getStartDate(),
                (new SimpleDateFormat(DATE_FORMAT)).parse(startDateString));
        assertNull("End date should be null. ",schedule_updated.getEndDate());
    }

    /**
     * Test creation of a new hourly schedule running as soon as possible
     * and forever at 15 minutes past the hour
     */
    public void testProcessRequestNewScheduleHourly() {
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put("name", new String[] {"schedule2"});
        parameterMap.put("update", new String[] {"1"});
        parameterMap.put("frequency", new String[] {"3"});
        parameterMap.put("timespan", new String[] {"hours"});
        parameterMap.put("harvestTime", new String[] {"aTime"});
        parameterMap.put("frequency_minutes", new String[] {"15"});
        parameterMap.put("beginAt", new String[] {"asSoonAsPossible"});
        parameterMap.put("continue", new String[] {"forever"});
        final TestServletRequest servletRequest = new TestServletRequest();;
        servletRequest.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(servletRequest);
        ScheduleDefinition.processRequest(pageContext, I18N);
        //
        // Check (some of the) values
        //
        TimedSchedule schedule_updated = (TimedSchedule) sdao.read("schedule2");
        assertTrue("Frequency should be hourly", schedule_updated.getFrequency()
                instanceof HourlyFrequency);
        assertEquals("Should run every third hour",3, schedule_updated.getFrequency().getNumUnits());
        assertNull("No start date should be specified", schedule_updated.getStartDate());
        assertNull("No end date should be specified", schedule_updated.getEndDate()) ;
    }

    /**
     * Test creation of a new daily schedule, running at a particular time,
     * beginning at a particular time, and continuing forever.
     * @throws ParseException
     */
    public void testProcessRequestNewDailySchedule() throws ParseException {
        String startDateString = "12/01 2007 16:16";
        Date startDate = (new SimpleDateFormat(DATE_FORMAT).parse(startDateString));
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put("name", new String[] {"schedule2"});
        parameterMap.put("update", new String[] {"1"});
        parameterMap.put("frequency", new String[] {"2"});
        parameterMap.put("timespan", new String[] {"days"});
        parameterMap.put("harvestTime", new String[] {"aTime"});
        parameterMap.put("frequency_hours", new String[] {"14"});
        parameterMap.put("frequency_minutes", new String[] {"25"});
        parameterMap.put("beginAt", new String[] {"beginning"});
        parameterMap.put("firstHarvestTime", new String[] {startDateString});
        parameterMap.put("continue", new String[] {"forever"});
        final TestServletRequest servletRequest = new TestServletRequest();;
        servletRequest.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(servletRequest);
        ScheduleDefinition.processRequest(pageContext, I18N);
        //
        // Check (some of the) values
        //
        TimedSchedule schedule_updated = (TimedSchedule) sdao.read("schedule2");
        DailyFrequency freq = (DailyFrequency) schedule_updated.getFrequency();
        assertEquals("Hours should be as set", 14, freq.getHour());
        assertEquals("Start date should be as set.", startDate, schedule_updated.getStartDate());
    }

    /**
     * Test creation of a new weekly schedule, running at any time and as soon
     * as possible, ending on a specific date.
     * @throws ParseException
     */
    public void testProcessRequestNewWeeklySchedule() throws ParseException {
        String endDateString = "12/01 2007 16:16";
        Date endDate = (new SimpleDateFormat(DATE_FORMAT).parse(endDateString));
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put("name", new String[] {"schedule2"});
        parameterMap.put("update", new String[] {"1"});
        parameterMap.put("frequency", new String[] {"5"});
        parameterMap.put("timespan", new String[] {"weeks"});
        parameterMap.put("harvestTime", new String[] {"whenever"});
        parameterMap.put("beginAt", new String[] {"asSoonAsPossible"});
        parameterMap.put("continue", new String[] {"toTime"});
        parameterMap.put("endHarvestTime", new String[] {endDateString});
        final TestServletRequest servletRequest = new TestServletRequest();;
        servletRequest.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(servletRequest);
        ScheduleDefinition.processRequest(pageContext, I18N);
        //
        // Check (some of the) values
        //
        TimedSchedule schedule_updated = (TimedSchedule) sdao.read("schedule2");
        WeeklyFrequency freq = (WeeklyFrequency) schedule_updated.getFrequency();
        assertEquals("End time should be as set.", endDate,  schedule_updated.getEndDate());
        assertNull("Start time should be null", schedule_updated.getStartDate());
    }

    /**
     * Test creation of a new monthly schedule, running at a particular time,
     * starting as soon as possible, and running for a specific number of
     * harvests.
     * @throws ParseException
     */
    public void testProcessRequestNewMonthlySchedule() throws ParseException {
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put("name", new String[] {"schedule2"});
        parameterMap.put("update", new String[] {"1"});
        parameterMap.put("frequency", new String[] {"2"});
        parameterMap.put("timespan", new String[] {"months"});
        parameterMap.put("harvestTime", new String[] {"whenever"});
        parameterMap.put("beginAt", new String[] {"asSoonAsPossible"});
        parameterMap.put("continue", new String[] {"numberOfHarvests"});
        parameterMap.put("numberOfHarvests", new String[] {"20"});
        final TestServletRequest servletRequest = new TestServletRequest();;
        servletRequest.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(servletRequest);
        ScheduleDefinition.processRequest(pageContext, I18N);
        //
        // Check (some of the) values
        //
        RepeatingSchedule schedule_updated = (RepeatingSchedule) sdao.read("schedule2");
        MonthlyFrequency freq = (MonthlyFrequency) schedule_updated.getFrequency();
        assertEquals("Number of harvests should be as specified", 20, schedule_updated.getRepeats());
    }

}
