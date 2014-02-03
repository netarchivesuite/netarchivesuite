/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.dom4j.Document;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.dao.DataModelTestDao;
import dk.netarkivet.harvester.dao.DomainDAO;
import dk.netarkivet.harvester.dao.HarvestDefinitionDAO;
import dk.netarkivet.harvester.dao.JobDAO;
import dk.netarkivet.harvester.dao.ParameterMap;
import dk.netarkivet.harvester.dao.ScheduleDAO;
import dk.netarkivet.harvester.dao.TemplateDAO;
import dk.netarkivet.harvester.dao.spec.DBSpecifics;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.SetSystemProperty;

/**
 * A generic superclass for the harvest definition tests.  This
 * sets up the various DAOs etc.
 */
public class DataModelTestCase extends TestCase {

	SetSystemProperty derbyLog
	= new SetSystemProperty(
			"derby.stream.error.file",
			new File(TestInfo.TEMPDIR, "derby.log")
			.getAbsolutePath());
	ReloadSettings rs = new ReloadSettings();
	File commonTempdir = new File(TestInfo.TEMPDIR, "commontempdir");
	
	public DataModelTestCase(String s) {
		super(s); 
	}

	//TODO this method is highly derby-specific. Implement a mechanism, e.g. a
	//command-line system parameter, to switch between derby and MySQL for
	//unit tests.
	public void setUp() throws Exception {
		super.setUp();
		rs.setUp();
		FileUtils.removeRecursively(TestInfo.TEMPDIR);
		assertFalse("Tempdir '" +  TestInfo.TEMPDIR.getAbsolutePath()
				+  "' exists ", TestInfo.TEMPDIR.exists());
		TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);

		derbyLog.setUp();
		String derbyDBUrl = "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath()
				+ "/fullhddb;upgrade=true";
		Settings.set(CommonSettings.DB_BASE_URL, derbyDBUrl);
		Settings.set(CommonSettings.DB_MACHINE, "");
		Settings.set(CommonSettings.DB_PORT, "");
		Settings.set(CommonSettings.DB_DIR, "");

		commonTempdir.mkdir();
		Settings.set(CommonSettings.DIR_COMMONTEMPDIR,
				commonTempdir.getAbsolutePath());

		Settings.set(CommonSettings.NOTIFICATIONS_CLASS,
				RememberNotifications.class.getName());
		HarvestDAOUtils.resetDAOs();

		Connection c = DatabaseTestUtils.getHDDB(TestInfo.DBFILE, "fullhddb",
				TestInfo.TEMPDIR);
		if (c == null) {
			fail("No connection to Database: "
					+ TestInfo.DBFILE.getAbsolutePath());
		}

		assertEquals("DBUrl wrong", Settings.get(CommonSettings.DB_BASE_URL),
				derbyDBUrl);
		
		DBSpecifics.getInstance().updateTables();
	}

	public void tearDown() throws Exception {
		super.tearDown();
		DatabaseTestUtils.dropHDDB();
		// null field instance in DBSpecifics.
		Field f = ReflectUtils.getPrivateField(DBSpecifics.class, "instance");
		f.set(null, null);
		derbyLog.tearDown();
		//don't work on windows derby.log seem to be locked
		try{
			FileUtils.removeRecursively(TestInfo.TEMPDIR);
		}
		catch(IOFailure ie)
		{

		}
		HarvestDAOUtils.resetDAOs();
		rs.tearDown();
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
	public static void createTestJobs(long startJobId, long endJobId) {
		HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
		List<DomainConfiguration> list = new ArrayList<DomainConfiguration>();
		list.add(DomainDAO.getInstance().read("netarkivet.dk").getDefaultConfiguration());
		Schedule sched = new RepeatingSchedule(new Date(), 10,
				new HourlyFrequency(1),
				"MySched", "No comments");
		ScheduleDAO.getInstance().create(sched);
		PartialHarvest hd1 = new PartialHarvest(list, sched, "HD#1", "No comments", "Everybody");
		hddao.create(hd1);
		FullHarvest hd2 = new FullHarvest("HD#2", "No comments", null,
				Constants.DEFAULT_MAX_OBJECTS,
				Constants.DEFAULT_MAX_BYTES,
				Constants.DEFAULT_MAX_JOB_RUNNING_TIME,
				false);
		hd2.setSubmissionDate(new GregorianCalendar(1970, Calendar.JANUARY, 1).getTime());
		hddao.create(hd2);
		FullHarvest hd3 = new FullHarvest("HD#3", "No comments", hd2.getOid(),
				Constants.DEFAULT_MAX_OBJECTS,
				Constants.DEFAULT_MAX_BYTES,
				Constants.DEFAULT_MAX_JOB_RUNNING_TIME,
				false);
		hd3.setSubmissionDate(new GregorianCalendar(1970, Calendar.FEBRUARY, 1).getTime());
		hddao.create(hd3);
		FullHarvest hd4 = new FullHarvest("HD#4", "No comments", null,
				Constants.DEFAULT_MAX_OBJECTS,
				Constants.DEFAULT_MAX_BYTES,
				Constants.DEFAULT_MAX_JOB_RUNNING_TIME,
				false);
		hd4.setSubmissionDate(new GregorianCalendar(1970, Calendar.MARCH, 1).getTime());
		hddao.create(hd4);
		FullHarvest hd5 = new FullHarvest("HD#5", "No comments", hd4.getOid(),
				Constants.DEFAULT_MAX_OBJECTS,
				Constants.DEFAULT_MAX_BYTES,
				Constants.DEFAULT_MAX_JOB_RUNNING_TIME, 
				false);
		hd5.setSubmissionDate(new GregorianCalendar(1970, Calendar.APRIL, 1).getTime());
		hddao.create(hd5);

		Map<String, String> dcmap = new HashMap<String, String>();
		dcmap.put("netarkivet.dk", hd1.getDomainConfigurations().next().getName());
		Document defaultOrderXmlDocument = TemplateDAO.getInstance().read("default_orderxml").getTemplate();
		Job j2 = getNewNetarkivetJob(hd1, dcmap, false, defaultOrderXmlDocument, 0);

		JobDAO.getInstance().create(j2);
		assertEquals("Job IDs in database have changed."
				+ "Please update unit test to reflect.",
				startJobId, j2.getJobID().longValue());

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
		assertEquals("Job IDs in database have changed. "
				+ "Please update unit test to reflect.", endJobId,
				j15.getJobID().longValue());
	}

	private static Job getNewNetarkivetJob(HarvestDefinition hd,
			Map<String, String> dcmap, boolean snapshot,
			Document defaultOrderXmlDocument, int harvestNum) {
		Job j = new Job(hd.getOid(),
				dcmap, "test", snapshot,
				Constants.DEFAULT_MAX_OBJECTS,
				Constants.DEFAULT_MAX_BYTES,
				Constants.DEFAULT_MAX_JOB_RUNNING_TIME,
				JobStatus.NEW, "default_orderxml",
				defaultOrderXmlDocument, "netarkivet.dk", harvestNum, null);
		j.setDefaultHarvestNamePrefix();
		return j;
	}

	public static void addHarvestDefinitionToDatabaseWithId(final long id) throws SQLException {
		DataModelTestDao dao = DataModelTestDao.getInstance();
		int rows = dao.executeUpdate("INSERT INTO harvestdefinitions ("
				+ "harvest_id, name, comments, numevents, submitted,"
				+ "isactive,edition)"
				+ "VALUES(:harvestId,:name,:comments,:numevents,:submitted,:isActive,:edition)",
				new ParameterMap(
						"harvestId", id,
						"name", "name"+  id,
						"comments", "NoComments",
						"numevents", 0,
						"submitted", new Date(),
						"isActive", 0,
						"edition", 1));
		ArgumentNotValid.checkTrue(rows == 1, "Insert was not successful");
		
		rows = dao.executeUpdate("INSERT INTO fullharvests  (harvest_id, maxobjects, maxbytes,"
				+ " previoushd) VALUES (:harvestId, :maxObjects, :maxBytes, :previousHd)",
				new ParameterMap(
						"harvestId", id,
						"maxObjects", Constants.DEFAULT_MAX_OBJECTS,
						"maxBytes", Constants.DEFAULT_MAX_BYTES,
						"previousHd", null));
		ArgumentNotValid.checkTrue(rows == 1, "Insert was not successful");
	}
}
