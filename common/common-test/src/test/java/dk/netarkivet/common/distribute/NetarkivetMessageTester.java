/*
 * #%L
 * Netarchivesuite - common - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.common.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;

/**
 * Tests the general class for distributed messages in Netarkivet.
 */
@SuppressWarnings({"serial"})
public class NetarkivetMessageTester {
    private static final ChannelID toQ = Channels.getAnyBa();
    private static final ChannelID replyToQ = Channels.getError();
    private static final ChannelID aTopic = Channels.getAllBa();
    // private static final String PREFIX = "<a prefix for tests>";
    private static final String ERR_MSG = "<an error message for tests>";

    /**
     * Verify that constructor returns normally when given non-null inputs with to and replyTo different.
     */
    @Test
    public void testConstructor() {
        NetarkivetMessage m = new TestMessage(toQ, replyToQ);
        assertNotNull("Should not be null", m);
    }

    /**
     * Verify that constructor fails when "to" equals "replyTo" (this is disallowed in the current design because it is
     * error-prone. Remove this unit test if the design changes on this point.
     */
    @Test
    public void testConstructorSameQueue() {
        try {
            new TestMessage(toQ, toQ);
            fail("NetarkivetMessage() should fail on equal queues.");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Verify that getInstance() fails when given null first input.
     */
    @Test
    public void testgetInstanceNull1() {
        try {
            new TestMessage(null, replyToQ);
            fail("NetarkivetMessage() should fail on null input");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Verify that getInstance() fails when given null second input.
     */
    @Test
    public void testgetInstanceNull2() {
        try {
            new TestMessage(toQ, null);
            fail("NetarkivetMessage() should fail on null input");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * Verify that getId() always returns the same value on a given message.
     */
    @Test
    public void testGetIDSame() {
        NetarkivetMessage m = new TestMessage(toQ, replyToQ);
        JMSConnectionMockupMQ.updateMsgID(m, "TESTMSGID5");
        String id = m.getID();
        // should not update ID - id already set
        JMSConnectionMockupMQ.updateMsgID(m, "TESTMSGID6");
        assertEquals("Method getId() returned different values on same message.", id, m.getID());
    }

    /**
     * Verify that getId() always returns the same value on a given message.
     */
    @Test
    public void testGetIDThorwsExceptopn() {
        NetarkivetMessage m = new TestMessage(toQ, replyToQ);
        try {
            m.getID();
            fail("Should throw exception when no id is present");
        } catch (PermissionDenied e) {
            // expected
        }
    }

    /**
     * Verify that getTo() returns the ChannelID given to constructor().
     */
    @Test
    public void testGetTo() {
        NetarkivetMessage m = new TestMessage(toQ, replyToQ);
        assertEquals("Expected to-queue to be '" + toQ.getName() + "' but was '" + m.getTo() + "'", toQ.getName(), m
                .getTo().getName());
    }

    /**
     * Verify that getTo() returns the ChannelID given to contructor().
     */
    @Test
    public void testReplyTo() {
        NetarkivetMessage m = new TestMessage(toQ, replyToQ);
        assertEquals("Expected to-queue to be '" + replyToQ.getName() + "' but was '" + m.getReplyTo().getName() + "'",
                replyToQ.getName(), m.getReplyTo().getName());
    }

    /**
     * Verify that getErrorMsg() returns the given error message, if one was given.
     */
    @Test
    public void testErrMsgText() {
        NetarkivetMessage m = new TestMessage(toQ, replyToQ);
        m.setNotOk(ERR_MSG);
        assertEquals("Wrong error message: " + m.getErrMsg(), ERR_MSG, m.getErrMsg());
    }

    /**
     * Verify that getErrorMsg() fails if status is OK.
     */
    @Test
    public void testErrMsgFail() {
        NetarkivetMessage m = new TestMessage(toQ, replyToQ);
        try {
            String msg = m.getErrMsg();
            fail("No error message set; getErrMsg() should not return " + msg);
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /**
     * Verify that a fresh NetarkivetMessage is OK.
     */
    @Test
    public void testFreshIsOK() {
        NetarkivetMessage m = new TestMessage(toQ, replyToQ);
        assertTrue("Fresh message wasn't ok.", m.isOk());
    }

    /**
     * Verify that a message is not OK if this was signalled.
     */
    @Test
    public void testIsNotOK() {
        NetarkivetMessage m = new TestMessage(toQ, replyToQ);
        m.setNotOk(ERR_MSG);
        assertFalse("Nok OK message returned true on isOk().", m.isOk());
    }

    /**
     * Verify that several calls to isNotOk() results in appended error messages.
     */
    @Test
    public void testMultipleErrMsgs() {
        NetarkivetMessage m = new TestMessage(toQ, replyToQ);
        m.setNotOk(ERR_MSG);
        m.setNotOk(ERR_MSG);
        assertEquals("Wrong error message: " + m.getErrMsg(), ERR_MSG + NetarkivetMessage.ERROR_DELIMITTER + ERR_MSG,
                m.getErrMsg());
    }

    /**
     * Test that a message replies to a queue, not a topic.
     */
    @Test
    public void testNoTopicReply() {
        try {
            new TestMessage(toQ, aTopic);
            fail("Reply channel must be a Queue but " + aTopic.toString() + " is a Topic.");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    /**
     * Verify that the class is Serializable.
     *
     * @throws IOException If I/O fails.
     * @throws ClassNotFoundException If class loading fails.
     */
    @Test
    public void testSerializability() throws IOException, ClassNotFoundException {
        // Take a message:
        NetarkivetMessage m1 = new TestMessage(toQ, replyToQ);
        // Work on it for a bit:
        m1.setNotOk(ERR_MSG);
        // Now serialize and deserialize the study object:
        JMSConnectionMockupMQ.updateMsgID(m1, "TESTMESSAGEID54");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(m1);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        NetarkivetMessage m2;
        m2 = (NetarkivetMessage) ois.readObject();
        // Finally, compare their visible states:
        assertEquals("After serialization the states differed:\n" + relevantState(m1) + "\n" + relevantState(m2),
                relevantState(m1), relevantState(m2));
    }

    /**
     * Returns the relevant state of a NetarkivetMessage instance, encoded as a String.
     *
     * @param m The message that should be investigated.
     * @return Its relevant state, as a String.
     */
    private String relevantState(NetarkivetMessage m) {
        return m.getID() + "(" + m.getTo().getName() + "," + m.getReplyTo().getName() + ")" + ":" + m.isOk()
                + (m.isOk() ? "" : m.getErrMsg());
    }

    /**
     * An extension of NetarkivetMessage that does not add functionality (except public constructor).
     */
    private static class TestMessage extends NetarkivetMessage {
        public TestMessage(ChannelID to, ChannelID replyTo) {
            super(to, replyTo);
        }
    }
}
