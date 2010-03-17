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
package dk.netarkivet.harvester.tools;
/**
 * Tests of the tool to create metadata files.
 */

import javax.jms.Message;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


public class CreateCDXMetadataFileTester extends TestCase {
    private static String CONTENT = "This is a test message";
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams();
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR,
            TestInfo.WORKING_DIR);
    private MockupJMS mjms = new MockupJMS();
    TestMessageListener listener;

    File job2MetadataFile = new File("2-metadata-1.arc");
    File job4MetadataFile = new File("4-metadata-1.arc");
    File job70MetadataFile = new File("70-metadata-1.arc");
    private UseTestRemoteFile utrf = new UseTestRemoteFile();

    public CreateCDXMetadataFileTester(String s) {
        super(s);
    }

    public void setUp(){
        utrf.setUp();
        mjms.setUp();
        listener = new BatchListener();
        JMSConnectionFactory.getInstance().setListener(Channels.getTheRepos(), listener);
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
        utrf.tearDown();

        FileUtils.remove(job2MetadataFile);
        FileUtils.remove(job4MetadataFile);
        FileUtils.remove(job70MetadataFile);
    }

    /** Test that arguments are handled correctly.
     *
     */
    public void testMain() {
        ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baosOut));
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baosErr));

        // Check lack of args
        try {
            CreateCDXMetadataFile.main(new String[]{ });
            fail("Should System.exit(1) on no args");
        } catch (SecurityException e) {
            System.out.flush();
            assertEquals("Should not write anything to stdout",
                    "", baosOut.toString());
            System.err.flush();
            StringAsserts.assertStringContains("Should have usage in stderr",
                    "Usage: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile jobID",
                    baosErr.toString());
            baosOut.reset();
            baosErr.reset();
        }

        // Check too many args
        try {
            CreateCDXMetadataFile.main(new String[]{ "11", "42", "77"});
            fail("Should System.exit(1) on illegal args");
        } catch (SecurityException e) {
            System.out.flush();
            assertEquals("Should not write anything to stdout",
                    "", baosOut.toString());
            System.err.flush();
            StringAsserts.assertStringMatches(
                    "Should have usage and errors in stderr",
                    "Too many arguments: '11', '42', '77'"
                            + ".*Usage: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile jobID",
                    baosErr.toString());
            baosOut.reset();
            baosErr.reset();
        }

        // Check illegal arg: negative
        try {
            CreateCDXMetadataFile.main(new String[]{ "-11"});
            fail("Should System.exit(1) on illegal args");
        } catch (SecurityException e) {
            System.out.flush();
            assertEquals("Should not write anything to stdout",
                    "", baosOut.toString());
            System.err.flush();
            StringAsserts.assertStringMatches(
                    "Should have usage and errors in stderr",
                    "-11 is not a valid job ID"
                            + ".*Usage: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile jobID",
                    baosErr.toString());
            baosOut.reset();
            baosErr.reset();
        }

        // Check illegal arg: 0
        try {
            CreateCDXMetadataFile.main(new String[]{ "0"});
            fail("Should System.exit(1) on illegal args");
        } catch (SecurityException e) {
            System.out.flush();
            assertEquals("Should not write anything to stdout",
                    "", baosOut.toString());
            System.err.flush();
            StringAsserts.assertStringMatches(
                    "Should have usage and errors in stderr",
                    "0 is not a valid job ID"
                            + ".*Usage: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile jobID",
                    baosErr.toString());
            baosOut.reset();
            baosErr.reset();
        }

        // Check illegal arg: non-numeral
        try {
            CreateCDXMetadataFile.main(new String[]{ "foo42bar"});
            fail("Should System.exit(1) on illegal args");
        } catch (SecurityException e) {
            System.out.flush();
            assertEquals("Should not write anything to stdout",
                    "", baosOut.toString());
            System.err.flush();
            StringAsserts.assertStringMatches(
                    "Should have usage and errors in stderr",
                    "'foo42bar' is not a valid job ID"
                            + ".*Usage: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile jobID",
                    baosErr.toString());
            baosOut.reset();
            baosErr.reset();
        }
    }

    public void testRunSingleJob() {
        try {
            CreateCDXMetadataFile.main(new String[] { "4" });
        } catch (SecurityException e) {
            assertEquals("Should have exited normally",
                         0, pse.getExitValue());
        }
        assertTrue("Should have a result file",
                job4MetadataFile.exists());
        assertFalse("Should not have other results files",
                job2MetadataFile.exists());
        assertFalse("Should not have other results files",
                job70MetadataFile.exists());
        FileAsserts.assertFileContains("Should have first line",
                "http://netarkivet.dk/fase2index-da.php 130.225.27.144 20060329091238 ",
                job4MetadataFile
        );
        FileAsserts.assertFileContains("Should have some intermediate line",
                "dns:netarkivet.dk 130.225.24.33", job4MetadataFile
        );
        FileAsserts.assertFileContains("Should have last line",
                "http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_13.gif 130.225.27.144 20060329091256",
                job4MetadataFile
        );
    }

    public void testRunFailingJob() {
        // Test with failure
        File outputFile = new File(TestInfo.WORKING_DIR, "tmpout");
        outputFile.delete();
        outputFile.mkdir(); // Force unwriteable file.
        try {
            CreateCDXMetadataFile.main(new String[] { "5" });
        } catch (SecurityException e) {
            assertEquals("Should have exited normally",
                         0, pse.getExitValue());
        }        
        // Should not die on errors in the batch job (null result file)
    }

    /**
     * This class is a MessageListener that responds to BatchMessage,
     * simulating an ArcRepository.
     */
    private static class BatchListener extends TestMessageListener {
        public BatchListener() {
        }
        public void onMessage(Message o) {
            super.onMessage(o);
            NetarkivetMessage nmsg = received.get(received.size() - 1);
            if (nmsg instanceof BatchMessage) {
                BatchMessage m = (BatchMessage) nmsg;
                int count = 0;
                List<File> emptyList = Collections.emptyList();
                RemoteFile rf;
                try {
                    File output = new File(TestInfo.WORKING_DIR, "tmpout");
                    BufferedReader reader = new BufferedReader(new FileReader(
                            new File(TestInfo.DATA_DIR, "jobs-2-4-70.cdx")));
                    FileWriter writer = new FileWriter(output);
                    String line;
                    Pattern p = Pattern.compile("^(\\S+\\s+){5}"
                            + m.getJob().getFilenamePattern().pattern()
                            + "(\\s+\\S+){2}$");
                    while ((line = reader.readLine()) != null) {
                        if (p.matcher(line).matches()) {
                            writer.write(line + "\n");
                            count++;
                        }
                    }
                    reader.close();
                    writer.close();
                    rf = new TestRemoteFile(output, false, false, false);
                } catch (IOException e) {
                    System.out.println(e);
                    e.printStackTrace();
                    rf = null;
                }
                JMSConnectionFactory.getInstance().send(
                        new BatchReplyMessage(m.getReplyTo(),
                                Channels.getError(), m.getID(), count,
                                emptyList, rf));
            }
        }
    };
}