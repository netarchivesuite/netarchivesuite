/*$Id$
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
package dk.netarkivet.common.management;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * This class tests the class dk.netarkivet.common.management.SingleMBeanObject.
 */
public class SingleMBeanObjectTester extends TestCase {
    private ObjectName name;
    private MBeanServer platformMBeanServer;

    {
        try {
            name = new ObjectName(
                    "Test:location=NO,hostname="
                    + SystemUtils.getLocalHostName()
                    + ",httpport=1234,applicationname=TestApp1");
        } catch (MalformedObjectNameException e) {
            //never mind
        }
    }

    public SingleMBeanObjectTester(String s) {
        super(s);
    }

    public void setUp() {
        Settings.set(Settings.APPLICATIONNAME, "TestApp1");
        Settings.set(Settings.HTTP_PORT_NUMBER, "1234");
        Settings.set(Settings.ENVIRONMENT_THIS_LOCATION, "NO");
        platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    public void tearDown() throws Exception {
        Settings.reload();
        if (platformMBeanServer.isRegistered(name)) {
            platformMBeanServer.unregisterMBean(name);
        }
    }

    /**
     * Test constructor: Test nameProperties map is filled correctly, and
     * exceptions are thrown on wrong arguments.
     *
     * @throws Exception
     */
    public void testSingleMBeanObject() throws Exception {
        SingleMBeanObject test
                = new SingleMBeanObject("Test", new MyTestInterfaceObject(),
                                        MyTestInterface.class, ManagementFactory.getPlatformMBeanServer());
        assertEquals("Should have location in nameProperties",
                     "NO", test.getNameProperties().get("location"));
        assertEquals("Should have hostname in nameProperties",
                     SystemUtils.getLocalHostName(),
                     test.getNameProperties().get("hostname"));
        assertEquals("Should have httpport in nameProperties",
                     "1234", test.getNameProperties().get("httpport"));
        assertEquals("Should have applicationname in nameProperties",
                     "TestApp1",
                     test.getNameProperties().get("applicationname"));

        try {
            new SingleMBeanObject((String) null, new MyTestInterfaceObject(),
                                  MyTestInterface.class, ManagementFactory.getPlatformMBeanServer());
            fail("Should throw argument not valid on null argument");
        } catch (ArgumentNotValid e) {
            assertTrue("Should complain about the right parameter",
                       e.getMessage().contains("domain"));
        }

        try {
            new SingleMBeanObject("Test", null, MyTestInterface.class,
                                  ManagementFactory.getPlatformMBeanServer());
            fail("Should throw argument not valid on null argument");
        } catch (ArgumentNotValid e) {
            assertTrue("Should complain about the right parameter",
                       e.getMessage().contains("T o"));
        }

        try {
            new SingleMBeanObject("", new MyTestInterfaceObject(),
                                  MyTestInterface.class, ManagementFactory.getPlatformMBeanServer());
            fail("Should throw argument not valid on empty argument");
        } catch (ArgumentNotValid e) {
            assertTrue("Should complain about the right parameter",
                       e.getMessage().contains("domain"));
        }
    }

    /**
     * Tests that register works, and cannot be called twice.
     *
     * @throws Exception
     */
    public void testRegister() throws Exception {
        SingleMBeanObject test
                = new SingleMBeanObject("Test", new MyTestInterfaceObject(),
                                        MyTestInterface.class, ManagementFactory.getPlatformMBeanServer());
        assertFalse("Nothing should be registered under the name",
                    platformMBeanServer.isRegistered(name));
        test.register();
        assertTrue("Something should be registered under the name",
                   platformMBeanServer.isRegistered(name));
        Object attribute = platformMBeanServer.getAttribute(name, "TestString");
        assertEquals("Should get the right attribute", "Hello World",
                     attribute.toString());

        try {
            test.register();
            fail("Should not be able to register again");
        } catch (IllegalState e) {
            //expected
        }

    }

    /**
     * Tests that unregister works, and can be called twice.
     *
     * @throws Exception
     */
    public void testUnregister() throws Exception {
        SingleMBeanObject test
                = new SingleMBeanObject("Test", new MyTestInterfaceObject(),
                                        MyTestInterface.class, ManagementFactory.getPlatformMBeanServer());
        test.register();
        assertTrue("Something should be registered under the name",
                   platformMBeanServer.isRegistered(name));
        test.unregister();
        assertFalse("Nothing should be registered under the name",
                    platformMBeanServer.isRegistered(name));
        test.unregister();
        assertFalse("Nothing should be registered under the name",
                    platformMBeanServer.isRegistered(name));
    }


    public interface MyTestInterface {
        public String getTestString();
    }

    private class MyTestInterfaceObject implements MyTestInterface {
        public String getTestString() {
            return "Hello World";
        }
    }
}