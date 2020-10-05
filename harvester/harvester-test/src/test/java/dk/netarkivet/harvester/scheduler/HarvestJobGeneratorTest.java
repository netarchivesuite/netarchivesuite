/*
 * #%L
 * Netarchivesuite - harvester - test
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

package dk.netarkivet.harvester.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.harvester.datamodel.Schedule;
import dk.netarkivet.harvester.datamodel.ScheduleDAO;
import dk.netarkivet.harvester.datamodel.TemplateDAO;
import dk.netarkivet.harvester.datamodel.WeeklyFrequency;
import dk.netarkivet.harvester.scheduler.HarvestJobGenerator.JobGeneratorTask;
import dk.netarkivet.testutils.ThreadUtils;

@SuppressWarnings("unused")
public class HarvestJobGeneratorTest extends DataModelTestCase {
    @Test
    @Ignore
    public void testGenerateJobs() throws Exception {
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        TemplateDAO.getInstance();

        final String scheduleName = "foo";
        final String noComments = "";

        final GregorianCalendar cal = new GregorianCalendar();
        // Avoids tedious rounding problems
        cal.set(Calendar.MILLISECOND, 0);
        // Make sure existing job is in the future
        PartialHarvest hd = (PartialHarvest) hddao.read(Long.valueOf(42));
        cal.add(Calendar.YEAR, 2);
        Date now = cal.getTime();
        Schedule s = Schedule.getInstance(now, 2, new WeeklyFrequency(2), scheduleName, noComments);
        ScheduleDAO.getInstance().create(s);
        hd.setSchedule(s);
        hd.reset();
        hddao.update(hd);

        cal.setTime(new Date()); // reset
        now = cal.getTime();
        generateJobs(now);
        JobDAO jobdao = JobDAO.getInstance();
        List<Job> jobs = IteratorUtils.toList(jobdao.getAll(JobStatus.NEW));
        assertEquals("Should get no job for the HD in the future", 0, jobs.size());
        // Make some harvest definitions with schedules around the given time
        // Requires a list of configurations for each HD
        List<DomainConfiguration> cfgs = new ArrayList<DomainConfiguration>();
        Domain domain = DomainDAO.getInstance().read("netarkivet.dk");
        cfgs.add(domain.getDefaultConfiguration());
        final ScheduleDAO sdao = ScheduleDAO.getInstance();
        HarvestDefinition hd1 = HarvestDefinition.createPartialHarvest(cfgs, sdao.read("Hver hele time"), "Hele time",
                noComments, "EveryBody");
        hd1.setSubmissionDate(new Date());
        hddao.create(hd1);
        HarvestDefinition hd2 = HarvestDefinition.createPartialHarvest(cfgs, sdao.read("Hver nat kl 4.00"), "Kl. 4",
                noComments, "EveryBody");
        hd2.setSubmissionDate(new Date());
        hddao.create(hd2);
        generateJobs(now);
        List<Job> jobs1 = IteratorUtils.toList(jobdao.getAll(JobStatus.NEW));
        assertEquals("Should get jobs for no new defs immediately", 0, jobs1.size());

        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MINUTE, 1); // a bit more to avoid rounding errors /tra
        now = cal.getTime();
        generateJobs(now);
        jobs1 = IteratorUtils.toList(jobdao.getAll(JobStatus.NEW));
        assertEquals("Should get jobs for both new defs after a day", 2, jobs1.size());
        // Check that the right HD's came in
        Job j1 = jobs1.get(0);
        Job j2 = jobs1.get(1);
        assertTrue("Neither job must be for HD 42",
                j1.getOrigHarvestDefinitionID() != hd.getOid() && j2.getOrigHarvestDefinitionID() != hd.getOid());
        assertTrue(
                "One of the jobs must be for the new HD " + hd1.getOid() + ", but we got " + jobs1,
                j1.getOrigHarvestDefinitionID().equals(hd1.getOid())
                        || j2.getOrigHarvestDefinitionID().equals(hd1.getOid()));
        assertTrue(
                "One of the jobs must be for the new HD " + hd1.getOid() + ", but we got " + jobs1,
                j1.getOrigHarvestDefinitionID().equals(hd2.getOid())
                        || j2.getOrigHarvestDefinitionID().equals(hd2.getOid()));
        generateJobs(now);
        List<Job> jobs2 = IteratorUtils.toList(jobdao.getAll(JobStatus.NEW));
        assertEquals("Should generate one more job because we are past the time"
                + " when the next hourly job should have been scheduled.", jobs1.size() + 1, jobs2.size());
    }

    /**
     * test that skipping a scheduling because the previous scheduling is still running, or at least the system still
     * thinks it is running. because the id of the harvest is contained in the set
     * HarvestJobGenerator#harvestDefinitionsBeingScheduled
     *
     * @throws Exception
     */

    public void testSkippingScheduling() throws Exception {
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();

        List<DomainConfiguration> cfgs = new ArrayList<DomainConfiguration>();
        Domain domain = DomainDAO.getInstance().read("netarkivet.dk");
        cfgs.add(domain.getDefaultConfiguration());
        final ScheduleDAO sdao = ScheduleDAO.getInstance();
        Date now = new Date();
        Date yesterday = new Date(now.getTime() - (24 * 60 * 60 * 1000));
        HarvestDefinition hd1 = HarvestDefinition.createPartialHarvest(cfgs, sdao.read("Hver hele time"), "Hele time",
                "", "EveryBody");
        hd1.setSubmissionDate(now);
        hddao.create(hd1);
        hddao.updateNextdate(((PartialHarvest) hd1).getOid(), yesterday);
        Iterable<Long> readyHarvestDefinitions = hddao.getReadyHarvestDefinitions(now);
        Iterator<Long> iterator = readyHarvestDefinitions.iterator();
        if (!iterator.hasNext()) {
            fail("At least one harvestdefinition should be ready for scheduling");
        }

        // take the next ready definition, and inject the id of this
        // into the harvestDefinitionsBeingScheduled datastructure and
        Long readyHarvestId = iterator.next();
        @SuppressWarnings("rawtypes")
        Class c = Class.forName(HarvestJobGenerator.class.getName());
        Field f = c.getDeclaredField("harvestDefinitionsBeingScheduled");
        Field f1 = c.getDeclaredField("schedulingStartedMap");
        Set<Long> harvestDefinitionsBeingScheduled = Collections.synchronizedSet(new HashSet<Long>());
        harvestDefinitionsBeingScheduled.add(readyHarvestId);

        Map<Long, Long> schedulingStartedMap = Collections.synchronizedMap(new HashMap<Long, Long>());
        Long acceptabledelay = 5L * 60 * 1000;
        Long scheduledTime = System.currentTimeMillis() - acceptabledelay - 1000L;
        schedulingStartedMap.put(readyHarvestId, scheduledTime);
        f1.set(null, schedulingStartedMap);
        f.set(null, harvestDefinitionsBeingScheduled);
        // redirect Stdout til myOut
        PrintStream origStdout = System.out;
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(myOut));
        try {
            generateJobs(new Date());
        } finally {
            myOut.close();
            System.setOut(origStdout);
        }
        final String expectedOutput = "[WARNING-Notification] Not creating jobs "
                + "for harvestdefinition #44 (Hele time) " + "as the previous scheduling is still running\n";
        assertTrue("The excepted notification should have been sent, but received instead: " + myOut.toString(), myOut
                .toString().equals(expectedOutput));
    }

    /**
     * Run job generation and wait for the threads created to finish.
     */
    void generateJobs(Date time) throws Exception {
        HarvestChannelRegistry harvestChannelRegistry = new HarvestChannelRegistry();
        harvestChannelRegistry.register("FOCUSED", "");
        JobGeneratorTask jobGeneratorTask = new JobGeneratorTask(harvestChannelRegistry);
        jobGeneratorTask.generateJobs(time);
        waitForJobGeneration();
    }

    private static void waitForJobGeneration() throws TimeoutException, InterruptedException {
        for (int waits = 1; waits <= 20; waits++) {
            boolean threadsRemain = false;
            Thread.sleep(1000);
            final Thread[] threads = ThreadUtils.getAllThreads();
            for (Thread thread : threads) {
                if (thread.getName().indexOf("JobGeneratorTask") != -1) {
                    threadsRemain = true;
                    continue;
                }
            }
            if (!threadsRemain) {
                return;
            }
        }
        throw new TimeoutException(ThreadUtils.getAllThreads().length
                + "JobGeneratorTask thread remain after 20 seconds");
    }
}
