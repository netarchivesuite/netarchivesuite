/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import java.util.List;

import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery.UI_FIELD;

/**
 * This page provides support for the HarvestStatus pages of the web interface.
 */

public class HarvestStatus {
	
	private long fullResultsCount;
	private List<JobStatusInfo> jobs; 
	
	public HarvestStatus(long fullResultsCount, List<JobStatusInfo> jobs) {
		this.fullResultsCount = fullResultsCount;
		this.jobs = jobs;
	}
	
	public long getFullResultsCount() {
		return fullResultsCount;
	}

	public List<JobStatusInfo> getJobStatusInfo() {
		return jobs;
	}

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
        
        // Check if it's a multiple resubmit query        
        String resubmitJobIds = UI_FIELD.RESUBMIT_JOB_IDS.getValue(
        		context.getRequest());
		if (! resubmitJobIds.isEmpty()) {
			String[] ids = resubmitJobIds.split(";");
			for (String idStr : ids) {
				resubmitJob(context, i18n, Long.parseLong(idStr));
			}
		} else {
			// Might be a single resubmit
			Long jobID = HTMLUtils.parseOptionalLong(context,
					Constants.JOB_RESUBMIT_PARAM, null);
			if ((jobID != null)) {
				resubmitJob(context, i18n, jobID);
			}
		}
        
    }
    
    private static void resubmitJob(PageContext context, I18n i18n, Long jobID) {
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
        return "<a href=\"/History/Harveststatus-perharvestrun.jsp?"
        		+ HarvestStatusQuery.UI_FIELD.HARVEST_ID.name() + "="
                + harvestID + "&amp;" + Constants.HARVEST_NUM_PARAM
                + "=" + harvestRun 
                + "&amp;" + HarvestStatusQuery.UI_FIELD.JOB_STATUS.name() + "="
                + HarvestStatusQuery.JOBSTATUS_ALL + "\">"
                + harvestRun + "</a>";
    }

    /**
     * Calculate list of job information to be shown.
     * @param query the query with its filters.
     * @return a list of job status info objects
     */
    public static HarvestStatus getjobStatusList(
    		HarvestStatusQuery query) {    	
    	return JobDAO.getInstance().getStatusInfo(query);
    }
    
}
