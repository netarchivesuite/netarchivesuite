/* File: $Id$
 * Revision: $Revision$
 * Author: $Author$
 * Date: $Date$
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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;


/**
 * Unit tests for class DoOneCrawlMessage.
 */
public class DoOneCrawlMessageTester extends TestCase {

    TestInfo info = new TestInfo();
    /**
     We use (arbitrarily) THIS_CLIENT as channel for testing.
     */
    private static final ChannelID CHAN1 = Channels.getThisReposClient();
    ReloadSettings rs = new ReloadSettings();

    public DoOneCrawlMessageTester(String sTestName) {
        super(sTestName);
    }

    /**
     * Setup method common to all unittests.
     */
    public void setUp() throws SQLException, IllegalAccessException,
            IOException, NoSuchFieldException, ClassNotFoundException {
        rs.setUp();
        if (!TestInfo.WORKING_DIR.exists()) {
            TestInfo.WORKING_DIR.mkdir();
        }
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        ChannelsTester.resetChannels();
    }

    /**
     * Teardown method common to all unittests.
     */
    public void tearDown()
            throws SQLException, IllegalAccessException, NoSuchFieldException {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        ChannelsTester.resetChannels();
        rs.tearDown();
    }

    /** Test one of constructor. */
    public void testCTOR1() {
        try {
            new DoOneCrawlMessage(null, CHAN1, TestInfo.emptyMetadata);
            fail("Calling CTOR with null value for Job should throw exception !");
        } catch (ArgumentNotValid e) {
            //expected case
        }
    }
    
    /** Test two of constructor. */
    public void testCTOR2() {
        try {
            new DoOneCrawlMessage(TestInfo.getJob(), null, TestInfo.emptyMetadata);
            fail("Calling CTOR with null value for to-queue should throw exception !");
        } catch (ArgumentNotValid e) {
            //expected case
        }
    }

    /** Test three of constructor. */
    public void testCTOR3() {
        try {
            new DoOneCrawlMessage(TestInfo.getJob(),
                    CHAN1, null);
            fail("Calling CTOR with null value for metadata should throw exception !");
        } catch (ArgumentNotValid e) {
            //expected case
        }
    }
    
    /** Test four of constructor. */
    public void testCTOR4() {
        try {
            new DoOneCrawlMessage(TestInfo.getJob(), CHAN1,
                    TestInfo.emptyMetadata);
        } catch (ArgumentNotValid e) {
            fail("Calling CTOR with valid arguments should not throw exception !");
        }
    }
    
    /** Test the getJob() method. */
    public void testGetJob() {
        Job j = TestInfo.getJob();
        DoOneCrawlMessage docm = new DoOneCrawlMessage(j, CHAN1,
                TestInfo.emptyMetadata);
        assertSame("Job is not the same object", j, docm.getJob());
    }
    
    /** Test the getMetadata() method. */
    public void testGetMetadata() {
        Job j = TestInfo.getJob();
        DoOneCrawlMessage docm = new DoOneCrawlMessage(j, CHAN1,
                TestInfo.emptyMetadata);
        assertEquals("metadata is not the same object", TestInfo.emptyMetadata,
                docm.getMetadata());
    }

    /**
     * tests serialization - generating 2 DoOneCrawlMessages -
     * put one of them through serialization/deserialization
     * testing that certain fields in the two objects are still the same !
     * Should possible be tested more overall
     * (waiting for NHC to make some kind of framework for testing serialization
     */
    public void testSerialization() {
        Job j = TestInfo.getJob();
        TestInfo.oneMetadata.add(TestInfo.sampleEntry);
        DoOneCrawlMessage docm1 = new DoOneCrawlMessage(j,
                CHAN1, TestInfo.oneMetadata);
        DoOneCrawlMessage docm2 = new DoOneCrawlMessage(j,
                CHAN1, TestInfo.oneMetadata);
        docm1.setNotOk("test of errormessage");
        docm2.setNotOk("test of errormessage");

        //Now serialize and deserialize the studied doOneCrawlMessage (but NOT the reference):
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream ous = new ObjectOutputStream(baos);
            ous.writeObject(docm1);
            ous.close();
            baos.close();
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(baos.toByteArray()));
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

        if (!docm1.getMetadata().get(0).getURL().
                equals(docm2.getMetadata().get(0).getURL())) {
            fail("metadata is no longer the same !");
        }

    }
}
