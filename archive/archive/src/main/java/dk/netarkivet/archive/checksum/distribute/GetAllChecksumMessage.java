package dk.netarkivet.archive.checksum.distribute;

import java.io.File;

import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * The GetChecksumMessage has the purpose to retrieve the checksum of all 
 * the file. The output is in the form of a map, where the keys are the 
 * filenames and the values are checksums.
 * 
 * This is checksum replica alternative to sending a ChecksumBatchJob.
 */
public class GetAllChecksumMessage extends ArchiveMessage {
    /** A random generated serial version UID.*/
    private static final long serialVersionUID = 5944687747568698584L;

    /** The file containing the output.*/
    private RemoteFile remoteFile;
    /** The id for the replica where this message should be sent.*/
    private String replicaId;

    /**
     * Constructor.
     * 
     * @param to Where this message is headed.
     * @param replyTo Where the reply on this message is sent.
     * @param repId The replica where the job involved in this message is
     * to be performed.
     */
    public GetAllChecksumMessage(ChannelID to, ChannelID replyTo, 
	    String repId) {
	super(to, replyTo);
	this.replicaId = repId;
    }
    
    /**
     * Method for setting the resulting file. This file will be retrieved from 
     * the caller of this message. This should be a movable instance since the
     * temporary file should be removed after is has been retrieved.
     * 
     * @param file The file with the checksum message.
     */
    public void setFile(File file) {
	ArgumentNotValid.checkNotNull(file, "File file");
	
	remoteFile = RemoteFileFactory.getMovefileInstance(file);
    }
    
    /**
     * Method for retrieving the resulting file.
     * 
     * @param toFile The file for the remotely retrieved content.
     */
    public void getData(File toFile) {
	ArgumentNotValid.checkNotNull(toFile, "File toFile");
        if (remoteFile == null) {
            throw new IOFailure("The remote file is not valid. "
        	    + "Data cannot be retrieved.");
        }
        remoteFile.copyTo(toFile);
        try {
            remoteFile.cleanup();
        } catch (IOFailure e) {
            //Just log errors on deleting. They are fairly harmless.
            // Can't make Logger a field, as this class is Serializable
            LogFactory.getLog(getClass().getName()).warn(
                    "Could not delete remote file " + remoteFile.getName());
        }
        remoteFile = null;
    }
    
    /**
     * Method for retrieving the id for the replica where this message should 
     * be sent.
     * 
     * @return The id for the replica.
     */
    public String getReplicaId() {
	return replicaId;
    }

    /**
     * Generate String representation of this object.
     * 
     * @return String representation of this object
     */
    public String toString() {
	return super.toString();
    }

    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }
}
