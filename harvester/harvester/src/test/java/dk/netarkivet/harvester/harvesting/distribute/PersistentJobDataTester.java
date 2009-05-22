/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Test class PersistentJobData.
 */
public class PersistentJobDataTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    /**
     * Test constructor for PersistentJobData.
     * 1. Throws ArgumentNotValid, if file argument null or file does not exist.
     * 2. accepts existingdir as argument
     */
    public void testConstructor(){
        try {
            new PersistentJobData(null);
            fail("PersistentJobData should have thrown an exception when given null-argument");
        }    catch (ArgumentNotValid e) {
            //expected
        }

        try {
            new PersistentJobData(new File("nonExistingDir"));
            fail("PersistentJobData should have thrown an exception when given " +
                    " non existingdir as argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        // Check that an existing dir doesn't throw an exception.
        new PersistentJobData(TestInfo.TEST_CRAWL_DIR);
    }

    /**
     * Test that the write(Job) method persists all necessary information
     * about the current harvest-job.
     * @throws Exception If failure to persist the information
     *                   or unable to access DB
     */
    public void testWrite() throws Exception {
        TestInfo.WORKING_DIR.mkdirs();
        File crawldir = new File(TestInfo.WORKING_DIR, "my-crawldir");
        crawldir.mkdir();
        PersistentJobData pjd = new PersistentJobData(crawldir);
        Job testJob = TestInfo.getJob();
        testJob.setJobID(42L);
        pjd.write(testJob);

        PersistentJobData pjdNew = new PersistentJobData(crawldir);

        assertEquals("retrieved jobID is not the same as original jobID",
                testJob.getJobID(), pjdNew.getJobID());
        assertEquals("retrieved jobpriority is not the same as original job priority",
                testJob.getPriority(), pjdNew.getJobPriority());
        assertEquals("retrived maxBytesPerDomain is not the same as original job maxBytesPerDomain",
                testJob.getMaxBytesPerDomain(), pjdNew.getMaxBytesPerDomain());
        assertEquals("retrived maxObjectsPerDomain is not the same as original job maxObjectsPerDomain",
                testJob.getMaxObjectsPerDomain(), pjdNew.getMaxObjectsPerDomain());
        assertEquals("retrived harvestNum is not the same as original job harvestNum",
                testJob.getHarvestNum(), pjdNew.getJobHarvestNum());
        assertEquals("retrived orderXMlName is not the same as original job orderXMLName",
                testJob.getOrderXMLName(), pjdNew.getOrderXMLName());
        assertEquals("retrived origHarvestDefinitionID is not the same as original ID",
                testJob.getOrigHarvestDefinitionID(),
                pjdNew.getOrigHarvestDefinitionID());
        
        // cleanup after this unit-test.
        FileUtils.removeRecursively(crawldir);
    }

}
