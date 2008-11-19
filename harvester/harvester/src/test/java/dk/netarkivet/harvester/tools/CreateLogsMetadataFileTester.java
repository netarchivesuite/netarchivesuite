/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.harvester.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.cdx.CDXReader;
import dk.netarkivet.common.utils.cdx.CDXUtils;
import dk.netarkivet.testutils.ARCTestUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

public class CreateLogsMetadataFileTester extends TestCase {
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams();
    private File JOBID_HARVESTID_MAPPING_FILE =  new File(TestInfo.WORKING_DIR, "jobid-harvestid.txt");
    private File TestOldjobs9999 = new File(TestInfo.WORKING_DIR, "9999_1140687204304");
    //private File TestOldjobs5 = new File(TestInfo.WORKING_DIR, "5_1140687204304");
    private File TestOldjobs4 = new File(TestInfo.WORKING_DIR, "4_1140687099617");
    //private File TestOldjobs11 = new File(TestInfo.WORKING_DIR, "11_1140688359422");

    public void setUp() throws Exception {
        FileUtils.createDir(TestInfo.WORKING_DIR);
        // Create dummy jobsdir certain not to be found in jobid-harvestid.txt
        FileUtils.createDir(new File(TestInfo.WORKING_DIR, "9999_1140687204304"));
        FileUtils.copyFile(TestInfo.JOBID_HARVESTID_MAPPING_FILE, JOBID_HARVESTID_MAPPING_FILE);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.OLDJOBS_DIR, TestInfo.WORKING_DIR);

        pse.setUp();
        pss.setUp();
    }

    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.remove(new File("4-metadata-2.arc"));
        FileUtils.remove(new File("4-metadata-2.cdx"));
        FileUtils.remove(new File("4-metadata-2.cdx.sorted"));
        if (pse != null) {
            pse.tearDown();
        }
        if (pss != null) {
            pss.tearDown();
        }
    }

    /**
     * Test dk.netarkivet.harvester.tools.CreateLogsMetadataFile.main.
     * @throws IOException
     * @throws URISyntaxException
     */
    public void testCreateLogsMetadataFile() throws IOException, URISyntaxException {
        ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baosOut));
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baosErr));
        try {
            CreateLogsMetadataFile.main(new String[]{});
            fail("Should System.exit(1) on no args");
        } catch (SecurityException e) {
            System.err.flush();

            StringAsserts.assertStringMatches("Should have usage in stderr",
                    "Too few arguments", baosErr.toString());

            System.out.flush();
            assertEquals("Should not output anything to stdout",
                    "",
                    baosOut.toString());
            baosOut.reset();
            baosErr.reset();
        }

        assertFalse("The file 4-metadata-2.arc should not exist now", new File("4-metadata-2.arc").exists());
        // test output, if failure to find harvestInfo.xml in jobsdir
        try {
            CreateLogsMetadataFile.main(new String[]{
                    JOBID_HARVESTID_MAPPING_FILE.getAbsolutePath(),
                    TestOldjobs9999.getAbsolutePath()   });
            fail("Should System.exit(1) on bad args");
        } catch (SecurityException e) {
            System.err.flush();
            StringAsserts.assertStringMatches("Should have usage in stderr",
                    "The second argument is not a valid jobsdir. "
                       + "It does not contain a harvestInfo.xml file",
                    baosErr.toString());

            System.out.flush();
            assertEquals("Should not output anything to stdout",
                    "",
                    baosOut.toString());
            baosOut.reset();
            baosErr.reset();
        }


        // test output, if oldjobsdir for job id x can't be looked up in jobid-harvestid.txt
        try {
            FileUtils.copyFile(new File(TestOldjobs4, "harvestInfo.xml"),
                    new File(TestOldjobs9999, "harvestInfo.xml"));
            CreateLogsMetadataFile.main(new String[]{
                    JOBID_HARVESTID_MAPPING_FILE.getAbsolutePath(),
                    TestOldjobs9999.getAbsolutePath()
                    });
            fail("Should System.exit(1) on bad args");
        } catch (SecurityException e) {
            System.err.flush();

            StringAsserts.assertStringMatches("Should have usage in stderr",
                    "Unable to lookup Jobid 9999 in jobid-harvestid.txt",
                    baosErr.toString());

            System.out.flush();
            StringAsserts.assertStringMatches("Should not really output anything significant to stdout",
                    "5781 jobid-harvestid mappings found",
                    baosOut.toString());
            baosOut.reset();
            baosErr.reset();
        }



        // Test, if CreateLogsMetadataFile.main creates
        // a proper metadata-arcfile
        try {
            CreateLogsMetadataFile.main(
                    new String[]{
                            JOBID_HARVESTID_MAPPING_FILE.getAbsolutePath(),
                            TestOldjobs4.getAbsolutePath()
                    });
            fail("SecurityException expected on valid arguments");
        } catch (SecurityException e) {
            assertEquals("Should return succes",0, pse.getExitValue());
        }

        File metadataArcFile = new File("4-metadata-2.arc");
        File cdxFile = new File("4-metadata-2.cdx");
        File cdxFileSorted = new File("4-metadata-2.cdx.sorted");

        assertTrue("The file 4-metadata-2.arc should exist now", metadataArcFile.exists());
        System.err.flush();
        System.out.flush();
        baosOut.reset();
        baosErr.reset();
        pss.tearDown();

        // test the contents of the 4-metadata-2.arc file
        CDXUtils.writeCDXInfo(metadataArcFile, new FileOutputStream(cdxFile));
        assertTrue("The file 4-metadata-2.cdx should exist now", cdxFile.exists());
        assertEquals("Number of CDXlines should be 15", FileUtils.countLines(cdxFile), 15L);
        FileUtils.makeSortedFile(cdxFile, cdxFileSorted);
        //System.out.println(FileUtils.readFile(cdxFileSorted));
        CDXReader cdxReader = new CDXReader();
        cdxReader.addCDXFile(cdxFileSorted);
        String actualHeritrixVersion = "1.5.0-200506132127";
        String prefix =  "metadata://netarkivet.dk/crawl/";
        String suffix =  "?heritrixVersion=" + actualHeritrixVersion
            + "&harvestid=5&jobid=4";
        String[] infixes = new String[] { "setup/order.xml",
                "setup/harvestInfo.xml", "setup/seeds.txt",
                "reports/crawl-report.txt", "reports/responsecode-report.txt",
                "reports/frontier-report.txt",  "reports/hosts-report.txt",
                "reports/mimetype-report.txt", "reports/processors-report.txt",
                "reports/seeds-report.txt", "logs/crawl.log", "logs/local-errors.log",
                "logs/progress-statistics.log", "logs/runtime-errors.log",
                "logs/uri-errors.log" };

        Map<String, String> infixMap = new HashMap<String, String>(infixes.length);
        for (String s : infixes) {
            infixMap.put(prefix + s + suffix, s);
        }
        ARCReader arcReader = ARCReaderFactory.get(metadataArcFile);
        Iterator arcs = arcReader.iterator();
        while (arcs.hasNext()) {
            ARCRecord rec = (ARCRecord)arcs.next();
            String url = rec.getMetaData().getUrl();
            if (url.equals("filedesc://4-metadata-2.arc")) {
                continue; // Added at start of arc file
            }
            assertTrue("URI " + url + " must exist in infixmap",
                    infixMap.containsKey(url));
            String infix = infixMap.get(url);
            String tmpString = infix.replaceFirst("setup/", "");
            tmpString = tmpString.replace("reports/", "");
            String oldFileAsString =
                    FileUtils.readFile(new File(TestInfo.OLDJOBS_DIR
                            + "/4_1140687099617/" + tmpString));
            String recString = ARCTestUtils.readARCRecord(rec);
            assertEquals("Should have same contents in file and arc record",
                    oldFileAsString, recString);
            infixMap.remove(url);
        }
        assertEquals("Should have empty infix set after all are removed",
                Collections.emptyMap(), infixMap);
    }


    /**
    * Copies the content of an InputStream to an OutputStream.
    * This method constructs an efficient buffer and pipes bytes from stream
    * to stream through that buffer.
    * The OutputStream is flushed after all bytes have been copied.
    *
    * @param content Source of the copy operation.
    * @param out Destination of the copy operation.
    */
    private static void copy(InputStream content, OutputStream out) {
     BufferedInputStream page = new BufferedInputStream(content);
     BufferedOutputStream responseOut = new BufferedOutputStream(out);
     try {
         byte[] buffer = new byte[4096];
         int bytesRead;
         while ((bytesRead = page.read(buffer)) != -1) {
             responseOut.write(buffer, 0, bytesRead);
         }
         responseOut.flush();
     } catch (IOException e) {
          throw new IOFailure("Could not read or write data", e);
     }
    }


    /**
     * Creates a file with the given file name.
     * Copy a record from content to a file with the given name.
     *
     * @param content The InputStream containing the record.
     * @param fileName The name of the file to create for the record.
     */
    private static void processRecord(InputStream content,String fileName) {
        try {
            File destination = new File(TestInfo.WORKING_DIR, fileName);
            if (!destination.getParentFile().exists()) {
                FileUtils.createDir(destination.getParentFile());
            }
            //System.out.println("destination: " + destination.getAbsolutePath());
            OutputStream out = new FileOutputStream(destination);
            copy(content,out);
            out.close();
        } catch (FileNotFoundException e) {
            throw new IOFailure("Exception caught. Does the output file already exist? " + e,e);
        } catch (IOException e) {
            throw new IOFailure("Exception caught." + e,e);
        }
    }

}
