/*
 * #%L
 * Netarchivesuite - harvester
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

import java.util.List;

import javax.servlet.jsp.PageContext;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery.UI_FIELD;


/**
 * This page provides support for the HarvestStatus pages of the web interface.
 */
public class HarvestStatus {

    /** The logger to use. */
    //protected static final Log log = LogFactory.getLog(HarvestStatus.class.getName());
    protected static final Logger log = LoggerFactory.getLogger(HarvestStatus.class);

    /** The total number in the full resultset. */
    private final long fullResultsCount;

    /** The list of jobs in this HarvestStatus object. */
    private final List<JobStatusInfo> jobs;

    /**
     * Constructor for the HarvestStatus class.
     *
     * @param fullResultsCount The total number of entries in the full resultset
     * @param jobs The list of jobs
     */
    public HarvestStatus(long fullResultsCount, List<JobStatusInfo> jobs) {
        this.fullResultsCount = fullResultsCount;
        this.jobs = jobs;
    }

    /**
     * @return The total number in the full resultset
     */
    public long getFullResultsCount() {
        return fullResultsCount;
    }

    /**
     * @return The list of jobs in this HarvestStatus object.
     */
    public List<JobStatusInfo> getJobStatusInfo() {
        return jobs;
    }

    /**
     * Process a request from Harveststatus-alljobs.
     * <p>
     * Will resubmit a job if requested, otherwise do nothing.
     *
     * @param context The web context used for processing
     * @param i18n The resource i18n context.
     * @throws ForwardedToErrorPage If an error occurs that stops processing and forwards the user to an error page.
     */
    public static void processRequest(PageContext context, I18n i18n) throws ForwardedToErrorPage {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        // Check if it's a multiple resubmit query
        String resubmitJobIds = UI_FIELD.RESUBMIT_JOB_IDS.getValue(context.getRequest());
        Long resubmitJobID = HTMLUtils.parseOptionalLong(context, Constants.JOB_RESUBMIT_PARAM, null);
        Long rejectJobID = HTMLUtils.parseOptionalLong(context, Constants.JOB_REJECT_PARAM, null);
        Long unrejectJobID = HTMLUtils.parseOptionalLong(context, Constants.JOB_UNREJECT_PARAM, null);
        if (!resubmitJobIds.isEmpty()) {
            String[] ids = resubmitJobIds.split(";");
            for (String idStr : ids) {
                resubmitJob(context, i18n, Long.parseLong(idStr));
            }
        } else if (resubmitJobID != null) {
            resubmitJob(context, i18n, resubmitJobID);
        } else if (rejectJobID != null) {
            rejectFailedJob(context, i18n, rejectJobID);
        } else if (unrejectJobID != null) {
            unrejectRejectedJob(context, i18n, unrejectJobID);
        }
    }

    /**
     * Marks a failed job as rejected for resubmission. Throws a ForwardedToErrorPage if jobID is null or if it refers
     * to a job that is not in the state FAILED to start with.
     *
     * @param context the context for forwarding errors
     * @param i18n the internationalisation to use
     * @param jobID the job to reject
     */
    public static void rejectFailedJob(PageContext context, I18n i18n, Long jobID) {
        try {
            Job job = JobDAO.getInstance().read(jobID);
            if (!job.getStatus().equals(JobStatus.FAILED)) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;job.unable.to.reject", jobID);
                throw new ForwardedToErrorPage("Cannot reject job in status " + job.getStatus());
            }
            job.setStatus(JobStatus.FAILED_REJECTED);
            JobDAO.getInstance().update(job);
        } catch (ArgumentNotValid argumentNotValid) {
            HTMLUtils.forwardOnEmptyParameter(context, "jobID");
            throw new ForwardedToErrorPage("jobID parameter is null");
        } catch (UnknownID unknownID) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;job.unknown.id.0", jobID);
            throw new ForwardedToErrorPage("Job " + jobID + " not found");
        } catch (IOFailure ioFailure) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, ioFailure, "errormsg;job.unable.to.reject", jobID);
            throw new ForwardedToErrorPage("Error resubmitting job " + jobID);
        }

    }

    /**
     * Marks as failed. Throws a ForwardedToErrorPage if the job is not in the state FAILED_REJECTED to start with.
     *
     * @param context the context for forwarding errors
     * @param i18n the internationalisation to use
     * @param jobID the job to unreject
     */
    public static void unrejectRejectedJob(PageContext context, I18n i18n, Long jobID) {
        try {
            Job job = JobDAO.getInstance().read(jobID);
            if (!job.getStatus().equals(JobStatus.FAILED_REJECTED)) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;job.unable.to.reject", jobID);
                throw new ForwardedToErrorPage("Cannot unreject job in status " + job.getStatus());
            }
            job.setStatus(JobStatus.FAILED);
            JobDAO.getInstance().update(job);
        } catch (ArgumentNotValid argumentNotValid) {
            HTMLUtils.forwardOnEmptyParameter(context, "jobID");
            throw new ForwardedToErrorPage("jobID parameter is null");
        } catch (UnknownID unknownID) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;job.unknown.id.0", jobID);
            throw new ForwardedToErrorPage("Job " + jobID + " not found");
        } catch (IOFailure ioFailure) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, ioFailure, "errormsg;job.unable.to.reject", jobID);
            throw new ForwardedToErrorPage("Error resubmitting job " + jobID);
        }
    }

    /**
     * Helpermethod to resubmit a job with a given jobID.
     *
     * @param context the current pageContext (used in error-handling only)
     * @param i18n the given internalisation object.
     * @param jobID The ID for the job that we want to resubmit.
     */
    private static void resubmitJob(PageContext context, I18n i18n, Long jobID) {
        try {
            JobDAO.getInstance().rescheduleJob(jobID);
        } catch (UnknownID e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;job.unknown.id.0", jobID);
            throw new ForwardedToErrorPage("Job " + jobID + " not found");
        } catch (IOFailure e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, e, "errormsg;job.unable.to.resubmit.id.0", jobID);
            throw new ForwardedToErrorPage("Error resubmitting job " + jobID);
        }
    }

    /**
     * Create a link to the harvest-run page for a given run.
     *
     * @param harvestID The ID of the harvest
     * @param harvestRun The run # of the harvest (always 0 for snapshots)
     * @return A properly encoded HTML string with a link and the harvest run as the text. Select all jobs to be shown.
     */
    public static String makeHarvestRunLink(long harvestID, int harvestRun) {
        ArgumentNotValid.checkNotNegative(harvestID, "harvestID");
        ArgumentNotValid.checkNotNegative(harvestRun, "harvestRun");
        return "<a href=\"/History/Harveststatus-perharvestrun.jsp?" + HarvestStatusQuery.UI_FIELD.HARVEST_ID.name()
                + "=" + harvestID + "&amp;" + Constants.HARVEST_NUM_PARAM + "=" + harvestRun + "&amp;"
                + HarvestStatusQuery.UI_FIELD.JOB_STATUS.name() + "=" + HarvestStatusQuery.JOBSTATUS_ALL + "\">"
                + harvestRun + "</a>";
    }

    /**
     * Calculate list of job information to be shown.
     *
     * @param query the query with its filters.
     * @return a list of job status info objects
     */
    public static HarvestStatus getjobStatusList(HarvestStatusQuery query) {
        log.debug("Getting a jobstatuslist based on the current query. ");
        return JobDAO.getInstance().getStatusInfo(query);
    }

    /**
     * Check if next link is active.
     *
     * @param pageSize the size of the page
     * @param totalResultsCount the number of results.
     * @param endIndex the index of the last result shown on this page
     * @return true, if link to next page is active
     */
    public static boolean isNextLinkActive(long pageSize, long totalResultsCount, long endIndex) {
        if (pageSize != 0 && totalResultsCount > 0 && endIndex < totalResultsCount) {
            return true;
        }
        return false;
    }

    /**
     * Check if previous link is active.
     *
     * @param pageSize the size of the page
     * @param totalResultsCount the number of results.
     * @param startIndex the index of the first result shown on this page
     * @return true, if link to previous page is active
     */
    public static boolean isPreviousLinkActive(long pageSize, long totalResultsCount, long startIndex) {
        if (pageSize != 0 && totalResultsCount > 0 && startIndex > 1) {
            return true;
        }
        return false;
    }
}
