/*
 * #%L
 * Netarchivesuite - harvester - test
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

package dk.netarkivet.harvester.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.dao.DAOProviderFactory;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.SetSystemProperty;

/**
 * A generic superclass for the harvest definition tests. This sets up the various DAOs etc.
 */
public class DataModelTestCase {
    Logger log = LoggerFactory.getLogger(DataModelTestCase.class);

    SetSystemProperty derbyLog = new SetSystemProperty("derby.stream.error.file", new File(TestInfo.TEMPDIR,
            "derby.log").getAbsolutePath());
    ReloadSettings rs = new ReloadSettings();
    File commonTempdir = new File(TestInfo.TEMPDIR, "commontempdir");

    @Before
    public void setUp() throws Exception {
        rs.setUp();

        FileUtils.removeRecursively(TestInfo.TEMPDIR);

        assertFalse("Tempdir '" + TestInfo.TEMPDIR.getAbsolutePath() + "' exists ", TestInfo.TEMPDIR.exists());
        TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);

        derbyLog.setUp();

        String derbyDBUrl = "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath() + "/fullhddb;create=true";
        Settings.set(CommonSettings.DB_BASE_URL, derbyDBUrl);
        Settings.set(CommonSettings.DB_MACHINE, "");
        Settings.set(CommonSettings.DB_PORT, "");
        Settings.set(CommonSettings.DB_DIR, "");

        commonTempdir.mkdir();

        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, commonTempdir.getAbsolutePath());

        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
        HarvestDAOUtils.resetDAOs();
        log.trace("setup() DatabaseTestUtils.getHDDB " + TestInfo.DBFILE + "  fullhddb " + TestInfo.TEMPDIR);
        DatabaseTestUtils.createHDDB(TestInfo.DBFILE, "fullhddb", TestInfo.TEMPDIR);
        assertEquals("DBUrl wrong", Settings.get(CommonSettings.DB_BASE_URL), derbyDBUrl);
    }

    @After
    public void tearDown() throws Exception {
        log.trace("tearDown() DatabaseTestUtils.dropHDDB()");
        DatabaseTestUtils.dropHDDB();
        // null field instance in DBSpecifics.
        Field f = ReflectUtils.getPrivateField(DBSpecifics.class, "instance");
        f.set(null, null);
        log.trace("tearDown() derbyLog.tearDown()");
        derbyLog.tearDown();
        log.trace("tearDown() FileUtils.removeRecursively");

        // don't work on windows derby.log seem to be locked
        try {
            FileUtils.removeRecursively(TestInfo.TEMPDIR);
        } catch (IOFailure ie) {

        }
        log.trace("tearDown() HarvestDAOUtils.resetDAOs()");

        HarvestDAOUtils.resetDAOs();
        log.trace("tearDown() HarvestDBConnection.cleanup()");

        HarvestDBConnection.cleanup();
        log.trace("tearDown() rs.tearDown()");

        rs.tearDown();
        log.trace("tearDown() done");

    }

    /**
     * Creates the following jobs and hds: HDs: HD#1: selective HD#2: full; no previous HD#3: full; previous: HD#2 HD#4:
     * full; no previous, later than HD#1, HD#2 HD#5: full; previous: HD#4, later than HD#1, HD#2
     * <p>
     * Jobs: HD#1, harvest 0: 2,3 Jobs: HD#1, harvest 1: 4,5 Jobs: HD#1, harvest 2: 6,7 Jobs: HD#2, harvest 0: 8,9 Jobs:
     * HD#3, harvest 0: 10,11 Jobs: HD#4, harvest 0: 12,13 Jobs: HD#5, harvest 0: 14,15
     */
    public static void createTestJobs(long startJobId, long endJobId) {
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        List<DomainConfiguration> list = new ArrayList<DomainConfiguration>();
        list.add(DomainDAO.getInstance().read("netarkivet.dk").getDefaultConfiguration());
        Schedule sched = new RepeatingSchedule(new Date(), 10, new HourlyFrequency(1), "MySched", "No comments");
        ScheduleDAO.getInstance().create(sched);
        PartialHarvest hd1 = new PartialHarvest(list, sched, "HD#1", "No comments", "Everybody");
        hddao.create(hd1);
        FullHarvest hd2 = new FullHarvest("HD#1", "No comments", null, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, Constants.DEFAULT_MAX_JOB_RUNNING_TIME, false,
                DAOProviderFactory.getHarvestDefinitionDAOProvider(), DAOProviderFactory.getJobDAOProvider(),
                DAOProviderFactory.getExtendedFieldDAOProvider(), DAOProviderFactory.getDomainDAOProvider());
        hd2.setSubmissionDate(new GregorianCalendar(1970, Calendar.JANUARY, 1).getTime());
        hddao.create(hd2);
        FullHarvest hd3 = new FullHarvest("HD#2", "No comments", hd2.getOid(), Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, Constants.DEFAULT_MAX_JOB_RUNNING_TIME, false,
                DAOProviderFactory.getHarvestDefinitionDAOProvider(), DAOProviderFactory.getJobDAOProvider(),
                DAOProviderFactory.getExtendedFieldDAOProvider(), DAOProviderFactory.getDomainDAOProvider());
        hd3.setSubmissionDate(new GregorianCalendar(1970, Calendar.FEBRUARY, 1).getTime());
        hddao.create(hd3);
        FullHarvest hd4 = new FullHarvest("HD#3", "No comments", null, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, Constants.DEFAULT_MAX_JOB_RUNNING_TIME, false,
                DAOProviderFactory.getHarvestDefinitionDAOProvider(), DAOProviderFactory.getJobDAOProvider(),
                DAOProviderFactory.getExtendedFieldDAOProvider(), DAOProviderFactory.getDomainDAOProvider());
        hd4.setSubmissionDate(new GregorianCalendar(1970, Calendar.MARCH, 1).getTime());
        hddao.create(hd4);
        FullHarvest hd5 = new FullHarvest("HD#4", "No comments", hd4.getOid(), Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, Constants.DEFAULT_MAX_JOB_RUNNING_TIME, false,
                DAOProviderFactory.getHarvestDefinitionDAOProvider(), DAOProviderFactory.getJobDAOProvider(),
                DAOProviderFactory.getExtendedFieldDAOProvider(), DAOProviderFactory.getDomainDAOProvider());
        hd5.setSubmissionDate(new GregorianCalendar(1970, Calendar.APRIL, 1).getTime());
        hddao.create(hd5);

        Map<String, String> dcmap = new HashMap<String, String>();
        dcmap.put("netarkivet.dk", hd1.getDomainConfigurations().next().getName());
        /** FIXME is this code still used. */
        HeritrixTemplate defaultOrderXmlDocument = TemplateDAO.getInstance().read("default_orderxml");
        Job j2 = getNewNetarkivetJob(hd1, dcmap, false, defaultOrderXmlDocument, 0);

        JobDAO.getInstance().create(j2);
        assertEquals("Job IDs in database have changed." + "Please update unit test to reflect.", startJobId,
                j2.getJobID().longValue());

        Job j3 = getNewNetarkivetJob(hd1, dcmap, false, defaultOrderXmlDocument, 0);
        JobDAO.getInstance().create(j3);
        Job j4 = getNewNetarkivetJob(hd1, dcmap, false, defaultOrderXmlDocument, 1);
        JobDAO.getInstance().create(j4);
        Job j5 = getNewNetarkivetJob(hd1, dcmap, false, defaultOrderXmlDocument, 1);
        JobDAO.getInstance().create(j5);
        Job j6 = getNewNetarkivetJob(hd1, dcmap, false, defaultOrderXmlDocument, 2);
        JobDAO.getInstance().create(j6);
        Job j7 = getNewNetarkivetJob(hd1, dcmap, false, defaultOrderXmlDocument, 2);
        JobDAO.getInstance().create(j7);
        Job j8 = getNewNetarkivetJob(hd2, dcmap, true, defaultOrderXmlDocument, 0);
        JobDAO.getInstance().create(j8);
        Job j9 = getNewNetarkivetJob(hd2, dcmap, true, defaultOrderXmlDocument, 0);
        JobDAO.getInstance().create(j9);
        Job j10 = getNewNetarkivetJob(hd3, dcmap, true, defaultOrderXmlDocument, 0);
        JobDAO.getInstance().create(j10);
        Job j11 = getNewNetarkivetJob(hd3, dcmap, true, defaultOrderXmlDocument, 0);
        JobDAO.getInstance().create(j11);
        Job j12 = getNewNetarkivetJob(hd4, dcmap, true, defaultOrderXmlDocument, 0);
        JobDAO.getInstance().create(j12);
        Job j13 = getNewNetarkivetJob(hd4, dcmap, true, defaultOrderXmlDocument, 0);
        JobDAO.getInstance().create(j13);
        Job j14 = getNewNetarkivetJob(hd5, dcmap, true, defaultOrderXmlDocument, 0);
        JobDAO.getInstance().create(j14);
        Job j15 = getNewNetarkivetJob(hd5, dcmap, true, defaultOrderXmlDocument, 0);
        JobDAO.getInstance().create(j15);
        assertEquals("Job IDs in database have changed. " + "Please update unit test to reflect.", endJobId, j15
                .getJobID().longValue());
    }

    private static Job getNewNetarkivetJob(HarvestDefinition hd, Map<String, String> dcmap, boolean snapshot,
            HeritrixTemplate defaultOrderXmlDocument, int harvestNum) {
        Job j = new Job(hd.getOid(), dcmap, "test", snapshot, Constants.DEFAULT_MAX_OBJECTS,
                Constants.DEFAULT_MAX_BYTES, Constants.DEFAULT_MAX_JOB_RUNNING_TIME, JobStatus.NEW, "default_orderxml",
                defaultOrderXmlDocument, "netarkivet.dk", harvestNum, null);
        j.setDefaultHarvestNamePrefix();
        return j;
    }

    public static void addHarvestDefinitionToDatabaseWithId(long id) throws SQLException {
        Connection con = HarvestDBConnection.get();
        try {
            final String sqlInsert = "INSERT INTO harvestdefinitions ("
                    + "harvest_id, name, comments, numevents, submitted," + "isactive,edition)"
                    + "VALUES(?,?,?,?,?,?,?)";
            PreparedStatement statement = con.prepareStatement(sqlInsert);
            statement.setLong(1, id);
            statement.setString(2, "name" + id);
            statement.setString(3, "NoComments");
            statement.setInt(4, 0);
            statement.setDate(5, new java.sql.Date(System.currentTimeMillis()));
            statement.setInt(6, 0);
            statement.setInt(7, 1);
            int rows = statement.executeUpdate();
            ArgumentNotValid.checkTrue(rows == 1, "Insert was not successful");
            statement.close();
            String addFullharvestInsert = "INSERT INTO fullharvests "
                    + "( harvest_id, maxobjects, maxbytes, previoushd )" + "VALUES ( ?, ?, ?, ? )";
            statement = con.prepareStatement(addFullharvestInsert);
            statement.setLong(1, id);
            statement.setLong(2, Constants.DEFAULT_MAX_OBJECTS);
            statement.setLong(3, Constants.DEFAULT_MAX_BYTES);
            statement.setNull(4, Types.BIGINT);
            rows = statement.executeUpdate();
            ArgumentNotValid.checkTrue(rows == 1, "Insert was not successful");
        } finally {
            HarvestDBConnection.release(con);
        }

    }
}
