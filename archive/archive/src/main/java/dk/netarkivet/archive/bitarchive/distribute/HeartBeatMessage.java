package dk.netarkivet.archive.bitarchive.distribute;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Simple class representing a HeartBeat message from a bit archive application.
 * A heartbeat has an applicationId, that identifies the application
 * that generated the heartbeat.
 *
 * TODO This class should probably contain more status data from
   bit archive application later.
 *
 */
@SuppressWarnings({ "serial"})
public class HeartBeatMessage extends ArchiveMessage {

    /** time when heartbeat occurred. Note that timestamps cannot be compared
         between processes.
      */
    private long timestamp;
    /** id of the application sending the heartbeat.*/
    private String applicationId;

    /**
     * Creates a heartbeat message.
     * The time of the heartbeat is set to the creation of this object.
     *
     * @param inReceiver   ChannelID for the recipient of this message.
     * @param applicationId - id of the application that sent the heartbeat
     */
    public HeartBeatMessage(ChannelID inReceiver, String applicationId) {
        super(inReceiver, Channels.getError());
        ArgumentNotValid.checkNotNullOrEmpty(applicationId, "applicationId");
        timestamp = System.currentTimeMillis();
        this.applicationId = applicationId;
    }

    /**
     * @return time of heartbeat occurrence.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return id of the application that generated the heartbeat.
     */
    public String getBitarchiveID() {
        return applicationId;
    }

    /**
     * Retrieval of a string representation of this instance.
     * 
     * @return The string representation of this instance.
     */
    public String toString() {
        return ("Heartbeat for " + applicationId + " at " + timestamp);
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
}
