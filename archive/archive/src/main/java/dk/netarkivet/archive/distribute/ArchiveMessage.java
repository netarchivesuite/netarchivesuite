
package dk.netarkivet.archive.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Common base class for messages exchanged between an archive server and an
 * archive client (or within an archive).
 *
 * @see NetarkivetMessage
 */
@SuppressWarnings({ "serial"})
public abstract class ArchiveMessage extends NetarkivetMessage {
    /**
     * Creates a new ArchiveMessage.
     *
     * @param to        the initial receiver of the message
     * @param replyTo   the initial sender of the message
     * @throws ArgumentNotValid if to==replyTo or there is a null parameter.
     */
    protected ArchiveMessage(ChannelID to, ChannelID replyTo) 
            throws ArgumentNotValid {
        super(to, replyTo);
    }

    /**
     * Should be implemented as a part of the visitor pattern. e.g.: public void
     * accept(ArchiveMessageVisitor v) { v.visit(this); }
     *
     * @see ArchiveMessageVisitor
     *
     * @param v A message visitor
     */
    public abstract void accept(ArchiveMessageVisitor v);
}
