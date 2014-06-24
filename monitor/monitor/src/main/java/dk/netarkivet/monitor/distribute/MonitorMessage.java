
package dk.netarkivet.monitor.distribute;

import java.io.Serializable;

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
public abstract class MonitorMessage extends NetarkivetMessage
        implements Serializable {
    /**
     * Creates a new MonitorMessage.
     *
     * @param to        the initial receiver of the message
     * @param replyTo   the initial sender of the message
     * @throws ArgumentNotValid if to==replyTo or there is a null parameter.
     */
    protected MonitorMessage(ChannelID to, ChannelID replyTo) {
        super(to, replyTo);
    }

    /**
     * Should be implemented as a part of the visitor pattern. e.g.: public void
     * accept(MonitorMessageVisitor v) { v.visit(this); }
     *
     * @see MonitorMessageVisitor
     *
     * @param v A message visitor
     */
    public abstract void accept(MonitorMessageVisitor v);
}