/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.common.distribute;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import junit.framework.TestCase;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.TestInfo;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;


/**
 * Class testing the FTPRemoteFile class.
 */
public class IntegrityTestsFTP extends TestCase {
    private static final File TESTLOGPROP = new File(
            "tests/dk/netarkivet/testlog.prop");
    //private static final File LOGFILE = new File(
    //        "tests/testlogs/netarkivtest.log");
    private static final String FILE_1_CONTENTS = "The contents of file 1";
    private static final String FILE_2_CONTENTS = "File 2 contains\n" +
            "this and this\nit also has\nsome more\nlike this\nBurma-Shave";
    private FTPClient theFTPClient;
    private ArrayList<RemoteFile> upLoadedFTPRemoteFiles = new ArrayList<RemoteFile>();
    private ArrayList<String> upLoadedFiles = new ArrayList<String>();

    /** testFile1-3 represents duplicates of TestInfo.TESTXML */
    private File testFile1;
    private File testFile2;
    private File testFile3;

    // A named logger for this class is retrieved
    protected final Logger logger = Logger.getLogger(getClass().getName());

    public IntegrityTestsFTP(String sTestName) {
        super(sTestName);
    }

    ReloadSettings rs = new ReloadSettings();

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

            /** enable logging as defined in testlog.prop file. */
            try {
                LogManager.getLogManager().readConfiguration(new FileInputStream(
                        TESTLOGPROP));
            } catch (IOException e) {
                fail("Could not load the testlog.prop file");
            }
        } catch (Exception e) {
            fail("Could not setup configuration for");
        }

        /* Read ftp-related settings from settings.xml. */
        final String ftpServerName = Settings.get(
                FTPRemoteFile.FTP_SERVER_NAME);
        final int ftpServerPort = Integer.parseInt(Settings.get(
                FTPRemoteFile.FTP_SERVER_PORT));
        final String ftpUserName = Settings.get(FTPRemoteFile.FTP_USER_NAME);
        final String ftpUserPassword = Settings.get(
                FTPRemoteFile.FTP_USER_PASSWORD);

        /* Connect to test ftp-server. */
        theFTPClient = new FTPClient();
        
        try {
            theFTPClient.connect(ftpServerName, ftpServerPort);
            assertTrue("Could not login to ' + " + ftpServerName
            		+ ":" + ftpServerPort + "' with username,password="
            		+ ftpUserName + "," + ftpUserPassword,
            		theFTPClient.login(ftpUserName, ftpUserPassword));
            assertTrue("Must be possible to set the file type to binary after login",
                    theFTPClient.setFileType(FTPClient.BINARY_FILE_TYPE));
        } catch (SocketException e) {
            throw new IOFailure("Connect to " + ftpServerName + " failed",
                    e.getCause());
        } catch (IOException e) {
            throw new IOFailure("Connect to " + ftpServerName + " failed",
                    e.getCause());
        }
        
        
        /** Do not send notification by email. Print them to STDOUT. */
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
    }

    public void tearDown() throws IOException {
        /** delete all uploaded files on ftp-server and then disconnect. */
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
     * test arguments extensively
     * @throws FileNotFoundException
     */
    public void testArguments() throws FileNotFoundException {
        /** test 1: test that FTPRemoteFile.getInstance(null) throws an ArgumentNotValid exception */
        try {
            FTPRemoteFile.getInstance(null, true, false, true);
            fail("ArgumentNotValid Expected");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        /** test 3: test that FTPRemoteFile.appendTo(null) throws an ArgumentNotValid exception */
        try {
            RemoteFile rf = FTPRemoteFile.getInstance(testFile1, true, false,
                                                      true);
            rf.appendTo(null);
            fail("ArgumentNotValid Expected");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        /** test 4: test that FTPRemoteFile.copyTo(null) throws an ArgumentNotValid exception */
        try {
            RemoteFile rf = FTPRemoteFile.getInstance(testFile2, true, false,
                                                      true);
            upLoadedFTPRemoteFiles.add(rf);

            rf.copyTo(null);
            fail("ArgumentNotValid Expected");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        /** test 5: test that FTPRemoteFile.copyTo(destFile) throws an ArgumentNotValid exception
         *  if destFile is not an acceptable destinationFile, i.e. the file 'destFile' is writable
         */
            RemoteFile rf = FTPRemoteFile.getInstance(testFile2, true, false,
                                                      true);

            /** this creates a File object pointing to an illegal file */
            File destFile = new File(TestInfo.TEMPDIR.getAbsolutePath() + "/" +
                    TestInfo.TEMPDIR.getName() + "/" +
                    TestInfo.TESTXML.getName());
            assertFalse(destFile.getAbsolutePath() + " should not exist!",
                    destFile.exists());

        try {
            rf.copyTo(destFile); /* This operation tries to copy the file to an file, which cannot be created */
            fail("ArgumentNotValid Expected");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }


    /**
     * (1) Test, if uploaded and retrieved file are equal
     * (2) test that rf.getSize() reports the correct value;
     * @throws IOException
     */
    public void testUploadAndRetrieve() throws IOException {
        File testFile = TestInfo.TESTXML;
        RemoteFile rf = FTPRemoteFile.getInstance(testFile, true, false, true);

        File newFile = new File(TestInfo.TEMPDIR, "newfile.xml");

        /** register that testFile should now be present on ftp-server */
        upLoadedFTPRemoteFiles.add(rf);

        assertEquals("The size of the file written to the ftp-server " +
                "should not differ from the original size", rf.getSize(),
                testFile.length());

        rf.copyTo(newFile);

        /** check, if the original file and the same file retrieved
         * from the ftp-server contains the same contents
         */
        byte[] datasend = FileUtils.readBinaryFile(testFile);
        byte[] datareceived = FileUtils.readBinaryFile(newFile);
        boolean isok = Arrays.equals(datareceived, datasend);
        assertTrue(" verify the same data received as uploaded ", isok);
    }

    /**
     * Check that the delete method can delete a file on the ftp server
     * @throws FileNotFoundException
     */
    public void testDelete() throws FileNotFoundException {
        File testFile = TestInfo.TESTXML;
        RemoteFile rf = FTPRemoteFile.getInstance(testFile, true, false, true);

        File newFile = new File(TestInfo.TEMPDIR, "newfile.xml");

        //Check that file is actually there
        rf.copyTo(newFile);

        // Delete the file (both locally and one the server)
        newFile.delete();
        rf.cleanup();

        //And check to see that it's gone
        try {
            rf.copyTo(newFile);
            fail("Should throw an exception getting deleted file");
        } catch (IOFailure e) {
            //expected
        }
    }

    /** Test that multiple uploads of the same file does not clash.
     * Test for bug #135
     * @throws IOException
     */
    public void testDoubleUpload() throws IOException {
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, 
        "dk.netarkivet.common.distribute.FTPRemoteFile");
        File testFile = TestInfo.TESTXML;
        PrintWriter write1 = new PrintWriter(new FileWriter(testFile));
        write1.print(FILE_1_CONTENTS);
        write1.close();

        RemoteFile rf1 = RemoteFileFactory.getInstance(testFile, true, false,
                                                       true);

        /** register that testFile should now be present on ftp-server */
        upLoadedFTPRemoteFiles.add(rf1);

        PrintWriter write2 = new PrintWriter(new FileWriter(testFile));
        write2.print(FILE_2_CONTENTS);
        write2.close();

        RemoteFile rf2 = RemoteFileFactory.getInstance(testFile, true, false,
                                                       true);

        upLoadedFTPRemoteFiles.add(rf2);

        File newFile = new File(TestInfo.TEMPDIR, "newfile.xml");
        rf1.copyTo(newFile);
        assertEquals("Contents of first file should be preserved",
                FILE_1_CONTENTS, FileUtils.readFile(newFile));

        rf2.copyTo(newFile);
        assertEquals("Contents of second file should be preserved",
                FILE_2_CONTENTS, FileUtils.readFile(newFile));
    }

    public void tet501MFile() throws FileNotFoundException {
        File zipFile = new File(TestInfo.DATADIR,
                TestInfo.FIVE_HUNDRED_MEGA_FILE_ZIPPED);
        assertTrue("File '" + TestInfo.FIVE_HUNDRED_MEGA_FILE_ZIPPED +
                " does not exist!", zipFile.exists());

        if (!TestInfo.unzipTo(zipFile, TestInfo.TEMPDIR)) {
            logger.warning("Unzipping operation failed!!");
        }

        File unzippedFile = new File(TestInfo.TEMPDIR,
                TestInfo.FIVE_HUNDRED_MEGA_FILE_UNZIPPED);

        assertTrue("File '" + TestInfo.FIVE_HUNDRED_MEGA_FILE_UNZIPPED +
                " does not exist!", unzippedFile.exists());

        RemoteFile rf = FTPRemoteFile.getInstance(unzippedFile, true, false,
                                                  true);

        upLoadedFTPRemoteFiles.add(rf);

        assertEquals("Size of Uploaded data should be the same as original data",
                unzippedFile.length(), rf.getSize());

        File destinationFile = new File(TestInfo.TEMPDIR,
                unzippedFile.getName() + ".new");

        rf.copyTo(destinationFile);

        //throw new IOFailure("Unable to copy remoteFile: " + rf +
        //    " to " + destinationFile);
        /** Check filesizes, and see, if they differ */
        assertEquals("Length of original unzipped file " +
                " and unzipped file retrieved from the ftp-server should not differ!",
                unzippedFile.length(), destinationFile.length());
    }

    // public void testSerializability(){
    //     fail("test of serializability not yet implemented!");
    //
    //}
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

    public void testWrongChecksumThrowsError() throws Exception {
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, 
                "dk.netarkivet.common.distribute.FTPRemoteFile");
        RemoteFile rf = RemoteFileFactory.getInstance(testFile2, true, false,
                                                      true);
        //upload error to ftp server
        File temp = File.createTempFile("foo", "bar");
        FTPClient client = new FTPClient();
        client.connect(Settings.get(FTPRemoteFile.FTP_SERVER_NAME), Integer.parseInt(
                Settings.get(FTPRemoteFile.FTP_SERVER_PORT)));
        client.login(Settings.get(FTPRemoteFile.FTP_USER_NAME),
                     Settings.get(FTPRemoteFile.FTP_USER_PASSWORD));
        Field field = FTPRemoteFile.class.getDeclaredField("ftpFileName");
        field.setAccessible(true);
        String filename = (String)field.get(rf);
        client.storeFile(filename, new ByteArrayInputStream("foo".getBytes()));
        client.logout();
        try {
            rf.copyTo(temp);
            fail("Should throw exception on wrong checksum");
        } catch(IOFailure e) {
            //expected
        }
        assertFalse("Destination file should not exist", temp.exists());
    }

}
