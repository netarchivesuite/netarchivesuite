/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;

/**
 * This page provides support for the HarvestStatus pages of the web interface.
 */

public class HarvestStatus {
    /* parameters used by Harveststatus-alljobs.jsp */
    public static final String[] DEFAULTABLE_PARAMETERS = new String[]{
    	Constants.JOBSTATUS_PARAM,
    	Constants.JOBIDORDER_PARAM
    };

    //Sort order constants for job id
    public static final String SORTORDER_ASCENDING = "ASC";
    public static final String SORTORDER_DESCENDING = "DESC";

    //String code for choice of all statuses
    public static final String JOBSTATUS_ALL = "ALL";

    //Default values for display parameters
    public static final String DEFAULT_SORTORDER = SORTORDER_ASCENDING;
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

    /** Create a link to the harvest-run page for a given run
     *
     * @param harvestID The ID of the harvest
     * @param harvestRun The run # of the harvest (always 0 for snapshots)
     * @return A properly encoded HTML string with a link and the harvest run
     * as the text.
     */
    public static String makeHarvestRunLink(long harvestID, int harvestRun) {
        ArgumentNotValid.checkNotNegative(harvestID, "harvestID");
        ArgumentNotValid.checkNotNegative(harvestRun, "harvestRun");
        return "<a href=\"/History/Harveststatus-perharvestrun.jsp?harvestID="
                + harvestID + "&amp;harvestNum=" + harvestRun + "\">"
                + harvestRun + "</a>";
    }

    /** Find Job status to be shown based on parameters, including possibility
     *  for All statuses
     *
     * @param dfltRequest contains defaulted parameters
     * @return Integer value being the ordinal of a JobStatus or -1 for ALL 
     *         (job statuses)
     * @throws ArgumentNotValid, IllegalArgumentException
     */
    public static int getSelectedJobStatusCode(DefaultedRequest dfltRequest) {
        ArgumentNotValid.checkNotNull(dfltRequest, "dfltRequest");

        String s = dfltRequest.getParameter(Constants.JOBSTATUS_PARAM); 
       
        if (s.equals(JOBSTATUS_ALL)) { 
        	return -1; 
        } else {
        	return JobStatus.valueOf(s).ordinal();
        }       
    }

    /** Find sort order of job ids to be shown based on parameters
     *
     * @param dfltRequest contains defaulted parameters
     * @return String constant for selected order
     * @throws ArgumentNotValid
     */
    public static String getSelectedSortOrder(DefaultedRequest dfltRequest) {
        ArgumentNotValid.checkNotNull(dfltRequest, "dfltRequest");

        String s = dfltRequest.getParameter(Constants.JOBIDORDER_PARAM); 

        if (s.equals(SORTORDER_ASCENDING) || s.equals(SORTORDER_DESCENDING)) {
            return s; 
        } else {
            throw new ArgumentNotValid("Invalid order parameter " + s); 
        }       
    }

    /** Calculate list of job information to be shown
     *
     * @param selectedJobStatusCode integer code for job statuses to be shown
     * @param selectedSortOrder string code whether job ids should come in asc.
     *        or desc. order
     * @return list of job (status) information to be shown
     * @throws ArgumentNotValid
     */
    public static List<JobStatusInfo> getjobStatusList(
    		                             int selectedJobStatusCode, 
    		                             String selectedSortOrder
    		                          ) {
        boolean asc = selectedSortOrder.equals(SORTORDER_ASCENDING);
        if (!asc && !selectedSortOrder.equals(SORTORDER_DESCENDING)) { 
            throw new ArgumentNotValid(
                         "Invalid sort Order " + selectedSortOrder
                      );
        }
        if (selectedJobStatusCode==-1) { 
            return JobDAO.getInstance().getStatusInfo(asc); 
        } else {
            JobStatus sortJobStatus = 
                JobStatus.fromOrdinal(selectedJobStatusCode);
            return JobDAO.getInstance().getStatusInfo(sortJobStatus, asc); 
        }
    }

    /** This class encapsulates a request for reload, making non-existing
     * parameters appear as there default value.
     */
    public static class DefaultedRequest {
        ServletRequest req;

        /** Constructor to 
         * @param req with parameters to jsp page which can be defaulted
         * @throws ArgumentNotValid
         */
        public DefaultedRequest(ServletRequest req) {
            ArgumentNotValid.checkNotNull(req, "ServletRequest req");
            this.req = req;
        }
        
        /** Gets a parameter from the original request, except if the
         * parameter is unset, return the default value for the parameter
         * @param paramName a parameter
         * @return The parameter or the default value; never null.
         * @throws ArgumentNotValid
         */
        public String getParameter(String paramName) {
            ArgumentNotValid.checkNotNull(paramName, "paramName");
            String value = req.getParameter(paramName);
            if (value == null || value.length() == 0) {
                if (paramName.equals(Constants.JOBSTATUS_PARAM)) {
                    return DEFAULT_JOBSTATUS;
                } else {
                    if (paramName.equals(Constants.JOBIDORDER_PARAM)) {
                        return DEFAULT_SORTORDER;
                    } else {
                        throw new ArgumentNotValid(
                                     "Invalid parameter name " + paramName
                                  );
                    }
                } 
            } else {
                return value;  
            }
        }
    }
}
    