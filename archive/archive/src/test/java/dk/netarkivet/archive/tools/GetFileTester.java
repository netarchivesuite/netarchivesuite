/*$Id: GetRecordTester.java 79 2007-09-26 08:27:29Z kfc $
* $Revision: 79 $
* $Date: 2007-09-26 10:27:29 +0200 (Wed, 26 Sep 2007) $
* $Author: kfc $
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import junit.framework.TestCase;

import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Test the GetFile tool.
 */
public class GetFileTester extends TestCase {
    private static String CONTENT = "This is a test message";
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams();
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR,
            TestInfo.WORKING_DIR);
    private MockupJMS mjms = new MockupJMS();
    TestMessageListener listener;
    ReloadSettings rs = new ReloadSettings();

    public void setUp(){
        rs.setUp();
        mjms.setUp();
        listener = new GetFileListener(mtf.working(
        		new File(TestInfo.DATA_DIR, "test1.arc")));
        JMSConnectionFactory.getInstance().setListener(Channels.getTheRepos(), listener);
        Settings.set(CommonSettings.REMOTE_FILE_CLASS,
                     "dk.netarkivet.common.distribute.NullRemoteFile");
        mtf.setUp();
        pss.setUp();
        pse.setUp();
   
    }
    public void tearDown(){
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
        JMSConnectionFactory.getInstance().removeListener(Channels.getTheRepos(), listener);
        mjms.tearDown();
        rs.tearDown();
    }
    
    /**
     * Test that download of a small file succeeds.
     */
    public void testMain() {
    	if (!TestUtils.runningAs("SVC")) {
    		return;
    	}
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        GetFile.main(new String[]{
        		"test1.arc", "download.arc"}
        );
        System.out.flush();
        String returnedContent = new String(baos.toByteArray());        
        assertEquals("Should return content unchanged, but was: "
                + returnedContent,CONTENT,returnedContent);
    }

    /**
     * This class is a MessageListener that responds to GetFileMessage,
     * simulating an ArcRepository. It sends a constant response
     * if the GetFileMessage matches the values given to GetFileListener's constructor,
     * otherwise it sends null file as response.
     */
    private static class GetFileListener extends TestMessageListener {
        private String arcFileName;
        private File data = new File(TestInfo.TEST_ENTRY_FILENAME);
        public GetFileListener(File arcFile) {
            this.arcFileName = arcFile.getName();
            this.data = arcFile;
        }
        public void onMessage(Message o) {
            super.onMessage(o);
            NetarkivetMessage nmsg =
                   received.get(received.size() - 1);
            if (nmsg instanceof GetFileMessage) {
                GetFileMessage m = (GetFileMessage) nmsg;
                if (arcFileName.equals(m.getArcfileName())) {      	
                	m.setFile(data);
                } else {
                    m.setFile(null);
                }
                JMSConnectionFactory.getInstance().reply(m);
            }
        }
    };
}
