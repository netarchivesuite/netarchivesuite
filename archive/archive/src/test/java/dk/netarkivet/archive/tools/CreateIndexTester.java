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
import dk.netarkivet.archive.indexserver.distribute.IndexRequestMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.ZipUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import junit.framework.TestCase;

public class CreateIndexTester extends TestCase {
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR,
            TestInfo.WORKING_DIR);
    private MockupJMS mjms = new MockupJMS();
    TestMessageListener listener;
    ReloadSettings rs = new ReloadSettings();

    public void setUp(){
        rs.setUp();
        mjms.setUp();
        listener = new CreateIndexListener();
        JMSConnectionFactory.getInstance().setListener(Channels.getTheIndexServer(), listener);
        Settings.set(CommonSettings.REMOTE_FILE_CLASS,
                     "dk.netarkivet.common.distribute.NullRemoteFile");
        Settings.set(CommonSettings.CACHE_DIR, TestInfo.CACHE_DIR.getPath());
        mtf.setUp();
        pss.setUp();
        pse.setUp();
    }
    public void tearDown(){
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
        JMSConnectionFactory.getInstance().removeListener(Channels.getTheIndexServer(), listener);
        mjms.tearDown();
        rs.tearDown();
    }
    
    /**
     * Verify that it has a utility class constructor.
     */
    public void testConstructor() {
        ReflectUtils.testUtilityConstructor(CreateIndex.class);
    }
    
    /**
     * Test that download of a small file succeeds.
     */
    public void testMain() {
        String[] args = new String[]{"-tDEDUP", "-l1"};
        
        ZipUtils.gzipFiles(TestInfo.CACHE_TEMP_DIR, TestInfo.CACHE_OUTPUT_DIR);
        
        pss.tearDown();
        CreateIndex.main(args);
    }
    
    public void testBadArguments1() {
        String[] args = new String[]{"-asdf"};
        String expectedMsg = "Parsing of parameters failed: Unrecognized option: -a";
        
        try {
            CreateIndex.main(args);
            fail("It should not be allowed to call CreateIndex with bad arguments");
        } catch (SecurityException e) {
            // expected
        }
        
        String errMsg = pss.getErr();
        int exitCode = pse.getExitValue();
        
        assertEquals("Should have exit code 1, but was: " + exitCode, 
                1, exitCode);
        assertTrue("The error message should contain '" + expectedMsg + "', but saw '" + errMsg + "'.", 
                errMsg.contains(expectedMsg));
    }
    
    public void testBadArguments2() {
        String[] args = new String[]{"-tCDX"};
        String expectedMsg = "Some of the required parameters are missing: -l";
        
        try {
            CreateIndex.main(args);
            fail("It should not be allowed to call CreateIndex with bad arguments");
        } catch (SecurityException e) {
            // expected
        }
        
        String errMsg = pss.getErr();
        int exitCode = pse.getExitValue();

        assertEquals("Should have exit code 1, but was: " + exitCode, 
                1, exitCode);
        assertTrue("The error message should contain '" + expectedMsg + "', but saw '" + errMsg + "'.", 
                errMsg.contains(expectedMsg));
    }

    public void testBadArguments3() {
        String[] args = new String[]{"-l1"};
        String expectedMsg = "Some of the required parameters are missing: -t";
        
        try {
            CreateIndex.main(args);
            fail("It should not be allowed to call CreateIndex with bad arguments");
        } catch (SecurityException e) {
            // expected
        }
        
        String errMsg = pss.getErr();
        int exitCode = pse.getExitValue();

        assertEquals("Should have exit code 1, but was: " + exitCode, 
                1, exitCode);
        assertTrue("The error message should contain '" + expectedMsg + "', but saw '" + errMsg + "'.", 
                errMsg.contains(expectedMsg));
    }
    
    public void testBadArguments4() {
        String[] args = new String[]{"-tMYINDEX", "-l1"};
        String expectedMsg = "Unknown indextype 'MYINDEX' requested.";
        
        try {
            CreateIndex.main(args);
            fail("It should not be allowed to call CreateIndex with bad arguments");
        } catch (SecurityException e) {
            // expected
        }
        
        String errMsg = pss.getErr();
        int exitCode = pse.getExitValue();

        assertEquals("Should have exit code 1, but was: " + exitCode, 
                1, exitCode);
        assertTrue("The error message should contain '" + expectedMsg + "', but saw '" + errMsg + "'.", 
                errMsg.contains(expectedMsg));
    }


    /**
     * This class is a MessageListener that responds to GetFileMessage,
     * simulating an ArcRepository. It sends a constant response
     * if the GetFileMessage matches the values given to GetFileListener's constructor,
     * otherwise it sends null file as response.
     */
    private static class CreateIndexListener extends TestMessageListener {
        public CreateIndexListener() {
        }
        public void onMessage(Message o) {
            super.onMessage(o);
            NetarkivetMessage nmsg =
                   received.get(received.size() - 1);
            if(nmsg instanceof IndexRequestMessage) {
                IndexRequestMessage irm = (IndexRequestMessage) nmsg;
                
                irm.setFoundJobs(irm.getRequestedJobs());
                irm.setResultFile(RemoteFileFactory.getCopyfileInstance(TestInfo.CACHE_ZIP_FILE));
                
                JMSConnectionFactory.getInstance().reply(irm);
            } else {
                nmsg.setNotOk("Not instance of IndexRequestMessage as required!");
                JMSConnectionFactory.getInstance().reply(nmsg);
            }
        }
    };
}
