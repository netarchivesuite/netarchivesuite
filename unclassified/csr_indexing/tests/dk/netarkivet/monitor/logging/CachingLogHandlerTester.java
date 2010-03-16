/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.monitor.logging;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.management.Constants;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.monitor.MonitorSettings;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Test behavior of the CachingLogHandler, and its exposure of log records using
 * JMX.
 */
public class CachingLogHandlerTester extends TestCase {
    private CachingLogHandler cachingLogHandler;
    private MBeanServer mBeanServer;
    private static final int LOG_HISTORY_SIZE = 42;
    private static final String METHOD_NAME = "myMethod";
    private static final String LOG_MESSAGE = "Log message ";
    ReloadSettings rs = new ReloadSettings();

    public CachingLogHandlerTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        //Get the MBean server
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        //Set Settings to what we expect
        Settings.set(CommonSettings.THIS_PHYSICAL_LOCATION, "physLocationOne");
        Settings.set(CommonSettings.APPLICATION_NAME, "TestApp1");
        Settings.set(MonitorSettings.LOGGING_HISTORY_SIZE, Integer.toString(LOG_HISTORY_SIZE));
        Settings.set(CommonSettings.HTTP_PORT_NUMBER, "8076");
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY, "HIGHPRIORITY");
        Settings.set(CommonSettings.USE_REPLICA_ID, "ONE");
    }

    public void tearDown() {
        if (cachingLogHandler != null) {
            cachingLogHandler.close();
        }
        rs.tearDown();
    }

    /**
     * Test that the constructor exposes N MBeans, and each can be connected to
     * and returns the empty string. It is tested that the names of the objects
     * are generated from: "location" = 
     * Settings.get(CommonSettings.ENVIRONMENT_THIS_PHYSICAL_LOCATION)
     * "machine" = InetAddress.getLocalHost().getCanonicalHostName() 
     * "httpport" = Settings.get(CommonSettings.HTTP_PORT_NUMBER) 
     * "applicationname" = Settings.get(CommonSettings.APPLICATION_NAME) 
     * "applicationinstid" =
     * Settings.get(CommonSettings.APPLICATIONINSTANCE_ID) 
     * "index" = (index in the cache; 0
     * is always the most recent log record)
     *
     * It is also tested that no MBeans were registered before this call.
     *
     * @throws Exception
     */
    public void testCachingLogHandler() throws Exception {

        //Check no mbeans of this type before
        int before = mBeanServer.getMBeanCount();
        ObjectName name = getObjectName(-1);
        assertEquals("Should have 0 mbeans matching object name",
                     0, mBeanServer.queryNames(name, null).size());

        //Call constructor
        cachingLogHandler = new CachingLogHandler();

        //Check all mbeans are registered
        int after = mBeanServer.getMBeanCount();
        assertEquals(LOG_HISTORY_SIZE + " new MBeans should be registered.",
                     LOG_HISTORY_SIZE, after - before);

        Set<ObjectName> names = mBeanServer.queryNames(null, null);
        for (ObjectName currentName : names) {
            System.out.println(currentName);
        }
        //assertTrue(names.size() == 42);
        
        
        //Check 42 mbeans of this type
        assertEquals("Should have " + LOG_HISTORY_SIZE
                    + " mbeans matching object name '" + name + "'",
                     LOG_HISTORY_SIZE,
                     mBeanServer.queryNames(name, null).size());

        //Check two interesting mbeans: The first and the last.
        ObjectInstance mbean = getObjectInstance(mBeanServer, 0);
        assertTrue("Must be of the right type",
                   SingleLogRecord.class.isAssignableFrom(
                           Class.forName(mbean.getClassName())));

        mbean = getObjectInstance(mBeanServer, LOG_HISTORY_SIZE - 1);
        assertTrue("Must be of the right type",
                   SingleLogRecord.class.isAssignableFrom(
                           Class.forName(mbean.getClassName())));

        //Check that there are no more.
        try {
            getObjectInstance(mBeanServer, LOG_HISTORY_SIZE);
            fail("There should be no instance number " + LOG_HISTORY_SIZE);
        } catch (InstanceNotFoundException e) {
            //Expected
        }

        //Check they all return the empty string
        for (int i = 0; i < LOG_HISTORY_SIZE; i++) {
            String logRecordI
                    = getLogRecordAtIndex(i, mBeanServer);
            assertEquals("Should have empty content",
                         "", logRecordI);
        }
    }

    /**
     * Tests that publish registers the message in memory, so that it is
     * accessible through JMX.
     *
     * Test that publishing more than 42 messages lets the oldest one fall out.
     *
     * Tests that publishing more than a set
     *
     * @throws Exception
     */
    public void testPublish() throws Exception {
        cachingLogHandler = new CachingLogHandler();

        //Get logrecord at index 0
        String logRecord0 = getLogRecordAtIndex(0, mBeanServer);
        //Get logrecord at index 1
        String logRecord1 = getLogRecordAtIndex(1, mBeanServer);

        //Check no content yet at index 0
        assertEquals("Should have no log record yet",
                     "", logRecord0);

        //publish a log record
        cachingLogHandler.publish(
                generateLogRecord(Level.WARNING, 1));

        //Get logrecord at index 0
        logRecord0 = getLogRecordAtIndex(0, mBeanServer);
        //Check content at index 0
        assertLogRecordLogged(logRecord0, 1, "WARNING");

        //publish a new log record
        cachingLogHandler.publish(
                generateLogRecord(Level.INFO, 2));

        //Get logrecord at index 0
        logRecord0 = getLogRecordAtIndex(0, mBeanServer);
        //Get logrecord at index 1
        logRecord1 = getLogRecordAtIndex(1, mBeanServer);
        //Check new content at index 0
        assertLogRecordLogged(logRecord0, 2, "INFO");
        //Check old content at index 1
        assertLogRecordLogged(logRecord1, 1, "WARNING");

        //publish null
        cachingLogHandler.publish(null);

        //Get logrecord at index 0
        logRecord0 = getLogRecordAtIndex(0, mBeanServer);
        //Get logrecord at index 1
        logRecord1 = getLogRecordAtIndex(1, mBeanServer);
        //Check no new content at index 0
        assertLogRecordLogged(logRecord0, 2, "INFO");
        //Check no new content at index 1
        assertLogRecordLogged(logRecord1, 1, "WARNING");

        //Publish 42 log records, with numbers from 42 to 83
        for (int i = LOG_HISTORY_SIZE; i < 2 * LOG_HISTORY_SIZE; i++) {
            //publish a new log record
            cachingLogHandler.publish(generateLogRecord(Level.FINE, i));
        }

        //Check all 42 MBeans
        for (int i = 0; i < LOG_HISTORY_SIZE; i++) {
            //Get logrecord at index i
            String logRecordI
                    = getLogRecordAtIndex(i, mBeanServer);
            //Check content (starting at 83 going down to 42)
            assertLogRecordLogged(logRecordI,
                                  2 * LOG_HISTORY_SIZE - 1 - i, "FINE");
        }
    }

    public void testGetNthLogRecord() throws Exception {
        LogRecord record1 = generateLogRecord(Level.WARNING, 1);
        LogRecord record2 = generateLogRecord(Level.INFO, 2);
        cachingLogHandler = new CachingLogHandler();
        cachingLogHandler.publish(
                record1);
        cachingLogHandler.publish(
                record2);
        assertEquals("Should have last record as record 0 from top",
                     record2, cachingLogHandler.getNthLogRecord(0));
        assertEquals("Should have first record as record 1 from top",
                     record1, cachingLogHandler.getNthLogRecord(1));
        assertNull("Should have null as record 2 from top",
                   cachingLogHandler.getNthLogRecord(2));

        try {
            cachingLogHandler.getNthLogRecord(-1);
            fail("Should throw ArgumentNotValid on negative");
        } catch(ArgumentNotValid e) {
            //expected
        }

        try {
            cachingLogHandler.getNthLogRecord(LOG_HISTORY_SIZE);
            fail("Should throw ArgumentNotValid on too large number");
        } catch(ArgumentNotValid e) {
            //expected
        }
    }

    private LogRecord generateLogRecord(Level level, int number) {
        LogRecord logRecord = new LogRecord(level, generateLogMessage(number));
        logRecord.setSourceClassName(getClass().getName());
        logRecord.setSourceMethodName(METHOD_NAME);
        return logRecord;
    }

    private static String generateLogMessage(int i) {
        return LOG_MESSAGE + i;
    }

    /**
     * Asserts a log record with the message "Log Record <<number>>" is logged
     * at level <<level>>.
     *
     * @param logRecord
     * @param number    The number in the log record message.
     * @param level     The logging level of the log record.
     */
    private static void assertLogRecordLogged(
            String logRecord,
            int number,
            String level) {
        StringAsserts.assertStringContains(
                "Should contain the log message",
                generateLogMessage(number), logRecord);
        StringAsserts.assertStringContains(
                "Should contain the logging level",
                level, logRecord);
        StringAsserts.assertStringContains(
                "Should contain the stack top",
                METHOD_NAME, logRecord);
    }

    /**
     * Test that close unregisters the registered mbeans.
     *
     * @throws Exception
     */
    public void testClose() throws Exception {
        //Check no mbeans of this type before
        int before = mBeanServer.getMBeanCount();
        ObjectName name = getObjectName(-1);
        assertEquals("Should have 0 mbeans matching object name",
                     0, mBeanServer.queryMBeans(name, null).size());

        //Call constructor
        cachingLogHandler = new CachingLogHandler();

        //Check all mbeans are registered
        int after = mBeanServer.getMBeanCount();
        assertEquals("42 new MBeans should be registered.",
                     LOG_HISTORY_SIZE, after - before);

        //Check 42 mbeans of this type
        assertEquals("Should have 42 mbeans matching object name",
                     LOG_HISTORY_SIZE,
                     mBeanServer.queryMBeans(name, null).size());

        cachingLogHandler.close();

        //Check all mbeans are unregistered
        int afterClose = mBeanServer.getMBeanCount();
        assertEquals("0 MBeans should be registered.",
                     0, afterClose - before);

        //Check no mbeans of this type
        assertEquals("Should have 0 mbeans matching object name",
                     0, mBeanServer.queryMBeans(name, null).size());

    }

    /**
     * Finds mbean at log index given.
     *
     * @param index       The index to find the log record at.
     * @param mBeanServer The mbean server to find it at.
     * @return A CachingLogRecordMBean.
     * @throws MalformedObjectNameException
     * @throws InstanceNotFoundException
     * @throws JMException
     */
    private static String getLogRecordAtIndex(
            int index,
            MBeanServer mBeanServer) throws JMException {
                    
        return (String) mBeanServer.getAttribute(getObjectName(index),
                                                 "RecordString");
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
        return new ObjectName("dk.netarkivet.common.logging:"
                              + Constants.PRIORITY_KEY_LOCATION + "="
                              + Settings.get(
                                      CommonSettings.THIS_PHYSICAL_LOCATION)
                              + "," +  Constants.PRIORITY_KEY_MACHINE + "=" 
                              + SystemUtils.getLocalHostName() + ","
                              + Constants.PRIORITY_KEY_HTTP_PORT + "="
                              + Settings.get(CommonSettings.HTTP_PORT_NUMBER)
                              + "," + Constants.PRIORITY_KEY_REPLICANAME + "=BarOne,"
                              + Constants.PRIORITY_KEY_PRIORITY + "=HIGHPRIORITY"
                              + "," + Constants.PRIORITY_KEY_APPLICATIONNAME + "="
                              + Settings.get(CommonSettings.APPLICATION_NAME)
                              + "," + Constants.PRIORITY_KEY_APPLICATIONINSTANCEID + "="
                              + Settings.get(CommonSettings.APPLICATION_INSTANCE_ID)
                              + "," + (index == -1 ? "*" 
                                      : Constants.PRIORITY_KEY_INDEX + "=" + index));
    }

    /**
     * Returns a JMX object instance as expected by our CachingLogRecordMBean.
     *
     * @param mBeanServer The MBeanServer to get the object from.
     * @param index       Index of LogRecord
     * @return An ObjectInstance
     * @throws InstanceNotFoundException
     * @throws MalformedObjectNameException
     */
    private static ObjectInstance getObjectInstance(
            MBeanServer mBeanServer, int index) throws InstanceNotFoundException,
                                                       MalformedObjectNameException {
        return mBeanServer.getObjectInstance(
                getObjectName(index));
    }
}