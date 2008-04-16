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

package dk.netarkivet.harvester.datamodel;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.dom4j.Document;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.testutils.DBUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.SetSystemProperty;

/**
 * A generic superclass for the harvest definition tests.  This
 * sets up the various DAOs etc.
 */
public class DataModelTestCase extends TestCase {
    
    SetSystemProperty derbyLog
            = new SetSystemProperty("derby.stream.error.file",
                                    new File(TestInfo.TEMPDIR, "derby.log").getAbsolutePath());

    public DataModelTestCase(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
        derbyLog.setUp();
        Settings.set(Settings.DB_URL, "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath() + "/fullhddb");
        Settings.set(Settings.NOTIFICATIONS_CLASS,
                     RememberNotifications.class.getName());
        //TestUtils.resetDAOs();

        Connection c = DBUtils.getHDDB(TestInfo.DBFILE, TestInfo.TEMPDIR);
        if (c == null) {
            fail("No connection to Database: " + TestInfo.DBFILE.getAbsolutePath());
        }

        assertEquals("DBUrl wrong", Settings.get(Settings.DB_URL), "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath() + "/fullhddb");
        TemplateDAO.getInstance();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        DBUtils.dropHDDB();
        Field f = ReflectUtils.getPrivateField(DBSpecifics.class, "instance");
        f.set(null, null);
        derbyLog.tearDown();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        Settings.reload();
        TestUtils.resetDAOs();
    }

    /** Creates the following jobs and hds:
     * HDs:
     * HD#1: selective
     * HD#2: full; no previous
     * HD#3: full; previous: HD#2
     * HD#4: full; no previous, later than HD#1, HD#2
     * HD#5: full; previous: HD#4, later than HD#1, HD#2
     *
     * Jobs: HD#1, harvest 0: 2,3
     * Jobs: HD#1, harvest 1: 4,5
     * Jobs: HD#1, harvest 2: 6,7
     * Jobs: HD#2, harvest 0: 8,9
     * Jobs: HD#3, harvest 0: 10,11
     * Jobs: HD#4, harvest 0: 12,13
     * Jobs: HD#5, harvest 0: 14,15
     *
     */
    public static void createTestJobs() {
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        List<DomainConfiguration> list = new ArrayList<DomainConfiguration>();
        list.add(DomainDAO.getInstance().read("netarkivet.dk").getDefaultConfiguration());
        Schedule sched = new RepeatingSchedule(new Date(), 10,
                                               new HourlyFrequency(1),
                                               "MySched", "No comments");
        ScheduleDAO.getInstance().create(sched);
        PartialHarvest hd1 = new PartialHarvest(list, sched, "HD#1", "No comments");
        hddao.create(hd1);
        FullHarvest hd2 = new FullHarvest("HD#2", "No comments", null,
                                          Constants.DEFAULT_MAX_OBJECTS,
                                          Constants.DEFAULT_MAX_BYTES);
        hd2.setSubmissionDate(new GregorianCalendar(1970, Calendar.JANUARY, 1).getTime());
        hddao.create(hd2);
        FullHarvest hd3 = new FullHarvest("HD#3", "No comments", hd2.getOid(),
                                          Constants.DEFAULT_MAX_OBJECTS,
                                          Constants.DEFAULT_MAX_BYTES);
        hd3.setSubmissionDate(new GregorianCalendar(1970, Calendar.FEBRUARY, 1).getTime());
        hddao.create(hd3);
        FullHarvest hd4 = new FullHarvest("HD#4", "No comments", null,
                                          Constants.DEFAULT_MAX_OBJECTS,
                                          Constants.DEFAULT_MAX_BYTES);
        hd4.setSubmissionDate(new GregorianCalendar(1970, Calendar.MARCH, 1).getTime());
        hddao.create(hd4);
        FullHarvest hd5 = new FullHarvest("HD#5", "No comments", hd4.getOid(),
                                          Constants.DEFAULT_MAX_OBJECTS,
                                          Constants.DEFAULT_MAX_BYTES);
        hd5.setSubmissionDate(new GregorianCalendar(1970, Calendar.APRIL, 1).getTime());
        hddao.create(hd5);

        Map<String, String> dcmap = new HashMap<String, String>();
        dcmap.put("netarkivet.dk", hd1.getDomainConfigurations().next().getName());
        Document defaultOrderXmlDocument = TemplateDAO.getInstance().read("default_orderxml").getTemplate();
        Job j2 = new Job(hd1.getOid(), dcmap, JobPriority.HIGHPRIORITY, Constants.DEFAULT_MAX_OBJECTS, Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 0);
        JobDAO.getInstance().create(j2);
        assertEquals("Job IDs in database have changed. Please update unit test to reflect.", 2L, j2.getJobID().longValue());
        Job j3 = new Job(hd1.getOid(), dcmap, JobPriority.HIGHPRIORITY, Constants.DEFAULT_MAX_OBJECTS, Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 0);
        JobDAO.getInstance().create(j3);
        Job j4 = new Job(hd1.getOid(), dcmap, JobPriority.HIGHPRIORITY, Constants.DEFAULT_MAX_OBJECTS, Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 1);
        JobDAO.getInstance().create(j4);
        Job j5 = new Job(hd1.getOid(), dcmap, JobPriority.HIGHPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 1);
        JobDAO.getInstance().create(j5);
        Job j6 = new Job(hd1.getOid(), dcmap, JobPriority.HIGHPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 2);
        JobDAO.getInstance().create(j6);
        Job j7 = new Job(hd1.getOid(), dcmap, JobPriority.HIGHPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 2);
        JobDAO.getInstance().create(j7);
        Job j8 = new Job(hd2.getOid(), dcmap, JobPriority.LOWPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 0);
        JobDAO.getInstance().create(j8);
        Job j9 = new Job(hd2.getOid(), dcmap, JobPriority.LOWPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 0);
        JobDAO.getInstance().create(j9);
        Job j10 = new Job(hd3.getOid(), dcmap, JobPriority.LOWPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 0);
        JobDAO.getInstance().create(j10);
        Job j11 = new Job(hd3.getOid(), dcmap, JobPriority.LOWPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 0);
        JobDAO.getInstance().create(j11);
        Job j12 = new Job(hd4.getOid(), dcmap, JobPriority.LOWPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 0);
        JobDAO.getInstance().create(j12);
        Job j13 = new Job(hd4.getOid(), dcmap, JobPriority.LOWPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 0);
        JobDAO.getInstance().create(j13);
        Job j14 = new Job(hd5.getOid(), dcmap, JobPriority.LOWPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 0);
        JobDAO.getInstance().create(j14);
        Job j15 = new Job(hd5.getOid(), dcmap, JobPriority.LOWPRIORITY, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", 0);
        JobDAO.getInstance().create(j15);
        assertEquals("Job IDs in database have changed. Please update unit test to reflect.", 15L, j15.getJobID().longValue());
    }
}
