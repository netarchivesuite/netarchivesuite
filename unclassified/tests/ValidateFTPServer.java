import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.FTPRemoteFile;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;


public class ValidateFTPServer {

    private FTPClient theFTPClient;
    private ArrayList<RemoteFile> upLoadedFTPRemoteFiles = new ArrayList<RemoteFile>();
    private ArrayList<String> upLoadedFiles = new ArrayList<String>();
    
    private File tmpDir = new File(this.getClass().getSimpleName());

    private File testFile1;
    private File testFile2;
    private File testFile3;
    
    private String ftpHost;
    private String ftpUser;
    private String ftpPasswd;
    private int ftpPort;
    
    public ValidateFTPServer(String ftphost2, int port, String user, String passwd) throws Exception {
        ftpHost = ftphost2;
        ftpUser = user;
        ftpPasswd = passwd;
        ftpPort = port;
        
        Settings.set(FTPRemoteFile.FTP_SERVER_NAME, ftpHost);
        Settings.set(FTPRemoteFile.FTP_SERVER_PORT, port + "");
        Settings.set(FTPRemoteFile.FTP_USER_NAME, ftpUser);
        Settings.set(FTPRemoteFile.FTP_USER_PASSWORD, ftpPasswd);
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, FTPRemoteFile.class.getName());
        if (tmpDir.exists()) {
            FileUtils.removeRecursively(tmpDir);
            if (tmpDir.exists()) {
                System.out.println("Unable to delete tmpdir '" 
                        + tmpDir.getAbsolutePath() + "'");
                System.exit(1);
            }
        } 
        
        if (!tmpDir.mkdir()) {
            System.out.println("Unable to create tmpdir '" 
                    + tmpDir.getAbsolutePath() + "'");
            System.exit(1);    
        }
        
    }

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
       if (args.length != 4) {
           printArgs();
       }
       String ftphost = args[0];
       int ftpPort = Integer.parseInt(args[1]);
       String user = args[2];
       String passwd = args[3];
       System.out.println("Confirming ftp-server at '" + ftphost 
               + "' using username/passwd='" + user + "/" + passwd 
               + "'");
       
       ValidateFTPServer validate = new ValidateFTPServer(ftphost, ftpPort, user, passwd);
       boolean result = validate.test();
       if (result == false) {
           System.out.println("test failed");
       } else {
           System.out.println("test succeeded");
       }
    }

    private boolean test() throws Exception {
        /* make 3 duplicates of TestInfo.TESTXML: test1.xml, test2.xml, test3.xml */
        testFile1 = new File("test1000.xml");
        testFile2 = new File("test2000.xml");
        testFile3 = new File("test3000.xml");
        String fileAsString = "<test>" 
            + "<file>"
            + "<attachHere>Should go away</attachHere>"
            + "<keepThis>Should be kept</keepThis>"
            + "</file>"
            + "<foo>"
            + "<attachHere>Should stay</attachHere>"
            + "</foo>"
            + "</test>";
        List<String> fileAsList = new ArrayList<String>();
        fileAsList.add(fileAsString);
        FileUtils.writeCollectionToFile(testFile1, fileAsList);
        FileUtils.writeCollectionToFile(testFile2, fileAsList);
        FileUtils.writeCollectionToFile(testFile3, fileAsList);        

        /* Connect to test ftp-server. */
        theFTPClient = new FTPClient();
        
        try {
            theFTPClient.connect(ftpHost, ftpPort);
            if (!theFTPClient.login(ftpUser, ftpPasswd)) {
                System.out.println("Could not login to ' + " + ftpHost
                        + ":" + ftpPort + "' with username,password="
                        + ftpUser + "," + ftpPasswd);
                System.exit(1);
            }
            if (!theFTPClient.setFileType(FTPClient.BINARY_FILE_TYPE)) {
                System.out.println("Unable to set the file type to binary after login");
                System.exit(1);
            }
            if (!testConfigSettings()) {
                return false;
            }
            if (!testUploadAndRetrieve()) {
                return false;
            }
            if (!testDelete()) {
                return false;
            }
            if (!test501MFile()) {
                return false;
            }
            if (!testWrongChecksumThrowsError()) {
                return false;
            }
            
        } catch (SocketException e) {
            e.printStackTrace();
            throw new IOFailure("Connect to " + ftpHost + ":" + ftpPort +
                                " failed", e.getCause());
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOFailure("Connect to " + ftpHost + ":" + ftpPort +
                                " failed",  e.getCause());
        } finally {
           if (theFTPClient != null) {
               theFTPClient.disconnect();
           }
        }
        
            
        
        
     return true;
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
    public boolean testConfigSettings() throws IOException {
        /** this code has been tested with
         * the ftp-server proftpd (www.proftpd.org), using
         * the configuration stored in CVS here: /projects/webarkivering/proftpd.org
         */
        String nameOfUploadedFile;
        String nameOfUploadedFile3;
        
        File inputFile = testFile1;
        File inputFile2 = testFile2;
        File inputFile3 = testFile3;
        
        InputStream in = new FileInputStream(inputFile);
        InputStream in2 = new FileInputStream(inputFile2);
        InputStream in3 = new FileInputStream(inputFile3);

        nameOfUploadedFile = inputFile.getName();
        nameOfUploadedFile3 = inputFile3.getName();
        

        /** try to append data to file on FTP-server. */
        /** Assumption: File does not exist on FTP-server. */
        if (onServer(nameOfUploadedFile)) {
            System.out.println("File '" + nameOfUploadedFile 
                    + "' should not exist already on server");
            return false;
        }
        if (!theFTPClient.appendFile(nameOfUploadedFile, in)) {
            System.out.println("Appendfile operation failed");
            return false;
        }
        upLoadedFiles.add(nameOfUploadedFile);

        /** try to append data to file on the FTP-server. */
        if (!theFTPClient.appendFile(nameOfUploadedFile, in2)) {
            System.out.println("Appendfile operation 2 failed");
            return false; 
        }
        
        if (!upLoadedFiles.contains(nameOfUploadedFile)) {
            upLoadedFiles.add(nameOfUploadedFile);
        }

        /** try to store data to a file on the FTP-server. */
        if (!theFTPClient.storeFile(nameOfUploadedFile3, in3)) {
            System.out.println("Store operation failed");
            return false;
        }
        upLoadedFiles.add(nameOfUploadedFile3);
        return true;
    }
    
    

    private static void printArgs() {
        System.out.println("ValidateFTPServer ftphost ftpPort user passwd");
        System.exit(1);
    }
    
    
    /**
     * (1) Test, if uploaded and retrieved file are equal
     * (2) test that rf.getSize() reports the correct value;
     * @throws IOException
     */
    public boolean testUploadAndRetrieve() throws IOException {
        File testFile = testFile1;
        RemoteFile rf = FTPRemoteFile.getInstance(testFile, true, false, true);

        File newFile = new File(tmpDir, "newfile.xml");

        /** register that testFile should now be present on ftp-server */
        upLoadedFTPRemoteFiles.add(rf);

        if (rf.getSize() != testFile.length()) {
            System.out.println("The size of the file written to the ftp-server " +
                "should not differ from the original size");
            return false;
        }
        rf.copyTo(newFile);

        /** check, if the original file and the same file retrieved
         * from the ftp-server contains the same contents
         */
        byte[] datasend = FileUtils.readBinaryFile(testFile);
        byte[] datareceived = FileUtils.readBinaryFile(newFile);
        boolean isok = Arrays.equals(datareceived, datasend);
        if (!isok) {
            System.out.println("verify the same data received as uploaded ");
            return false;
        }
        
        return true;
    }

    /**
     * Check that the delete method can delete a file on the ftp server
     * @throws FileNotFoundException
     */
    public boolean testDelete() throws FileNotFoundException {
        File testFile = testFile1;
        RemoteFile rf = FTPRemoteFile.getInstance(testFile, true, false, true);

        File newFile = new File(tmpDir, "newfile.xml");

        //Check that file is actually there
        rf.copyTo(newFile);

        // Delete the file (both locally and one the server)
        newFile.delete();
        rf.cleanup();

        //And check to see that it's gone
        try {
            rf.copyTo(newFile);
            System.out.println("Should throw an exception getting deleted file");
            return false;
        } catch (IOFailure e) {
            //expected
        }
        return true;
    }

//    /** Test that multiple uploads of the same file does not clash.
//     * Test for bug #135
//     * @throws IOException
//     */
//    public void testDoubleUpload() throws IOException {
//        Settings.set(CommonSettings.REMOTE_FILE_CLASS, 
//        "dk.netarkivet.common.distribute.FTPRemoteFile");
//        File testFile = testFile1;
//        PrintWriter write1 = new PrintWriter(new FileWriter(testFile));
//        write1.print(FILE_1_CONTENTS);
//        write1.close();
//
//        RemoteFile rf1 = RemoteFileFactory.getInstance(testFile, true, false,
//                                                       true);
//
//        /** register that testFile should now be present on ftp-server */
//        upLoadedFTPRemoteFiles.add(rf1);
//
//        PrintWriter write2 = new PrintWriter(new FileWriter(testFile));
//        write2.print(FILE_2_CONTENTS);
//        write2.close();
//
//        RemoteFile rf2 = RemoteFileFactory.getInstance(testFile, true, false,
//                                                       true);
//
//        upLoadedFTPRemoteFiles.add(rf2);
//
//        File newFile = new File(tmpDir, "newfile.xml");
//        rf1.copyTo(newFile);
//        assertEquals("Contents of first file should be preserved",
//                FILE_1_CONTENTS, FileUtils.readFile(newFile));
//
//        rf2.copyTo(newFile);
//        assertEquals("Contents of second file should be preserved",
//                FILE_2_CONTENTS, FileUtils.readFile(newFile));
//    }

    public boolean test501MFile() throws Exception {
        long FiveHundredMbytes = 530000000;
        File bigFile = new File(tmpDir, "500-mega");
        writeBytesToFile(FiveHundredMbytes, bigFile);
        
        if (!bigFile.exists()) {
            System.out.println("File '" + bigFile.getAbsolutePath() +
                "' does not exist!");
            return false;
        }
        
        RemoteFile rf = FTPRemoteFile.getInstance(bigFile, true, false, true);

        upLoadedFTPRemoteFiles.add(rf);
        if (bigFile.length() != rf.getSize()) {
            System.out.println("Size of Uploaded data should be the same as original data");
            return false;
        }
 
        File destinationFile = new File(tmpDir,
                bigFile.getName() + ".new");

        rf.copyTo(destinationFile);

        /** Check filesizes, and see, if they differ. */
        if (bigFile.length() != destinationFile.length()) {
        System.out.println("Length of original unzipped file ' " 
                + bigFile.getAbsolutePath() 
                + "' and unzipped file retrieved from the ftp-server '" 
                + destinationFile.getAbsolutePath() +  "'" 
                + "should not differ!");
            return false;
        }
        return true;
    }

    
    public boolean testWrongChecksumThrowsError() throws Exception {
        RemoteFile rf = RemoteFileFactory.getInstance(testFile2, true, false,
                                                      true);
        if (!(rf instanceof FTPRemoteFile)) {
            System.out.println("The remotefile returned from the factory was incorrect type: " 
                    + rf.getClass().getName());  
            return false;
        }
        //upload error to ftp server
        File temp = File.createTempFile("foo", "bar", tmpDir);
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
            System.out.println("Should throw exception on wrong checksum");
            return false;
        } catch(IOFailure e) {
            //expected
        }
        if (temp.exists()) {
            System.out.println(
                    "Destination file '" +  temp.getAbsolutePath() 
                    + "' should not exist");
            return false;
        }
        return true;
    }

    public boolean onServer(String nameOfUploadedFile)
    throws IOException {
        ArgumentNotValid.checkNotNull(theFTPClient, "theFTPClient should not be null");

        FTPFile[] listOfFiles = theFTPClient.listFiles();

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
    
    public static void writeBytesToFile(long bytes, File destination) throws Exception {
        // A reasonably optimal value for the chunksize
        int byteChunkSize = 10000000;

        long nbytes = bytes;
        File outputFile = destination;
        byte[] byteArr = new byte[byteChunkSize];
        FileOutputStream os = new FileOutputStream(outputFile);
        FileChannel chan = os.getChannel();
        for (int i = 0; i < nbytes / byteChunkSize; i++) {
            chan.write(ByteBuffer.wrap(byteArr));
        }
        os.close();
        chan.close();
    }
}
