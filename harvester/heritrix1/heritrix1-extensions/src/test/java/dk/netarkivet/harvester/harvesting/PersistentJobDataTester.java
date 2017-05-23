/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2017 The Royal Danish Library, 
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
package dk.netarkivet.harvester.harvesting;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobTest;
import dk.netarkivet.testutils.TestResourceUtils;

public class PersistentJobDataTester {
    private final static File CRAWL_DIR = new File("target/crawldir");
    protected static final String HARVEST_INFO_XML = "harvestInfo.xml";

    @Before
    public void initialize() {
        FileUtils.removeRecursively(CRAWL_DIR);
        FileUtils.createDir(CRAWL_DIR);
    }

    @Test
    public void testConstructor() {
        new PersistentJobData(CRAWL_DIR);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testConstructorWithNullCrawlDir() {
        new PersistentJobData(null);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testConstructorWithNullNonExistentCrawlDir() {
        new PersistentJobData(new File("nonExistingCrawlDir"));
    }

    @Test
    public void testWriteRead() {
        PersistentJobData pjd = new PersistentJobData(CRAWL_DIR);
        Job testJob = JobTest.createDefaultJob();
        testJob.setJobID(1L);
        testJob.setSubmittedDate(new Date());
        testJob.setHarvestAudience("Default Audience");
        pjd.write(testJob, new HarvestDefinitionInfo("test", "test", "test"));

        PersistentJobData pjdNew = new PersistentJobData(CRAWL_DIR);

        assertEquals(testJob.getJobID(), pjdNew.getJobID());
        assertEquals(testJob.getChannel(), pjdNew.getChannel());
        assertEquals(testJob.getMaxBytesPerDomain(), pjdNew.getMaxBytesPerDomain());
        assertEquals(testJob.getMaxObjectsPerDomain(), pjdNew.getMaxObjectsPerDomain());
        assertEquals(testJob.getHarvestNum(),pjdNew.getJobHarvestNum());
        assertEquals(testJob.getOrderXMLName(), pjdNew.getOrderXMLName());
        assertEquals(testJob.getOrigHarvestDefinitionID(), pjdNew.getOrigHarvestDefinitionID());
        assertEquals(pjdNew.getPerformer(), null);
    }
    
    @Test
    public void testReadCurrentVersion0_6() {
        File hiVersion03 = new File(TestResourceUtils.getFilePath("harvestInfo.xml"));
        FileUtils.copyFile(hiVersion03, new File(CRAWL_DIR, HARVEST_INFO_XML));
        PersistentJobData pjd = new PersistentJobData(CRAWL_DIR);
        pjd.getVersion();
    }

    @Test
    public void testReadVersion0_5() {
        File hiVersion03 = new File(TestResourceUtils.getFilePath("harvestInfo-0.5.xml"));
        FileUtils.copyFile(hiVersion03, new File(CRAWL_DIR, HARVEST_INFO_XML));
        PersistentJobData pjd = new PersistentJobData(CRAWL_DIR);
        pjd.getVersion();
    }

    @Test
    public void testReadVersion0_4() {
        File hiVersion03 = new File(TestResourceUtils.getFilePath("harvestInfo-0.4.xml"));
        FileUtils.copyFile(hiVersion03, new File(CRAWL_DIR, HARVEST_INFO_XML));
        PersistentJobData pjd = new PersistentJobData(CRAWL_DIR);
        pjd.getVersion();
    }
}
