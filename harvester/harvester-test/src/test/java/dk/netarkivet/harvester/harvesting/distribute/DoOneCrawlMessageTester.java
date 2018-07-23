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
package dk.netarkivet.harvester.harvesting.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobTest;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for class DoOneCrawlMessage.
 */
public class DoOneCrawlMessageTester {

    /**
     * We use (arbitrarily) THIS_CLIENT as channel for testing.
     */
    private static final ChannelID CHAN1 = Channels.getThisReposClient();
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() throws SQLException, IllegalAccessException, IOException, NoSuchFieldException,
            ClassNotFoundException {
        rs.setUp();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        Channels.reset();
    }

    @After
    public void tearDown() throws SQLException, IllegalAccessException, NoSuchFieldException {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        Channels.reset();
        rs.tearDown();
    }

    /** Test one of constructor. */
    @Test
    public void testCTOR1() {
        try {
            new DoOneCrawlMessage(null, CHAN1, new HarvestDefinitionInfo("test", "test", "test"),
                    TestInfo.emptyMetadata);
            fail("Calling CTOR with null value for Job should throw exception !");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }

    /** Test two of constructor. */
    @Test
    public void testCTOR2() {
        try {
            new DoOneCrawlMessage(JobTest.createDefaultJob(), null, new HarvestDefinitionInfo("test", "test", "test"),
                    TestInfo.emptyMetadata);
            fail("Calling CTOR with null value for to-queue should throw exception !");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }

    /** Test three of constructor. */
    @Test
    public void testCTOR3() {
        try {
            new DoOneCrawlMessage(JobTest.createDefaultJob(), CHAN1, new HarvestDefinitionInfo("test", "test", "test"), null);
            fail("Calling CTOR with null value for metadata should throw exception !");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }

    /** Test four of constructor. */
    @Test
    public void testCTOR4() {
        try {
            new DoOneCrawlMessage(JobTest.createDefaultJob(), CHAN1, new HarvestDefinitionInfo("test", "test", "test"),
                    TestInfo.emptyMetadata);
        } catch (ArgumentNotValid e) {
            fail("Calling CTOR with valid arguments should not throw exception !");
        }
    }

    /** Test the getJob() method. */
    @Test
    public void testGetJob() {
        Job j = JobTest.createDefaultJob();
        DoOneCrawlMessage docm = new DoOneCrawlMessage(j, CHAN1, new HarvestDefinitionInfo("test", "test", "test"),
                TestInfo.emptyMetadata);
        assertSame("Job is not the same object", j, docm.getJob());
    }

    /** Test the getMetadata() method. */
    @Test
    public void testGetMetadata() {
        Job j = JobTest.createDefaultJob();
        DoOneCrawlMessage docm = new DoOneCrawlMessage(j, CHAN1, new HarvestDefinitionInfo("test", "test", "test"),
                TestInfo.emptyMetadata);
        assertEquals("metadata is not the same object", TestInfo.emptyMetadata, docm.getMetadata());
    }

    /**
     * tests serialization - generating 2 DoOneCrawlMessages - put one of them through serialization/deserialization
     * testing that certain fields in the two objects are still the same ! Should possible be tested more overall
     * (waiting for NHC to make some kind of framework for testing serialization
     */
    @Test
    public void testSerialization() {
        Job j = JobTest.createDefaultJob();
        TestInfo.oneMetadata.add(TestInfo.sampleEntry);
        DoOneCrawlMessage docm1 = new DoOneCrawlMessage(j, CHAN1, new HarvestDefinitionInfo("test", "test", "test"),
                TestInfo.oneMetadata);
        DoOneCrawlMessage docm2 = new DoOneCrawlMessage(j, CHAN1, new HarvestDefinitionInfo("test", "test", "test"),
                TestInfo.oneMetadata);
        docm1.setNotOk("test of errormessage");
        docm2.setNotOk("test of errormessage");

        // Now serialize and deserialize the studied doOneCrawlMessage (but NOT the reference):
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream ous = new ObjectOutputStream(baos);
            ous.writeObject(docm1);
            ous.close();
            baos.close();
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            docm1 = (DoOneCrawlMessage) ois.readObject();
        } catch (IOException e) {
            fail(e.toString());
        } catch (ClassNotFoundException e) {
            fail(e.toString());
        }

        if (!docm1.getErrMsg().equals(docm2.getErrMsg())) {
            fail("Error message is no longer the same !");
        }

        if (!docm1.getTo().getName().equals(docm2.getTo().getName())) {
            fail("ChannelID (to) is no longer the same !");
        }

        if (!docm1.getReplyTo().getName().equals(docm2.getReplyTo().getName())) {
            fail("ChannelID (replyTo) is no longer the same !");
        }

        if (!docm1.getMetadata().get(0).getURL().equals(docm2.getMetadata().get(0).getURL())) {
            fail("metadata is no longer the same !");
        }
    }

    @Test
    public void testHarvestDefinitionInfo() {
        try {
            new HarvestDefinitionInfo("test", "some comments", "testSchedule");
        } catch (ArgumentNotValid e) {
            fail("Should not throw ArgumentNotValid with valid args");
        }

        try {
            new HarvestDefinitionInfo("test", "", "");
        } catch (ArgumentNotValid e) {
            fail("Should not throw ArgumentNotValid with valid args");
        }

        try {
            new HarvestDefinitionInfo("test", "Some comments", "");
        } catch (ArgumentNotValid e) {
            fail("Should not throw ArgumentNotValid with valid args");
        }

        try {
            new HarvestDefinitionInfo("", "", "");
            fail("Should throw ArgumentNotValid with empty harvest name");
        } catch (ArgumentNotValid e) {
            //
        }

        try {
            new HarvestDefinitionInfo((String) null, (String) null, (String) null);
            fail("Should throw ArgumentNotValid with null args");
        } catch (ArgumentNotValid e) {
            //
        }
    }
}
