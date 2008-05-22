/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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

import java.util.Iterator;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Interface for creating and accessing jobs in persistent storage.
 */
public abstract class JobDAO implements Iterable<Job> {
    /** The database singleton model. */
    private static JobDAO instance;

    /**
     * constructor used when creating singleton. Do not call directly.
     */
    protected JobDAO() {
    }


    /**
     * Gets the JobDAO singleton.
     *
     * @return the JobDAO singleton
     */
    public static synchronized JobDAO getInstance() {
        if (instance == null) {
            instance = new JobDBDAO();
        }
        return instance;
    }

    /**
     * Creates an instance in persistent storage of the given job.
     * If the job doesn't have an ID, one is generated for it.
     *
     * @param job
     * @throws PermissionDenied If a job already exists in persistent storage
     *                          with id of the given job
     * @throws IOFailure        If some IOException occurs while
     *                          writing the job
     */
    public abstract void create(Job job);

    /** Check whether a particular job exists.
     *
     * @param jobID Id of the job.
     * @return true if the job exists in any state.
     */
    public abstract boolean exists(Long jobID);

    /**
     * Generates the next id of job.
     *
     * @return id
     */
    abstract Long generateNextID();

    /**
     * Returns the number of jobs existing.
     *
     * @return Number of jobs in jobs dir
     */
    public abstract int getCountJobs();

    /**
     * Reads a job from persistent storage.
     *
     * @param jobID The ID of the job to read
     * @return a Job instance
     * @throws ArgumentNotValid If failed to create job instance
     *                          (in case configuration or priority is null or harvestID is invalid.
     * @throws UnknownID        If the job with the given jobID
     *                          does not exist in persistent storage.
     * @throws IOFailure        If the loaded ID of job does not match the expected.
     */
    public abstract Job read(Long jobID)
            throws ArgumentNotValid, UnknownID, IOFailure;

    /**
     * Update a Job in persistent storage.
     *
     * @param job The Job to update
     * @throws ArgumentNotValid If the Job is null
     * @throws UnknownID If the Job doesn't exist in the DAO
     * @throws IOFailure If writing the job to persistent storage fails
     * @throws PermissionDenied If the job has been updated behind our backs
     */
    public abstract void update(Job job) throws IOFailure;

    /**
     * Reset the DAO instance.  Only for use from within tests.
     */
    public static void reset() {
        instance = null;
    }

    /**
     * Return a list of all jobs with the given status.
     *
     * @param status A given status.
     * @return A list of all job with given status
     * @throws ArgumentNotValid If the given status is not one of the five valid statuses,
     *                          specified in Job.
     */
    public abstract Iterator<Job> getAll(JobStatus status);

    /**
     * Return a list of all job_id's representing jobs with the given status.
     *
     * @param status A given status.
     * @return A list of all job_id's representing jobs with given status
     * @throws ArgumentNotValid If the given status is not one of the five valid statuses,
     *                          specified in Job.
     */
    public abstract Iterator<Long> getAllJobIds(JobStatus status);

    /**
     * Return a list of all jobs .
     *
     * @return A list of all jobs
     */
    public abstract Iterator<Job> getAll();

    /** Gets an iterator of all jobs.
     * Implements the Iterable interface.
     *
     * @return Iterator of all jobs, regardless of status.
     */
    public Iterator<Job> iterator() {
        return getAll();
    }

    /**
     * Return a list of all job_ids .
     *
     * @return A list of all job_ids
     */
    public abstract Iterator<Long> getAllJobIds();

    /** Return status information for all jobs.
     *
     * @return A list of status objects with the pertinent information for
     * all jobs.
     */
    public abstract List<JobStatusInfo> getStatusInfo();

    /** Return status information for all jobs for a given harvest definition.
     *
     * @param harvestId The ID of a harvest definition.
     * @param numEvent The harvest run number
     * @return A list of status objects with the pertinent information for
     *         all jobs for a given harvest definition.
     * @throws IOFailure on trouble in database access
     */
    public abstract List<JobStatusInfo> getStatusInfo(long harvestId,
                                                      long numEvent);

    /** Return status information for all jobs in given job id order.
     *
     * @param asc True if result must be given in ascending order, false
     *        if result must be given in descending order
     * @return A list of status objects with the pertinent information for 
     *         all jobs with given job status.
     * @throws IOFailure on trouble in database access
     */
    public abstract List<JobStatusInfo> getStatusInfo(boolean asc);

    /** Return status information for all jobs with given job status.
     *
     * @param status The status asked for.
     * @param asc True if result must be given in ascending order, false
     *        if result must be given in descending order
     * @return A list of status objects with the pertinent information for 
     *         all jobs with given job status and in given job id order.
     * @throws IOFailure on trouble in database access
     */
    public abstract List<JobStatusInfo> getStatusInfo(JobStatus status, boolean asc);

    /** Return status information for all jobs with given job status.
     *
     * @param status The status asked for.
     * @return A list of status objects with the pertinent information for 
     *         all jobs with given job status.
     * @throws IOFailure on trouble in database access
     */
    public abstract List<JobStatusInfo> getStatusInfo(JobStatus status);

    /** Calculate all jobIDs to use for duplication reduction.
     *
     * More precisely, this method calculates the following:
     * If the job ID corresponds to a partial harvest, all jobIDs from the
     * previous scheduled harvest are returned, or the empty list if this
     * harvest hasn't been scheduled before.
     *
     * If the job ID corresponds to a full harvest, the entire chain of harvests
     * this is based on is returned, and all jobIDs from the previous chain
     * of full harvests is returned.
     *
     * @param jobID The job ID to find duplicate reduction data for.
     * @return A list of job IDs (possibly empty) of potential previous harvests
     * of this job, to use for duplicate reduction.
     * @throws UnknownID if job ID is unknown
     * @throws IOFailure on trouble getting ids from metadata storage
     */
    public abstract List<Long> getJobIDsForDuplicateReduction(long jobID)
            throws UnknownID;

    /** Reschedule a job by creating a new job (in status NEW) and setting the
     * old job to status RESUBMITTED.
     *
     * Notice the slightly confusing naming: The only job is marked RESUBMITTED,
     * but the new job is not really submitted, that happens in a separate
     * stage, the new job is in status NEW.
     *
     * @param oldJobID ID of a job to reschedule
     * @return ID of the newly created job
     * @throws UnknownID if no job exists with id jobID
     * @throws IllegalState if the job with id jobID is not SUBMITTED or FAILED.
     */
    public abstract long rescheduleJob(long oldJobID);
}
