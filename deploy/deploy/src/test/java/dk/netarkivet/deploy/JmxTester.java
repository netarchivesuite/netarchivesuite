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
package dk.netarkivet.deploy;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.monitor.logging.CachingLogHandler;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class JmxTester extends TestCase{
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);
    private MBeanServer mbeanserver;
    
    public JmxTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
        mbeanserver = ManagementFactory.getPlatformMBeanServer();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }
    
    public void testJmx() throws Exception {
        int before = mbeanserver.getMBeanCount();
        //assertTrue("Before should be 0, but is " + before, before==0);
        Set<ObjectName> jmxBeans = mbeanserver.queryNames(null, null);
        for (ObjectName anObjectName: jmxBeans) {
            System.out.println("Mbean is instance of " +  anObjectName.getClass().getName());
            System.out.println("ObjectName: " + anObjectName.getCanonicalName());
            Hashtable table = anObjectName.getKeyPropertyList();
            for (Object key: table.keySet()) {
                System.out.println("key: " + key);
            }
        }
        
        CachingLogHandler loghandler = new CachingLogHandler();
        //Set<Object> set = mbeanserver.queryMBeans(new ObjectName("dk.netarkivet.common.logging:*"), null);
        Set theSet = mbeanserver.queryMBeans(new ObjectName("dk.netarkivet.common.logging:*"), null);
        assertTrue(theSet.size() > 0);
        for (Object o: theSet) {
            System.out.println(o.getClass().getCanonicalName());
            ObjectInstance o1 = (ObjectInstance) o;
            System.out.println("objectname: " + o1.getObjectName().getCanonicalName());
            System.out.println("classname: " + o1.getClassName());
            
        }
        if (true) {
            return;
        }
        jmxBeans = mbeanserver.queryNames(null, new ObjectName("dk.netarkivet.logging:"));
        for (Object anObject: jmxBeans) {
            System.out.println("Mbean is instance of " +  anObject.getClass().getName());
            if (anObject instanceof ObjectName) {               
                ObjectName anObjectName = (ObjectName) anObject;
                System.out.println("ObjectName: " + anObjectName.getCanonicalName());
                System.out.println("class: " 
                        + anObjectName.getClass().getCanonicalName());
                Hashtable table = anObjectName.getKeyPropertyList();
                for (Object key: table.keySet()) {
                    System.out.println("key: " + key);
                }                
                //mbeanserver.getAttribute(anObjectName, "RecordString");
                MBeanInfo info = mbeanserver.getMBeanInfo(anObjectName);
                MBeanAttributeInfo[] array = info.getAttributes();
                for(MBeanAttributeInfo element: array){
                    System.out.println("attribute: " + element.getName());
                    System.out.println("attribute-type: " + element.getType());
                    System.out.println("attribute-value: " + 
                            mbeanserver.getAttribute(anObjectName, element.getName()));   
                }
            }
            

            
        }
        
        
    }
    
    /**
     * Name a JMX object name as expected by our CachingLogRecordMBean.
     *
     * @param index The index attribute - may be null, for all
     * @return An ObjectName.
     * @throws MalformedObjectNameException
     */
    private static ObjectName getObjectName(int index) throws
                                                       MalformedObjectNameException {
        return new ObjectName("dk.netarkivet.common.logging:location="
                              + Settings.get(Settings.ENVIRONMENT_THIS_LOCATION)
                              + ",hostname=" + SystemUtils.getLocalHostName()
                              + ",httpport="
                              + Settings.get(Settings.HTTP_PORT_NUMBER)
                              + ",applicationname="
                              + Settings.get(Settings.APPLICATIONNAME) + "," + (
                index == -1 ? "*" : "index=" + index));
    }
    
}
