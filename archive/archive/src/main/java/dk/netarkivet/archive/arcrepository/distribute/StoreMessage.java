package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Messages requesting store of file. This message is sent to the arc repository
 * which distributes the file to the known bitarchives, and checks the result,
 * and then responds to the sender.
 */
@SuppressWarnings({ "serial"})
public class StoreMessage extends ArchiveMessage {
    /** The actual data. */
    private RemoteFile theRemoteFile;

    /**
     * Construct StoreMessage.
     * @param replyTo Channel to reply back to
     * @param arcfile The file to store
     */
    public StoreMessage(ChannelID replyTo, File arcfile) {
        super(Channels.getTheRepos(), replyTo);
        ArgumentNotValid.checkNotNull(arcfile, "arcfile");
        theRemoteFile = RemoteFileFactory.getDistributefileInstance(arcfile);
    }

  /**
   * Retrieve name of the stored file.
   * @return current value of arcfileName
   */
    public String getArcfileName() {
      return theRemoteFile.getName();
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
        return super.toString() + " Arcfile: " + getArcfileName();
    }

}
