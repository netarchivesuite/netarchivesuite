/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.util.Enumeration;

import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * An implementation of ObjectMessage to be used in tests.  So far only allows
 * setObject and getObject();
 *
 */

public class TestObjectMessage implements ObjectMessage {
    Serializable obj;

    public TestObjectMessage(Serializable obj) {
        setObject(obj);
    }

    public void setObject(Serializable serializable) {
        obj = serializable;
    }

    public Serializable getObject() {
        return obj;
    }

    public String getJMSMessageID() throws JMSException {
        return ((NetarkivetMessage) obj).getID();
    }

    public void setJMSMessageID(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public long getJMSTimestamp() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setJMSTimestamp(long l) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setJMSCorrelationID(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getJMSCorrelationID() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public Destination getJMSReplyTo() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setJMSReplyTo(Destination destination) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public Destination getJMSDestination() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setJMSDestination(Destination destination) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public int getJMSDeliveryMode() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setJMSDeliveryMode(int i) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public boolean getJMSRedelivered() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setJMSRedelivered(boolean b) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getJMSType() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setJMSType(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public long getJMSExpiration() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setJMSExpiration(long l) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public int getJMSPriority() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setJMSPriority(int i) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void clearProperties() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public boolean propertyExists(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public boolean getBooleanProperty(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public byte getByteProperty(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public short getShortProperty(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public int getIntProperty(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public long getLongProperty(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public float getFloatProperty(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public double getDoubleProperty(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public String getStringProperty(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public Object getObjectProperty(String s) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public Enumeration getPropertyNames() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setBooleanProperty(String s, boolean b) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setByteProperty(String s, byte b) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setShortProperty(String s, short i) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setIntProperty(String s, int i) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setLongProperty(String s, long l) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setFloatProperty(String s, float v) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setDoubleProperty(String s, double v) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setStringProperty(String s, String s1) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void setObjectProperty(String s, Object o) throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void acknowledge() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    public void clearBody() throws JMSException {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }
}
