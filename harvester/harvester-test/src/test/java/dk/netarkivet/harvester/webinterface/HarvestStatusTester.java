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

package dk.netarkivet.harvester.webinterface;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.PageContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.SlowTest;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobDAOTester;
import dk.netarkivet.harvester.datamodel.JobDBDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.datamodel.TestInfo;

/**
 * Test of Harvest Status utility method for resubmitting jobs.
 */
@Category(SlowTest.class)
public class HarvestStatusTester extends HarvesterWebinterfaceTestCase {
    private static final I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);

    private HarvestChannel testChan;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testChan = new HarvestChannel("test", false, true, "");
        try {
            DataModelTestCase.addHarvestDefinitionToDatabaseWithId(dk.netarkivet.harvester.datamodel.TestInfo.HARVESTID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /** Simple unittest for the HarvestStatus constructor and associated getters. */
    @Test
    public void testHarvestStatusConstructor() {
        List<JobStatusInfo> jsiList = new ArrayList<JobStatusInfo>();
        HarvestStatus hs = new HarvestStatus(420L, jsiList);

        assertEquals("Should have returned the correct fullresultsCount", 420L, hs.getFullResultsCount());
        assertEquals("Should have returned the same empty list " + "of JobstatusInfo objects", jsiList,
                hs.getJobStatusInfo());
    }

    @Test
    public void testRejectFailedJob() throws SQLException {
        JobDAO jobDAO = JobDBDAO.getInstance();
        Job job = JobDAOTester.createDefaultJobInDB(0);
        job.setStatus(JobStatus.FAILED);
        JobDAO.getInstance().update(job);
        HarvestStatus.rejectFailedJob(null, null, job.getJobID());
        assertEquals("Job should now be in status FAILED_REJECTED", jobDAO.read(job.getJobID()).getStatus(),
                JobStatus.FAILED_REJECTED);
        try {
            HarvestStatus.rejectFailedJob(null, null, job.getJobID());
            fail("Expect to throw exception in rejecting an already rejected job");
        } catch (Exception e) {
            // expected
        }

    }

    @Test
    public void testUnrejectRejectedJob() throws SQLException {
        JobDAO jobDAO = JobDBDAO.getInstance();
        Job job = JobDAOTester.createDefaultJobInDB(0);
        job.setStatus(JobStatus.FAILED_REJECTED);
        jobDAO.update(job);
        HarvestStatus.unrejectRejectedJob(null, null, job.getJobID());
        assertEquals("Job should now be in status FAILED", jobDAO.read(job.getJobID()).getStatus(), JobStatus.FAILED);
        try {
            HarvestStatus.unrejectRejectedJob(null, null, job.getJobID());
            fail("Expect to throw exception in unrejecting a failed job");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testProcessRequest() throws Exception {
        JobDAO jobDAO = JobDBDAO.getInstance();
        Job job = JobDAOTester.createDefaultJobInDB(0);

        int origJobs = jobDAO.getCountJobs();

        // null context
        try {
            HarvestStatus.processRequest((PageContext) null, I18N);
            fail("Should have thrown ANV on null parameter.");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // null i18n
        TestServletRequest servletRequest = new TestServletRequest();
        try {
            HarvestStatus.processRequest(new TestPageContext(servletRequest), (I18n) null);
            fail("Should have thrown ANV on null parameter.");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // no resubmit parameter
        HarvestStatus.processRequest(new TestPageContext(servletRequest), I18N);
        assertEquals("Should not have generated any new jobs", origJobs, jobDAO.getCountJobs());

        // non-integer resubmit parameter
        Map<String, String[]> parms = new HashMap<String, String[]>();
        parms.put(Constants.JOB_RESUBMIT_PARAM, new String[] {"x"});
        servletRequest.setParameterMap(parms);
        try {
            HarvestStatus.processRequest(new TestPageContext(servletRequest), I18N);
            fail("Should have forwarded me to an error page on non integer.");
        } catch (ForwardedToErrorPage e) {
            // Expected
        }
        assertEquals("Should not have generated any new jobs", origJobs, jobDAO.getCountJobs());

        // unknown resubmit parameter
        parms.put(Constants.JOB_RESUBMIT_PARAM, new String[] {"999999"});
        try {
            HarvestStatus.processRequest(new TestPageContext(servletRequest), I18N);
            fail("Should have forwarded me to an error page on unknown job.");
        } catch (ForwardedToErrorPage e) {
            // Expected
        }
        assertEquals("Should not have generated any new jobs", origJobs, jobDAO.getCountJobs());

        // correct parameter, check resubmit
        JobDAOTester.changeStatus(job.getJobID(), JobStatus.FAILED);
        parms.put(Constants.JOB_RESUBMIT_PARAM, new String[] {"1"});
        HarvestStatus.processRequest(new TestPageContext(servletRequest), I18N);
        assertEquals("Should have generated one new job", origJobs + 1, jobDAO.getCountJobs());
        assertEquals("Old job should have status resubmitted", JobStatus.RESUBMITTED, jobDAO.read(1L).getStatus());
        assertEquals("New job should have status new", JobStatus.NEW, jobDAO.read(2L).getStatus());

    }

    @Test
    public void testGetjobStatusList() throws Exception {

        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put(HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(), new String[] {"ASC"});
        List<JobStatusInfo> list = HarvestStatus.getjobStatusList(getTestQuery(params)).getJobStatusInfo();
        assertEquals("Number of jobs should be 0", 0, list.size());

        Set<Integer> testStatuscodeSet = new HashSet<Integer>();
        testStatuscodeSet.add(0);

        try {
            params.clear();
            params.put(HarvestStatusQuery.UI_FIELD.JOB_STATUS.name(), new String[] {"bogus"});
            HarvestStatus.getjobStatusList(getTestQuery(params)).getJobStatusInfo();
            fail("Should have forwarded me to ArgumentNotValid for wrong job status.");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        try {
            params.clear();
            params.put(HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(), new String[] {"XX"});
            HarvestStatus.getjobStatusList(getTestQuery(params)).getJobStatusInfo();
            fail("Should have forwarded me to ArgumentNotValid for unknown sort order.");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        // Test non failing result
        // add default job to test-database
        Job job = JobDAOTester.createDefaultJobInDB(0); // added job with harvestid=TestInfo.HARVESTID (5678L);
        //System.out.println(job);
        
        HarvestStatusQuery hsq = new HarvestStatusQuery(TestInfo.HARVESTID, 0); 
        HarvestStatus hs = JobDAO.getInstance().getStatusInfo(hsq);
        List<JobStatusInfo> jobs = hs.getJobStatusInfo();
        assertTrue(job.getHarvestNum() == 0);
        assertTrue(job.getOrigHarvestDefinitionID() == TestInfo.HARVESTID);
        assertTrue("Number of jobs matching harvest #" +  TestInfo.HARVESTID + " should be 1, but was " + jobs.size(), jobs.size() == 1);
    }

    @Test
    public void testGetSelectedSortOrder() throws Exception {
        TestServletRequest servletRequest = new TestServletRequest();

        HarvestStatusQuery query = new HarvestStatusQuery(servletRequest);

        // check default sorting, which from NAS 5.2 is Descending.
        assertFalse("Expected other default sort order", query.isSortAscending());

        // check error on faulty parameter
        Map<String, String[]> parms = new HashMap<String, String[]>();
        parms.put(HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(), new String[] {"XX"});
        servletRequest.setParameterMap(parms);

        try {
            new HarvestStatusQuery(servletRequest);
            fail("Should have forwarded me to an error page on wrong order parameter.");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // check set order parameter
        parms = new HashMap<String, String[]>();
        parms.put(HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(),
                new String[] {HarvestStatusQuery.SORT_ORDER.ASC.name()});
        servletRequest.setParameterMap(parms);
        query = new HarvestStatusQuery(servletRequest);
        assertTrue("Expected ascending sort order", query.isSortAscending());
    }

    @Test
    public void testGetSelectedJobStatusCode() throws Exception {
        TestServletRequest servletRequest = new TestServletRequest();

        HarvestStatusQuery query = new HarvestStatusQuery(servletRequest);

        // check default
        JobStatus[] selectedStatuses = query.getSelectedJobStatuses();
        assertTrue("No statuscode (same as ALL) should have selected.", selectedStatuses.length == 0);

        //assertTrue("Expected other default sort order", query.isSortAscending());

        // check error on faulty parameter
        Map<String, String[]> parms = new HashMap<String, String[]>();
        parms.put(HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(), new String[] {"XX"});
        servletRequest.setParameterMap(parms);

        // FIXME this says that it should forward to error page on wrong order parameters
        // But it does not!
        try {
            new HarvestStatusQuery(servletRequest);
            fail("Should have forwarded me to an error page on wrong order parameter.");
            // } catch (IllegalArgumentException e) {
            // //Expected
        } catch (ArgumentNotValid e) {
            // Expected
        }
        // check set order parameter
        parms = new HashMap<String, String[]>();
        parms.put(HarvestStatusQuery.UI_FIELD.JOB_STATUS.name(), new String[] {JobStatus.FAILED.name()});
        servletRequest.setParameterMap(parms);
        query = new HarvestStatusQuery(servletRequest);
        assertEquals("Only one statuscode should have selected", query.getSelectedJobStatuses().length, 1);
        assertEquals("Expected failed job status for selection", JobStatus.FAILED.name(),
                query.getSelectedJobStatuses()[0].name());

    }

    public static HarvestStatusQuery getTestQuery(Map<String, String[]> params) {
        TestServletRequest req = new TestServletRequest();
        req.setParameterMap(params);
        return new HarvestStatusQuery(req);
    }

}
