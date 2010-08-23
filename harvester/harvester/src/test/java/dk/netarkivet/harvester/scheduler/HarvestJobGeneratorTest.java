/* File:    $Id: $
 * Revision: $Revision: $
 * Author:   $Author: $
 * Date:     $Date: $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.harvester.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;
import dk.netarkivet.common.utils.IteratorUtils;
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

public class HarvestJobGeneratorTest extends TestCase {    
    private final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();

    /**
     * Test that we can get jobs created from HDs.
     *
     * @throws Exception
     */
    public void testGenerateJobs() throws Exception {
        TemplateDAO.getInstance();

        final GregorianCalendar cal = new GregorianCalendar();
        // Avoids tedious rounding problems
        cal.set(Calendar.MILLISECOND, 0);
        // Make sure existing job is in the future
        PartialHarvest hd = (PartialHarvest) hddao.read(new Long(42));
        cal.add(Calendar.YEAR, 2);
        Date now = cal.getTime();
        Schedule s = Schedule.getInstance(now, 2, new WeeklyFrequency(2),
                "foo", "");
        ScheduleDAO.getInstance().create(s);
        hd.setSchedule(s);
        hd.reset();
        hddao.update(hd);
        // Remove the full harvest -- we don't want it interfering here.
        hddao.delete(new Long(43));
        cal.setTime(new Date()); // reset
        now = cal.getTime();
        generateJobs(0);
        JobDAO jobdao = JobDAO.getInstance();
        List<Job> jobs = IteratorUtils.toList(jobdao.getAll(JobStatus.NEW));
        assertEquals("Should get no job for the HD in the future", 0, jobs
                .size());
        // Make some harvest definitions with schedules around the given time
        // Requires a list of configurations for each HD
        List<DomainConfiguration> cfgs = new ArrayList<DomainConfiguration>();
        Domain domain = DomainDAO.getInstance().read("netarkivet.dk");
        cfgs.add(domain.getDefaultConfiguration());
        final ScheduleDAO sdao = ScheduleDAO.getInstance();
        HarvestDefinition hd1 = HarvestDefinition.createPartialHarvest(
                cfgs, 
                sdao.read("Hver hele time"),
                "Hele time",
        "");
        hd1.setSubmissionDate(new Date());
        hddao.create(hd1);
        HarvestDefinition hd2 = HarvestDefinition.createPartialHarvest(
                cfgs,
                sdao.read("Hver nat kl 4.00"),
                "Kl. 4",
        "");
        hd2.setSubmissionDate(new Date());
        hddao.create(hd2);
        generateJobs(0);
        List<Job> jobs1 = IteratorUtils.toList(jobdao.getAll(JobStatus.NEW));
        assertEquals("Should get jobs for no new defs immediately", 0, jobs1
                .size());

        cal.add(Calendar.DAY_OF_MONTH, 1);
        now = cal.getTime();
        generateJobs(2);
        jobs1 = IteratorUtils.toList(jobdao.getAll(JobStatus.NEW));
        assertEquals("Should get jobs for both new defs after a day", 2, jobs1
                .size());
        // Check that the right HD's came in
        Job j1 = (Job) jobs1.get(0);
        Job j2 = (Job) jobs1.get(1);
        assertTrue("Neither job must be for HD 42", j1
                .getOrigHarvestDefinitionID() != hd.getOid() &&
                j2.getOrigHarvestDefinitionID()
                != hd.getOid());
        assertTrue("One of the jobs must be for the new HD " + hd1.getOid()
                + ", but we got " + jobs1, j1.getOrigHarvestDefinitionID()
                .equals(hd1.getOid())
                || j2.getOrigHarvestDefinitionID().equals(
                        hd1.getOid()));
        assertTrue("One of the jobs must be for the new HD " + hd1.getOid()
                + ", but we got " + jobs1, j1.getOrigHarvestDefinitionID()
                .equals(hd2.getOid())
                || j2.getOrigHarvestDefinitionID().equals(hd2.getOid()));
        generateJobs(1);
        List<Job> jobs2 = IteratorUtils.toList(jobdao.getAll(JobStatus.NEW));
        assertEquals(
                "Should generate one more job because we are past the time"
                + " when the next hourly job should have been scheduled.",
                jobs1.size() + 1, jobs2.size());
    }
    
    /**
     * Run job generation and wait for the threads created to finish.
     */
    public static void generateJobs() throws Exception {
        generateJobs(-1);
    }
    
    /**
     * Run job generation and wait for the threads created to finish.
     *
     * @param hddao       The dao to run on.
     * @param now         The time that job generation should be done for
     * @param expectedHDs How many harvest definitions to expect jobs being created
     *                    from, or -1 to not check thread count.
     * @throws InterruptedException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static void generateJobs(int expectedHDs)
            throws Exception {
            JobGeneratorTask generator = new JobGeneratorTask(); 
            String threadPrefix = "Thread-";
            int beforeCount = countThreadsNamed(threadPrefix);
            generator.run();
            int afterCount = countThreadsNamed(threadPrefix);
            if (expectedHDs != -1) {
                assertEquals("Expected " + expectedHDs
                             + " harvest definitions to start scheduling",
                             expectedHDs, afterCount - beforeCount);
            }
        waitForJobGeneration();
    }


    /**
     * Count the number of threads whose name starts with the given prefix. For
     * this to make sense, we must be sure that all threads with the given prefix
     * are waiting, e.g. on a synchronized object.
     *
     * @param threadPrefix A prefix, e.g. "Thread-"
     * @return The number of running threads with the given prefix.
     */
    private static int countThreadsNamed(final String threadPrefix) {
        final Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        int count = 0;
        for (int i = 0; i < threads.length; i++) {
            // This is a sub-thread.
            if (threads[i] != null
                && threads[i].getName().startsWith(threadPrefix)) {
                count++;
            }
        }
        return count;
    }
    

    public static void waitForJobGeneration() 
    throws TimeoutException, InterruptedException {
        for (int waits = 1; waits <= 20 ; waits++ ) {
            boolean threadsRemain = false;
            Thread.sleep(1000);
            final Thread[] threads = ThreadUtils.getAllThreads();
            for ( Thread thread : threads ) {
                if ( thread.getName().indexOf( "JobGeneratorTask") != -1) {
                    System.out.println("Still waiting for "+thread.getName()+ " after "+waits + " seconds");
                    threadsRemain = true;
                    continue;
                } 
            }
            if (!threadsRemain) return;
        }
        throw new TimeoutException(ThreadUtils.getAllThreads().length + "JobGeneratorTask thread remain after 20 seconds" );
    }    
}
