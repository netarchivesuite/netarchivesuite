package dk.netarkivet.archive.bitarchive.distribute;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;


/**
 * Container for upload request.
 *
 */
@SuppressWarnings({ "serial"})
public class UploadMessage extends ArchiveMessage {
    /** the name of the file to upload. */
    private String arcfileName;

    /** The actual data. */
    private RemoteFile theRemoteFile;


    /**
     * Construct UploadMessage.
     * @param to      Channel to message to
     * @param replyTo Channel to reply back to
     * @param rf The RemoteFile to upload
     */
    public UploadMessage(ChannelID to, ChannelID replyTo, RemoteFile rf) {
        super(to, replyTo);
        ArgumentNotValid.checkNotNull(rf, "rf");
        arcfileName = rf.getName();
        theRemoteFile = rf;
    }

  /**
   * Retrieve name of the uploaded file.
   * @return current value of arcfileName
   */
    public String getArcfileName() {
      return arcfileName;
    }

  /**
   * Get method for field theRemoteFile.
   * @return Current value of theRemoteFile
   */
    public RemoteFile getRemoteFile() {
        return theRemoteFile;
    }

    /**
     * Should be implemented as a part of the visitor pattern. fx.: public void
     * accept(ArchiveMessageVisitor v) { v.visit(this); }
     *
     * @param v A message visitor
     */
    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }

    /**
     * Generate String representation of this object.
     * @return String representation of this object
     */
    public String toString() {
        return super.toString() + " Arcfile: " + arcfileName;
    }

}
