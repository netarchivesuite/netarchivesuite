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
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.QueueSender;
import javax.jms.TopicPublisher;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.util.Enumeration;
import java.util.Map;
import java.util.logging.Logger;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.testutils.ReflectUtils;

/**
 * Tests JMSConnection, the class that handles all JMS operations for
 * Netarkivet.
 */
public class JMSConnectionTester extends TestCase {

    private SecurityManager originalSecurityManager;

    /**
     * setUp() method for this testsuite.
     */
    public void setUp() {
        originalSecurityManager = System.getSecurityManager();
        SecurityManager manager = new SecurityManager() {
            public void checkPermission(Permission perm) {
                if(perm.getName().equals("exitVM")) {
                    throw new SecurityException("Thou shalt not exit in a unit test");
                }
            }
        };
        System.setSecurityManager(manager);
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
    }

    /**
     * tearDown() method for this testsuite.
     */
    public void tearDown() {
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnectionFactory.getInstance().close();
        Settings.reload();
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * Test that asking for a fake JMSConnection actually gets you just that.
     */
    public void testFakeJMSConnection() {
        JMSConnectionTestMQ.useJMSConnectionTestMQ();

        assertTrue("Fake JMS connection must be of type JMSConnectionTestMQ",
                JMSConnectionFactory.getInstance() instanceof JMSConnectionTestMQ);
    }

    /**
     * Tests for null parameters.
     */
    public void testUnpackParameterIsNull() {
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionTestMQ");
        try {
            JMSConnection.unpack(null);
            fail("Should throw an ArgumentNotValidException when given a " +
                    "null parameter");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Tests for wrong parameters.
     */
    public void testUnpackParameterIsAnObjectMessage() {
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionTestMQ");
        try {
            JMSConnection.unpack(new DummyMapMessage());
            fail("Should throw an ArgumentNotValid exception when given a " +
                    "wrong message type");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Tests for correct error handling if ObjectMessage has the wrong
     * payload.
     */
    public void testUnpackInvalidPayload() {
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionTestMQ");
        try {
            JMSConnection.unpack(new TestObjectMessage(
                    new DummySerializableClass()));
            fail("Should throw an ArgumentNotValidException when given a " +
                    "wrong payload");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Tests if correct payload is unwrapped.
     */
    public void testUnpackOfCorrectPayload() {
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionTestMQ");
        String testID = "42";
        TestMessage testMessage = new TestMessage(Channels.getTheArcrepos(), Channels.getTheBamon(), testID);
        JMSConnectionTestMQ.updateMsgID(testMessage, "ID89");
        TestMessage msg = (TestMessage)JMSConnection.unpack(
                new TestObjectMessage(testMessage));
        assertEquals("Unpacking should have given correct ID",
                msg.testID, testID);
    }

    /**
     * Test resend() methods arguments.
     */
    public void testResendArgumentsNotNull() {
        /**
         * Check it is the correct resend method which is invoked and not
         * an overloaded version in fx. JMSConnectionTestMQ. Resend should be
         * declared final.
         */
        Class parameterTypes[] = {NetarkivetMessage.class, ChannelID.class};
        assertMethodIsFinal(JMSConnection.class, "resend", parameterTypes);

        /**
         * Set up JMSConnection and dummy receive servers.
         */
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionTestMQ");

        /**
         * Test if ArgumentNotValid is thrown if null
         * is given as first parameter.
         */
        try {
            JMSConnectionFactory.getInstance().resend(null, Channels.getError());
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        /**
         * Test if ArgumentNotValid is thrown if null
         * is given as second parameter
         */
        try {
            JMSConnectionFactory.getInstance().resend(new TestMessage(
                    Channels.getError(), Channels.getError(), ""), null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Test the resend method. Message shouldn't be sent according to the
     * address specified in the "to" field of the message. It should be sent to
     * the address given in the "to" parameter of the resend() method.
     */
    public void testResendCorrectSendBehaviour() {
        /**
         * Check it is the correct resend method which is invoked and not
         * an overloaded version in fx. JMSConnectionTestMQ. Resend should be
         * declared final.
         */
        Class parameterTypes[] = {NetarkivetMessage.class, ChannelID.class};
        assertMethodIsFinal(JMSConnection.class, "resend", parameterTypes);

        /**
         * Set up JMSConnection and dummy receive servers.
         */
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionTestMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();

        // Create dummy server and listen on the Error queue.
        DummyServer serverErrorQueue = new DummyServer();
        serverErrorQueue.reset();
        con.setListener(Channels.getError(), serverErrorQueue);

        // Create dummy server and listen on the TheArcrepos queue
        DummyServer serverTheArcreposQueue = new DummyServer();
        serverTheArcreposQueue.reset();
        con.setListener(Channels.getTheArcrepos(), serverTheArcreposQueue);

        // Create dummy server and listen on the TheArcrepos queue
        DummyServer serverTheBamonQueue = new DummyServer();
        serverTheBamonQueue.reset();
        con.setListener(Channels.getTheBamon(), serverTheBamonQueue);
        
        /**
         * The actual test.
         */
        assertEquals("Server should not have received any messages", 0, serverErrorQueue.msgReceived);
        assertEquals("Server should not have received any messages", 0, serverTheBamonQueue.msgReceived);
        assertEquals("Server should not have received any messages", 0, serverTheArcreposQueue.msgReceived);

        NetarkivetMessage msg = new TestMessage(Channels.getTheArcrepos(), Channels.getTheBamon(), "testMSG");
        con.resend(msg, Channels.getError());

        ((JMSConnectionTestMQ) con).waitForConcurrentTasksToFinish();

        assertEquals("Server should not have received any messages", 0, serverTheArcreposQueue.msgReceived);
        assertEquals("Server should not have received any messages", 0, serverTheBamonQueue.msgReceived);
        assertEquals("Server should have received 1 message", 1, serverErrorQueue.msgReceived);

        /**
         * Clean up.
         */
        Settings.reload();
    }

    /** Tests that initconnection actually starts a topic connection and a
     * queue connection.
     */
    public void testInitConnection() throws JMSException, NoSuchFieldException,
            IllegalAccessException {
        /*
         * Set up JMSConnection and dummy receive servers.
         */
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();
        Field QConnField = ReflectUtils.getPrivateField(JMSConnection.class,
                "myQConn");
        Field TConnField = ReflectUtils.getPrivateField(JMSConnection.class,
                "myTConn");
        con.initConnection();
        JMSConnectionMockupMQ.TestConnection qconn
                = (JMSConnectionMockupMQ.TestConnection)QConnField.get(con);
        assertTrue("Should have started the queue connection",
                qconn.isStarted);
        JMSConnectionMockupMQ.TestConnection tconn
                = (JMSConnectionMockupMQ.TestConnection)TConnField.get(con);
        assertTrue("Should have started the topic connection",
                tconn.isStarted);
    }

    public void testSendToQueue() throws JMSException, NoSuchFieldException,
            IllegalAccessException {
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.initConnection();

        Field senders = ReflectUtils.getPrivateField(JMSConnection.class,
                "senders");

        Map<String, QueueSender> sendersMap =
                (Map<String, QueueSender>) senders.get(con);

        ChannelID sendChannel = Channels.getTheArcrepos();
        ChannelID replyChannel = Channels.getTheBamon();
        NetarkivetMessage msg = new TestMessage(sendChannel,
                replyChannel, "testMSG");

        con.send(msg);

        String sendName = sendChannel.getName();

        JMSConnectionMockupMQ.TestQueueSender queueSender =
                (JMSConnectionMockupMQ.TestQueueSender)sendersMap.get(sendName);

        assertNotNull("Should have created a sender for " + sendName,
                queueSender);
        ObjectMessage sentSerialMsg = (ObjectMessage)queueSender.messagesSent.get(0);
        assertNotNull("Should have a sent message", sentSerialMsg);
        assertEquals("Received message should be the same as was sent",
                sentSerialMsg.getObject(), msg);
        assertNotNull("Message should now have an id", msg.getID());
    }

    public void testSendToTopic() throws JMSException, NoSuchFieldException,
            IllegalAccessException {
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.initConnection();

        Field senders = ReflectUtils.getPrivateField(JMSConnection.class,
                "publishers");

        Map<String, TopicPublisher> publishersMap =
                (Map<String, TopicPublisher>) senders.get(con);

        ChannelID sendChannel = Channels.getAllBa();
        ChannelID replyChannel = Channels.getTheBamon();
        NetarkivetMessage msg = new TestMessage(sendChannel,
                replyChannel, "testMSG");

        con.send(msg);

        String sendName = sendChannel.getName();

        JMSConnectionMockupMQ.TestTopicPublisher topicPublisher =
                (JMSConnectionMockupMQ.TestTopicPublisher)publishersMap.get(sendName);

        assertNotNull("Should have created a publisher for " + sendName,
                topicPublisher);
        ObjectMessage sentSerialMsg = (ObjectMessage)topicPublisher.messagesSent.get(0);
        assertNotNull("Should have a published message", sentSerialMsg);
        assertEquals("Received message should be the same as was published",
                sentSerialMsg.getObject(), msg);
        assertNotNull("Message should now have an id", msg.getID());
    }

    public void testSetListener() throws JMSException, NoSuchFieldException,
            IllegalAccessException {
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.initConnection();

        MessageListener listener1 = new MessageListener() {
            public void onMessage(Message message) {
                //TODO: implement method
                throw new NotImplementedException("Not implemented");
            }
            public String toString() {
                return "listener1";
            }
        };
        MessageListener listener2 = new MessageListener() {
            public void onMessage(Message message) {
                //TODO: implement method
                throw new NotImplementedException("Not implemented");
            }
            public String toString() {
                return "listener2";
            }
        };
        ChannelID anyBa = Channels.getAnyBa();
        con.setListener(anyBa, listener1);
        Field consumersField = ReflectUtils.getPrivateField(JMSConnection.class,
                "consumers");
        Map<String, MessageConsumer> consumerMap =
                (Map<String, MessageConsumer>)consumersField.get(con);
        MessageConsumer messageConsumer
                = consumerMap.get(anyBa.getName()
                + "##listener1");
        assertEquals("Should have added listener for queue",
                listener1, messageConsumer.getMessageListener());

        ChannelID allBa = Channels.getAllBa();
        con.setListener(allBa, listener2);
        messageConsumer
                = consumerMap.get(allBa.getName()
                + "##listener2");
        assertEquals("Should have added listener for topic",
                listener2, messageConsumer.getMessageListener());

        con.removeListener(anyBa, listener1);
        assertFalse("Should have lost the listener",
                consumerMap.containsKey(anyBa.getName() + "##listener1"));
        messageConsumer
                = consumerMap.get(allBa.getName()
                + "##listener2");
        assertEquals("Should still have listener for topic",
                listener2, messageConsumer.getMessageListener());
        con.setListener(anyBa, listener1);
        MessageConsumer messageConsumer1
                = consumerMap.get(anyBa.getName()
                + "##listener1");
        assertEquals("Should have two listeners now",
                2, consumerMap.size());
    }
    
    /**
     * Unittest for the removeAllMessages().
     * Uncommented until the method is implemented
     * correctly.
     */
//    public void testRemoveAllMessages() throws JMSException,
//            NoSuchFieldException, IllegalAccessException {
//        if (!TestUtils.runningAs("LC")) { // Need to get the right test queue
//            return;
//        }
//        JMSConnection con = JMSConnectionFactory.getInstance();
//        con.initConnection();
//
//        Field senders = ReflectUtils.getPrivateField(JMSConnection.class,
//                "senders");
//
//        Map<String, QueueSender> sendersMap =
//                (Map<String, QueueSender>) senders.get(con);
//
//        ChannelID sendChannel = Channels.getTheArcrepos();
//        ChannelID replyChannel = Channels.getTheBamon();
//        NetarkivetMessage msg = new TestMessage(sendChannel,
//                replyChannel, "testMsg1");
//
//        con.send(msg);
//
//        msg = new TestMessage(replyChannel, sendChannel, "testMsg2");
//
//        con.send(msg);
//
//        msg = new TestMessage(sendChannel, replyChannel, "testMsg3");
//
//        con.send(msg);
//
//        String sendName = sendChannel.getName();
//        String replyName = replyChannel.getName();
//
//        JMSConnectionMockupMQ.TestQueueSender queueSender =
//                (JMSConnectionMockupMQ.TestQueueSender)sendersMap.get(sendName);
//        JMSConnectionMockupMQ.TestQueueSender queueReplier =
//                (JMSConnectionMockupMQ.TestQueueSender)sendersMap.get(replyName);
//
//        assertNotNull("Should have created a sender for " + sendName,
//                queueSender);
//        assertEquals("Should have two messages for " + sendName,
//                2, queueSender.messagesSent.size());
//        assertNotNull("Should have created a sender for " + replyName,
//                queueReplier);
//        assertEquals("Should have one message for " + replyName,
//                1, queueReplier.messagesSent.size());
//        assertEquals("Should have not other senders",
//                2, sendersMap.size());
//
//        con.removeAllMessages(sendChannel);
//
//        assertEquals("Should have no messages for " + sendName,
//                1, queueSender.messagesSent.size());
//        assertEquals("Should have one message for " + replyName,
//                1, queueReplier.messagesSent.size());
//        assertEquals("Should have not other senders",
//                2, sendersMap.size());
//
//    }

    public void testGetConsumerKey() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        Method getConsumerKey = ReflectUtils.getPrivateMethod(JMSConnection.class,
                "getConsumerKey", ChannelID.class, MessageListener.class);
        MessageListener listener = new MessageListener() {
            public void onMessage(Message message) {
                //TODO: implement method
                throw new NotImplementedException("Not implemented");
            }
            public String toString() {
                return "ourListener";
            }
        };
        assertEquals("Should have expected key for topic",
                Channels.getAllBa().getName() + "##" + listener.toString(),
                getConsumerKey.invoke(null, Channels.getAllBa(), listener));
        assertEquals("Should have expected key for queue",
                Channels.getTheBamon().getName() + "##" + listener.toString(),
                getConsumerKey.invoke(null, Channels.getTheBamon(), listener));
    }

    public void testReply() throws JMSException, NoSuchFieldException,
            IllegalAccessException {
        Settings.set(Settings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.initConnection();

        NetarkivetMessage msg = new TestMessage(Channels.getTheArcrepos(),
                Channels.getTheBamon(), "testMSG");

        Field senders = ReflectUtils.getPrivateField(JMSConnection.class,
                "senders");

        Map<String, QueueSender> sendersMap =
                (Map<String, QueueSender>) senders.get(con);

        con.send(msg);
        String sendName = Channels.getTheArcrepos().getName();
        JMSConnectionMockupMQ.TestQueueSender queueSender =
                (JMSConnectionMockupMQ.TestQueueSender)sendersMap.get(sendName);
        ObjectMessage sentSerialMsg = ((ObjectMessage) queueSender.messagesSent.get(0));
        NetarkivetMessage sentMessage = (NetarkivetMessage)sentSerialMsg.getObject();
        sentMessage.setNotOk("Test error");
        con.reply(sentMessage);

        String replyName = Channels.getTheBamon().getName();
        queueSender =
                (JMSConnectionMockupMQ.TestQueueSender)sendersMap.get(replyName);
        assertNotNull("Should have a sender for " + replyName, queueSender);

        ObjectMessage receivedSerialMsg
                = (ObjectMessage)queueSender.messagesSent.get(0);
        NetarkivetMessage received
                = (NetarkivetMessage)receivedSerialMsg.getObject();
        assertEquals("Should have sent a message on " + queueSender,
                received, msg);
        assertFalse("Message should now be notOk",
                received.isOk());

        msg = new TestMessage(Channels.getTheArcrepos(),
                Channels.getTheBamon(), "testMSG");

        try {
            con.reply(msg);
            fail("Shouldn't be able to reply to unsent message.");
        } catch (PermissionDenied e) {
            // expected - msg has not been sent.
        }

        try {
            con.reply(null);
            fail("Should not allow null messages");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    private void assertMethodIsFinal(Class aClass, String name, Class[] parameterTypes) {
        try {
            Method m = aClass.getMethod(name, parameterTypes);
            assertTrue(name+"() in JMSConnection is not declared final!", Modifier.isFinal(m.getModifiers()));
        } catch (Exception e) {
            fail("Method " + name + " in JMSConnection doesn't exist!");
        }
    }

    public static class DummyServer implements MessageListener {

        private final Logger log = Logger.getLogger(getClass().getName());

        public int msgOK = 0;
        public int msgNotOK = 0;
        public int msgReceived = 0;

        /* (non-Javadoc)
         * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
         */
        public void onMessage(Message msg) {
            log.fine("DummyServer received message: "+msg);
            NetarkivetMessage netMsg = JMSConnection.unpack(msg);
            msgReceived = (netMsg.isOk() ? ++msgOK : ++msgNotOK);
        }

        public void reset() {
            msgOK = 0;
            msgNotOK = 0;
            msgReceived = 0;
        }
    }

    private static class DummyMapMessage implements MapMessage {

        public boolean getBoolean(String arg0) throws JMSException {
            return false;
        }

        public byte getByte(String arg0) throws JMSException {
            return 0;
        }

        public short getShort(String arg0) throws JMSException {
            return 0;
        }

        public char getChar(String arg0) throws JMSException {
            return 0;
        }

        public int getInt(String arg0) throws JMSException {
            return 0;
        }

        public long getLong(String arg0) throws JMSException {
            return 0;
        }

        public float getFloat(String arg0) throws JMSException {
            return 0;
        }

        public double getDouble(String arg0) throws JMSException {
            return 0;
        }

        public String getString(String arg0) throws JMSException {
            return null;
        }

        public byte[] getBytes(String arg0) throws JMSException {
            return null;
        }

        public Object getObject(String arg0) throws JMSException {
            return null;
        }

        public Enumeration getMapNames() throws JMSException {
            return null;
        }

        public void setBoolean(String arg0, boolean arg1) throws JMSException {
        }

        public void setByte(String arg0, byte arg1) throws JMSException {
        }

        public void setShort(String arg0, short arg1) throws JMSException {
        }

        public void setChar(String arg0, char arg1) throws JMSException {
        }

        public void setInt(String arg0, int arg1) throws JMSException {
        }

        public void setLong(String arg0, long arg1) throws JMSException {
        }

        public void setFloat(String arg0, float arg1) throws JMSException {
        }

        public void setDouble(String arg0, double arg1) throws JMSException {
        }

        public void setString(String arg0, String arg1) throws JMSException {
        }

        public void setBytes(String arg0, byte[] arg1) throws JMSException {
        }

        public void setBytes(String arg0, byte[] arg1, int arg2, int arg3) throws JMSException {
        }

        public void setObject(String arg0, Object arg1) throws JMSException {
        }

        public boolean itemExists(String arg0) throws JMSException {
            return false;
        }

        public String getJMSMessageID() throws JMSException {
            return null;
        }

        public void setJMSMessageID(String arg0) throws JMSException {
        }

        public long getJMSTimestamp() throws JMSException {
            return 0;
        }

        public void setJMSTimestamp(long arg0) throws JMSException {
        }

        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            return null;
        }

        public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
        }

        public void setJMSCorrelationID(String arg0) throws JMSException {
        }

        public String getJMSCorrelationID() throws JMSException {
            return null;
        }

        public Destination getJMSReplyTo() throws JMSException {
            return null;
        }

        public void setJMSReplyTo(Destination arg0) throws JMSException {
        }

        public Destination getJMSDestination() throws JMSException {
            return null;
        }

        public void setJMSDestination(Destination arg0) throws JMSException {
        }

        public int getJMSDeliveryMode() throws JMSException {
            return 0;
        }

        public void setJMSDeliveryMode(int arg0) throws JMSException {
        }

        public boolean getJMSRedelivered() throws JMSException {
            return false;
        }

        public void setJMSRedelivered(boolean arg0) throws JMSException {
        }

        public String getJMSType() throws JMSException {
            return null;
        }

        public void setJMSType(String arg0) throws JMSException {
        }

        public long getJMSExpiration() throws JMSException {
            return 0;
        }

        public void setJMSExpiration(long arg0) throws JMSException {
        }

        public int getJMSPriority() throws JMSException {
            return 0;
        }

        public void setJMSPriority(int arg0) throws JMSException {
        }

        public void clearProperties() throws JMSException {
        }

        public boolean propertyExists(String arg0) throws JMSException {
            return false;
        }

        public boolean getBooleanProperty(String arg0) throws JMSException {
            return false;
        }

        public byte getByteProperty(String arg0) throws JMSException {
            return 0;
        }

        public short getShortProperty(String arg0) throws JMSException {
            return 0;
        }

        public int getIntProperty(String arg0) throws JMSException {
            return 0;
        }

        public long getLongProperty(String arg0) throws JMSException {
            return 0;
        }

        public float getFloatProperty(String arg0) throws JMSException {
            return 0;
        }

        public double getDoubleProperty(String arg0) throws JMSException {
            return 0;
        }

        public String getStringProperty(String arg0) throws JMSException {
            return null;
        }

        public Object getObjectProperty(String arg0) throws JMSException {
            return null;
        }

        public Enumeration getPropertyNames() throws JMSException {
            return null;
        }

        public void setBooleanProperty(String arg0, boolean arg1) throws JMSException {
        }

        public void setByteProperty(String arg0, byte arg1) throws JMSException {
        }

        public void setShortProperty(String arg0, short arg1) throws JMSException {
        }

        public void setIntProperty(String arg0, int arg1) throws JMSException {
        }

        public void setLongProperty(String arg0, long arg1) throws JMSException {
        }

        public void setFloatProperty(String arg0, float arg1) throws JMSException {
        }

        public void setDoubleProperty(String arg0, double arg1) throws JMSException {
        }

        public void setStringProperty(String arg0, String arg1) throws JMSException {
        }

        public void setObjectProperty(String arg0, Object arg1) throws JMSException {
        }

        public void acknowledge() throws JMSException {
        }

        public void clearBody() throws JMSException {
        }

    }

    private static class DummySerializableClass implements Serializable {

    }

    private static class TestMessage extends NetarkivetMessage {
        private String testID;

        public TestMessage(ChannelID to, ChannelID replyTo, String testID) {
            super(to, replyTo, "NetarkivetMessageTester.TestMessage");
            this.testID = testID;
        }

        public String getTestID() {
            return testID;
        }
    }
}