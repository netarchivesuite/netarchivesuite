package dk.netarkivet.archive.checksum;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.tools.LoadDatabaseChecksumArchive;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.ChecksumJob;

/**
 * ChecksumArchive persisted with a Berkeley DB JE Database.
 * Migrating from the {@link FileChecksumArchive} to the DatabaseChecksumArchive is 
 * done with the {@link LoadDatabaseChecksumArchive} tool.
 */
public class DatabaseChecksumArchive extends ChecksumArchive {
    
    /**
     * The logger used by this class.
     */
    private static Log log = LogFactory.getLog(FileChecksumArchive.class);
    /** The singleton instance of this class. */
    private static DatabaseChecksumArchive instance;
    /** The basedir for the database itself. */
    private File databaseBaseDir;
    /** The subdirectory to the databaseBaseDir, where the database is located. */ 
    private static final String DATABASE_SUBDIR = "DB";
    /** The name of the database. */
    private static final String DATABASE_NAME = "CHECKSUM";
    /** The Database environment. */
    private Environment env;
    /** The Database itself */
    private Database checksumDB;
    /** The minSpaceLeft value. */
    private long minSpaceLeft;
    
    /** The prefix to the removedEntryFile. */
    private static final String WRONG_FILENAME_PREFIX = "removed_";
    /** The suffix to the removedEntryFile. */
    private static final String WRONG_FILENAME_SUFFIX = ".checksum";
    
    /**
     * The file for storing all the deleted entries.
     * Each entry should be: 'date :' + 'wrongEntry'.
     */
    private File wrongEntryFile;
    
    /**
     * Method for obtaining the current singleton instance of this class.
     * If the instance of this class has not yet been constructed, then
     * it will be initialised.
     *  
     * @return The current instance of this class.
     * @throws Exception 
     */
    public static synchronized DatabaseChecksumArchive getInstance() throws Exception {
        if(instance == null) {
            instance = new DatabaseChecksumArchive();
        }
        return instance;
    }   
    
    /**
     * Constructor.
     * Retrieves the minimum space left variable, and ensures the existence of
     * the archive file. If the file does not exist, then it is created.
     * @throws Exception 
     */
    public DatabaseChecksumArchive() throws DatabaseException{
        super();
        
        // Get the minimum space left setting.
        long minSpaceLeft = Settings.getLong(
                ArchiveSettings.CHECKSUM_MIN_SPACE_LEFT);
        // make sure, that minSpaceLeft is non-negative.
        if(minSpaceLeft < 0) {
            String msg = "Wrong setting of minSpaceRequired read from "
                + "Settings: int " + minSpaceLeft;
            log.warn(msg);
            throw new ArgumentNotValid(msg);
        }
        
        // Initialize the checksum database.
        initializeDatabase();
        
        // Initialize Wrong Entry file
        initializeWrongEntryFile();
    }

    private void initializeWrongEntryFile() {
        String WrongEntryFilename = WRONG_FILENAME_PREFIX + Settings.get(
                    CommonSettings.USE_REPLICA_ID) + WRONG_FILENAME_SUFFIX;
        wrongEntryFile = new File(databaseBaseDir, WrongEntryFilename);
        
        // ensure that the file exists.
        if(!wrongEntryFile.exists()) {
            try {
                wrongEntryFile.createNewFile();
            } catch (IOException e) {
                String msg = "Cannot create 'wrongEntryFile'!";
                log.error(msg);
                throw new IOFailure(msg, e);
            }
        }
    }

    private void initializeDatabase() throws DatabaseException{
        databaseBaseDir = Settings.getFile(ArchiveSettings.CHECKSUM_BASEDIR);
        File homeDirectory = new File(databaseBaseDir, DATABASE_SUBDIR);
        if (!homeDirectory.isDirectory()) {
            homeDirectory.mkdirs();
        }
        log.info("Opening DB-environment in: " + homeDirectory.getAbsolutePath());

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        
        env = new Environment(homeDirectory, envConfig);
        checksumDB = env.openDatabase(null, DATABASE_NAME, dbConfig);
    }

    @Override
    public boolean hasEnoughSpace() {
        if (checkDatabaseDir(databaseBaseDir)
                && (FileUtils.getBytesFree(databaseBaseDir) > minSpaceLeft)) {
            return true;
        }
        return false;
    }
    
    private boolean checkDatabaseDir(File file) {
        // The file must exist.
        if (!file.isDirectory()) {
            log.warn("The file '" + file.getAbsolutePath()
                    + "' is not a valid directory.");
            return false;
        }
        // It must be writable.
        if (!file.canWrite()) {
            log.warn("The directory '" + file.getAbsolutePath() 
                    + "' is not writable");
            return false;
        }
        return true;
    }
    

    @Override
    public File correct(String filename, File correctFile) throws IOFailure,
            ArgumentNotValid, IllegalState {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(correctFile, "File correctFile");

        // If no file entry exists, then IllegalState
        if(!hasEntry(filename)) {
            String errMsg = "No file entry for file '" + filename + "'.";
            log.error(errMsg);
            throw new IllegalState(errMsg);
        }
        
        // retrieve the checksum
        String currentChecksum = getChecksum(filename);
        
        // Calculate the new checksum and verify that it is different.
        String newChecksum = calculateChecksum(correctFile);
        if(newChecksum.equals(currentChecksum)) {
            // This should never occur.
            throw new IllegalState("The checksum of the old 'bad' entry is "
                    + " the same as the checksum of the new correcting entry");
        }
        
        // Make entry in the wrongEntryFile.
        String badEntry = ChecksumJob.makeLine(filename, 
                currentChecksum);
        appendWrongRecordToWrongEntryFile(badEntry);
        
        // Correct the bad entry, by changing the value to the newChecksum.'
        // Since the checksumArchive is a hashmap, then putting an existing 
        // entry with a new value will override the existing one.
        put(filename, newChecksum);
                
        // Make the file containing the bad entry be returned in the 
        // CorrectMessage.
        File removedEntryFile; 
        try {
            // Initialise file and writer.
            removedEntryFile = File.createTempFile(filename, "tmp", 
                    FileUtils.getTempDir());
            FileWriter fw = new FileWriter(removedEntryFile);
            
            // Write the bad entry.
            fw.write(badEntry);
            
            // flush and close.
            fw.flush();
            fw.close();
        } catch (IOException e) {
            throw new IOFailure("Unable to create return file for "
                    + "CorrectMessage", e);
        }
        
        // Return the file containing the removed entry.
        return removedEntryFile;
    }

    /**
     * Method for appending a 'wrong' entry in the wrongEntryFile.
     * It will be noted which time the wrong entry was appended:
     * date + " : " + wrongRecord.
     * 
     * @param wrongRecord The record to append.
     * @throws IOFailure If the wrong record cannot be appended correctly.
     */
    private synchronized void appendWrongRecordToWrongEntryFile(String 
            wrongRecord) throws IOFailure {
        try {
            // Create the string to append: date + 'wrong record'.
            String entry = new Date().toString() + " : " + wrongRecord + "\n";

            // get a filewriter for the checksum file, and append the record. 
            boolean appendToFile = true;
            FileWriter fwrite = new FileWriter(wrongEntryFile, appendToFile);
            fwrite.append(entry);

            // close fileWriter.
            fwrite.flush();
            fwrite.close();
        } catch (IOException e) {
            String errMsg = "Cannot put a bad record to the 'wrongEntryFile'.";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }

    @Override
    public String getChecksum(String filename) {
        byte[] keyBytes = null;
        try {
            keyBytes = filename.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalState("Unexpected '" 
                    + UnsupportedEncodingException.class.getName()
                    + "' exception: " + e);
        }
        DatabaseEntry key = new DatabaseEntry(keyBytes);
        DatabaseEntry data = new DatabaseEntry();

        OperationStatus status = null;
        try {
            status = checksumDB.get(null, key, data, null);
        } catch (DatabaseException e) {
            throw new IllegalState("Unexpected '" 
                    + DatabaseException.class.getName() 
                    + "' exception: " + e);
        }
        String resultChecksum = null;
        if (status == OperationStatus.SUCCESS) {
          try {
            resultChecksum = new String(data.getData(), "UTF-8");
          } catch (UnsupportedEncodingException e) {
              throw new IllegalState("Unexpected '" 
                      + UnsupportedEncodingException.class.getName()
                      + "' exception: " + e);
          }
        }
        return resultChecksum;
    }

    @Override
    public boolean hasEntry(String filename) {
        return (getChecksum(filename) != null);
    }

    @Override
    public void upload(RemoteFile file, String filename) {
        ArgumentNotValid.checkNotNull(file, "RemoteFile file");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        InputStream input = null;

        try {
            input = file.getInputStream();
            String newChecksum = calculateChecksum(input);
            if (hasEntry(filename)) {
              // fetch already stored checksum
              String oldChecksum = getChecksum(filename);
              if (newChecksum.equals(oldChecksum)) {
                  log.warn("Cannot upload archivefile '" + filename + "', "
                          + "it is already archived with the same checksum: '"
                          + oldChecksum);
              } else {
                  throw new IllegalState("Cannot upload archivefile '" + filename 
                          + "', it is already archived with different checksum."
                          + " Archive checksum: '" + oldChecksum
                          + "' and the uploaded file has: '" + newChecksum + "'.");
              }
              // It is considered a success that it already is within the archive, 
              // thus do not throw an exception. 
              return;
            } else {
              put(filename, newChecksum);
            }
        } finally {
            if (input != null) {
                IOUtils.closeQuietly(input);
            }
        }

    }
    
    public void put(String filename, String checksum) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");

        DatabaseEntry theKey = null;
        DatabaseEntry theData = null;
        try {
            theKey = new DatabaseEntry(filename.getBytes("UTF-8"));
            theData = new DatabaseEntry(checksum.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ArgumentNotValid(e.toString());
        }

        try {
            checksumDB.put(null, theKey, theData);
        } catch (DatabaseException e) {
           throw new IOFailure("Database exception occuring during ingest", e);
        }
    }  
    
    @Override
    protected String calculateChecksum(File f) {
        return ChecksumCalculator.calculateMd5(f);
    }

    @Override
    protected String calculateChecksum(InputStream is) {
        return ChecksumCalculator.calculateMd5(is);
    }

    @Override
    public File getArchiveAsFile() {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("tmp", "tmp", 
                    FileUtils.getTempDir());
            dumpDatabaseToFile(tempFile, false);
        } catch (IOException e) {
            throw new IOFailure(e.toString());
        }
       
        return tempFile;
    }

    /** 
     * Write the contents of the database to the given file.
     * @param outputFile The outputfile whereto the data is written.
     * @param writeOnlyFilenames If true, we only write the filenames to the files, 
     * not the checksums
     * @throws IOException If unable to write to file for some reason
     */
    private void dumpDatabaseToFile(File tempFile, boolean writeOnlyFilenames) 
            throws IOException {
        Cursor cursor = null;
        File resultFile = tempFile;

        FileWriter fw = new FileWriter(resultFile);
        try { 
            cursor = checksumDB.openCursor(null, null);

            DatabaseEntry foundKey = new DatabaseEntry();
            DatabaseEntry foundData = new DatabaseEntry();

            while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) ==
                OperationStatus.SUCCESS) {
                String keyString = new String(foundKey.getData());
                String dataString = new String(foundData.getData());
                if (writeOnlyFilenames){
                    fw.append(keyString);
                } else {
                    fw.append(keyString); fw.append(ChecksumJob.STRING_FILENAME_SEPARATOR); 
                    fw.append(dataString);
                }
                fw.append('\n'); // end with newline
            }
            fw.flush();
        } catch (DatabaseException de) {
            throw new IOFailure("Error accessing database." + de);
        } finally {
            if (fw != null) {
                IOUtils.closeQuietly(fw);
            }
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (DatabaseException e) {
                    log.warn("Database error occurred when closing the cursor: " + e);
                }
            }
        }
    }

    @Override
    public File getAllFilenames()  {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("tmp", "tmp", 
                    FileUtils.getTempDir());
        } catch (IOException e) {
            throw new IOFailure(e.toString());
        }
        
        try {
            dumpDatabaseToFile(tempFile, true);
        } catch (IOException e) {
            throw new IOFailure("Error during the getAllFilenames operation: " + e);
        }
        
        return tempFile;
    }

    @Override
    public void cleanup() {
        if (checksumDB != null) {
            try {
                checksumDB.close();
            } catch (DatabaseException e) {
                log.warn("Unable to close database. The error was :" + e);
            }
        }
    }
}
