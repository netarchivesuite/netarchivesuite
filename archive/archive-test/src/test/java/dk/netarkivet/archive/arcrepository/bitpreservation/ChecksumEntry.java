package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.batch.ChecksumJob;

/**
 * Method for storing a checksum along with its filename.
 * 
 * This class also holds the method for extracting the results of a 
 * ChecksumJob into a list of ChecksumEntry, which is used by the 
 * BitPreservationDAO.
 */
public class ChecksumEntry extends Object {
    /** The name of the file for which the checksum belongs.*/
    private String filename;
    /** The checksum of the file.*/
    private String checksum;
    
    /**
     * Constructor.
     * 
     * @param filename The name of the file.
     * @param checksum The checksum of the file.
     */
    public ChecksumEntry(String filename, String checksum) {
        this.filename = filename;
        this.checksum = checksum;
    }
    
    /**
     * Retrieves the filename.
     * 
     * @return The filename.
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Retrieves the checksum.
     * 
     * @return The checksum.
     */
    public String getChecksum() {
        return checksum;
    }
    
    /**
     * Retrieval of the hashCode of this instance.
     * 
     * @return The hashCode of this instance.
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result;
        if(checksum != null) {
            result += checksum.hashCode();
        }
        result = prime * result;
        if(filename != null) {
            result += filename.hashCode();
        }
        return result;
    }

    /**
     * Method for testing whether a ChecksumEntry is identical to another
     * ChecksumEntry. 
     * 
     * @param obj The object to evaluate whether it is identical to this 
     * ChecksumEntry.
     * @return Whether the argument has the same values as this ChecksumEntry.
     * It returns false if the argument is not of type ChecksumEntry, or if
     * it has either different filename or different checksum.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ChecksumEntry other = (ChecksumEntry) obj;
        if (checksum == null) {
            if (other.checksum != null) {
                return false;
            }
        } else if (!checksum.equals(other.checksum)) {
            return false;
        }
        if (filename == null) {
            if (other.filename != null) {
                return false;
            }
        } else if (!filename.equals(other.filename)) {
            return false;
        }       
        return true;
    }

    /**
     * Make human readable string.
     * 
     * @return This instance as a human readable string.
     */
    public String toString() {
        return filename + "##" + checksum;
    }
    
    /**
     * Method for changing the resulting file of a checksum job into a list of 
     * ChecksumEntry.
     * 
     * @param checksumjobOutput The file with the output from a checksum job.
     * @return The list of the checksum entries.
     * @throws ArgumentNotValid If the checksumjobOutput is null.
     */
    public static List<ChecksumEntry> parseChecksumJob(File checksumjobOutput) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(checksumjobOutput, 
                "File checksumjobOutput");
        
        // make the result list.
        List<ChecksumEntry> res = new ArrayList<ChecksumEntry>();

        // go through all entries in the file
        List<String> lines = FileUtils.readListFromFile(checksumjobOutput);

        for (String line : lines) {
            // parse the input.
            KeyValuePair<String, String> kvp = ChecksumJob.parseLine(line);
            // put it into the resulting list.
            res.add(new ChecksumEntry(kvp.getKey(), kvp.getValue()));
        }

        return res;
    }
}
