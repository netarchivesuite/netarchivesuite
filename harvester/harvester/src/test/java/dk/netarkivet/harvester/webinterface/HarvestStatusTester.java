/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobDAOTester;
import dk.netarkivet.harvester.datamodel.JobDBDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;

/**
 * Test of Harvest Status utility method for resubmitting jobs.
 */
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
        
        JobDAO jobDAO = JobDBDAO.getInstance();
        DataModelTestCase.addHarvestDefinitionToDatabaseWithId(42L);
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
                    (I18n) null);
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
        
        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put(
        		HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(), 
        		new String[]{"ASC"});
        List<JobStatusInfo> l = HarvestStatus.getjobStatusList(
        		getTestQuery(params)).getJobStatusInfo();
    	assertEquals("Number of jobs should be 0", 0, l.size());

    	Set<Integer> testStatuscodeSet = new HashSet<Integer>();
    	testStatuscodeSet.add(0);
    	
    	try {
    		params.clear();
        	params.put(
            		HarvestStatusQuery.UI_FIELD.JOB_STATUS.name(), 
            		new String[]{ "bogus" });
        	l = HarvestStatus.getjobStatusList(getTestQuery(params)).getJobStatusInfo();
            fail("Should have forwarded me to ArgumentNotValid for wrong job status.");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
        	params.clear();
        	params.put(
            		HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(), 
            		new String[]{ "XX" });
        	l = HarvestStatus.getjobStatusList(getTestQuery(params)).getJobStatusInfo();
        	fail("Should have forwarded me to ArgumentNotValid for unknown sort order.");
        } catch (ArgumentNotValid e) {
            //Expected
        }
    }
    
    public void testGetSelectedSortOrder () throws Exception {
    	TestServletRequest servletRequest = new TestServletRequest();
        
    	HarvestStatusQuery query = new HarvestStatusQuery(servletRequest);
        
        //check default
        assertTrue(
        		"Expected other default sort order", query.isSortAscending());

        //check error on faulty parameter
        Map<String, String[]> parms = new HashMap<String, String[]>();
        parms.put(
        		HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(), 
        		new String[]{"XX"});
        servletRequest.setParameterMap(parms);
        
        try {
        	query = new HarvestStatusQuery(servletRequest);
            fail("Should have forwarded me to an error page on wrong order parameter.");
        } catch (ArgumentNotValid e) {
           //Expected
        }

        //check set order parameter
        parms = new HashMap<String, String[]>();
        parms.put(
        		HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(), 
        		new String[]{
        			HarvestStatusQuery.SORT_ORDER.DESC.name()}
        		);
        servletRequest.setParameterMap(parms);
        query = new HarvestStatusQuery(servletRequest);
        assertFalse(
        		"Expected descending sort order",
        		query.isSortAscending());
    }
    
    public void testGetSelectedJobStatusCode() throws Exception {
    	TestServletRequest servletRequest = new TestServletRequest();
        
    	HarvestStatusQuery query = new HarvestStatusQuery(servletRequest);
        
        //check default
    	JobStatus[] selectedStatuses = query.getSelectedJobStatuses();
        assertTrue("Only one statuscode should have selected", 
        		selectedStatuses.length == 1);
        
        assertTrue("Expected other default sort order",
         		query.isSortAscending());

        //check error on faulty parameter
        Map<String, String[]> parms = new HashMap<String, String[]>();
        parms.put(
        		HarvestStatusQuery.UI_FIELD.JOB_ID_ORDER.name(), 
        		new String[]{"XX"});
        servletRequest.setParameterMap(parms);
        
        try {
        	query = new HarvestStatusQuery(servletRequest);
            fail("Should have forwarded me to an error page on wrong order parameter.");
        } catch (IllegalArgumentException e) {
           //Expected
        }

        //check set order parameter
        parms = new HashMap<String, String[]>();
        parms.put(
        		HarvestStatusQuery.UI_FIELD.JOB_STATUS.name(), 
        		new String[]{JobStatus.FAILED.name()});
        servletRequest.setParameterMap(parms);
        query = new HarvestStatusQuery(servletRequest);
        assertEquals("Only one statuscode should have selected", 
                query.getSelectedJobStatuses().length, 1);
        assertEquals("Expected failed job status for selection",
        		JobStatus.FAILED.name(),
        		query.getSelectedJobStatuses()[0].name());
    	
    }
    
    public static HarvestStatusQuery getTestQuery(
    		Map<String, String[]> params) {
    	TestServletRequest req = new TestServletRequest();        
        req.setParameterMap(params);
        return new HarvestStatusQuery(req);
    }
    
}