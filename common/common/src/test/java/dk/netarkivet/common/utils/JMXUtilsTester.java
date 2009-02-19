/* $Id$
 * $Revision$
 * $Date$
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
package dk.netarkivet.common.utils;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.net.MalformedURLException;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

import com.sun.jndi.rmi.registry.RegistryContextFactory;

/**
 * 
 * Unit tests for the JMXUtils class.
 *
 */
public class JMXUtilsTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();


    public JMXUtilsTester(String s) {
        super(s);
    }

    public void setUp() {

        // Set JMX_timeout to 1 second.
        // When run as part of the unittestersuite, it has no
        // effect.
        
        Settings.set(CommonSettings.JMX_TIMEOUT, "1");
        // Note: without the next statement, the former statement has no effect!
        int JMXMaxtries = JMXUtils.MAX_TRIES;
        
        rs.setUp();
    }

    public void tearDown() {
        rs.tearDown();
    }

    public void testGetJMXConnector() throws IOException {
        try {
            JMXUtils.getJMXConnector("Ahost.invalid", 1234, "login", "password");
            fail("Should throw IOFailure when failing to connect to JMX host");
        } catch (IOFailure e) {
            //expected
        }
/* This code block launches a local jmx service, but it's commented out because it doesn't
test any project code
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
        url = new JMXServiceURL("rmi", "localhost", 1234, "");
                 Map<String,String[]> credentials = new HashMap<String,String[]>(1);
        credentials.put("jmx.remote.credentials",
                new String[]{"user", "pass"});
        JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url,credentials,mbs);
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        JMXConnector conn1 = JMXConnectorFactory.connect(addr, credentials);
        //JMXConnector conn = JMXUtils.getJMXConnector("localhost", 1234, "user", "pass");
        conn1.close();
        cs.stop();*/
    }



    public void testGetAttribute() throws Exception {
        TestMBeanServerConnection connection = new TestMBeanServerConnection(0);
        Object ret = connection.getAttribute(JMXUtils.getBeanName("a:aBean=1"), "anAttribute");
        assertEquals("Should have composed bean/attribute name",
                     "a:aBean=1:anAttribute", ret);

        Object o = JMXUtils.getAttribute("a:aBean=1", "anAttribute", connection);
        assertEquals("Should have composed bean/attribute name",
                     "a:aBean=1:anAttribute", o);

        Date then = new Date();
        connection.failCount = JMXUtils.MAX_TRIES + 1;
        try {
            o = JMXUtils.getAttribute("a:aBean=1", "anAttribute", connection);
            fail("Should time out");
        } catch (IOFailure e) {
            //Expected
        }
        Date now = new Date();
        long time = now.getTime()-then.getTime();
        assertTrue("Should take at least 2^" + JMXUtils.MAX_TRIES
                   + " milliseconds, but was " + time + ", should be "
                   + Math.pow(2, JMXUtils.MAX_TRIES),
                   time >= Math.pow(2, JMXUtils.MAX_TRIES));
        assertEquals("Should have been called " + JMXUtils.MAX_TRIES + " times.",
                    1,
                    connection.failCount);
    }

    class TestMBeanServerConnection implements MBeanServerConnection {
        int failCount;

        /** Create a test MBeanServerConnection that fails a number of times.
         *
         * @param failCount Number of times the getAttribute/executeCommand
         * methods should be called before they succeed.
         */
        TestMBeanServerConnection(int failCount) {
            this.failCount = failCount;
        }

        public Object getAttribute(ObjectName beanName, String attribute)
                throws InstanceNotFoundException {
            if (failCount-- > 0) {
                throw new InstanceNotFoundException();
            }
            return beanName + ":" + attribute;
        }

        public ObjectInstance createMBean(String className, ObjectName name)
                throws
                ReflectionException,
                InstanceAlreadyExistsException,
                MBeanException,
                NotCompliantMBeanException,
                IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ObjectInstance createMBean(String className, ObjectName name,
                                          ObjectName loaderName) throws
                                                                 ReflectionException,
                                                                 InstanceAlreadyExistsException,
                                                                 MBeanException,
                                                                 NotCompliantMBeanException,
                                                                 InstanceNotFoundException,
                                                                 IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ObjectInstance createMBean(String className, ObjectName name,
                                          Object params[], String signature[])
        throws ReflectionException, InstanceAlreadyExistsException,
               MBeanException,
               NotCompliantMBeanException, IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ObjectInstance createMBean(String className, ObjectName name,
                                          ObjectName loaderName, Object params[],
                                          String signature[]) throws
                                                              ReflectionException,
                                                              InstanceAlreadyExistsException,
                                                              MBeanException,
                                                              NotCompliantMBeanException,
                                                              InstanceNotFoundException,
                                                              IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void unregisterMBean(ObjectName name) throws
                                                     InstanceNotFoundException,
                                                     MBeanRegistrationException,
                                                     IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ObjectInstance getObjectInstance(ObjectName name)
        throws InstanceNotFoundException, IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Set queryMBeans(ObjectName name, QueryExp query) throws IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Set queryNames(ObjectName name, QueryExp query) throws IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public boolean isRegistered(ObjectName name) throws IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Integer getMBeanCount() throws IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public AttributeList getAttributes(ObjectName name, String[] attributes)
        throws InstanceNotFoundException, ReflectionException, IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void setAttribute(ObjectName name, Attribute attribute) throws
                                                                       InstanceNotFoundException,
                                                                       AttributeNotFoundException,
                                                                       InvalidAttributeValueException,
                                                                       MBeanException,
                                                                       ReflectionException,
                                                                       IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public AttributeList setAttributes(ObjectName name,
                                           AttributeList attributes)
        throws InstanceNotFoundException, ReflectionException, IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Object invoke(ObjectName name, String operationName, Object params[],
                             String signature[]) throws InstanceNotFoundException,
                                                        MBeanException,
                                                        ReflectionException,
                                                        IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String getDefaultDomain() throws IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String[] getDomains() throws IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void addNotificationListener(ObjectName name,
                                            NotificationListener listener,
                                            NotificationFilter filter,
                                            Object handback)
        throws InstanceNotFoundException, IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void addNotificationListener(ObjectName name, ObjectName listener,
                                            NotificationFilter filter,
                                            Object handback)
        throws InstanceNotFoundException, IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void removeNotificationListener(ObjectName name, ObjectName listener)
        throws InstanceNotFoundException, ListenerNotFoundException,
               IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void removeNotificationListener(ObjectName name, ObjectName listener,
                                               NotificationFilter filter,
                                               Object handback) throws
                                                                InstanceNotFoundException,
                                                                ListenerNotFoundException,
                                                                IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void removeNotificationListener(ObjectName name,
                                               NotificationListener listener)
                throws
                InstanceNotFoundException,
                ListenerNotFoundException,
                IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void removeNotificationListener(ObjectName name,
                                               NotificationListener listener,
                                               NotificationFilter filter,
                                               Object handback) throws
                                                                InstanceNotFoundException,
                                                                ListenerNotFoundException,
                                                                IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public MBeanInfo getMBeanInfo(ObjectName name) throws
                                                       InstanceNotFoundException,
                                                       IntrospectionException,
                                                       ReflectionException,
                                                       IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public boolean isInstanceOf(ObjectName name, String className)
        throws InstanceNotFoundException, IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }
    }

}
