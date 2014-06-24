
package dk.netarkivet.harvester.distribute;

import java.io.Serializable;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Common base class for messages exchanged between a harvester server and
 * a harvester client.
 */
@SuppressWarnings({ "serial"})
public abstract class HarvesterMessage extends NetarkivetMessage
        implements Serializable {
    /**
     * Creates a new HarvesterMessage.
     *
     * @param to        the initial receiver of the message
     * @param replyTo   the initial sender of the message
     * @throws ArgumentNotValid if to==replyTo or there is a null parameter.
     */
    protected HarvesterMessage(ChannelID to, ChannelID replyTo) {
        super(to, replyTo);
    }

    /**
     * Should be implemented as a part of the visitor pattern. fx.: public void
     * accept(HarvesterMessageVisitor v) { v.visit(this); }
     *
     * @param v A message visitor
     */
    public abstract void accept(HarvesterMessageVisitor v);
}
