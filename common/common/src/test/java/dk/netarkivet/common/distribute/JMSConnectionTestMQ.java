/* $Id$
 * $Date$
 * $Revision$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.common.distribute;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.TestJob;

/**
 *
 */
public class JMSConnectionTestMQ extends JMSConnection {
    /** Singleton pattern is be used for this class. This is the one instance. */
    private static JMSConnectionTestMQ instance = null;


    private Map<String, TestJob> sentTestJobs;
    private List<ChannelIDListenerPair> channelIDListenerPairs;

    // notifyAll is called on this object whenever all send or reply calls have been completed
    private Set<Thread> concurrentTasksToComplete;
    static private int idcount = 0;

    private JMSConnectionTestMQ() {
        log.info("Creating instance of TestMQ");
    }

    /**
     * Establishes a connection to a specifik JMS-Broker defined in the settings
     * file.
     *
     * @return A JMSConnection
     * @throws UnknownID
     * @throws IOFailure
     *             when connection to JMS broker failed
     */
    public static JMSConnectionTestMQ getInstance() throws UnknownID, IOFailure {
        if (instance == null) {
            instance = new JMSConnectionTestMQ();
            instance.initConnection();
        }
        return instance;
    }

    protected void initConnection() {
        channelIDListenerPairs = Collections.synchronizedList(new ArrayList<ChannelIDListenerPair>());
        sentTestJobs = new HashMap<String, TestJob>();
        concurrentTasksToComplete = new HashSet<Thread>();
    }

    public static void clearTestQueues() {
        getInstance().initConnection();
    }

    public boolean waitForConcurrentTasksToFinish() {
        try {
            synchronized (concurrentTasksToComplete) {
                while (!concurrentTasksToComplete.isEmpty()) {
                    concurrentTasksToComplete.wait();
                }
            }
        }
        catch (InterruptedException e) {
            log.fatal(
                    "Interrupted whilst waiting for all send(..) and reply(..) to finish.");
            return false;
        }
        return true;
    }

    /**
     * Test version of the JMSConnection.sendMessage. Simulates communication
     * with a JMS broker.
     */
    protected void sendMessage(NetarkivetMessage nMsg, ChannelID to) {
        if (nMsg instanceof BatchMessage) {
            FileBatchJob job = ((BatchMessage) nMsg).getJob();
            if (job instanceof TestJob) {
                TestJob testJob = (TestJob) job;
                testJob.setSentToChannel(to);
                sentTestJobs.put(testJob.getTestId(), testJob);
            }
        }

        nMsg.updateId("ID" + idcount);
        idcount++;

        callAllListeners(nMsg, true, to);
    }

    /**
     * Returns a list of all MessageListeners listening to a particular channel
     *
     * @param channel
     * @return list of listeners
     */
    public List<MessageListener> getListeners(ChannelID channel) {
        ArrayList<MessageListener> listeners = new ArrayList<MessageListener>();
        synchronized(channelIDListenerPairs) {
            for (ChannelIDListenerPair pair : channelIDListenerPairs) {
                if (pair.channelID.equals(channel)) {
                    listeners.add(pair.messageListener);
                }
            }
        }
        return listeners;
    }

    public void reply(NetarkivetMessage nMsg) {
        callAllListeners(nMsg, false, nMsg.getTo());
    }

    private void callAllListeners(NetarkivetMessage nMsg, boolean is_send,
                                  ChannelID to) {
        if (channelIDListenerPairs == null) {
            return;
        }

        ChannelID sentTo = null;
        if (is_send) {
            sentTo = to;
        } else {
            sentTo = nMsg.getReplyTo();
        }
        checkChannelID(sentTo);

        // Create a list of threads that can call the listeners
        List<Thread> newTasksTocomplete = new ArrayList<Thread>();
        synchronized(channelIDListenerPairs) {
            for (ChannelIDListenerPair channelIDListenerPair :
                    channelIDListenerPairs) {
                if ((channelIDListenerPair).channelID.equals(sentTo)) {
                    //channelIDListenerPair.messageListener.onMessage(new WrappedMessage(nMsg));
                    log.debug("Delivered message '" + nMsg + "' to listener '"
                              + to + "'");
                    CallOnMessageThread t = new CallOnMessageThread(
                            channelIDListenerPair.messageListener,
                            new WrappedMessage(nMsg));
                    newTasksTocomplete.add(t);
                    synchronized (concurrentTasksToComplete) {
                        concurrentTasksToComplete.add(t);
                    }
                    if (!sentTo.isTopic()) {
                        break;
                    }
                }
            }
        }
        if (newTasksTocomplete.isEmpty()) {
            log.warn("Noone was listening for the message '" + nMsg
                     + "' on channel '" + sentTo + "'. Listeners are: "
                     + channelIDListenerPairs);
        }

        // Start all threads that call the listeners
        for (Thread task : newTasksTocomplete) {
            task.start();
        }
    }

    public void addListener(ChannelID mq, MessageListener ml)
            throws IOFailure {
        checkChannelID(mq);
        channelIDListenerPairs.add(new ChannelIDListenerPair(mq, ml));
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        //just retry sleeping
                    }
                }
            }
        }.start();
    }

    public void setListener(ChannelID mq, MessageListener ml)
            throws IOFailure {
        ArgumentNotValid.checkNotNull(mq, "mq");
        addListener(mq, ml);
    }

    public void removeListener(ChannelID mq, MessageListener ml) {
        channelIDListenerPairs.remove(new ChannelIDListenerPair(mq, ml));
    }

    public List<Message> removeAllMessages(ChannelID mq) {
        // We don't keep the messages, but fire them off at once. So
        // this method returns the empty list.
        log.info("Removing messages from '" + mq + "'");
        return new ArrayList<Message>();
    }

    public String getHost() {
        return "localhost";
    }

    public int getPort() {
        return 0;
    }

    /**
     * Check that the name of the channel is legal for JMS queues.
     */
    private void checkChannelID(ChannelID mq) {
        // TODO: Find a more accurate definition of what are legal queue names
        if (mq.getName().indexOf(" ") > -1 || mq.getName().indexOf(".") > -1) {
            throw new IOFailure("Illegal queue name '" + mq.getName() + "'");
        }
    }

    public boolean isSentToChannel(TestJob job, ChannelID channelID) {

        checkChannelID(channelID);
        TestJob foundJob = (TestJob) sentTestJobs.get(job.getTestId());
        return (foundJob != null && foundJob.getSentToChannel().equals(
                channelID));
    }

    protected void close() {
        channelIDListenerPairs.clear();
        sentTestJobs = null;
        instance = null;
    }

    public void cleanup() {
        instance = null;
        super.cleanup();
        channelIDListenerPairs.clear();
        sentTestJobs = null;
    }

    private class ChannelIDListenerPair {

        ChannelID channelID;
        MessageListener messageListener;

        ChannelIDListenerPair(ChannelID mq, MessageListener ml) {
            this.channelID = mq;
            this.messageListener = ml;
        }

        public String toString() {
            return "[" + channelID + " -> " + messageListener + "]";
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ChannelIDListenerPair)) {
                return false;
            }

            final ChannelIDListenerPair channelIDListenerPair
                    = (ChannelIDListenerPair) o;

            if (!channelID.equals(channelIDListenerPair.channelID)) {
                return false;
            }
            if (!messageListener.equals(
                    channelIDListenerPair.messageListener)) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            int result;
            result = channelID.hashCode();
            result = 29 * result + messageListener.hashCode();
            return result;
        }
    }

    private class CallOnMessageThread extends Thread {
        protected final Logger log = Logger.getLogger(getClass().getName());
        private MessageListener listener;
        private WrappedMessage wMsg;

        public CallOnMessageThread(MessageListener in_listener,
                                   WrappedMessage in_wMsg) {
            listener = in_listener;
            wMsg = in_wMsg;
        }

        public void run() {
            if (instance != null) {
                synchronized (listener) {
                    listener.onMessage(wMsg);
                }
                log.finer("Called onMessage for " + listener.toString());
            } else {
                log.warning(
                        "Found connection already closed when trying to send message to "
                        + listener.toString());
            }

            synchronized (concurrentTasksToComplete) {
                concurrentTasksToComplete.remove(Thread.currentThread());
                if (concurrentTasksToComplete.isEmpty()) {
                    concurrentTasksToComplete.notifyAll();
                }
            }
        }
    }

    private static class WrappedMessage implements ObjectMessage {

        private NetarkivetMessage netarkivetMessage;

        public WrappedMessage(NetarkivetMessage netarkivetMessage) {
            this.netarkivetMessage = netarkivetMessage;
        }

        public Serializable getObject() throws JMSException {
            return netarkivetMessage;
        }

        /**
         * Empty implementation in methods - require to be implemented by the
         * javax.jms.ObjectMessage intyerface
         */

        public void setObject(Serializable object) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }


        public void setJMSType(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public String getJMSMessageID() throws JMSException {
            netarkivetMessage.updateId("ID" + idcount);
            idcount++;
            return netarkivetMessage.getID();
        }

        public void setJMSMessageID(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public long getJMSTimestamp() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSTimestamp(long l) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSCorrelationIDAsBytes(byte[] bytes)
                throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSCorrelationID(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public String getJMSCorrelationID() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public Destination getJMSReplyTo() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSReplyTo(Destination destination) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public Destination getJMSDestination() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSDestination(Destination destination)
                throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public int getJMSDeliveryMode() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSDeliveryMode(int i) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public boolean getJMSRedelivered() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSRedelivered(boolean b) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public String getJMSType() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public long getJMSExpiration() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSExpiration(long l) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public int getJMSPriority() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSPriority(int i) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void clearProperties() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public boolean propertyExists(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public boolean getBooleanProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public byte getByteProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public short getShortProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public int getIntProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public long getLongProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public float getFloatProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public double getDoubleProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public String getStringProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public Object getObjectProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public Enumeration getPropertyNames() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setBooleanProperty(String s, boolean b)
                throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setByteProperty(String s, byte b) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setShortProperty(String s, short i) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setIntProperty(String s, int i) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setLongProperty(String s, long l) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setFloatProperty(String s, float v) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setDoubleProperty(String s, double v) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setStringProperty(String s, String s1) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setObjectProperty(String s, Object o) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void acknowledge() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void clearBody() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public String toString() {
            return "WrappedMessage: " + netarkivetMessage.toString();
        }

    } // end WrappedMessage

    /**
     * Wrap a NetarkivetMessage into an ObjectMessage
     *
     * @param nMsg a NetarkivetMessage
     * @return an ObjectMessage
     * @throws IOFailure if the conversion failed
     */
    public ObjectMessage getObjectMessage(NetarkivetMessage nMsg)
            throws IOFailure {
        return new WrappedMessage(nMsg);
    }

    /**
     * For testing purposes: Set the ID of a message
     *
     * @param msg The message to set the id on
     * @param id  the new id
     */
    public static void updateMsgID(NetarkivetMessage msg, String id) {
        msg.updateId(id);
    }

    public static void useJMSConnectionTestMQ() {
        Settings.set(CommonSettings.JMS_BROKER_CLASS,
                     "dk.netarkivet.common.distribute.JMSConnectionTestMQ");
        getInstance().close();
    }

    protected QueueConnectionFactory getQueueConnectionFactory()
            throws JMSException {
        return null;
    }

    protected TopicConnectionFactory getTopicConnectionFactory()
            throws JMSException {
        return null;
    }

    protected Queue getQueue(String queueName) throws JMSException {
        return null;
    }

    protected Topic getTopic(String topicName) throws JMSException {
        return null;
    }

    public void onException(JMSException arg0) {
        // TODO Auto-generated method stub
        
    }

}
