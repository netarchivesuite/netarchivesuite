/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.jms.Message;

import org.archive.io.ArchiveRecord;
import org.archive.format.arc.ARCConstants;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.LogbackRecorder.DenyFilter;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

/**
 * Unit test for the GetRecord tool.
 */
public class GetRecordTester {

    private static String CONTENT = "This is a test message";
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams(true);

    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR, TestInfo.WORKING_DIR);

    private MockupJMS mjms = new MockupJMS();
    TestMessageListener listener;

    @Before
    public void setUp() {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionMockupMQ.clearTestQueues();
        mjms.setUp();
        listener = new GetListener(TestInfo.TEST_ENTRY_FILENAME, TestInfo.TEST_ENTRY_OFFSET);
        JMSConnectionFactory.getInstance().setListener(Channels.getTheRepos(), listener);
        mtf.setUp();
        pss.setUp();
        pse.setUp();
    }

    @After
    public void tearDown() {
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
        JMSConnectionFactory.getInstance().removeListener(Channels.getTheRepos(), listener);
        mjms.tearDown();
    }

    @Test
    public void testMain() {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        lr.addFilter(new DenyFilter(), ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        try {
            GetRecord.main(new String[] {TestInfo.INDEX_DIR.getAbsolutePath(), TestInfo.TEST_ENTRY_URI});
            fail("Should system exit");
        } catch (SecurityException e) {
            assertEquals("Should have exited normally", 0, pse.getExitValue());
        }
        System.out.flush();
        String returnedContent = pss.getOut();
        assertEquals("Should return content unchanged, but was: " + returnedContent, CONTENT, returnedContent);
        lr.clearAllFilters(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        lr.stopRecorder();
    }

    @Test
    public void testFail() {
        String expectedResults = "indexfile uri";
        try {
            GetRecord.main(new String[] {TestInfo.INDEX_DIR.getAbsolutePath()});
            fail("System should exit");
        } catch (SecurityException e) {
            assertEquals("Should have exited with failure", 1, pse.getExitValue());
        }
        System.out.flush();
        String returned = pss.getErr();
        assertTrue("Should contain the required arguments: '" + expectedResults + "', but was: '" + returned + "'.",
                returned.contains(expectedResults));
    }

    /**
     * This class is a MessageListener that responds to GetMessage, simulating an ArcRepository. It sends a constant
     * response if the GetMessage matches the values given to GetListener's constructor, otherwise it sends a null
     * record as response.
     */
    private static class GetListener extends TestMessageListener {
        private String arcFileName;
        private long offset;
        private ARCRecord myRec;

        public GetListener(String arcFileName, long offset) {
            this.arcFileName = arcFileName;
            this.offset = offset;
            try {
                HashMap<String, Object> map = new HashMap<String, Object>();
                for (Object o : ARCConstants.REQUIRED_VERSION_1_HEADER_FIELDS) {
                    // Some pieces of code check the length field, so make sure
                    // this has right value and foo the rest:
                    map.put((String) o, Integer.toString(CONTENT.length()));
                }
                // insert dummy offset
                map.put(ARCConstants.ABSOLUTE_OFFSET_KEY, Long.valueOf(0L));
                ARCRecordMetaData meta = new ARCRecordMetaData("foo", map);
                InputStream is = new ByteArrayInputStream(CONTENT.getBytes());
                myRec = new ARCRecord(is, meta, 0, false, false, false);
            } catch (IOException e) {
                myRec = null;
            }
        }

        public void onMessage(Message o) {
            super.onMessage(o);
            NetarkivetMessage nmsg = received.get(received.size() - 1);
            if (nmsg instanceof GetMessage) {
                GetMessage m = (GetMessage) nmsg;
                if ((arcFileName.equals(m.getArcFile())) && (offset == m.getIndex())) {
                    m.setRecord(new BitarchiveRecord((ArchiveRecord) myRec, m.getArcFile()));
                } else {
                    m.setRecord(new BitarchiveRecord(null, ""));
                }
                JMSConnectionFactory.getInstance().reply(m);
            }
        }
    }

    ;
}
