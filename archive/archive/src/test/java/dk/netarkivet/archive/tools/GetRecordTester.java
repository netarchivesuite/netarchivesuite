/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.tools;

import javax.jms.Message;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;

import junit.framework.TestCase;
import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

/**
 * Unit test for the GetRecord tool.
 *
 */
public class GetRecordTester extends TestCase {
    private static String CONTENT = "This is a test message";
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR,
            TestInfo.WORKING_DIR);
    private MockupJMS mjms = new MockupJMS();
    TestMessageListener listener;

    public void setUp(){
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionMockupMQ.clearTestQueues();
        mjms.setUp();
        listener = new GetListener(
                TestInfo.TEST_ENTRY_FILENAME,
                TestInfo.TEST_ENTRY_OFFSET);
        JMSConnectionFactory.getInstance().setListener(
                Channels.getTheRepos(), listener);
        mtf.setUp();
        pss.setUp();
        pse.setUp();
    }
    public void tearDown(){
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
        JMSConnectionFactory.getInstance().removeListener(
                Channels.getTheRepos(), listener);
        mjms.tearDown();
    }

    public void testMain() {
        try {
            GetRecord.main(new String[]{
                    TestInfo.INDEX_DIR.getAbsolutePath(),
                    TestInfo.TEST_ENTRY_URI});
            fail("Should system exit");
        } catch (SecurityException e) {
            assertEquals("Should have exited normally",
                         0, pse.getExitValue());
        }
        System.out.flush();
        String returnedContent = pss.getOut();
        assertEquals("Should return content unchanged, but was: "
                + returnedContent, CONTENT, returnedContent);
    }
    
    public void testFail() {
        String expectedResults = "indexfile uri";
        try {
            GetRecord.main(new String[]{
                    TestInfo.INDEX_DIR.getAbsolutePath()});
            fail("System should exit");
        } catch (SecurityException e) {
            assertEquals("Should have exited with failure",
                    1, pse.getExitValue());
        }
        System.out.flush();
        String returned = pss.getErr();
        assertTrue("Should contain the required arguments: '" + expectedResults
                + "', but was: '" + returned + "'.", 
                returned.contains(expectedResults));
    }

    /**
     * This class is a MessageListener that responds to GetMessage,
     * simulating an ArcRepository. It sends a constant response
     * if the GetMessage matches the values given to GetListener's constructor,
     * otherwise it sends a null record as response.
     */
    private static class GetListener extends TestMessageListener {
        private String arcFileName;
        private long offset;
        private ARCRecord myRec;
        public GetListener(String arcFileName, long offset) {
            this.arcFileName = arcFileName;
            this.offset = offset;
            try {
                HashMap<String,Object> map = new HashMap<String,Object>();
                for (Object o : ARCConstants.REQUIRED_VERSION_1_HEADER_FIELDS) {
                    //Some pieces of code check the length field, so make sure
                    // this has right value and foo the rest:
                    map.put((String) o, Integer.toString(CONTENT.length()));
                }
                //insert dummy offset
                map.put(ARCConstants.ABSOLUTE_OFFSET_KEY, new Long(0L));
                ARCRecordMetaData meta = new ARCRecordMetaData("foo", map);
                InputStream is = new ByteArrayInputStream(CONTENT.getBytes());
                myRec = new ARCRecord(is, meta, 0, false, false, false);
            } catch (IOException e) {
                myRec = null;
            }
        }
        public void onMessage(Message o) {
            super.onMessage(o);
            NetarkivetMessage nmsg =
                   received.get(received.size() - 1);
            if (nmsg instanceof GetMessage) {
                GetMessage m = (GetMessage) nmsg;
                if ((arcFileName.equals(m.getArcFile()))
                        && (offset == m.getIndex())) {
                    m.setRecord(new BitarchiveRecord(myRec));
                } else {
                    m.setRecord(new BitarchiveRecord(null));
                }
                JMSConnectionFactory.getInstance().reply(m);
            }
        }
    };
}
