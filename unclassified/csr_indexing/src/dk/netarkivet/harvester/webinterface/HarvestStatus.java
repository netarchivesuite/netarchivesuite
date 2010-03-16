/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.webinterface;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;

/**
 * This page provides support for the HarvestStatus pages of the web interface.
 */

public class HarvestStatus {
    /** Parameters used by the Harveststatus-alljobs.jsp page. */
    public static final String[] DEFAULTABLE_PARAMETERS = new String[]{
        Constants.JOBSTATUS_PARAM,
        Constants.JOBIDORDER_PARAM
    };

    /** Ascending sort order for job id. */
    public static final String SORTORDER_ASCENDING = "ASC";
    /** Descending sort order for job id. */
    public static final String SORTORDER_DESCENDING = "DESC";

    /** The String code to select all states. */
    public static final String JOBSTATUS_ALL = "ALL";

    /** Default sortorder (ascending). */
    public static final String DEFAULT_SORTORDER = SORTORDER_ASCENDING;
    /** Default Jobstatus (STARTED). */
    public static final String DEFAULT_JOBSTATUS = JobStatus.STARTED.name();

    /**
     * Process a request from Harveststatus-alljobs.
     *
     * Will resubmit a job if requested, otherwise do nothing.
     *
     * @param context The web context used for processing
     * @param i18n The resource i18n context.
     * @throws ForwardedToErrorPage If an error occurs that stops processing
     * and forwards the user to an error page.
     */
    public static void processRequest(PageContext context, I18n i18n)
            throws ForwardedToErrorPage {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        Long jobID = HTMLUtils.parseOptionalLong(context,
                Constants.JOB_RESUBMIT_PARAM, null);
        if ((jobID != null)) {
            try {
                JobDAO.getInstance().rescheduleJob(jobID);
            } catch (UnknownID e) {
                HTMLUtils.forwardWithErrorMessage(context, i18n,
                        "errormsg;job.unknown.id.0", jobID);
                throw new ForwardedToErrorPage("Job " + jobID + " not found");
            } catch (IOFailure e) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, e,
                        "errormsg;job.unable.to.resubmit.id.0", jobID);
                throw new ForwardedToErrorPage("Error resubmitting job "
                        + jobID);
            }
        }
    }

    /** Create a link to the harvest-run page for a given run.
     *
     * @param harvestID The ID of the harvest
     * @param harvestRun The run # of the harvest (always 0 for snapshots)
     * @return A properly encoded HTML string with a link and the harvest run
     * as the text. Select all jobs to be shown.
     */
    public static String makeHarvestRunLink(long harvestID, int harvestRun) {
        ArgumentNotValid.checkNotNegative(harvestID, "harvestID");
        ArgumentNotValid.checkNotNegative(harvestRun, "harvestRun");
        return "<a href=\"/History/Harveststatus-perharvestrun.jsp?harvestID="
                + harvestID + "&amp;" + Constants.HARVEST_NUM_PARAM
                + "=" + harvestRun 
                + "&amp;" + Constants.JOBSTATUS_PARAM + "="
                + HarvestStatus.JOBSTATUS_ALL + "\">"
                + harvestRun + "</a>";
    }

    /** Find Job status to be shown based on parameters, including possibility
     *  for All statuses.
     *
     * @param dfltRequest contains defaulted parameters
     * @return Integer value being the ordinal of a JobStatus or -1 for ALL 
     *         (job statuses)
     * @throws ArgumentNotValid, IllegalArgumentException
     */
    public static Set<Integer> getSelectedJobStatusCodes(
            DefaultedRequest dfltRequest) {
        ArgumentNotValid.checkNotNull(
                dfltRequest, "DefaultedRequest dfltRequest");
        String[] values = dfltRequest.getParameter(Constants.JOBSTATUS_PARAM);
        if (values == null || values.length == 0) {
            throw new ArgumentNotValid("No Jobstatus selected");
        }
        Set<Integer> selectedJobStatusCodesSet = new HashSet<Integer>();
        for (String value: values) {
            if (value.equals(JOBSTATUS_ALL)) {
                selectedJobStatusCodesSet = new HashSet<Integer>();
                selectedJobStatusCodesSet.add(JobStatus.ALL_STATUS_CODE);
                break;
            } else {
                selectedJobStatusCodesSet.add(
                        JobStatus.valueOf(value).ordinal());
            }
        }
        return selectedJobStatusCodesSet;
    }

    /** Find sort order of job ids to be shown based on parameters.
     *
     * @param dfltRequest contains defaulted parameters
     * @return String constant for selected order
     * @throws ArgumentNotValid
     */
    public static String getSelectedSortOrder(DefaultedRequest dfltRequest) {
        ArgumentNotValid.checkNotNull(dfltRequest, "dfltRequest");
        // Only one JOBIDORDER value allowed at any one time.
        String[] selectedSortOrders = dfltRequest.getParameter(
                Constants.JOBIDORDER_PARAM);
        if (selectedSortOrders.length != 1) {
            throw new ArgumentNotValid(
                    "Multiple values for order parameter selected: " 
                    + StringUtils.conjoin(",", selectedSortOrders)); 
        }
        String s = selectedSortOrders[0]; 

        if (s.equals(SORTORDER_ASCENDING) || s.equals(SORTORDER_DESCENDING)) {
            return s; 
        } else {
            throw new ArgumentNotValid("Invalid order parameter " + s); 
        }       
    }

    /** Calculate list of job information to be shown.
     *
     * @param selectedJobStatusCodes integer codes for job statuses to be shown
     * @param selectedSortOrder string code whether job ids should come in asc.
     *        or desc. order
     * @return list of job (status) information to be shown
     * @throws ArgumentNotValid
     */
    public static List<JobStatusInfo> getjobStatusList(
            Set<Integer> selectedJobStatusCodes, String selectedSortOrder) {
        ArgumentNotValid.checkNotNullOrEmpty(selectedJobStatusCodes, 
                "selectedJobStatusCodes");
        ArgumentNotValid.checkNotNullOrEmpty(selectedSortOrder,
                "selectedSortOrder");
        
        boolean asc = selectedSortOrder.equals(SORTORDER_ASCENDING);
        if (!asc && !selectedSortOrder.equals(SORTORDER_DESCENDING)) { 
            throw new ArgumentNotValid(
                         "Invalid sort Order " + selectedSortOrder
                      );
        }
        
        if (selectedJobStatusCodes.contains(
                new Integer(JobStatus.ALL_STATUS_CODE))) { 
            return JobDAO.getInstance().getStatusInfo(asc); 
        } else {
            List<JobStatus> selectedJobStates = new ArrayList<JobStatus>();
            for (Integer jobcode : selectedJobStatusCodes) {
                JobStatus sortJobStatus = 
                    JobStatus.fromOrdinal(jobcode);
                selectedJobStates.add(sortJobStatus);
            }
            
            JobStatus[] jobstatusArray
                = new JobStatus[selectedJobStates.size()];
            return JobDAO.getInstance().getStatusInfo(
                    asc, selectedJobStates.toArray(jobstatusArray)); 
        }
    }
    
    
    /** Calculate list of job information to be shown.
    *
    * @param harvestId Select only jobs generated from this harvestdefinition
    * @param harvestNum Select only jobs with this harvestNumber.
    * @param selectedJobStatusCodes integer codes for job statuses to be shown
    * @param selectedSortOrder string code whether job ids should come in asc.
    *        or desc. order
    * @return list of job (status) information to be shown
    * @throws ArgumentNotValid If some of the arguments are invalid
    */
   public static List<JobStatusInfo> getjobStatusList(
                                          long harvestId,
                                          long harvestNum,
                                          Set<Integer> selectedJobStatusCodes, 
                                          String selectedSortOrder
                                     ) {
       ArgumentNotValid.checkNotNullOrEmpty(selectedJobStatusCodes, 
               "selectedJobStatusCodes");
       ArgumentNotValid.checkNotNullOrEmpty(selectedSortOrder,
               "selectedSortOrder");
       ArgumentNotValid.checkNotNegative(harvestId, "long harvestId");
       ArgumentNotValid.checkNotNegative(harvestNum, "long harvestNum");

       boolean asc = selectedSortOrder.equals(SORTORDER_ASCENDING);
       if (!asc && !selectedSortOrder.equals(SORTORDER_DESCENDING)) {
           // no logging not considered necessary at this time
           throw new ArgumentNotValid("Invalid sort Order: "
                   + selectedSortOrder);
       }
       
       if (selectedJobStatusCodes.contains(
               new Integer(JobStatus.ALL_STATUS_CODE))) { 
           return JobDAO.getInstance().getStatusInfo(
                   harvestId, harvestNum, asc);
       } else {
           Set<JobStatus> selectedJobStatusSet = new HashSet<JobStatus>();
           for(int jobStatusOrdinal : selectedJobStatusCodes) {
              selectedJobStatusSet.add(JobStatus.fromOrdinal(
                      jobStatusOrdinal)); 
           }
           return JobDAO.getInstance().getStatusInfo(
                   harvestId, harvestNum, 
                   asc, selectedJobStatusSet); 
       }
   }

    /** This class encapsulates a request for reload, making non-existing
     * parameters appear as there default value.
     */
    public static class DefaultedRequest {
        /** the encapsulated servlet request. */
        ServletRequest req;

        /** Constructor.
         * @param req with parameters to jsp page which can be defaulted
         * @throws ArgumentNotValid
         */
        public DefaultedRequest(ServletRequest req) {
            ArgumentNotValid.checkNotNull(req, "ServletRequest req");
            this.req = req;
        }
        
        /** Gets a parameter from the original request, except if the
         * parameter is unset, return the default value for the parameter.
         * @param paramName a parameter
         * @return The parameter or the default value; never null or empty.
         * @throws ArgumentNotValid
         */
        public String[] getParameter(String paramName) {
            ArgumentNotValid.checkNotNullOrEmpty(paramName, "paramName");
            String[] values = req.getParameterValues(paramName);
            if (values == null || values.length == 0) {
                if (paramName.equals(Constants.JOBSTATUS_PARAM)) {
                    return new String[]{DEFAULT_JOBSTATUS};
                } else {
                    if (paramName.equals(Constants.JOBIDORDER_PARAM)) {
                        return new String[]{DEFAULT_SORTORDER};
                    } else {
                        // no logging not considered necessary at this time
                        throw new ArgumentNotValid(
                                     "Invalid parameter name '" + paramName
                                     + "'");
                    }
                } 
            } else {
                return values;  
            }
        }
    }
}
