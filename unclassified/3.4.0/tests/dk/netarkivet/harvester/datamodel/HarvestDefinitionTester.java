/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
package dk.netarkivet.harvester.datamodel;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.testutils.CollectionAsserts;


/**
 */
public class HarvestDefinitionTester extends DataModelTestCase {
    private Schedule schedule;

    public HarvestDefinitionTester(String sTestName) {
        super(sTestName);
    }

    /**
     * Creating a valid schedule.
     * @throws Exception
     */
    public void setUp() throws Exception {
        super.setUp();
        schedule = TestInfo.getDefaultSchedule();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testValidityOfConstructorArguments() {

        /* Test validity of constructor parameters:
         * (List domainConfigurations,
                             Schedule schedule,
                             String harvestDefName,
                             String harvestDefComment)

         1. domainConfigurations:
           domainConfigurations != null

           More checks of validity of each DomainConfiguration?

         2. schedule must exist:
           schedule != null && schedule in ScheduleDAO.getAllSchedules()

         3. harvestDefName != null && harvestDefName != ""

         4. harvestDefComment != null
        */

        // Ad. 1:
        try {

            HarvestDefinition.createPartialHarvest(null, schedule,
                                                   TestInfo.DEFAULT_HARVEST_NAME,
                                                   TestInfo.DEFAULT_HARVEST_COMMENT);
            fail("List of domainConfigurations must not be null");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // Ad. 2:

        // Create a legal list of configurations
        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);
        List<DomainConfiguration> domainConfigs = new ArrayList<DomainConfiguration>();
        domainConfigs.add(cfg1);

        try {
            HarvestDefinition.createPartialHarvest(domainConfigs, null,
                                                   TestInfo.DEFAULT_HARVEST_NAME,
                                                   TestInfo.DEFAULT_HARVEST_COMMENT);
            fail("Null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        Schedule unknown_schedule
                = Schedule.getInstance(null, null, new HourlyFrequency(1),
                                       "UnknownSchedule", "");
        try {
            HarvestDefinition.createPartialHarvest(domainConfigs, unknown_schedule,
                                                   TestInfo.DEFAULT_HARVEST_NAME,
                                                   TestInfo.DEFAULT_HARVEST_COMMENT);
            fail("Unknown schedule");
        } catch (UnknownID e) {
            // expected
        }

        // Ad. 3:
        try {
            HarvestDefinition.createPartialHarvest(domainConfigs, schedule, null,
                                                   TestInfo.DEFAULT_HARVEST_COMMENT);
            fail("Null not a valid harvest definition name");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            HarvestDefinition.createPartialHarvest(domainConfigs, schedule, "",
                                                   TestInfo.DEFAULT_HARVEST_COMMENT);
            fail("Empty string not a valid harvest definition name");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // Ad. 4:
        try {
            HarvestDefinition.createPartialHarvest(domainConfigs, schedule,
                                                   TestInfo.DEFAULT_HARVEST_NAME, null);
            fail("Null not a valid harvest definition comment");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    public void testValidityOfArgumentsNextDate() {
        //  Test exceptions for setters of nextDate and numEvents
        // 1) Setting nextDate to null should _not_ throw exception

        // Create a legal list of configurations
        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);
        List<DomainConfiguration> domainConfigs = new ArrayList<DomainConfiguration>();
        domainConfigs.add(cfg1);

        PartialHarvest harvestDef
                = HarvestDefinition.createPartialHarvest(domainConfigs, schedule,
                                                         TestInfo.DEFAULT_HARVEST_NAME,
                                                         TestInfo.DEFAULT_HARVEST_COMMENT);
        try {
            harvestDef.setNextDate(null);
        } catch (ArgumentNotValid e) {
            fail("No exception expected on null-date");
        }
    }

    public void testValidityOfArgumentsNumEvents() {
        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);
        List<DomainConfiguration> domainConfigs = new ArrayList<DomainConfiguration>();
        domainConfigs.add(cfg1);

        PartialHarvest harvestDef
                = HarvestDefinition.createPartialHarvest(domainConfigs, schedule,
                                                         TestInfo.DEFAULT_HARVEST_NAME,
                                                         TestInfo.DEFAULT_HARVEST_COMMENT);
        try {
            harvestDef.setNumEvents(-1);
            fail("Expected exception on negative numEvents");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            harvestDef.setNumEvents(0);
        } catch (ArgumentNotValid e) {
            fail("No exception expected on negative numEvents");
        }
    }


    /**
     * Verify that a HarvestDefinition can be created and the correct data
     * retrieved.
     */
    public void testSetAndGet() {

        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);

        List<DomainConfiguration> domainConfigs = new ArrayList<DomainConfiguration>();
        domainConfigs.add(cfg1);

        Date before = new Date(System.currentTimeMillis() / 1000 * 1000);

        PartialHarvest harvestDef
                = HarvestDefinition.createPartialHarvest(domainConfigs, schedule, TestInfo.DEFAULT_HARVEST_NAME,
                                                         TestInfo.DEFAULT_HARVEST_COMMENT);

        assertNotNull("A valid harvest definition expected", harvestDef);

        Date after = new Date(System.currentTimeMillis() / 1000 * 1000);

        // verify the start date and the number of events are set correctly

        // Note, we can't get the time that was "now" when the HarvestDefinition
        // was created, so the best we can do is check that the date is correct
        // for a date either before or after the harvest definition was
        // created or the schedule starts "now".
        // This should be okay, unless the creation takes more than an hour
        Date firstEvent = harvestDef.getNextDate();
        assertTrue("The first event must not happen before now (" + before
                   + "), but happens at "
                   + firstEvent, firstEvent.compareTo(before) <= 0);
        assertTrue("The first event must be consistent with schedule",
                   firstEvent.equals(schedule.getFirstEvent(before)) ||
                   firstEvent.equals(schedule.getFirstEvent(after)) ||
                   schedule.getFirstEvent(firstEvent).equals(firstEvent));

        assertEquals("The new harvest definition must have run 0 times",
                     0, harvestDef.getNumEvents());

        // verify that data can be retrieved from the harvest definition again:
        assertEquals("Value from CTOR expected",
                     schedule, harvestDef.getSchedule());
        assertEquals("Value from CTOR expected",
                     TestInfo.DEFAULT_HARVEST_NAME, harvestDef.getName());
        assertEquals("Value from CTOR expected",
                     TestInfo.DEFAULT_HARVEST_COMMENT, harvestDef.getComments());
        CollectionAsserts.assertIteratorEquals("Value from CTOR expected",
                                               domainConfigs.iterator(), harvestDef.getDomainConfigurations());

        //verify getters and setters
        Long id = new Long(42);
        harvestDef.setOid(id);
        assertEquals("ID  set expected", id, harvestDef.getOid());

        Date date = new Date();
        harvestDef.setSubmissionDate(date);
        assertEquals("Submission date value set expected",
                     date, harvestDef.getSubmissionDate());

        harvestDef.setNumEvents(42);
        assertEquals("Number of events getter should get what I set",
                     42, harvestDef.getNumEvents());

        harvestDef.setNextDate(date);
        assertEquals("Next date getter should get what I set",
                     date, harvestDef.getNextDate());
    }

    /**
     * Check that we can set and get the DomainConfigurations
     */
    public void testSetConfigurations() {
        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);
        cfg1.setOrderXmlName(TestInfo.ORDER_XML_NAME);

        List<DomainConfiguration> domainConfigs = new ArrayList<DomainConfiguration>();
        domainConfigs.add(cfg1);

        PartialHarvest harvestDef
                = HarvestDefinition.createPartialHarvest(domainConfigs,
                                                         schedule, TestInfo.DEFAULT_HARVEST_NAME, TestInfo.DEFAULT_HARVEST_COMMENT);

        //Create two new DomainConfigurations
        DomainConfiguration cfg2 = TestInfo.getConfig(d, "config2");
        DomainConfiguration cfg3 = TestInfo.getDefaultConfig(d);
        //Add cfg2
        List<DomainConfiguration> l1 = new ArrayList<DomainConfiguration>();
        l1.add(cfg2);
        l1.add(cfg3);
        harvestDef.setDomainConfigurations(l1);
        Iterator l2 = harvestDef.getDomainConfigurations();
        assertTrue("Should get at least one element", l2.hasNext());
        Object o1 = l2.next();
        // Check that we get two elements out
        assertTrue("Expected to get two configurations: ", l2.hasNext());
        Object o2 = l2.next();
        assertTrue("Did not get the same objects back",
                   (cfg2 == o1 && cfg3 == o2) || (cfg2 == o2 && cfg3 == o1));
        assertFalse("Expected no more than two configurations", l2.hasNext());
    }

    /**
     * Test that duplicate objects in the configuration list are removed
     */
    public void testSetConfigurationsWithDuplicates() {
        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);

        List<DomainConfiguration> domainConfigs = new ArrayList<DomainConfiguration>();
        domainConfigs.add(cfg1);

        PartialHarvest harvestDef
                = HarvestDefinition.createPartialHarvest(domainConfigs,
                                                         schedule, TestInfo.DEFAULT_HARVEST_NAME, TestInfo.DEFAULT_HARVEST_COMMENT);

        //Create two new DomainConfigurations
        DomainConfiguration cfg2 = TestInfo.getConfig(d, "config2");
        DomainConfiguration cfg3 = TestInfo.getDefaultConfig(d);
        //Add cfg2
        List<DomainConfiguration> l1 = new ArrayList<DomainConfiguration>();
        l1.add(cfg2);
        l1.add(cfg3);
        l1.add(cfg3);
        harvestDef.setDomainConfigurations(l1);
        Iterator l2 = harvestDef.getDomainConfigurations();
        assertTrue("Should get at least one element", l2.hasNext());
        Object o1 = l2.next();
        // Check that we get two elements out
        assertTrue("Expected to get two configurations: ", l2.hasNext());
        Object o2 = l2.next();
        assertTrue("Did not get the same objects back",
                   (cfg2 == o1 && cfg3 == o2) || (cfg2 == o2 && cfg3 == o1));
        assertFalse("Expected no more than two configurations", l2.hasNext());
    }


    /**
     * Test runNow for actually returning the correct boolean
     */
    public void testRunNowPartialHarvest() {
        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);
        List<DomainConfiguration> domainConfigs = new ArrayList<DomainConfiguration>();
        domainConfigs.add(cfg1);

        PartialHarvest harvestDef
                = HarvestDefinition.createPartialHarvest(domainConfigs,
                                                         schedule, TestInfo.DEFAULT_HARVEST_NAME, TestInfo.DEFAULT_HARVEST_COMMENT);
        Calendar cal = new GregorianCalendar(2005, Calendar.FEBRUARY, 21,
                                             12, 0, 0);
        Date testDate = cal.getTime();
        cal.add(Calendar.SECOND, -1);
        Date beforeDate = cal.getTime();
        cal.add(Calendar.SECOND, 2);
        Date afterDate = cal.getTime();
        harvestDef.setNextDate(testDate);

        assertTrue("Should start now if nextDate is now",
                   harvestDef.runNow(testDate));
        assertTrue("Should start now if now is after nextDate",
                   harvestDef.runNow(afterDate));
        assertFalse("Should not start now if now is before nextDate",
                    harvestDef.runNow(beforeDate));

    }


    /**
     * Test runNow for actually returning the correct boolean
     */
    public void testRunNowFullHarvest() {
        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);

        FullHarvest harvestDef
                = HarvestDefinition.createFullHarvest(TestInfo.DEFAULT_HARVEST_NAME,
                                                      TestInfo.DEFAULT_HARVEST_COMMENT, null, 10000, Constants.DEFAULT_MAX_BYTES);
        Long id = new Long(42);
        harvestDef.setOid(id);
        harvestDef.setSubmissionDate(new Date());

        assertTrue("Before creating any jobs, runNow() should be true",
                   harvestDef.runNow(new Date()));

        harvestDef.createJobs();

        assertFalse("After all subsequent creations of jobs, runNow() should return false",
                    harvestDef.runNow(new Date()));

        harvestDef.createJobs();
        assertFalse("After all subsequent creations of jobs, runNow() should return false",
                    harvestDef.runNow(new Date()));
    }

    /**
     * Test createJobs updates numEvents and nextDate correctly on job creation
     */
    public void testCreateJobsUpdatesEventFields() {
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();

        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);
        List<DomainConfiguration> domainConfigs = new ArrayList<DomainConfiguration>();
        domainConfigs.add(cfg1);

        DomainDAO.getInstance().create(d);

        //A schedule that runs forever
        Schedule sched0 = Schedule.getInstance(null, null, new DailyFrequency(1), "sched0", "");
        ScheduleDAO.getInstance().create(sched0);
        //A schedule that ends _now_
        Schedule sched1 = Schedule.getInstance(null, new Date(), new DailyFrequency(1), "sched1", "");
        ScheduleDAO.getInstance().create(sched1);
        //A schedule that ends after one harvest
        Schedule sched2 = Schedule.getInstance(null, 1, new DailyFrequency(1), "sched2", "");
        ScheduleDAO.getInstance().create(sched2);

        PartialHarvest harvestDef0
                = HarvestDefinition.createPartialHarvest(domainConfigs,
                                                         sched0, TestInfo.DEFAULT_HARVEST_NAME + "1", TestInfo.DEFAULT_HARVEST_COMMENT);
        harvestDef0.setSubmissionDate(new Date());

        PartialHarvest harvestDef1
                = HarvestDefinition.createPartialHarvest(domainConfigs,
                                                         sched1, TestInfo.DEFAULT_HARVEST_NAME + "2", TestInfo.DEFAULT_HARVEST_COMMENT);
        //Hack - the schedule has already timed out at this point, but we want
        //there to be one event, so we force the first date to be set.
        harvestDef1.setNextDate(new Date());
        harvestDef1.setSubmissionDate(new Date());

        PartialHarvest harvestDef2
                = HarvestDefinition.createPartialHarvest(domainConfigs,
                                                         sched2, TestInfo.DEFAULT_HARVEST_NAME + "3", TestInfo.DEFAULT_HARVEST_COMMENT);
        harvestDef2.setSubmissionDate(new Date());

        dao.create(harvestDef0);
        dao.create(harvestDef1);
        dao.create(harvestDef2);

        int numEvents0 = harvestDef0.getNumEvents();
        Date nextDate0 = harvestDef0.getNextDate();
        harvestDef0.createJobs();
        assertEquals("Must count up number of events on generating jobs",
                     numEvents0 + 1, harvestDef0.getNumEvents());
        assertEquals("Must set next date on generating jobs",
                     sched0.getNextEvent(nextDate0, 1),
                     harvestDef0.getNextDate());

        int numEvents1 = harvestDef1.getNumEvents();
        harvestDef1.createJobs();
        assertEquals("Must count up number of events on generating jobs",
                     numEvents1 + 1, harvestDef1.getNumEvents());
        assertEquals("No more events, schedule time ended",
                     null,
                     harvestDef1.getNextDate());

        int numEvents2 = harvestDef2.getNumEvents();
        harvestDef2.createJobs();
        assertEquals("Must count up number of events on generating jobs",
                     numEvents2 + 1, harvestDef2.getNumEvents());
        assertEquals("No more events, just one harvest",
                     null,
                     harvestDef2.getNextDate());
    }

    /** Tests that when creating jobs from a harvest definition, we skip some
     * some jobs if the harvesting has been delayed.
     */
    public void testCreateJobsSkipsEvents() {
        Calendar threeHoursAgo = GregorianCalendar.getInstance();
        threeHoursAgo.add(Calendar.HOUR_OF_DAY, -3);

        TimedSchedule hourlySchedule = new TimedSchedule(null, null,
                                                         new HourlyFrequency(1),
                                                         "hourly", "");
        ScheduleDAO.getInstance().create(hourlySchedule);

        //A harvest definition with an hourly schedule and next date set to
        //three hours ago
        final Domain defaultDomain = TestInfo.getDefaultDomain();
        DomainDAO.getInstance().create(defaultDomain);
        List<DomainConfiguration> dc =
                Collections.singletonList(defaultDomain.getDefaultConfiguration());
        PartialHarvest hd = HarvestDefinition.createPartialHarvest(dc,
                                                                   hourlySchedule, "hd", "");
        hd.setNextDate(threeHoursAgo.getTime());
        hd.setSubmissionDate(new Date());
        HarvestDefinitionDAO.getInstance().create(hd);

        Date now = new Date();
        //The job should run now
        assertTrue("Should be ready to create jobs", hd.runNow(now));

        //One job should have been created
        assertEquals("Should create one job", 1, hd.createJobs());

        //Next date should be in the future
        assertTrue("Next date should be in the future",
                   hd.getNextDate().after(now));

        //No more jobs should be created
        assertFalse("Should create no more jobs", hd.runNow(now));
    }

    /** Tests that inactive harvestdefinitions return runNow=false
     */
    public void testSkipInactive() {
        Calendar threeHoursAgo = GregorianCalendar.getInstance();
        threeHoursAgo.add(Calendar.HOUR_OF_DAY, -3);

        TimedSchedule hourlySchedule = new TimedSchedule(null, null,
                                                         new HourlyFrequency(1),
                                                         "hourly", "");
        ScheduleDAO.getInstance().create(hourlySchedule);

        //A harvest definition with an hourly schedule and next date set to
        //three hours ago
        List<DomainConfiguration> dc =
                Collections.singletonList(TestInfo.getDefaultDomain().getDefaultConfiguration());
        PartialHarvest hd = HarvestDefinition.createPartialHarvest(dc,
                                                                   hourlySchedule, "hd", "");
        hd.setNextDate(threeHoursAgo.getTime());
        hd.setSubmissionDate(new Date());
        HarvestDefinitionDAO.getInstance().create(hd);

        Date now = new Date();

        //The job should run now
        assertTrue("Active definition expected (default value)", hd.getActive());
        assertTrue("Should be ready to create jobs", hd.runNow(now));

        // Inactivate the definition
        hd.setActive(false);
        assertFalse("Inactive definition expected (default value)", hd.getActive());
        assertFalse("Inactive defintions shoud NOT be run", hd.runNow(now));

    }

    public void testReset() {
        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);
        List<DomainConfiguration> domainConfigs = new ArrayList<DomainConfiguration>();
        domainConfigs.add(cfg1);

        Date first = new GregorianCalendar(2100, 0, 0).getTime();

        schedule = Schedule.getInstance(first, null,
                                        new DailyFrequency(1),
                                        "test", "");
        ScheduleDAO.getInstance().create(schedule);

        PartialHarvest harvestDef
                = HarvestDefinition.createPartialHarvest(domainConfigs,
                                                         schedule, TestInfo.DEFAULT_HARVEST_NAME, TestInfo.DEFAULT_HARVEST_COMMENT);

        harvestDef.setNextDate(new Date());
        harvestDef.setNumEvents(99);

        harvestDef.reset();

        assertEquals("Numevents should be reset", 0, harvestDef.getNumEvents());
        assertEquals("Date should be reset", first, schedule.getFirstEvent(new Date()));
    }

    /**
     * Verify that jobs are created correctly according to the expected
     * size of the domains.
     * The size constraint is initially defined so that all domains
     * fit into the same job.
     * The size constraint is then changed so that two jobs are created
     * Finally the size constraint is changed so that a job is created for
     * each domain
     */
    public void testCreateJobsBySize() {
        // get harvestdefinition (all configurations use same order.xml)
        //Note: The configurations have these expectations:
        //500, 1400, 2400, 4000
        HarvestDefinition hd = TestInfo.getOneOrderXmlConfig();
        HarvestDefinitionDAO hdao = HarvestDefinitionDAO.getInstance();
        hd.setSubmissionDate(new Date());
        hdao.create(hd);

        // set upper limit to allow all configurations in one job
        Settings.set(Settings.JOBS_MAX_RELATIVE_SIZE_DIFFERENCE, "50");
        Settings.set(Settings.JOBS_MAX_TOTAL_JOBSIZE, "40000");
        Settings.set(Settings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE, "100");
        hd = hdao.read(hd.getOid());
        int jobsMade = hd.createJobs();

        // verify only one job is created
        assertEquals("Limits set to allow one job", 1, jobsMade);

        // set upper limit to allow all configurations in one job by using
        // absolute size difference (4000-3500 = 500)
        Settings.set(Settings.JOBS_MAX_RELATIVE_SIZE_DIFFERENCE, "1");
        Settings.set(Settings.JOBS_MAX_TOTAL_JOBSIZE, "40000");
        Settings.set(Settings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE, "3500");
        hd = hdao.read(hd.getOid());
        jobsMade = hd.createJobs();

        // verify only one job is created
        assertEquals("Limits set to allow one job", 1, jobsMade);

        // set upper limit to require 2 jobs
        // (2400 is more than 4 times 500, thus jobs are (500,1400)
        // and (2400,4000))
        Settings.set(Settings.JOBS_MAX_RELATIVE_SIZE_DIFFERENCE, "4");
        Settings.set(Settings.JOBS_MAX_TOTAL_JOBSIZE, "40000");
        Settings.set(Settings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE, "100");
        hd = hdao.read(hd.getOid());
        jobsMade = hd.createJobs();


        // verify that 2 jobs are created
        assertEquals("Limits set to allow 2 jobs", 2, jobsMade);

        // set upper limit to require each configuration resulting in
        // a separate job
        // All configurations are more than 1 time as great as another
        Settings.set(Settings.JOBS_MAX_RELATIVE_SIZE_DIFFERENCE, "1");
        Settings.set(Settings.JOBS_MAX_TOTAL_JOBSIZE, "40000");
        Settings.set(Settings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE, "10");
        hd = hdao.read(hd.getOid());
        jobsMade = hd.createJobs();

        // verify one job per configuration
        assertEquals("Limits set to allow 4 jobs", 4, jobsMade);
    }

    /**
     * Verify that configurations with different order.xml files are separated
     * into different jobs.
     */
    public void testCreateJobsByOrderXml() {
        // get harvestdefinition consisting of configurations with
        // same expected size, 1400, but using 3 different order.xmls
        HarvestDefinition hd = TestInfo.getMultipleOrderXmlConfig();
        HarvestDefinitionDAO hdao = HarvestDefinitionDAO.getInstance();
        hd.setSubmissionDate(new Date());
        hdao.create(hd);

        // set upper limit to allow all configurations in one job
        Settings.set(Settings.JOBS_MAX_RELATIVE_SIZE_DIFFERENCE, "50");
        Settings.set(Settings.JOBS_MAX_TOTAL_JOBSIZE, "40000");
        Settings.set(Settings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE, "10000");
        int jobsMade = hd.createJobs();

        assertEquals("3 different order.xmls used", 3, jobsMade);
    }

    /**
     * Verify that jobs are created in a way where the maximum and
     * minimum Total size limits are obeyed if possible.
     */
    public void testCreateJobsByTotalSizeLimits() {
        // get harvestdefinition (all configurations use same order.xml)
        HarvestDefinition hd = TestInfo.getOneOrderXmlConfig();
        HarvestDefinitionDAO hdao = HarvestDefinitionDAO.getInstance();
        hd.setSubmissionDate(new Date());
        hdao.create(hd);

        // set upper limit to allow all configurations in one job
        Settings.set(Settings.JOBS_MAX_RELATIVE_SIZE_DIFFERENCE, "50");
        Settings.set(Settings.JOBS_MAX_TOTAL_JOBSIZE, "40000");
        Settings.set(Settings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE, "100");
        int jobsMade = hd.createJobs();

        // verify only one job is created
        assertEquals("Limits set to allow one job", 1, jobsMade);

        // set upper limit to require 2 jobs
        Settings.set(Settings.JOBS_MAX_RELATIVE_SIZE_DIFFERENCE, "50");
        Settings.set(Settings.JOBS_MAX_TOTAL_JOBSIZE, "6500");
        Settings.set(Settings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE, "100");
        jobsMade = hd.createJobs();

        // verify that 2 jobs are created
        assertEquals("Limits set to allow 2 jobs", 2, jobsMade);

        // set upper limit to require each configuration resulting in
        // a separate job
        Settings.set(Settings.JOBS_MAX_RELATIVE_SIZE_DIFFERENCE, "50");
        Settings.set(Settings.JOBS_MAX_TOTAL_JOBSIZE, "0");
        Settings.set(Settings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE, "100");
        jobsMade = hd.createJobs();

        // verify one job per configuration
        assertEquals("Limits set to allow 4 jobs", 4, jobsMade);

    }

    /**
     * Get snapshot harvest definition ready for usage in tests.
     * The harvestdefinition is persisted using the DAO before a
     * reference is returned, making it ready for job creation.
     *
     * @return snapshot harvest definition
     */
    public static HarvestDefinition getTestSnapShotHarvestDefinition() {
        HarvestDefinition hd = HarvestDefinition.createFullHarvest("snapshot",
                                                                   "test", null, 124, Constants.DEFAULT_MAX_BYTES);
        hd.setSubmissionDate(new Date());
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
        dao.create(hd);
        return hd;
    }

    /**
     * Verify that a snapshot harvesdefinition creates jobs for all domains
     */
    public void testCreateSnapShot_allDomains() {

        HarvestDefinition hd = getTestSnapShotHarvestDefinition();
        // verify that the harvestdefintion generates jobs for all domains
        List createdJobs = createAndGetJobs(hd);
        DomainDAO dao = DomainDAO.getInstance();
        int countDomains = dao.getCountDomains();
        // count the number of domains jobs are created for
        int totalCountDomains = 0;
        for (Object createdJob : createdJobs) {
            final Job job = (Job) createdJob;
            totalCountDomains += job.getCountDomains();
        }
        assertEquals("Jobs should be created for all domains", totalCountDomains, countDomains);
    }

    /**
     * Verify that a snapshot harvestdefinition is set to start immediately
     */

    public void testCreateSnapShot_scheduleImmediately() {
        HarvestDefinition hd = HarvestDefinition.createFullHarvest("snapshot", "test", null, 124,
                                                                   Constants.DEFAULT_MAX_BYTES);
        assertTrue("The job should start immediately", hd.runNow(new Date()));
    }

    /**
     * Verify that a inactive snapshot harvestdefinition is set not to start
     */

    public void testCreateSnapShot_Inactive() {
        HarvestDefinition hd = HarvestDefinition.createFullHarvest("snapshot", "test", null, 124,
                                                                   Constants.DEFAULT_MAX_BYTES);
        assertTrue("Active definition expected (default)", hd.getActive());
        assertTrue("The job should start immediately", hd.runNow(new Date()));
        // Inactivate and test again
        hd.setActive(false);
        assertFalse("Inactive definition expected (default)", hd.getActive());
        assertFalse("Inactive jobs should not start", hd.runNow(new Date()));
    }

    /**
     * Verify that the constructor supplied harvest limits are used when snapshot harvest jobs are created
     */
    public void testCreateSnapShot_maxObjects() {
        HarvestDefinition hd = getTestSnapShotHarvestDefinition();
        // verify that the harvestdefintion generates jobs for all domains
        List createdJobs = createAndGetJobs(hd);
        for (Object createdJob : createdJobs) {
            final Job job = (Job) createdJob;
            assertEquals("Job should have been created by given HD",
                         hd.getOid(), job.getOrigHarvestDefinitionID());
            // verify that the jobs created are set to max. harvest 124 objects per domain
            assertEquals(
                    "Harvestdefinition settings should override config settings",
                    124, job.getMaxObjectsPerDomain());
        }
    }


    /**
     * Verify the creation of an incremental snapshot harvest.
     */
    public void testCreateIncrementalSnapShot() {
        // Create a set fake historical data, marking one domain fully harvested
        // two as stopped by size or object limit, and not marking the fourth domain.
        // Also some irrelevant data from other harvests is added.
        // A snapshot based on this info should return exactly 3 configurations,
        // namely one for each domain not marked as complete.
        DomainDAO dao = DomainDAO.getInstance();

        assertEquals("There should be 4 domains. If more are added, update this unit test",
                     4, dao.getCountDomains());

        HarvestDefinition hd = HarvestDefinition.createFullHarvest("Full Harvest", "Test of full harvest", null, 2000,
                                                                   Constants.DEFAULT_MAX_BYTES);
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        hd.setSubmissionDate(new Date());
        hddao.create(hd);
        Long hdOID = hd.getOid();

        Domain domain0 = dao.read("dr.dk");
        DomainConfiguration config0 = domain0.getDefaultConfiguration();
        Domain domain1 = dao.read("netarkivet.dk");
        DomainConfiguration config1 = domain1.getDefaultConfiguration();
        Domain domain2 = dao.read("statsbiblioteket.dk");
        DomainConfiguration config2 = domain2.getDefaultConfiguration();
        Domain domain3 = dao.read("kb.dk");
        DomainConfiguration config3 = domain3.getDefaultConfiguration();

        //Reset milliseconds since this is done in the DAO
        long time = System.currentTimeMillis()/1000*1000;
        //An older harvest info that should NOT be returned
        Date then = new Date(time);
        HarvestInfo old_hi0 = new HarvestInfo(new Long(42L), domain0.getName(), config0.getName(), then, 1L, 1L, StopReason.DOWNLOAD_COMPLETE);
        config0.addHarvestInfo(old_hi0);
        dao.update(domain0);

        //An older harvest info from the same hd that should NOT be returned
        HarvestInfo old_hi1 = new HarvestInfo(hdOID, domain1.getName(), config1.getName(), then, 1L, 1L, StopReason.DOWNLOAD_COMPLETE);
        config1.addHarvestInfo(old_hi1);
        dao.update(domain1);

        //Four harvest infos, one for each type
        Date now = new Date(time + 1000);
        HarvestInfo hi0 = new HarvestInfo(hdOID, domain0.getName(),
                                          config0.getName(), now, 1L, 1L,
                                          StopReason.OBJECT_LIMIT);
        config0.addHarvestInfo(hi0);
        dao.update(domain0);

        HarvestInfo hi1 = new HarvestInfo(hdOID, domain1.getName(),
                                          config1.getName(), now, 1L, 1L,
                                          StopReason.SIZE_LIMIT);
        config1.addHarvestInfo(hi1);
        dao.update(domain1);

        HarvestInfo hi2 = new HarvestInfo(hdOID, domain2.getName(),
                                          config2.getName(), now, 1L, 1L,
                                          StopReason.DOWNLOAD_COMPLETE);
        config2.addHarvestInfo(hi2);
        dao.update(domain2);

        HarvestInfo hi3 = new HarvestInfo(hdOID, domain3.getName(),
                                          config3.getName(), now, 1L, 1L,
                                          StopReason.CONFIG_SIZE_LIMIT);
        config3.addHarvestInfo(hi3);
        config3.setMaxBytes(1L);
        dao.update(domain3);

        //An newer harvest info that should NOT be returned
        Date later = new Date(time + 2000);
        HarvestInfo new_hi0 = new HarvestInfo(new Long(43L), domain0.getName(),
                                              config0.getName(), later, 1L, 1L,
                                              StopReason.DOWNLOAD_COMPLETE);
        config0.addHarvestInfo(new_hi0);
        dao.update(domain0);


        hddao.update(hd);

        // Now create the incremental harvestdefinition
        FullHarvest fh = HarvestDefinition.createFullHarvest("Test incremental",
                                                             "Just a test",
                                                             hd.getOid(),
                                                             9999,
                                                             Constants.DEFAULT_MAX_BYTES);
        fh.setSubmissionDate(new Date());
        hddao.create(fh);

        // Verify that only jobs are created for the domains not marked
        // harvested by previous domain
        List<Job> createdJobs = createAndGetJobs(fh);

        List<String> domains = new ArrayList<String>();

        for (final Job job : createdJobs) {
            Set<String> jobdomains = job.getDomainConfigurationMap().keySet();
            for (String jobdomain : jobdomains) {
                domains.add(jobdomain);
            }
        }

        assertEquals("2 domains should be harvested", 2, domains.size());
        assertTrue("dr.dk should be harvested", domains.indexOf("dr.dk") >= 0);
        assertTrue("netarkivet.dk should be harvested", domains.indexOf("netarkivet.dk") >= 0);
    }

    private static List<Job> createAndGetJobs(HarvestDefinition fh) {
        List<Job> oldJobs = IteratorUtils.toList(JobDAO.getInstance().getAll());
        fh.createJobs();
        List<Job> createdJobs = IteratorUtils.toList(JobDAO.getInstance().getAll());
        for (Iterator<Job> i = createdJobs.iterator(); i.hasNext(); ) {
            Job job = i.next();
            for (Job oldjob : oldJobs) {
                if (job.getJobID().equals(oldjob.getJobID())) {
                    i.remove();
                }
            }
        }
        return createdJobs;
    }

    /** Test that we can create a full harvest, even with prev being the same
     */
    public void testCreateFullHarvest() {
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();

        FullHarvest hd1 = new FullHarvest("foo", "bar", null, 2, Constants.DEFAULT_MAX_BYTES);
        hd1.setSubmissionDate(new Date());
        dao.create(hd1);
        FullHarvest hd1a = (FullHarvest) dao.read(hd1.getOid());
        assertEquals("HD read back should be the same", hd1, hd1a);

        hd1.setPreviousHarvestDefinition(hd1.getOid());
        dao.update(hd1);
        FullHarvest hd1b = (FullHarvest) dao.read(hd1.getOid());
        assertEquals("HD read back should be the same, even with cycles",
                     hd1, hd1b);
    }

    /**
     * Verify that it is possible to set and get the active state of a harvestDefinition
     *
     */
    public void testSetGetActive() {
        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(d);
        d.addConfiguration(cfg1);

        List<DomainConfiguration> domainConfigs = new ArrayList<DomainConfiguration>();
        domainConfigs.add(cfg1);

        // create a definition to use for testing
        PartialHarvest harvestDef
                = HarvestDefinition.createPartialHarvest(domainConfigs, schedule, TestInfo.DEFAULT_HARVEST_NAME,
                                                         TestInfo.DEFAULT_HARVEST_COMMENT);

        assertTrue("Initially a definition is assumed active - to be backward compatible", harvestDef.getActive());
        // Change the state and verify the changes
        harvestDef.setActive(true);
        assertTrue("Change should have an effect", harvestDef.getActive());
        harvestDef.setActive(false);
        assertFalse("Change should have an effect", harvestDef.getActive());
    }

    /**
     * Check that ordering done by CompareConfigsDesc is in the order:
     * - template first
     * - byte limit second
     * - expected number of objects third
     */
    public void testCompareConfigsDesc() throws NoSuchFieldException,
                                                IllegalAccessException,
                                                InvocationTargetException,
                                                InstantiationException,
                                                NoSuchMethodException {
        //Make some configs to sort...(Note: Expected number of objects are in
        //this case the same...)
        Domain d = Domain.getDefaultDomain("adomain.dk");
        List<SeedList> seedlists = Arrays.asList(new SeedList[]{d.getAllSeedLists().next()});
        DomainConfiguration cfg1 = new DomainConfiguration("config1", d,
                                                           seedlists,
                                                           new ArrayList<Password>());
        cfg1.setMaxBytes(1000000);
        cfg1.setOrderXmlName("X");
        d.addConfiguration(cfg1);
        DomainConfiguration cfg2 = new DomainConfiguration("config2", d,
                                                           seedlists,
                                                           new ArrayList<Password>());
        cfg2.setMaxBytes(3000000);
        cfg2.setOrderXmlName("X");
        d.addConfiguration(cfg2);
        DomainConfiguration cfg3 = new DomainConfiguration("config3", d,
                                                           seedlists,
                                                           new ArrayList<Password>());
        cfg3.setMaxBytes(2000000);
        cfg3.setOrderXmlName("Y");
        d.addConfiguration(cfg3);
        DomainConfiguration cfg4 = new DomainConfiguration("config4", d,
                                                           seedlists,
                                                           new ArrayList<Password>());
        cfg4.setMaxBytes(2000000);
        cfg4.setOrderXmlName("X");
        d.addConfiguration(cfg4);
        DomainConfiguration cfg5 = new DomainConfiguration("config5", d,
                                                           seedlists,
                                                           new ArrayList<Password>());
        cfg5.setMaxBytes(1000000);
        cfg5.setOrderXmlName("Y");
        d.addConfiguration(cfg5);
        DomainConfiguration cfg6 = new DomainConfiguration("config6", d,
                                                           seedlists,
                                                           new ArrayList<Password>());
        cfg6.setMaxBytes(3000000);
        cfg6.setOrderXmlName("Y");
        d.addConfiguration(cfg6);
        List<DomainConfiguration> list
                = Arrays.asList(new DomainConfiguration[] {
                    cfg1,cfg2,cfg3,cfg4,cfg5,cfg6});

        //Get the private inner class
        Class c = null;
        Class[] cs = HarvestDefinition.class.getDeclaredClasses();
        for (Class ac : cs) {
            if (ac.getName().endsWith("$CompareConfigsDesc")) {
                c = ac;
            }
        }
        assertNotNull("If this fails we no longer have the comparator as inner "
                      + "class."
                      + " In that case update this unit test to make sure we"
                      + " get the CompareConfigDesc comparator.", c);
        Constructor declaredConstructor = c.getDeclaredConstructor(long.class,
                                                                   long.class);

        //Make a comparator where the deciding limit is always the domain config
        Comparator<DomainConfiguration> compareconfigdesc
            = (Comparator<DomainConfiguration>)
                declaredConstructor.newInstance(-1, 4000000L);
        Collections.sort(list, compareconfigdesc);

        //Test order
        assertEquals("First order template with highest byte limit expected",
                     cfg2, list.get(0));
        assertEquals("First order template with second byte limit expected",
                     cfg4, list.get(1));
        assertEquals("First order template with lowest byte limit expected",
                     cfg1, list.get(2));
        assertEquals("Second order template with highest byte limit expected",
                     cfg6, list.get(3));
        assertEquals("Second order template with second byte limit expected",
                     cfg3, list.get(4));
        assertEquals("Second order template with lowest byte limit expected",
                     cfg5, list.get(5));

        //resort
        list = Arrays.asList(new DomainConfiguration[] {
                    cfg1,cfg2,cfg3,cfg4,cfg5,cfg6});

        //Make a comparator where the deciding limit is in some cases the
        //harvest limit
        compareconfigdesc
            = (Comparator<DomainConfiguration>) declaredConstructor.newInstance(
                Constants.HERITRIX_MAXOBJECTS_INFINITY, 1500000L);

        Collections.sort(list, compareconfigdesc);

        //Test order
        assertEquals("First order template with first harvest limit expected",
                     cfg2, list.get(0));
        assertEquals("First order template with second harvest limit expected",
                     cfg4, list.get(1));
        assertEquals("First order template with lowest byte limit expected",
                     cfg1, list.get(2));
        assertEquals("Second order template with first harvest limit expected",
                     cfg3, list.get(3));
        assertEquals("Second order template with second harvest limit expected",
                     cfg6, list.get(4));
        assertEquals("Second order template with lowest byte limit expected",
                     cfg5, list.get(5));

        //Tweak expected number in the configurations by adding harvest info
        d.getHistory().addHarvestInfo(new HarvestInfo(42L, 1L, d.getName(),
                                                      cfg2.getName(),
                                                      new Date(), 1000000, 2000,
                                                      StopReason.DOWNLOAD_COMPLETE));
        d.getHistory().addHarvestInfo(new HarvestInfo(42L, 1L, d.getName(),
                                                      cfg3.getName(),
                                                      new Date(), 1000000, 3000,
                                                      StopReason.DOWNLOAD_COMPLETE));
        d.getHistory().addHarvestInfo(new HarvestInfo(42L, 1L, d.getName(),
                                                      cfg4.getName(),
                                                      new Date(), 1000000, 4000,
                                                      StopReason.DOWNLOAD_COMPLETE));
        d.getHistory().addHarvestInfo(new HarvestInfo(42L, 1L, d.getName(),
                                                      cfg1.getName(),
                                                      new Date(), 1000000, 1000,
                                                      StopReason.DOWNLOAD_COMPLETE));
        d.getHistory().addHarvestInfo(new HarvestInfo(42L, 1L, d.getName(),
                                                      cfg5.getName(),
                                                      new Date(), 1000000, 5000,
                                                      StopReason.DOWNLOAD_COMPLETE));
        d.getHistory().addHarvestInfo(new HarvestInfo(42L, 1L, d.getName(),
                                                      cfg6.getName(),
                                                      new Date(), 1000000, 6000,
                                                      StopReason.DOWNLOAD_COMPLETE));

        // Reset two config limits so we have equal limit and template in some
        // cases to make sure the expectation is the deciding factor
        cfg3.setMaxBytes(1000000);
        cfg4.setMaxBytes(1000000);

        //resort
        list = Arrays.asList(new DomainConfiguration[] {
                    cfg1,cfg2,cfg3,cfg4,cfg5,cfg6});

        //make a comparator with a high limit
        compareconfigdesc
            = (Comparator<DomainConfiguration>)
                declaredConstructor.newInstance(-1, 9000000L);
        Collections.sort(list, compareconfigdesc);

        //check order - as first order except where expectation decides
        assertEquals("First order template with highest byte limit expected",
                     cfg2, list.get(0));
        assertEquals("First order template with low byte limit and high "
                     + "expectation expected",
                     cfg4, list.get(1));
        assertEquals("First order template with low byte limit and low "
                     + "expectation expected",
                     cfg1, list.get(2));
        assertEquals("Second order template with highest byte limit expected",
                     cfg6, list.get(3));
        assertEquals("Second order template with low byte limit and high "
                     + "expectation expected",
                     cfg5, list.get(4));
        assertEquals("Second order template with low byte limit and low "
                     + "expectation expected",
                     cfg3, list.get(5));
    }

}
