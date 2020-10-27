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

import java.io.File;

import javax.jms.Message;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Test the GetFile tool.
 */
public class GetFileTester {
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR, TestInfo.WORKING_DIR);
    private MockupJMS mjms = new MockupJMS();
    TestMessageListener listener;
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        Channels.reset();
        rs.setUp();
        mjms.setUp();
        listener = new GetFileListener(mtf.working(new File(TestInfo.DATA_DIR, "test1.arc")));
        JMSConnectionFactory.getInstance().setListener(Channels.getTheRepos(), listener);
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, "dk.netarkivet.common.distribute.NullRemoteFile");
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
        rs.tearDown();
    }

    /**
     * Test that download of a small file succeeds.
     */
    @Test
    public void testMain() {
        String[] args = new String[] {"test1.arc", new File(TestInfo.DATA_DIR, "download.arc").getPath()};

        try {
            GetFile.main(args);
            fail("GetFile should try to exit");
        } catch (SecurityException e) {
            //
        }

        String errMsg = pss.getOut();
        int exitCode = pse.getExitValue();

        assertEquals("Should have exit code 0, but was: " + exitCode, 0, exitCode);
        assertTrue("The output message should claim to retrieve the file",
                errMsg.contains("Retrieving file 'test1.arc' from replica 'BarOne' as file "));
    }

    @Test
    public void testTooManyArguments() {
        String[] args = new String[] {"arg1.arc", "arg2.arc", "arg3.arc"};

        try {
            GetFile.main(args);
            fail("GetFile should try to exit");
        } catch (SecurityException e) {
            // This should occur.
        }

        String errMsg = pss.getErr();
        int exitCode = pse.getExitValue();

        assertEquals("Should have exit code 1, but was: " + exitCode, 1, exitCode);
        assertTrue("Should contain a message for the usage of the tool.",
                errMsg.contains(GetFile.class.getName() + " filename [destination-file]"));
    }

    @Test
    public void testNoArguments() {
        String[] args = new String[] {};

        try {
            GetFile.main(args);
            fail("GetFile should try to exit");
        } catch (SecurityException e) {
            // This should occur.
        }

        String errMsg = pss.getErr();
        int exitCode = pse.getExitValue();

        assertEquals("Should have exit code 1, but was: " + exitCode, 1, exitCode);
        assertTrue("Should contain a message for the usage of the tool.",
                errMsg.contains(GetFile.class.getName() + " filename [destination-file]"));
    }

    /**
     * This class is a MessageListener that responds to GetFileMessage, simulating an ArcRepository. It sends a constant
     * response if the GetFileMessage matches the values given to GetFileListener's constructor, otherwise it sends null
     * file as response.
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
            NetarkivetMessage nmsg = received.get(received.size() - 1);
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
    }

    ;
}
