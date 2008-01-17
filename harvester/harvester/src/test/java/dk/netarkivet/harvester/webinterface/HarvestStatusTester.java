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

package dk.netarkivet.harvester.webinterface;
/**
 * Test of Harvest Status utility method for resubmitting jobs.
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobDAOTester;
import dk.netarkivet.harvester.datamodel.JobDBDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.webinterface.HarvestStatus.DefaultedRequest;
import dk.netarkivet.testutils.TestUtils;


public class HarvestStatusTester extends WebinterfaceTestCase {
    private static final I18n I18N = new I18n(
            dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);

    public HarvestStatusTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testProcessRequest() throws Exception {
        if (!TestUtils.runningAs("SVC")) {
            //Excluded while migrating to decidingScope
            return;
        }
        JobDAO jobDAO = JobDBDAO.getInstance();
        Job job = Job.createJob(42L, DomainDAO.getInstance().read(
                "netarkivet.dk").getDefaultConfiguration(), 0);
        jobDAO.create(job);

        int origJobs = jobDAO.getCountJobs();

        //null context
        try {
            HarvestStatus.processRequest(null, I18N);
            fail("Should have thrown ANV on null parameter.");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        //null i18n
        TestServletRequest servletRequest = new TestServletRequest();
        try {
            HarvestStatus.processRequest(new TestPageContext(servletRequest),
                                         null);
            fail("Should have thrown ANV on null parameter.");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        //no resubmit parameter
        HarvestStatus.processRequest(new TestPageContext(servletRequest),
                                     I18N);
        assertEquals("Should not have generated any new jobs",
                     origJobs, jobDAO.getCountJobs());

        //non-integer resubmit parameter
        Map<String, String[]> parms = new HashMap<String, String[]>();
        parms.put(Constants.JOB_RESUBMIT_PARAM, new String[]{"x"});
        servletRequest.setParameterMap(parms);
        try {
            HarvestStatus.processRequest(new TestPageContext(servletRequest),
                                         I18N);
            fail("Should have forwarded me to an error page on non integer.");
        } catch (ForwardedToErrorPage e) {
            //Expected
        }
        assertEquals("Should not have generated any new jobs",
                     origJobs, jobDAO.getCountJobs());

        //unknown resubmit parameter
        parms.put(Constants.JOB_RESUBMIT_PARAM, new String[]{"999999"});
        try {
            HarvestStatus.processRequest(new TestPageContext(servletRequest),
                                         I18N);
            fail("Should have forwarded me to an error page on unknown job.");
        } catch (ForwardedToErrorPage e) {
            //Expected
        }
        assertEquals("Should not have generated any new jobs",
                     origJobs, jobDAO.getCountJobs());

        //correct parameter, check resubmit
        JobDAOTester.changeStatus(1, JobStatus.FAILED);
        parms.put(Constants.JOB_RESUBMIT_PARAM, new String[]{"1"});
        HarvestStatus.processRequest(new TestPageContext(servletRequest),
                                     I18N);
        assertEquals("Should have generated one new job",
                     origJobs + 1, jobDAO.getCountJobs());
        assertEquals("Old job should have status resubmitted",
                     JobStatus.RESUBMITTED, jobDAO.read(1L).getStatus());
        assertEquals("New job should have status new",
                     JobStatus.NEW, jobDAO.read(2L).getStatus());

    }
    
    public void testGetjobStatusList () throws Exception {
    	List<JobStatusInfo> l = HarvestStatus.getjobStatusList(-1,"ASC");
    	assertEquals("Number of jobs should be 0", 0, l.size());

    	l = HarvestStatus.getjobStatusList(0,"ASC");
    	assertEquals("Number of jobs should be 0", 0, l.size());

        try {
        	HarvestStatus.getjobStatusList(-2,"DESC");
            fail("Should have forwarded me to ArgumentNotValid for job status.");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
        	HarvestStatus.getjobStatusList(0,"XX");
           fail("Should have forwarded me to ArgumentNotValid for sort oder.");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
        	HarvestStatus.getjobStatusList(0,"");
           fail("Should have forwarded me to ArgumentNotValid for sort oder.");
        } catch (ArgumentNotValid e) {
            //Expected
        }
    }
    
    public void testGetSelectedSortOrder () throws Exception {
    	TestServletRequest servletRequest = new TestServletRequest();
    	TestPageContext context = new TestPageContext(servletRequest);
        
    	DefaultedRequest dfltRequest =
    	        new HarvestStatus.DefaultedRequest(servletRequest);
        String s;
        try {
            s = HarvestStatus.getSelectedSortOrder(null);
            fail("Should have thrown ANV on null parameter.");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        //check default
        s = HarvestStatus.getSelectedSortOrder(dfltRequest);
        assertEquals("Expected other default sort order",
         		HarvestStatus.DEFAULT_SORTORDER, s);

        //check error on faulty parameter
        Map<String, String[]> parms = new HashMap<String, String[]>();
        parms.put(Constants.JOBIDORDER_PARAM, new String[]{"XX"});
        servletRequest.setParameterMap(parms);
        context = new TestPageContext(servletRequest);
        dfltRequest = new HarvestStatus.DefaultedRequest(servletRequest);
        try {
            s = HarvestStatus.getSelectedSortOrder(dfltRequest);
            fail("Should have forwarded me to an error page on wrong order parameter.");
        } catch (ArgumentNotValid e) {
           //Expected
        }

        //check set order parameter
        parms = new HashMap<String, String[]>();
        parms.put(Constants.JOBIDORDER_PARAM, new String[]{HarvestStatus.SORTORDER_DESCENDING});
        servletRequest.setParameterMap(parms);
        context = new TestPageContext(servletRequest);
        dfltRequest = new HarvestStatus.DefaultedRequest(servletRequest);
        s = HarvestStatus.getSelectedSortOrder(dfltRequest);
        assertEquals("Expected descemnding sort order",
        		HarvestStatus.SORTORDER_DESCENDING, s);
    }
    
    public void testGetSelectedJobStatusCode() throws Exception {
    	TestServletRequest servletRequest = new TestServletRequest();
    	TestPageContext context = new TestPageContext(servletRequest);
        
    	DefaultedRequest dfltRequest =
    	        new HarvestStatus.DefaultedRequest(servletRequest);
        int i;
        try {
            i = HarvestStatus.getSelectedJobStatusCode(null);
            fail("Should have thrown ANV on null parameter.");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        //check default
        i = HarvestStatus.getSelectedJobStatusCode(dfltRequest);
        assertEquals("Expected other default sort order",
         		HarvestStatus.DEFAULT_JOBSTATUS, JobStatus.fromOrdinal(i).name());

        //check error on faulty parameter
        Map<String, String[]> parms = new HashMap<String, String[]>();
        parms.put(Constants.JOBSTATUS_PARAM, new String[]{"XX"});
        servletRequest.setParameterMap(parms);
        context = new TestPageContext(servletRequest);
        dfltRequest = new HarvestStatus.DefaultedRequest(servletRequest);
        try {
            i = HarvestStatus.getSelectedJobStatusCode(dfltRequest);
            fail("Should have forwarded me to an error page on wrong order parameter.");
        } catch (IllegalArgumentException e) {
           //Expected
        }

        //check set order parameter
        parms = new HashMap<String, String[]>();
        parms.put(Constants.JOBSTATUS_PARAM, new String[]{JobStatus.FAILED.name()});
        servletRequest.setParameterMap(parms);
        context = new TestPageContext(servletRequest);
        dfltRequest = new HarvestStatus.DefaultedRequest(servletRequest);
        i = HarvestStatus.getSelectedJobStatusCode(dfltRequest);
        assertEquals("Expected failed job status for selection",
        		JobStatus.FAILED.name(), JobStatus.fromOrdinal(i).name());
    	
    }
}