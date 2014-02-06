/* File:        $Id: JobDBDAO.java 2803 2013-10-29 15:42:35Z ngiraud $
 * Revision:    $Revision: 2803 $
 * Author:      $Author: ngiraud $
 * Date:        $Date: 2013-10-29 16:42:35 +0100 (Tue, 29 Oct 2013) $
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

package dk.netarkivet.harvester.dao;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.dao.spec.DBSpecifics;
import dk.netarkivet.harvester.datamodel.Constants;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.webinterface.HarvestStatus;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery.SORT_ORDER;

/**
 * A database-based implementation of the JobDAO class.
 * The statements to create the tables are now in
 * scripts/sql/createfullhddb.sql
 */
public class JobDBDAO extends JobDAO {

	/** The logger for this class. */
	private final Log log = LogFactory.getLog(getClass());

	/**
	 * Create a new JobDAO implemented using database.
	 * This constructor also tries to upgrade the jobs and jobs_configs tables
	 * in the current database.
	 * throws and IllegalState exception, if it is impossible to
	 * make the necessary updates.
	 */
	protected JobDBDAO() {
		super();
	}

	/**
	 * Creates an instance in persistent storage of the given job.
	 * If the job doesn't have an ID, one is generated for it.
	 *
	 * @param job a given job to add to persistent storage
	 * @throws PermissionDenied If a job already exists in persistent storage
	 *                          with the same id as the given job
	 * @throws IOFailure        If some IOException occurs while
	 *                          writing the job to persistent storage
	 */
	public synchronized void create(final Job job) {
		ArgumentNotValid.checkNotNull(job, "Job job");
		// Check that job.getOrigHarvestDefinitionID() refers to
		// existing harvestdefinition
		Long harvestId = job.getOrigHarvestDefinitionID();
		if (!HarvestDefinitionDAO.getInstance().exists(harvestId)) {
			throw new UnknownID("No harvestdefinition with ID=" + harvestId);
		}

		if (job.getJobID() != null) {
			log.warn("The jobId for the job is already set. "
					+ "This should probably never happen.");
		} else {
			job.setJobID(generateNextID());
		}

		if (job.getCreationDate() != null) {
			log.warn("The creation time for the job is already set. "
					+ "This should probably never happen.");
		} else {
			job.setCreationDate(new Date());
		}

		executeTransaction("doCreate", Job.class, job);
	}

	@SuppressWarnings("unused")
	private synchronized void doCreate(final Job job) {
		log.debug("Creating " + job.toString());
		long initialEdition = 1;
		executeUpdate(
				"INSERT INTO jobs (job_id, harvest_id, status, channel, forcemaxcount"
						+ ", forcemaxbytes, forcemaxrunningtime, orderxml, orderxmldoc, seedlist"
						+ ", harvest_num, startdate, enddate, submitteddate, creationdate"
						+ ", num_configs, edition, resubmitted_as_job, harvestname_prefix, snapshot)"
						+ " VALUES (:jobId,:harvestId,:status,:channel,:forceMaxCount,:forceMaxBytes"
						+ ",:forceMaxTime,:orderXmlName,:orderXmlDoc,:seeds,:harvestNum"
						+ ",:startDate,:endDate,:submitted,:created,:numConfigs,:edition,:resubmitId"
						+ ",:prefix,:snapshot)",
						getParameterMap(job, initialEdition));
	}

	/** Create the entries in the job_configs table for this job.
	 * Since some jobs have up to 10000 configs, this must be optimized.
	 * The entries are only created, if job.configsChanged is true.
	 * 
	 * @param job The job to store entries for
	 * @throws SQLException If any problems occur during creation of the
	 * new entries in the job_configs table.
	 */
	private void createJobConfigsEntries(final Job job) {
		if (job.isConfigsChanged()) {
			DBSpecifics specs = DBSpecifics.getInstance();
			String tmpTable = "";
			try {
				executeUpdate(
						"DELETE FROM job_configs WHERE job_id=:id",
						new ParameterMap("id", job.getJobID()));
				tmpTable = specs.getJobConfigsTmpTable();
				final Map<String, String> domainConfigurationMap = job.getDomainConfigurationMap();
				for (Map.Entry <String, String> entry : domainConfigurationMap.entrySet()) {
					executeUpdate(
							"INSERT INTO " + tmpTable + " (domain_name, config_name)"
									+ " VALUES (:domainName, :configName)",
									new ParameterMap(
											"domainName", entry.getKey(),
											"configName", entry.getValue()));
				}

				// Now we have a temp table with all the domains and configs
				int rows = executeUpdate(
						"INSERT INTO job_configs (job_id, config_id)"
								+ " SELECT :id, configurations.config_id"
								+ " FROM domains, configurations, " + tmpTable
								+ " WHERE domains.name=" + tmpTable + ".domain_name"
								+ " AND domains.domain_id=configurations.domain_id"
								+ " AND configurations.name=" + tmpTable + ".config_name",
								new ParameterMap("id", job.getJobID()));
				if (rows != domainConfigurationMap.size()) {
					log.debug("Domain or configuration in table for "
							+ job + " missing: Should have "
							+ domainConfigurationMap.size()
							+ ", got " + rows);
				}
			} finally {
				if (tmpTable != null) {
					specs.dropJobConfigsTmpTable(tmpTable);
				}
				job.setConfigsChanged(false);
			}
		}
	}

	/** Check whether a particular job exists.
	 *
	 * @param jobID Id of the job.
	 * @return true if the job exists in any state.
	 */
	@Override
	public boolean exists(final Long jobID) {
		ArgumentNotValid.checkNotNull(jobID, "Long jobID");
		return 1 == queryLongValue(
				"SELECT COUNT(*) FROM jobs WHERE job_id=:id", 
				new ParameterMap("id", jobID));
	}

	/**
	 * Generates the next id of job.
	 * @param c an open connection to the harvestDatabase
	 * @return id
	 */
	private synchronized Long generateNextID() {
		// Set to zero original, can be set after admin machine breakdown,
		// and the use this as the point of reference.
		Long restoreId = Settings.getLong(Constants.NEXT_JOB_ID);

		Long maxVal = queryLongValue("SELECT MAX(job_id) FROM jobs");
		if (maxVal == null) {
			maxVal = 0L;
		}
		// return the largest number of the two numbers: the NEXT_JOB_ID
		// declared in settings and max value of job_id used
		// in the jobs table.
		return ((restoreId > maxVal) ? restoreId : maxVal + 1L);
	}

	/**
	 * Update a Job in persistent storage.
	 *
	 * @param job The Job to update
	 * @throws ArgumentNotValid If the Job is null
	 * @throws UnknownID If the Job doesn't exist in the DAO
	 * @throws IOFailure If writing the job to persistent storage fails
	 * @throws PermissionDenied If the job has been updated behind our backs
	 */
	@Override
	public synchronized void update(final Job job) {
		ArgumentNotValid.checkNotNull(job, "job");

		final Long jobID = job.getJobID();
		if (!exists(jobID)) {
			throw new UnknownID("Job id " + jobID + " is not known in persistent storage");
		}

		// Not done as a transaction as it's awfully big.
		// TODO Make sure that a failed job update does... what?
		executeTransaction("doUpdate",Job.class, job);
	}

	@SuppressWarnings("unused")
	private synchronized void doUpdate(final Job job) {
		long edition = job.getEdition();
		long nextEdition = edition + 1;
		final int rows = executeUpdate(
				"UPDATE jobs SET harvest_id=:harvestId, status=:status, channel=:channel"
						+ ", forcemaxcount=:forceMaxCount, forcemaxbytes=:forceMaxBytes"
						+ ", forcemaxrunningtime=:forceMaxTime, orderxml=:orderXmlName"
						+ ", orderxmldoc=:orderXmlDoc, seedlist=:seeds, harvest_num=:harvestNum"
						+ ", harvest_errors=:errors, harvest_error_details=:errorDetails"
						+ ", upload_errors=:uploadErrors, upload_error_details=:uploadErrorDetails"
						+ ", startdate=:startDate, enddate=:endDate, num_configs=:numConfigs"
						+ ", edition=:nextEdition, submitteddate=:submitted, creationdate=:created"
						+ ", resubmitted_as_job=:resubmitId, harvestname_prefix=:prefix"
						+ ", snapshot=:snapshot"
						+ " WHERE job_id=:jobId AND edition=:edition",
						new ParameterMap(
								getParameterMap(job, edition), 
								"nextEdition", nextEdition));
		if (rows == 0) {
			String message = "Edition " + edition + " has expired, not updating";
			log.debug(message);
			throw new PermissionDenied(message);
		}
		createJobConfigsEntries(job);
		job.setEdition(nextEdition);
	}

	/** Read a single job from the job database.
	 *
	 * @param jobID ID of the job.
	 * @return A Job object
	 * @throws UnknownID if the job id does not exist.
	 * @throws IOFailure if there was some problem talking to the database.
	 */
	@Override
	public Job read(final Long jobID) {
		ArgumentNotValid.checkNotNull(jobID, "jobID");        
		return query(
				"SELECT harvest_id, status, channel, forcemaxcount, forcemaxbytes"
				+ ", forcemaxrunningtime, orderxml, orderxmldoc, seedlist, harvest_num"
				+ ", harvest_errors, harvest_error_details, upload_errors"
				+ ", upload_error_details, startdate, enddate, submitteddate"
				+ ", creationdate, edition, resubmitted_as_job, continuationof"
				+ ", harvestname_prefix, snapshot"
				+ " FROM jobs WHERE job_id=:id",
				new ParameterMap("id", jobID),
				new ResultSetExtractor<Job>() {
					@Override
					public Job extractData(ResultSet rs) 
							throws SQLException, DataAccessException {
						if (!rs.next()) {
							throw new UnknownID("Job id " 
									+ jobID + " is not known in persistent storage");
						}
						
						Document orderXMLdoc = null;
						boolean useClobs = DBSpecifics.getInstance().supportsClob();
						if (useClobs) {
							Clob clob = rs.getClob("orderxmldoc");
							orderXMLdoc = getOrderXMLdocFromClob(clob);
						} else {
							orderXMLdoc = XmlUtils.documentFromString(
									rs.getString("orderxmldoc"));
						}

						String seedlist = "";
						if (useClobs) {
							Clob clob = rs.getClob("seedlist");
							seedlist = clob.getSubString(1, (int) clob.length());
						} else {
							seedlist = rs.getString("seedlist");
						}
						
						final Job job = new Job(
								rs.getLong("harvest_id"),
								getDomainConfigurationMap(jobID),										
								rs.getString("channel"),  
								rs.getBoolean("snapshot"),
								rs.getLong("forcemaxcount"),
								rs.getLong("forcemaxbytes"),
								rs.getLong("forcemaxrunningtime"),
								JobStatus.fromOrdinal(rs.getInt("status")),
								rs.getString("orderxml"),
								orderXMLdoc, 
								seedlist, 
								rs.getInt("harvest_num"), 
								getLongKeepNull(rs, "continuationof"));
						
						job.appendHarvestErrors(rs.getString("harvest_errors"));
						job.appendHarvestErrorDetails(rs.getString("harvest_error_details"));
						job.appendUploadErrors(rs.getString("upload_errors"));
						job.appendUploadErrorDetails(rs.getString("upload_error_details"));
						
						
						Date startdate = getDateKeepNull(rs, "startdate");								
						if (startdate != null) {
							job.setActualStart(startdate);
						}
						
						Date stopdate = getDateKeepNull(rs, "enddate");								
						if (stopdate != null) {
							job.setActualStop(stopdate);
						}

						Date submittedDate = getDateKeepNull(rs, "submitteddate");								
						if (submittedDate != null) {
							job.setSubmittedDate(submittedDate);
						}

						Date creationDate = getDateKeepNull(rs, "creationdate");
						if (creationDate != null) {
							job.setCreationDate(creationDate);
						}

						job.setConfigsChanged(false);
						job.setJobID(jobID);
						job.setEdition(rs.getLong("edition"));

						Long resubmittedAsJob = getLongKeepNull(rs, "resubmitted_as_job");
						if (resubmittedAsJob != null) {
							job.setResubmittedAsJob(resubmittedAsJob);
						}
						
						String harvestnamePrefix = rs.getString("harvestname_prefix");
						if (harvestnamePrefix != null) {
							job.setDefaultHarvestNamePrefix();
						} else {
							job.setHarvestFilenamePrefix(harvestnamePrefix);
						}

						return job;
					}

				});
	}

	/** Try to extract an orderxmldoc from a given Clob.
	 * This method is used by the read() method, which catches the
	 * thrown DocumentException.
	 * @param clob a given Clob returned from the database
	 * @return a Document object based on the data in the Clob
	 * @throws SQLException if reading the clob failed
	 */
	private Document getOrderXMLdocFromClob(Clob clob) throws SQLException {
		Document doc;
		try {
			SAXReader reader = new SAXReader();
			doc = reader.read(clob.getCharacterStream());
		} catch (final DocumentException e) {
			throw new IOFailure(
					"Failed to read the contents of the clob as XML:"
					+  clob.getSubString(1, (int) clob.length()), e);
		}
		return doc;
	}

	/**
	 * Return a list of all jobs with the given status, ordered by id.
	 *
	 * @param status A given status.
	 * @return A list of all job with given status
	 */
	@Override
	public synchronized Iterator<Job> getAll(final JobStatus status) {
		ArgumentNotValid.checkNotNull(status, "JobStatus status");		
		List<Job> orderedJobs = new LinkedList<Job>();
		Iterator<Long> jobIds = getAllJobIds(status);
		while (jobIds.hasNext()) {
			orderedJobs.add(read(jobIds.next()));
		}
		return orderedJobs.iterator();
	}

	/**
	 * Return a list of all job_id's representing jobs with the given status.
	 *
	 * @param status A given status.
	 * @return A list of all job_id's representing jobs with given status
	 * @throws ArgumentNotValid If the given status is not one of the
	 *                          five valid statuses specified in Job.
	 */
	@Override
	public Iterator<Long> getAllJobIds(final JobStatus status) {
		ArgumentNotValid.checkNotNull(status, "JobStatus status");
		List<Long> idList = queryLongList(
				"SELECT job_id FROM jobs WHERE status=:status"
				+ " ORDER BY job_id",
				new ParameterMap("status", status.ordinal()));
		return idList.iterator();
	}

	@Override
	public Iterator<Long> getAllJobIds(
			final JobStatus status,
			final HarvestChannel channel) {
		ArgumentNotValid.checkNotNull(status, "JobStatus status");
		ArgumentNotValid.checkNotNull(channel, "Channel");
		List<Long> idList = queryLongList(
				"SELECT job_id FROM jobs WHERE status=:status AND channel=:channel"
				+ " ORDER BY job_id", 
				new ParameterMap(
						"status", status.ordinal(), 
						"channel", channel.getName()));
		return idList.iterator();
	}

	/**
	 * Return a list of all jobs.
	 *
	 * @return A list of all jobs
	 */
	@Override
	public synchronized Iterator<Job> getAll() {
		Iterator<Long> ids = getAllJobIds();
		List<Job> orderedJobs = new LinkedList<Job>();
		while (ids.hasNext()) {
			orderedJobs.add(read(ids.next()));
		}
		return orderedJobs.iterator();
	}

	/**
	 * Return a list of all job_ids .
	 * @return A list of all job_ids
	 */
	public Iterator<Long> getAllJobIds(){
		List<Long> idList = queryLongList("SELECT job_id FROM jobs ORDER BY job_id");
		return idList.iterator();
	}

	/**
	 * Get a list of small and immediately usable status information
	 * for given status and in given order. Is used by getStatusInfo
	 * functions in order to share code (and SQL)
	 * TODO should also include given harvest run
	 * @param jobStatusCode code for jobstatus, -1 if all
	 * @param asc true if it is to be sorted in ascending order,
	 *        false if it is to be sorted in descending order
	 * @return List of JobStatusInfo objects for all jobs.
	 * @throws ArgumentNotValid for invalid jobStatusCode
	 * @throws IOFailure on trouble getting data from database
	 */
	private List<JobStatusInfo> getStatusInfo(
			final int jobStatusCode, 
			final boolean asc) {
		// Validate jobStatusCode
		// Throws ArgumentNotValid if it is an invalid job status
		if (jobStatusCode != JobStatus.ALL_STATUS_CODE) {
			JobStatus.fromOrdinal(jobStatusCode);
		}

		StringBuffer sqlBuffer = new StringBuffer(
				"SELECT jobs.job_id, status, jobs.harvest_id"
				+ ", harvestdefinitions.name, harvest_num, harvest_errors"
				+ ", upload_errors, orderxml, num_configs, submitteddate, creationdate"
				+ ", startdate, enddate, resubmitted_as_job"
				+ " FROM jobs, harvestdefinitions"
				+ " WHERE harvestdefinitions.harvest_id=jobs.harvest_id");

		if (jobStatusCode != JobStatus.ALL_STATUS_CODE)  {
			sqlBuffer.append(" AND status = ").append(jobStatusCode);
		}
		
		sqlBuffer.append(" ORDER BY jobs.job_id");
		if (!asc)  { // Assume default is ASCENDING
			sqlBuffer.append(" " + HarvestStatusQuery.SORT_ORDER.DESC.name());
		}

		return query(
				sqlBuffer.toString(), 
				new ResultSetExtractor<List<JobStatusInfo>>() {					
					@Override
					public List<JobStatusInfo> extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						return makeJobStatusInfoListFromResultset(rs);
					}
				});
	}

	/**
	 * Get a list of small and immediately usable status information for given
	 * job status.
	 *
	 * @param status The status asked for.
	 * @return List of JobStatusInfo objects for all jobs with given job status
	 * @throws ArgumentNotValid for invalid jobStatus
	 * @throws IOFailure on trouble getting data from database
	 */
	@Override
	public List<JobStatusInfo> getStatusInfo(final JobStatus status) {
		ArgumentNotValid.checkNotNull(status, "status");
		return getStatusInfo(status.ordinal(), true);
	}

	/**
	 * Get a list of small and immediately usable status information for given
	 * job status and in given job id order.
	 *
	 * @param query the user query
	 * @throws IOFailure on trouble getting data from database
	 */
	@Override
	public HarvestStatus getStatusInfo(final HarvestStatusQuery query) {
		log.debug("Constructing Harveststatus based on given query.");
		// Obtain total count without limit
		// NB this will be a performance bottleneck if the table gets big
		long totalRowsCount = 0;

		HarvestStatusQueryBuilder hsqb = buildSqlQuery(query, true);
		totalRowsCount = queryIntValue(hsqb.sqlString, hsqb.paramMap);

		hsqb = buildSqlQuery(query, false);
		List<JobStatusInfo> jobs = query(
				hsqb.sqlString,
				hsqb.paramMap,
				new ResultSetExtractor<List<JobStatusInfo>>() {					
					@Override
					public List<JobStatusInfo> extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						return makeJobStatusInfoListFromResultset(rs);
					}
				});

		if (log.isDebugEnabled()) {
			log.debug("Harveststatus constructed based on given query.");
		}
		return new HarvestStatus(totalRowsCount, jobs);
	}


	/**
	 * Calculate all jobIDs to use for duplication reduction.
	 *
	 * More precisely, this method calculates the following: If the job ID
	 * corresponds to a partial harvest, all jobIDs from the previous scheduled
	 * harvest are returned, or the empty list if this harvest hasn't been
	 * scheduled before.
	 *
	 * If the job ID corresponds to a full harvest, the entire chain of harvests
	 * this is based on is returned, and all jobIDs from the previous chain of
	 * full harvests is returned.
	 *
	 * This method is synchronized to avoid DB locking.
	 *
	 * @param jobID The job ID to find duplicate reduction data for.
	 * @return A list of job IDs (possibly empty) of potential previous harvests
	 *         of this job, to use for duplicate reduction.
	 * @throws UnknownID if job ID is unknown
	 * @throws IOFailure on trouble querying database
	 */
	public synchronized List<Long> getJobIDsForDuplicateReduction(final long jobID)
			throws UnknownID {

		List<Long> jobs;
		//Select the previous harvest from the same harvestdefinition
		if (!exists(jobID)) {
			throw new UnknownID("Job ID '" + jobID + "' does not exist in database");
		}

		jobs = queryLongList(
				"SELECT jobs.job_id FROM jobs, jobs AS original_jobs"
				+ " WHERE original_jobs.job_id=:jobId"
				+ " AND jobs.harvest_id=original_jobs.harvest_id"
				+ " AND jobs.harvest_num=original_jobs.harvest_num-1",
				new ParameterMap("jobId", jobID));
		
		List<Long> harvestDefinitions = getPreviousFullHarvests(jobID);
		if (!harvestDefinitions.isEmpty()) {
			//Select all jobs from a given list of harvest definitions
			jobs.addAll(queryLongList(
					"SELECT jobs.job_id FROM jobs WHERE jobs.harvest_id IN ("
							+ StringUtils.conjoin(",", harvestDefinitions)
							+ ")"));
		}
		return jobs;
	}


	/**
	 * Find the harvest definition ids from this chain of snapshot harvests and
	 * the previous chain of snapshot harvests.
	 * @param jobID The ID of the job
	 * @return A (possibly empty) list of harvest definition ids
	 */
	private List<Long> getPreviousFullHarvests(final long jobID) {
		List<Long> results = new ArrayList<Long>();
		//Find the jobs' fullharvest id
		Long thisHarvest = queryLongValue(
				"SELECT jobs.harvest_id FROM jobs, fullharvests"
				+ " WHERE jobs.harvest_id=fullharvests.harvest_id"
				+ " AND jobs.job_id=:jobId",
				new ParameterMap("jobId", jobID));

		if (thisHarvest == null) {
			//Not a full harvest
			return results;
		}

		//Follow the chain of orginating IDs back
		for (Long originatingHarvest = thisHarvest;
				originatingHarvest != null;
				originatingHarvest = queryLongValue(
						"SELECT previoushd FROM fullharvests"
								+ " WHERE fullharvests.harvest_id=:harvestId",
								new ParameterMap("harvestId", originatingHarvest))) {
			if (!originatingHarvest.equals(thisHarvest)) {
				results.add(originatingHarvest);
			}
		}

		//Find the first harvest in the chain
		Long firstHarvest = thisHarvest;
		if (!results.isEmpty()) {
			firstHarvest = results.get(results.size() - 1);
		}

		//Find the last harvest in the chain before
		Long olderHarvest = queryLongValue(
				"SELECT fullharvests.harvest_id"
				+ " FROM fullharvests, harvestdefinitions, harvestdefinitions AS currenthd"
				+ " WHERE currenthd.harvest_id=:harvestId"
				+ " AND fullharvests.harvest_id=harvestdefinitions.harvest_id"
				+ " AND harvestdefinitions.submitted<currenthd.submitted"
				+ " ORDER BY harvestdefinitions.submitted "
				+ HarvestStatusQuery.SORT_ORDER.DESC.name(), 
				new ParameterMap("harvestId", firstHarvest));
		//Follow the chain of orginating IDs back
		for (Long originatingHarvest = olderHarvest;
				originatingHarvest != null;
				originatingHarvest = queryLongValue(
						"SELECT previoushd FROM fullharvests WHERE fullharvests.harvest_id=:id",
						new ParameterMap("id", originatingHarvest))) {
			results.add(originatingHarvest);
		}
		return results;
	}

	/**
	 * Returns the number of existing jobs.
	 *
	 * @return Number of jobs in 'jobs' table
	 */
	@Override
	public int getCountJobs() {
		return queryIntValue("SELECT COUNT(*) FROM jobs");
	}

	@Override
	public synchronized long rescheduleJob(final long oldJobID) {
		return (Long) executeTransaction(
				"doRescheduleJob", 
				Long.class, oldJobID,
				Long.class, generateNextID());
	}
	
	@SuppressWarnings("unused")
	private synchronized long doRescheduleJob(
			final long oldJobID,
			final long newJobID) {
		
		Integer statusOrdinal = queryIntValue(
				"SELECT status FROM jobs WHERE job_id=:id",
				new ParameterMap("id", oldJobID));
		if (statusOrdinal == null) {
			throw new UnknownID("No job with ID " + oldJobID + " to resubmit");
		}
		final JobStatus currentJobStatus = JobStatus.fromOrdinal(statusOrdinal);
		if (currentJobStatus != JobStatus.SUBMITTED
				&& currentJobStatus != JobStatus.FAILED) {
			throw new IllegalState("Job " + oldJobID + " is not ready to be copied.");
		}

		// Now do the actual copying.
		// Note that startdate, and enddate is not copied.
		// They must be null in JobStatus NEW.		
		executeUpdate(
				"INSERT INTO jobs (job_id, harvest_id, priority, status"
				+ ", forcemaxcount, forcemaxbytes, orderxml, orderxmldoc"
				+ ", seedlist, harvest_num, num_configs, edition, continuationof)"
				+ " SELECT :newJobId, harvest_id, priority, :status"
				+ ", forcemaxcount, forcemaxbytes, orderxml, orderxmldoc"
				+ ", seedlist, harvest_num, num_configs, :initialEdition, :continuationOf"
				+ " FROM jobs WHERE job_id=:oldJobId",
				new ParameterMap(
						"newJobId", newJobID,
						"status", JobStatus.NEW.ordinal(),
						"initialEdition", 1,
						// In case we want to try to continue using the Heritrix recover log
						"continuationOf", currentJobStatus == JobStatus.FAILED ? oldJobID : null,
						"oldJobId", oldJobID));

		executeUpdate(
				"INSERT INTO job_configs (job_id, config_id)"
				+ " SELECT :newJobId, config_id"
				+ " FROM job_configs"
				+ " WHERE job_id=:oldJobId",
				new ParameterMap(
						"newJobId", newJobID,
						"oldJobId", oldJobID));

		executeUpdate(
				"UPDATE jobs SET status=:status, resubmitted_as_job=:newJobId"
				+ " WHERE job_id=:oldJobId",
				new ParameterMap(
						"newJobId", newJobID,
						"oldJobId", oldJobID,
						"status", JobStatus.RESUBMITTED.ordinal()));
		
		log.info("Job # " + oldJobID + " successfully as job # " + newJobID);
		return newJobID;
	}

	/**
	 * Internal utility class to build a SQL query using a prepared statement.
	 */
	private class HarvestStatusQueryBuilder {
		/** The sql string. */
		private String sqlString;
		
		private ParameterMap paramMap;
		
		/**
		 * Constructor.
		 */
		HarvestStatusQueryBuilder() {
			super();
			this.paramMap = new ParameterMap();
		}

		/**
		 * @param sqlString the sqlString to set
		 */
		void setSqlString(String sqlString) {
			this.sqlString = sqlString;
		}

		/**
		 * Add a named parameter
		 * @param name
		 * @param value
		 */
		void addParameter(final String name, final Object value) {
			paramMap.put(name, value);
		}

	}

	/**
	 * Builds a query to fetch jobs according to selection criteria.
	 * @param query the selection criteria.
	 * @param count build a count query instead of selecting columns.
	 * @return the proper SQL query.
	 */
	private HarvestStatusQueryBuilder buildSqlQuery(
			HarvestStatusQuery query, boolean count) {

		HarvestStatusQueryBuilder sq = new HarvestStatusQueryBuilder();
		StringBuffer sql = new StringBuffer("SELECT");
		if (count) {
			sql.append(" count(*)");
		} else {
			sql.append(" jobs.job_id, status, jobs.harvest_id,");
			sql.append(" harvestdefinitions.name, harvest_num,");
			sql.append(" harvest_errors, upload_errors, orderxml,");
			sql.append(" num_configs, submitteddate, creationdate, startdate, enddate,");
			sql.append(" resubmitted_as_job");
		}

		sql.append(" FROM jobs, harvestdefinitions ");
		sql.append(" WHERE harvestdefinitions.harvest_id = jobs.harvest_id ");

		JobStatus[] jobStatuses = query.getSelectedJobStatuses();
		if (jobStatuses.length > 0) {
			if (jobStatuses.length == 1) {
				int statusOrdinal = jobStatuses[0].ordinal();
				sql.append(" AND status=:status");
				sq.addParameter("status", statusOrdinal);
			} else {
				sql.append("AND (status0=:status0");
				sq.addParameter("status0", jobStatuses[0].ordinal());
				for (int i = 1; i < jobStatuses.length; i++) {
					String paramName = "status" + i;
					sql.append(" OR status=:" + paramName);
					sq.addParameter(paramName, jobStatuses[i].ordinal());
				}
				sql.append(")");
			}
		}

		String harvestName = query.getHarvestName();
		boolean caseSensitiveHarvestName = query.getCaseSensitiveHarvestName();
		if (!harvestName.isEmpty()) {
			if(caseSensitiveHarvestName) {
				if (harvestName.indexOf(
						HarvestStatusQuery.HARVEST_NAME_WILDCARD) == -1) {
					// No wildcard, exact match
					sql.append(" AND harvestdefinitions.name=:harvestName");
					sq.addParameter("harvestName", harvestName);
				} else {
					String harvestNamePattern =
							harvestName.replaceAll("\\*", "%");
					sql.append(" AND harvestdefinitions.name LIKE :pattern");
					sq.addParameter("pattern", harvestNamePattern);
				}
			} else {
				harvestName = harvestName.toUpperCase();
				if (harvestName.indexOf(
						HarvestStatusQuery.HARVEST_NAME_WILDCARD) == -1) {
					// No wildcard, exact match
					sql.append(" AND UPPER(harvestdefinitions.name)=:harvestName");
					sq.addParameter("harvestName", harvestName);
				} else {
					String harvestNamePattern =
							harvestName.replaceAll("\\*", "%");
					sql.append(" AND UPPER(harvestdefinitions.name) LIKE :pattern");
					sq.addParameter("pattern", harvestNamePattern);
				}
			}
		}

		Long harvestRun = query.getHarvestRunNumber();
		if (harvestRun != null) {
			sql.append(" AND jobs.harvest_num=:harvestNum");
			sq.addParameter("harvestNum", harvestRun);
		}

		Long harvestId = query.getHarvestId();
		if (harvestId != null) {
			sql.append(" AND harvestdefinitions.harvest_id=:harvestId");
			sq.addParameter("harvestId", harvestId);
		}

		long startDate = query.getStartDate();
		if (startDate != HarvestStatusQuery.DATE_NONE) {
			sql.append(" AND startdate >= :startDate");
			sq.addParameter("startDate", startDate);
		}

		long endDate = query.getEndDate();
		if (endDate != HarvestStatusQuery.DATE_NONE) {
			sql.append(" AND enddate < :endDate");
			// end date must be set +1 day at midnight
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(endDate);
			cal.roll(Calendar.DAY_OF_YEAR, 1);
			sq.addParameter("endDate", cal.getTime());
		}

		if (!count) {
			sql.append(" ORDER BY jobs.job_id");
			if (!query.isSortAscending()) {
				sql.append(" " + SORT_ORDER.DESC.name());
			} else {
				sql.append(" " + SORT_ORDER.ASC.name());
			}

			long pagesize = query.getPageSize();
			if (pagesize != HarvestStatusQuery.PAGE_SIZE_NONE) {
				sql.append(" "
						+ DBSpecifics.getInstance().getOrderByLimitAndOffsetSubClause(
								pagesize,
								(query.getStartPageIndex() - 1)
								* pagesize));
			}
		}

		sq.setSqlString(sql.toString());
		return sq;
	}

	/**
	 * Get Jobstatus for the job with the given id.
	 * @param jobID A given Jobid
	 * @return the Jobstatus for the job with the given id.
	 * @throws UnknownID if no job exists with id jobID
	 */
	public JobStatus getJobStatus(Long jobID) {
		ArgumentNotValid.checkNotNull(jobID, "Long jobID");

		Integer statusAsInteger = queryIntValue(
				"SELECT status FROM jobs WHERE job_id=:id",
				new ParameterMap("id", jobID));
		if (statusAsInteger == null) {
			throw new UnknownID("No known job with id=" + jobID);
		}
		return JobStatus.fromOrdinal(statusAsInteger);
	}

	private ParameterMap getParameterMap(
			final Job job,
			final long initialEdition) {
		return new ParameterMap(
				"jobId", job.getJobID(),
				"harvestId", job.getOrigHarvestDefinitionID(),
				"status", job.getStatus().ordinal(),
				"channel", job.getChannel(),
				"forceMaxCount", job.getForceMaxObjectsPerDomain(),
				"forceMaxBytes", job.getForceMaxBytesPerDomain(),
				"forceMaxTime", job.getMaxJobRunningTime(),
				"orderXmlName", getMaxLengthStringValue(
						job, 
						"order.xml name", 
						job.getOrderXMLName(), 
						Constants.MAX_NAME_SIZE),
				"orderXmlDoc", getMaxLengthTextValue(
						job, 
						"order.xml contents", 
						job.getOrderXMLdoc().asXML(), 
						Constants.MAX_ORDERXML_SIZE),
				"seeds", getMaxLengthTextValue(
						job, 
						"order.xml contents", 
						job.getSeedListAsString(), 
						Constants.MAX_COMBINED_SEED_LIST_SIZE),
				"harvestNum", job.getHarvestNum(),
				"startDate", job.getActualStart(),
				"endDate", job.getActualStop(),
				"submitted", job.getSubmittedDate(),
				"created", job.getCreationDate(),
				"numConfigs", job.getDomainConfigurationMap().size(),
				"edition", initialEdition,
				"resubmitId", job.getResubmittedAsJob(),
				"prefix", job.getHarvestFilenamePrefix(),
				"snapshot", job.isSnapshot(),
				"errors", job.getHarvestErrors(),
				"errorDetails", job.getHarvestErrorDetails(),
				"uploadErrors", job.getUploadErrors(),
				"uploadErrorDetails", job.getUploadErrorDetails());
	}
	
	private Map<String, String> getDomainConfigurationMap(final long jobId) {
		String domainStatement =
				"SELECT domains.name, configurations.name"
				+ " FROM domains, configurations, job_configs"
				+ " WHERE job_configs.job_id=:id"
				+ "  AND job_configs.config_id=configurations.config_id"
				+ "  AND domains.domain_id =configurations.domain_id";
		if (Settings.get(CommonSettings.DB_SPECIFICS_CLASS).
				contains(CommonSettings.DB_IS_DERBY_IF_CONTAINS)) {
			domainStatement += " WITH UR";
		}
		return query(
				domainStatement,
				new ParameterMap("id", jobId),
				new ResultSetExtractor<Map<String, String>>() {
					@Override
					public Map<String, String> extractData(ResultSet rs) 
							throws SQLException, DataAccessException {
						Map<String, String> configurationMap = 
								new HashMap<String, String>();
						while (rs.next()) {
							configurationMap.put(
									rs.getString(1), 
									rs.getString(2));
						}
						return configurationMap;
					}										
				});
	}
	
	/** Helper-method that constructs a list of JobStatusInfo objects
     * from the given resultset.
     * @param res a given resultset
     * @return a list of JobStatusInfo objects
     * @throws SQLException If any problem with accessing the data in
     * the ResultSet
     */
    private List<JobStatusInfo> makeJobStatusInfoListFromResultset(ResultSet rs)
    throws SQLException{
    	List<JobStatusInfo> joblist = new ArrayList<JobStatusInfo>();
		while (rs.next()) {
			final long jobId = rs.getLong(1);
			joblist.add(new JobStatusInfo(
					jobId,
					JobStatus.fromOrdinal(rs.getInt("status")),
					rs.getLong("harvest_id"), 
					rs.getString("name"), 
					rs.getInt("harvest_num"),
					rs.getString("harvest_errors"),
					rs.getString("upload_errors"), 
					rs.getString("orderxml"),
					rs.getInt("num_configs"),
					getDateKeepNull(rs, "submitteddate"),
					getDateKeepNull(rs, "creationdate"),
					getDateKeepNull(rs, "startdate"),
					getDateKeepNull(rs, "enddate"),
					getLongKeepNull(rs, "resubmitted_as_job")));
		}
		return joblist;
    }
	
}
