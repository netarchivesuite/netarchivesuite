
package dk.netarkivet.monitor.distribute;

import dk.netarkivet.monitor.registry.distribute.RegisterHostMessage;

/**
 * Interface for all classes which handles monitor-related messages received
 * from a JMS server. This is implemented with a visitor pattern:  Upon
 * receipt, the MonitorMessageHandler.onMessage() method invokes the
 * MonitorMessage.accept() method on the message with itself as argument.
 * The accept() method in turn invokes the MonitorMessageVisitor.visit() method,
 * using method overloading to invoke the visit method for the message received.
 *
 * Thus to handle a message, you should subclass MonitorMessageHandler and
 * override the visit() method for that kind of message.  You should not
 * implement this interface in any other way.
 *
 */
public interface MonitorMessageVisitor {
    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(RegisterHostMessage msg);
}