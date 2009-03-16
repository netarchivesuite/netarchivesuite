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

import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * This class tests the class dk.netarkivet.common.management.SingleMBeanObject.
 */
public class SingleMBeanObjectTester extends TestCase {
    private ObjectName name;
    private MBeanServer platformMBeanServer;
    ReloadSettings rs = new ReloadSettings();

    {        
        try {
            name = new ObjectName(
                    "Test:" + Constants.PRIORITY_KEY_LOCATION + "=NO," 
                    + Constants.PRIORITY_KEY_MACHINE + "="
                    + SystemUtils.getLocalHostName() + ","
                    + Constants.PRIORITY_KEY_HTTP_PORT + "=1234,"
                    + Constants.PRIORITY_KEY_APPLICATIONNAME + "=TestApp1,"
                    + Constants.PRIORITY_KEY_APPLICATIONINSTANCEID + "=XX," 
                    + Constants.PRIORITY_KEY_PRIORITY + "=high,"
                    + Constants.PRIORITY_KEY_REPLICANAME + "=ReplicaOne");
        } catch (MalformedObjectNameException e) {
            System.out.println(e);
        }
    }
        
    public SingleMBeanObjectTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        Settings.set(CommonSettings.APPLICATION_NAME, "TestApp1");
        Settings.set(CommonSettings.APPLICATION_INSTANCE_ID, "XX");
        Settings.set(CommonSettings.HTTP_PORT_NUMBER, "1234");
        Settings.set(CommonSettings.THIS_PHYSICAL_LOCATION, "NO");
        Settings.set(CommonSettings.USE_REPLICA_ID, "ONE");
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY, "high");
        platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    public void tearDown() throws Exception {
        if (platformMBeanServer.isRegistered(name)) {
            platformMBeanServer.unregisterMBean(name);
        }
        rs.tearDown();
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
                                        MyTestInterface.class,
                                        ManagementFactory.getPlatformMBeanServer());
        assertEquals("Should have location in nameProperties",
                     "NO", test.getNameProperties().get(Constants.PRIORITY_KEY_LOCATION));
        assertEquals("Should have machine in nameProperties",
                     SystemUtils.getLocalHostName(),
                     test.getNameProperties().get(Constants.PRIORITY_KEY_MACHINE));
        assertEquals("Should have httpport in nameProperties",
                     "1234", test.getNameProperties().get(Constants.PRIORITY_KEY_HTTP_PORT));
        assertEquals("Should have applicationname in nameProperties",
                     "TestApp1",
                     test.getNameProperties().get(Constants.PRIORITY_KEY_APPLICATIONNAME));
        assertEquals("Should have applicationinstanceid in nameProperties",
                     "XX",
                      test.getNameProperties().get(Constants.PRIORITY_KEY_APPLICATIONINSTANCEID));

        try {
            new SingleMBeanObject((String) null, new MyTestInterfaceObject(),
                                  MyTestInterface.class, ManagementFactory.getPlatformMBeanServer());
            fail("Should throw argument not valid on null argument");
        } catch (ArgumentNotValid e) {
            assertTrue("Should complain about the right parameter 'domain'",
                       e.getMessage().contains("domain"));
        }

        try {
            new SingleMBeanObject("Test", null, MyTestInterface.class,
                                  ManagementFactory.getPlatformMBeanServer());
            fail("Should throw argument not valid on null argument");
        } catch (ArgumentNotValid e) {
            assertTrue("Should complain about the right parameter 'I object'",
                       e.getMessage().contains("I object"));
        }

        try {
            new SingleMBeanObject("", new MyTestInterfaceObject(),
                                  MyTestInterface.class,
                                  ManagementFactory.getPlatformMBeanServer());
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
                                        MyTestInterface.class,
                                        ManagementFactory.getPlatformMBeanServer());
        assertFalse("Nothing should be registered under the name '" + name + "'",
                    platformMBeanServer.isRegistered(name));
        test.register();
        assertTrue("Something should be registered under the name '" + name + "'",
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
                                        MyTestInterface.class,
                                        ManagementFactory.getPlatformMBeanServer());
        
        test.register();
        System.out.println("name of test:" + test.getName());
        System.out.println("name:" + name); 
        assertTrue("Something should be registered under the name " + name,
                   platformMBeanServer.isRegistered(name));
        test.unregister();
        assertFalse("Nothing should be registered under the name " + name,
                    platformMBeanServer.isRegistered(name));
        test.unregister();
        assertFalse("Nothing should be registered under the name" + name,
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