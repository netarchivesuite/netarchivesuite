package dk.netarkivet.archive.bitarchive.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Message for telling the bitarchives to terminate a specific batchjob.
 */
public class BatchTerminationMessage extends NetarkivetMessage {

    /** The ID of the batchjob to terminate.*/
    private String terminateID;
    
    /**
     * Constructor.
     * 
     * @param to Where the message should be sent.
     * @param replyTo Where the message is sent from.
     * @param batchID The ID of the batchjob to terminate.
     * @throws ArgumentNotValid If the batchID is either null or the empty
     * string.
     */
    public BatchTerminationMessage(ChannelID to, String batchID) 
            throws ArgumentNotValid {
        this(to, Channels.getError(), batchID);
    }
    
    /**
     * Constructor.
     * 
     * @param to Where the message should be sent.
     * @param replyTo Where the message is sent from.
     * @param batchID The ID of the batchjob to terminate.
     * @throws ArgumentNotValid If the batchID is either null or the empty
     * string.
     */
    public BatchTerminationMessage(ChannelID to, ChannelID replyTo, 
            String batchID) throws ArgumentNotValid {
        super(to, replyTo);
        ArgumentNotValid.checkNotNullOrEmpty(batchID, "String batchID");
        terminateID = batchID;
    }

    /**
     * Method for retrieving the ID of the batchjob to terminate.
     * @return The ID of the batchjob to terminate.
     */
    public String getTerminateID() {
        return terminateID;
    }
    
    /**
     * Extends the default toString of NetarkiveMessage with the terminateID.
     * @return The string representation of this message.
     */
    public String toString() {
        return super.toString() + ", terminateID = " + terminateID;
    }
}
