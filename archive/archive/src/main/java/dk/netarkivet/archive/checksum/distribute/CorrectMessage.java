package dk.netarkivet.archive.checksum.distribute;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * The message to correct something!
 */
public class CorrectMessage extends ArchiveMessage {
    
    static final String CORRECT_MESSAGE_PREFIX = "Correct";
    
    private RemoteFile theRemoteFile;
    private String arcFilename;
    private String theChecksum;

    public CorrectMessage(ChannelID to, ChannelID replyTo, String checksum, 
	    RemoteFile file) {
        super(to, replyTo, CORRECT_MESSAGE_PREFIX);
        ArgumentNotValid.checkNotNull(file, "RemoteFile file");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        this.theChecksum = checksum;
        this.theRemoteFile = file;
        this.arcFilename = file.getName();
    }

    /**
     * Retrieve name of the uploaded file.
     * @return current value of arcfileName
     */
      public String getArcfileName() {
        return arcFilename;
      }

    /**
     * Get method for retrieving the remote file.
     * 
     * @return The remote file.
     */
      public RemoteFile getRemoteFile() {
          return theRemoteFile;
      }
      
      /**
       * Method for retrieving the checksum for the archive entry.
       * 
       * @return The checksum for the archive entry.
       */
      public String getChecksum() {
	  return theChecksum;
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
          return super.toString() + " Arcfile: " + arcFilename;
      }
}
