/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.common.distribute;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import junit.framework.TestCase;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.TestInfo;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;


/**
 * Class testing the FTPRemoteFile class.
 */
public class IntegrityTestsFTPRemoteFile extends TestCase {
    private static final File TESTLOGPROP = new File(
            "tests/dk/netarkivet/testlog.prop");
    private static final File LOGFILE = new File(
            "tests/testlogs/netarkivtest.log");
    private static final String FILE_1_CONTENTS = "The contents of file 1";
    private static final String FILE_2_CONTENTS = "File 2 contains\n" +
            "this and this\nit also has\nsome more\nlike this\nBurma-Shave";
    private FTPClient theFTPClient;
    private List<RemoteFile> upLoadedFTPRemoteFiles = new ArrayList<RemoteFile>();
    private List<String> upLoadedFiles = new ArrayList<String>();

    /** testFile1-3 represents duplicates of TestInfo.TESTXML */
    private File testFile1;
    private File testFile2;
    private File testFile3;

    RemoteFile rf;

    // A named logger for this class is retrieved
    protected final Logger logger = Logger.getLogger(getClass().getName());

    ReloadSettings rs = new ReloadSettings();

    public IntegrityTestsFTPRemoteFile(String sTestName) {
        super(sTestName);
    }

    public void setUp() {
        rs.setUp();
        try {
            if (!TestInfo.TEMPDIR.exists()) {
                dk.netarkivet.common.utils.TestInfo.TEMPDIR.mkdir();
            }

            FileUtils.removeRecursively(TestInfo.TEMPDIR);
            TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);

            /* make 3 duplicates of TestInfo.TESTXML: test1.xml, test2.xml, test3.xml */
            testFile1 = new File(TestInfo.TEMPDIR, "test1.xml");
            testFile2 = new File(TestInfo.TEMPDIR, "test2.xml");
            testFile3 = new File(TestInfo.TEMPDIR, "test3.xml");
            assertTrue("The test xml file must exist", TestInfo.TESTXML.exists());
            FileUtils.copyFile(TestInfo.TESTXML, testFile1);
            FileUtils.copyFile(TestInfo.TESTXML, testFile2);
            FileUtils.copyFile(TestInfo.TESTXML, testFile3);

            /** enable logging as defined in testlog.prop file*/
            try {
                LogManager.getLogManager().readConfiguration(new FileInputStream(
                        TESTLOGPROP));
            } catch (IOException e) {
                fail("Could not load the testlog.prop file");
            }
        } catch (Exception e) {
            fail("Could not setup configuration for");
        }

        /** Read ftp-related settings from settings.xml. */
        final String ftpServerName = Settings.get(
                FTPRemoteFile.FTP_SERVER_NAME);
        final int ftpServerPort = Integer.parseInt(Settings.get(
                FTPRemoteFile.FTP_SERVER_PORT));
        final String ftpUserName = Settings.get(FTPRemoteFile.FTP_USER_NAME);
        final String ftpUserPassword = Settings.get(
                FTPRemoteFile.FTP_USER_PASSWORD);

        /** Connect to test ftp-server. */
        theFTPClient = new FTPClient();

        try {
            theFTPClient.connect(ftpServerName, ftpServerPort);
            theFTPClient.login(ftpUserName, ftpUserPassword);
            boolean b = theFTPClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            assertTrue("Must be possible to set the file type to binary after login",
                    b);
        } catch (SocketException e) {
            throw new IOFailure("Connect to " + ftpServerName + " failed",
                    e.getCause());
        } catch (IOException e) {
            throw new IOFailure("Connect to " + ftpServerName + " failed",
                    e.getCause());
        }
        theFTPClient.enterLocalPassiveMode();
        rf = FTPRemoteFile.getInstance(testFile1, true, false, true);

        /** Do not send notification by email. Print them to STDOUT. */
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
    }

    public void tearDown() throws IOException {
        /** delete all uploaded files on ftp-server and then disconnect */
        Iterator<String> fileIterator = upLoadedFiles.iterator();

        while (fileIterator.hasNext()) {
            String currentUploadedFile = (String) fileIterator.next();

            if (currentUploadedFile != null) {
                if (!theFTPClient.deleteFile(currentUploadedFile)) {
                    logger.warning("deleteFile operation failed on " +
                            currentUploadedFile + ". Reply from ftpserver: " +
                            theFTPClient.getReplyString());
                }
            }
        }

        if (!theFTPClient.logout()) {
            logger.warning("logout operation failed. Reply from ftp-server: " +
                    theFTPClient.getReplyString());
        }

        theFTPClient.disconnect();

        Iterator<RemoteFile> ftpIterator = upLoadedFTPRemoteFiles.iterator();
            
        while (ftpIterator.hasNext()) {
            FTPRemoteFile currentUploadedFile = (FTPRemoteFile) ftpIterator.next();

            if (currentUploadedFile != null) {
                currentUploadedFile.cleanup();
            }
        }

        FileUtils.removeRecursively(dk.netarkivet.common.utils.TestInfo.TEMPDIR);
        rs.tearDown();
    }




    /**
     * Initially verify that communication with the ftp-server succeeds
     * without using the RemoteFile.
     * (1) Verify, that you can upload a file to a ftp-server, and retrieve the
     * same file from this server-server.
     * (2) Verify, that file was not corrupted in transit
     * author: SVC
     * @throws IOException
     */
    public void testConfigSettings() throws IOException {
        /** this code has been tested with
         * the ftp-server proftpd (www.proftpd.org), using
         * the configuration stored in CVS here: /projects/webarkivering/proftpd.org
         */
        String nameOfUploadedFile;
        String nameOfUploadedFile2;

        File inputFile = TestInfo.TESTXML;
        File inputFile2 = TestInfo.INVALIDXML;

        InputStream in = new FileInputStream(inputFile);
        InputStream in2 = new FileInputStream(inputFile2);

        nameOfUploadedFile = inputFile.getName();
        nameOfUploadedFile2 = inputFile2.getName();

        /** try to append data to file on FTP-server */
        /** Assumption: File does not exist on FTP-server */
        assertFalse("File should not exist already on server",
                onServer(nameOfUploadedFile));

        assertTrue("Appendfile operation failed",
                theFTPClient.appendFile(nameOfUploadedFile, in));
        upLoadedFiles.add(nameOfUploadedFile);

        /** try to append data to file on the FTP-server */
        assertTrue("AppendFile operation 2 failed!",
                theFTPClient.appendFile(nameOfUploadedFile, in2));

        if (!upLoadedFiles.contains(nameOfUploadedFile)) {
            upLoadedFiles.add(nameOfUploadedFile);
        }

        /** try to store data to a file on the FTP-server */
        assertTrue("Store operation failed",
                theFTPClient.storeFile(nameOfUploadedFile2, in));
        upLoadedFiles.add(nameOfUploadedFile2);
    }

    /**
     * test 1: test that FTPRemoteFile.getInstance(null) throws an
     * ArgumentNotValid exception
     * @throws FileNotFoundException
     */
    public void testArgumentsNewInstanceNotNull() throws FileNotFoundException {
        try {
            FTPRemoteFile.getInstance(null, true, false, true);
            fail("ArgumentNotValid Expected");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * test 3: test that FTPRemoteFile.appendTo(null) throws an
     * ArgumentNotValid exception
     * @throws FileNotFoundException
     */
    public void testArgumentsRemoteFileAppendToNotNull() throws FileNotFoundException {
        try {
            //RemoteFile rf = FTPRemoteFile.getInstance(testFile1);
            rf.appendTo(null);
            fail("ArgumentNotValid Expected");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /** Test that writing to the original file after making a remotefile
     * does not change the remotefile contents.
     * Test for bug #289
     * @throws IOException
     */
    public void testContentsNotOverwriter() throws IOException {
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, 
        "dk.netarkivet.common.distribute.FTPRemoteFile");
        FileUtils.writeBinaryFile(testFile2, "another simple string".getBytes());
        String originalContents = FileUtils.readFile(testFile1);
        RemoteFile rf = RemoteFileFactory.getInstance(testFile1, true, false,
                                                      true);
        FileUtils.writeBinaryFile(testFile1, "a simple string".getBytes());
        FileAsserts.assertFileContains("Original file should be overwritten",
                "a simple string", testFile1);
        FileAsserts.assertFileNotContains("Contents from remotefile should not" +
                "exist before writing", testFile2, originalContents);
        rf.copyTo(testFile2);
        FileAsserts.assertFileContains("Contents from remotefile should not" +
                "be affected by overwriting testfile", originalContents,
                testFile2);
        assertEquals("MD5 sum should not be affected",
                MD5.generateMD5(originalContents.getBytes()), rf.getChecksum());
        FileAsserts.assertFileContains("Original file should be unchanged",
                "a simple string", testFile1);
    }

    public boolean onServer(String nameOfUploadedFile)
            throws IOException {
        assertTrue("theFTPClient should not be null", theFTPClient != null);

        FTPFile[] listOfFiles = theFTPClient.listFiles();

        //assertTrue("This list should not be null",listOfFiles != null);
        if (listOfFiles == null) {
            return false;
        }

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].getName().equals(nameOfUploadedFile)) {
                return true;
            }
        }

        return false;
    }
}
