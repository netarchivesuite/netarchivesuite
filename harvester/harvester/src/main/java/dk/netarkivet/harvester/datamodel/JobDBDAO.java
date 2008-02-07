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

package dk.netarkivet.harvester.datamodel;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FilterIterator;
import dk.netarkivet.common.utils.StringUtils;

/**
 * A database-based implementation of the JobDAO class.
 * If no jobs are found in the database, it will attempt to
 * migrate from the XML-based implementation.
 * The statements to create the tables are now in
 * scripts/sql/createfullhddb.sql
 */
public class JobDBDAO extends JobDAO {
    private final Log log = LogFactory.getLog(getClass());

    /** Create a new JobDAO implemented using database.
     */
    protected JobDBDAO() {
        DBConnect.checkTableVersion("jobs", 3);
        DBConnect.checkTableVersion("job_configs", 1);
    }

    /**
     * Creates an instance in persistent storage of the given job.
     * If the job doesn't have an ID, one is generated for it.
     *
     * @param job
     * @throws PermissionDenied If a job already exists in persistent storage
     *                          with id of the given job
     * @throws IOFailure        If some IOException occurs while
     *                          writing the job_<jobID>.xml
     */
    public synchronized void create(Job job) {
        ArgumentNotValid.checkNotNull(job, "Job job");
        if (job.getJobID() != null) {
            // If not, we're still migrating from XML.
        } else {
            job.setJobID(generateNextID());
        }
        
        log.debug("Creating " + job.toString());
        
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            c.setAutoCommit(false);
            s = c.prepareStatement("INSERT INTO jobs "
                                   + "(job_id, harvest_id, status, priority, forcemaxcount, "
                                   + "forcemaxbytes, orderxml, orderxmldoc, seedlist, harvest_num, "
                                   + "startdate, enddate, num_configs, edition) "
                                   + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
            

            s.setLong(1, job.getJobID());
            s.setLong(2, job.getOrigHarvestDefinitionID());
            s.setInt(3, job.getStatus().ordinal());
            s.setInt(4, job.getPriority().ordinal());
            s.setLong(5, job.getForceMaxObjectsPerDomain());
            s.setLong(6, job.getMaxBytesPerDomain());
            DBConnect.setStringMaxLength(s, 7, job.getOrderXMLName(),
                                         Constants.MAX_NAME_SIZE, job, "order.xml name");
            final String orderreader = job.getOrderXMLdoc().asXML();
            DBConnect.setClobMaxLength(s, 8, orderreader,
                                       Constants.MAX_ORDERXML_SIZE, job, "order.xml");
            DBConnect.setClobMaxLength(s, 9, job.getSeedListAsString(),
                                       Constants.MAX_COMBINED_SEED_LIST_SIZE, job, "seedlist");
            s.setInt(10, job.getHarvestNum());
            DBConnect.setDateMaybeNull(s, 11, job.getActualStart());
            DBConnect.setDateMaybeNull(s, 12, job.getActualStop());
            // The size of the configuration map == number of configurations
            s.setInt(13, job.getDomainConfigurationMap().size());
            long initialEdition = 1;
            s.setLong(14, initialEdition);
            s.executeUpdate();
            createJobConfigsEntries(c, job);
            c.commit();
            job.setEdition(initialEdition);
        } catch (SQLException e) {
            String message = "SQL error creating job " + job + " in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
            DBConnect.rollbackIfNeeded(c, "create job", job);
        }
    }

    /** Create the entries in the job_configs table for this job.
     * Since some jobs have up to 10000 configs, this must be optimized.
     * The entries are only created, if job.configsChanged is true.
     *
     * @param c A connection to work on
     * @param job The job to store entries for
     * @throws SQLException
     */
    private void createJobConfigsEntries(Connection c, Job job) throws SQLException {
        if (job.configsChanged) {
            PreparedStatement s = null;
            String tmpTable = null;
            Long jobID = job.getJobID();
            try {
                s = c.prepareStatement("DELETE FROM job_configs WHERE job_id = ?");
                s.setLong(1, jobID);
                s.executeUpdate();
                s.close();
                tmpTable = DBSpecifics.getInstance().getJobConfigsTmpTable(c);
                final Map<String, String> domainConfigurationMap
                        = job.getDomainConfigurationMap();
                s = c.prepareStatement("INSERT INTO " + tmpTable
                             + " ( domain_name, config_name ) "
                                       + " VALUES ( ?, ?)");
                for (Map.Entry <String,String> entry
                        : domainConfigurationMap.entrySet()) {
                    s.setString(1, entry.getKey());
                    s.setString(2, entry.getValue());
                    s.executeUpdate();
                    s.clearParameters();
                    }
                s.close();
                // Now we have a temp table with all the domains and configs
                s = c.prepareStatement("INSERT INTO job_configs "
                                       + "( job_id, config_id ) "
                                       + "SELECT ?, configurations.config_id "
                                       + "  FROM domains, configurations, " + tmpTable
                                       + " WHERE domains.name = " + tmpTable + ".domain_name"
                                       + "   AND domains.domain_id = configurations.domain_id"
                                       + "   AND configurations.name = " + tmpTable + ".config_name");
                s.setLong(1, jobID);
                int rows = s.executeUpdate();
                if (rows != domainConfigurationMap.size()) {
                    log.debug("Domain or configuration in table for "
                              + job + " missing: Should have "
                              + domainConfigurationMap.size() + ", got " + rows);
                }
                c.commit();
            } finally {
                DBConnect.closeStatementIfOpen(s);
                if (tmpTable != null) {
                    DBSpecifics.getInstance().dropJobConfigsTmpTable(c,
                            tmpTable);
                }
                job.configsChanged = false;
            }
        }
    }

    /** Check whether a particular job exists.
     *
     * @param jobID Id of the job.
     * @return true if the job exists in any state.
     */
    public synchronized boolean exists(Long jobID) {
        ArgumentNotValid.checkNotNull(jobID, "Long jobID");
        return 1 == DBConnect.selectLongValue(
                "SELECT COUNT(*) FROM jobs WHERE job_id = ?", jobID);
    }

    synchronized Long generateNextID() {
        Long maxVal = DBConnect.selectLongValue("SELECT MAX(job_id) FROM jobs");
        if (maxVal == null) {
            maxVal = 0L;
        }
        return maxVal + 1L;
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
    public synchronized void update(Job job) {
        ArgumentNotValid.checkNotNull(job, "job");
        final Long jobID = job.getJobID();
        if (!exists(jobID)) {
            throw new UnknownID("Job id " + jobID
                                + " is not known in persistent storage");
        }
        Connection c = DBConnect.getDBConnection();
        // Not done as a transaction as it's awfully big.
        // TODO: Make sure that a failed job update does... what?
        PreparedStatement s = null;
        try {
            c.setAutoCommit(false);
            s = c.prepareStatement("UPDATE jobs SET "
                                   + "harvest_id = ?, status = ?, priority = ?, "
                                   + "forcemaxcount = ?, forcemaxbytes = ?, "
                                   + "orderxml = ?, "
                                   + "orderxmldoc = ?, seedlist = ?, "
                                   + "harvest_num = ?, harvest_errors = ?, "
                                   + "harvest_error_details = ?, upload_errors = ?, "
                                   + "upload_error_details = ?, startdate = ?,"
                                   + "enddate = ?, num_configs = ?, edition = ? "
                                   + "WHERE job_id = ? AND edition = ?");
            s.setLong(1, job.getOrigHarvestDefinitionID());
            s.setInt(2, job.getStatus().ordinal());
            s.setInt(3, job.getPriority().ordinal());
            s.setLong(4, job.getForceMaxObjectsPerDomain());
            s.setLong(5, job.getMaxBytesPerDomain());
            DBConnect.setStringMaxLength(s, 6, job.getOrderXMLName(),
                                         Constants.MAX_NAME_SIZE, job, "order.xml name");
            final String orderreader = job.getOrderXMLdoc().asXML();
            DBConnect.setClobMaxLength(s, 7, orderreader,
                                       Constants.MAX_ORDERXML_SIZE, job, "order.xml");
            DBConnect.setClobMaxLength(s, 8, job.getSeedListAsString(),
                                       Constants.MAX_COMBINED_SEED_LIST_SIZE, job, "seedlist");
            s.setInt(9, job.getHarvestNum()); // Not in job yet
            DBConnect.setStringMaxLength(s, 10, job.getHarvestErrors(),
                                         Constants.MAX_ERROR_SIZE, job, "harvest_error");
            DBConnect.setStringMaxLength(s, 11, job.getHarvestErrorDetails(),
                                         Constants.MAX_ERROR_DETAIL_SIZE, job, "harvest_error_details");
            DBConnect.setStringMaxLength(s, 12, job.getUploadErrors(),
                                         Constants.MAX_ERROR_SIZE, job, "upload_error");
            DBConnect.setStringMaxLength(s, 13, job.getUploadErrorDetails(),
                                         Constants.MAX_ERROR_DETAIL_SIZE, job, "upload_error_details");
            long edition = job.getEdition() + 1;
            DBConnect.setDateMaybeNull(s, 14, job.getActualStart());
            DBConnect.setDateMaybeNull(s, 15, job.getActualStop());
            s.setInt(16, job.getDomainConfigurationMap().size());
            s.setLong(17, edition);
            s.setLong(18, job.getJobID());
            s.setLong(19, job.getEdition());
            final int rows = s.executeUpdate();
            if (rows == 0) {
                String message = "Edition " + job.getEdition() + " has expired, not updating";
                log.debug(message);
                throw new PermissionDenied(message);
            }
            createJobConfigsEntries(c, job);
            c.commit();
            job.setEdition(edition);
        } catch (SQLException e) {
            String message = "SQL error updating job " + job + " in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBConnect.rollbackIfNeeded(c, "update job", job);
            DBConnect.closeStatementIfOpen(s);
        }
    }

    /** Read a single job from the job database.
     *
     * @param jobID ID of the job.
     * @return A Job object
     * @throws UnknownID if the job id does not exist.
     * @throws IOFailure if there was some problem talking to the database.
     */
    public synchronized Job read(Long jobID) {
        if (!exists(jobID)) {
            throw new UnknownID("Job id "
                                + jobID + " is not known in persistent storage");
        }
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT "
                                   + "harvest_id, status, priority, forcemaxcount, "
                                   + "forcemaxbytes, orderxml, "
                                   + "orderxmldoc, seedlist, harvest_num,"
                                   + "harvest_errors, harvest_error_details, "
                                   + "upload_errors, upload_error_details, "
                                   + "startdate, enddate, edition "
                                   + "FROM jobs WHERE job_id = ?");
            s.setLong(1, jobID);
            ResultSet result = s.executeQuery();
            result.next();
            long harvestID = result.getLong(1);
            JobStatus status = JobStatus.fromOrdinal(result.getInt(2));
            JobPriority pri = JobPriority.fromOrdinal(result.getInt(3));
            long forceMaxCount = result.getLong(4);
            long forceMaxBytes = result.getLong(5);
            String orderxml = result.getString(6);
            Clob clob = result.getClob(7);
            SAXReader reader = new SAXReader();
            Document orderXMLdoc = reader.read(clob.getCharacterStream());
            clob = result.getClob(8);
            String seedlist = clob.getSubString(1, (int)clob.length());
            int harvestNum = result.getInt(9);
            String harvestErrors = result.getString(10);
            String harvestErrorDetails = result.getString(11);
            String uploadErrors = result.getString(12);
            String uploadErrorDetails = result.getString(13);
            Date startdate = DBConnect.getDateMaybeNull(result, 14);
            Date stopdate = DBConnect.getDateMaybeNull(result, 15);
            Long edition = result.getLong(16);
            s.close();
            // IDs should match up in a natural join
            s = c.prepareStatement("SELECT domains.name, configurations.name "
                                   + "FROM domains, configurations, job_configs "
                                   + "WHERE job_configs.job_id = ?"
                                   + "  AND job_configs.config_id = configurations.config_id"
                                   + "  AND domains.domain_id = configurations.domain_id");
            s.setLong(1, jobID);
            result = s.executeQuery();
            Map<String, String> configurationMap = new HashMap<String, String>();
            while (result.next()) {
                String domainName = result.getString(1);
                String configName = result.getString(2);
                configurationMap.put(domainName, configName);
            }
            final Job job = new Job(harvestID, configurationMap, pri, forceMaxCount,
                                    forceMaxBytes, status, orderxml, orderXMLdoc, seedlist, harvestNum);
            job.appendHarvestErrors(harvestErrors);
            job.appendHarvestErrorDetails(harvestErrorDetails);
            job.appendUploadErrors(uploadErrors);
            job.appendUploadErrorDetails(uploadErrorDetails);
            if (startdate != null) {
                job.setActualStart(startdate);
            }
            if (stopdate != null) {
                job.setActualStop(stopdate);
            }
            job.configsChanged = false;
            job.setJobID(jobID);
            job.setEdition(edition);
            return job;
        } catch (SQLException e) {
            String message = "SQL error reading job " + jobID + " in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } catch (DocumentException e) {
            String message = "XML error reading job " + jobID + " in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }
    }

    /**
     * Return a list of all jobs with the given status, ordered by id.
     *
     * @param status A given status.
     * @return A list of all job with given status
     */
    public synchronized Iterator<Job> getAll(JobStatus status) {
        try {
            List<Long> idList = DBConnect.selectLongList(
                    "SELECT job_id FROM jobs WHERE status = ? "
                    + "ORDER BY job_id", status.ordinal());
            return new FilterIterator<Long, Job>(idList.iterator()) {
                public Job filter(Long aLong) {
                    return read(aLong);
                }
            };
        } catch (SQLException e) {
            String message = "SQL error asking for job list in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }

    /**
     * Return a list of all job_id's representing jobs with the given status.
     *
     * @param status A given status.
     * @return A list of all job_id's representing jobs with given status
     * @throws ArgumentNotValid If the given status is not one of the
     *                          five valid statuses specified in Job.
     */
    public synchronized Iterator<Long> getAllJobIds(JobStatus status) {
        try {
            List<Long> idList = DBConnect.selectLongList(
                    "SELECT job_id FROM jobs WHERE status = ? "
                    + "ORDER BY job_id", status.ordinal());
            return idList.iterator();
        } catch (SQLException e) {
            String message = "SQL error asking for job list in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }

    /**
     * Return a list of all jobs .
     *
     * @return A list of all jobs
     */
    public synchronized Iterator<Job> getAll() {
        try {
            List<Long> idList = DBConnect.selectLongList(
                    "SELECT job_id FROM jobs ORDER BY job_id");
            return new FilterIterator<Long, Job>(idList.iterator()) {
                public Job filter(Long aLong) {
                    return read(aLong);
                }
            };
        } catch (SQLException e) {
            String message = "SQL error asking for job list in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }

    /**
     * Return a list of all job_ids .
     *
     * @return A list of all job_ids
     */
    public synchronized Iterator<Long> getAllJobIds(){
        try {
            List<Long> idList = DBConnect.selectLongList(
                    "SELECT job_id FROM jobs ORDER BY job_id");
            return idList.iterator();
        } catch (SQLException e) {
            String message = "SQL error asking for job list in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }

    /**
     * Get a list of small and immediately usable status information
     * for given status and in given order. Is used by getStatusInfo
     * functions in order to share code (and SQL)
     * TODO: should also include given harvest run
     *
     * @param jobStatusCode code for jobstatus, -1 if all
     * @param asc true if it is to be sorted in ascending order,
     *        false if it is to be sorted in descending order
     * @return List of JobStatusInfo objects for all jobs.
     * @throws ArgumentNotValid for invalid jobStatusCode
     * @throws IOFailure on trouble getting data from database
     */
    private List<JobStatusInfo> getStatusInfo(int jobStatusCode, boolean asc) {
        if (jobStatusCode != -1) {
        	//thorws ArgumentNotValid if it is an invalid job status
            JobStatus.fromOrdinal(jobStatusCode);
        }

        String sql;
        sql = "SELECT jobs.job_id, status, jobs.harvest_id, "
            + "harvestdefinitions.name, harvest_num, harvest_errors,"
            + " upload_errors, orderxml, num_configs, startdate, enddate"
            + " FROM jobs, harvestdefinitions "
            + " WHERE harvestdefinitions.harvest_id = jobs.harvest_id ";
        if (jobStatusCode != -1)  {
            sql = sql + " AND status = " + jobStatusCode;
        }
        sql = sql + " ORDER BY jobs.job_id";
        if (!asc)  {
            sql = sql + " DESC";
        }

        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(sql);
            ResultSet res = s.executeQuery();
            List<JobStatusInfo> joblist = new ArrayList<JobStatusInfo>();
            while (res.next()) {
                joblist.add(
                    new JobStatusInfo(
                    	    res.getLong(1),
                            JobStatus.fromOrdinal(res.getInt(2)),
                            res.getLong(3), res.getString(4), res.getInt(5),
                            res.getString(6),res.getString(7),
                            res.getString(8), res.getInt(9),
                            DBConnect.getDateMaybeNull(res, 10),
                            DBConnect.getDateMaybeNull(res, 11)
                ));
            }
            return joblist;
        } catch (SQLException e) {
            String message
                       = "SQL error asking for job status list in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }
    }

    /**
     * Get a list of small and immediately usable status information
     *
     * @return List of JobStatusInfo objects for all jobs.
     * @throws IOFailure on trouble getting data from database
     */
    public List<JobStatusInfo> getStatusInfo() {
        return getStatusInfo(-1, true);
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
    public List<JobStatusInfo> getStatusInfo(JobStatus status) {
        ArgumentNotValid.checkNotNull(status, "status");
        return getStatusInfo(status.ordinal(),true);
    }

    /**
     * Get a list of small and immediately usable status information in given
     * job id order.
     *
     * @param asc True if result must be given in ascending order, false
     *        if result must be given in descending order
     * @return List of JobStatusInfo objects for all jobs with given
     *         job id order
     * @throws IOFailure on trouble getting data from database
     */
    public List<JobStatusInfo> getStatusInfo(boolean asc) {
        return getStatusInfo(-1, asc);
    }

    /**
     * Get a list of small and immediately usable status information for given
     * job status and in given job id order.
     *
     * @param status The status asked for.
     * @param asc True if result must be given in ascending order, false
     *        if result must be given in descending order
     * @return List of JobStatusInfo objects for all jobs with given job status
     *         and job id order
     * @throws ArgumentNotValid for invalid jobStatusCode
     * @throws IOFailure on trouble getting data from database
     */
    public List<JobStatusInfo> getStatusInfo(JobStatus status, boolean asc) {
        ArgumentNotValid.checkNotNull(status, "status");
    	return getStatusInfo(status.ordinal(),asc);
    }

    /**
     * Get a list of small and immediately usable status information for
     * a given harvest run.
     *
     * @param harvestId The ID of the harvest
     * @param numEvent The harvest run number
     *
     * @return List of JobStatusInfo objects for all jobs for a given harvest
     * ID.
     * @throws IOFailure on trouble getting data from database
     */
    public List<JobStatusInfo> getStatusInfo(long harvestId, long numEvent) {
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT jobs.job_id, status, jobs.harvest_id, "
                                   + "harvestdefinitions.name, harvest_num, harvest_errors,"
                                   + " upload_errors, orderxml, num_configs, startdate, enddate"
                                   + " FROM jobs, harvestdefinitions "
                                   + " WHERE harvestdefinitions.harvest_id = jobs.harvest_id "
                                   + " AND jobs.harvest_id = " + harvestId
                                   + " AND jobs.harvest_num = " + numEvent
                                   + " ORDER BY jobs.job_id");
            ResultSet res = s.executeQuery();
            List<JobStatusInfo> joblist = new ArrayList<JobStatusInfo>();
            while (res.next()) {
                final long job_id = res.getLong(1);
                joblist.add(new JobStatusInfo(job_id,
                                              JobStatus.fromOrdinal(res.getInt(2)), res.getLong(3),
                                              res.getString(4), res.getInt(5), res.getString(6),
                                              res.getString(7), res.getString(8), res.getInt(9),
                                              DBConnect.getDateMaybeNull(res, 10),
                                              DBConnect.getDateMaybeNull(res, 11)));
            }
            return joblist;
        } catch (SQLException e) {
            String message = "SQL error asking for job status list in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }
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
    public synchronized List<Long> getJobIDsForDuplicateReduction(long jobID)
            throws UnknownID {
        if (!exists(jobID)) {
            throw new UnknownID("Job ID '" + jobID
                                + "' does not exist in database");
        }

        List<Long> jobs;
        try {
            //Select the previous harvest from the same harvestdefinition
            jobs = DBConnect.selectLongList(
                    "SELECT jobs.job_id FROM jobs, jobs AS original_jobs"
                    + " WHERE original_jobs.job_id=?"
                    + " AND jobs.harvest_id=original_jobs.harvest_id"
                    + " AND jobs.harvest_num=original_jobs.harvest_num-1",
                    jobID);
            List<Long> harvestDefinitions = getPreviousFullHarvests(jobID);
            if (!harvestDefinitions.isEmpty()) {
                //Select all jobs from a given list of harvest definitions
                jobs.addAll(DBConnect.selectLongList(
                        "SELECT jobs.job_id FROM jobs"
                        + " WHERE jobs.harvest_id IN ("
                        + StringUtils.conjoin(",",harvestDefinitions )
                        + ")"));
            }
        } catch (SQLException e) {
            String message = "SQL error asking for job list in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
        return jobs;
    }


    /**
     * Find the harvest definition ids from this chain of snapshot harvests and
     * the previous chain of snapshot harvests.
     * @param jobID The ID of the job
     * @return A (possibly empty) list of harvest definition ids
     */
    private List<Long> getPreviousFullHarvests(long jobID) {
        List<Long> results = new ArrayList<Long>();
        //Find the jobs' fullharvest id
        Long thisHarvest = DBConnect.selectFirstLongValueIfAny(
                "SELECT jobs.harvest_id FROM jobs, fullharvests"
                + " WHERE jobs.harvest_id=fullharvests.harvest_id"
                + " AND jobs.job_id=?", jobID);

        if (thisHarvest == null) {
            //Not a full harvest
            return results;
        }

        //Follow the chain of orginating IDs back
        for (Long originatingHarvest = thisHarvest;
             originatingHarvest != null;
             originatingHarvest = DBConnect.selectFirstLongValueIfAny(
                     "SELECT previoushd FROM fullharvests"
                     + " WHERE fullharvests.harvest_id=?",
                     originatingHarvest)) {
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
        Long olderHarvest = DBConnect.selectFirstLongValueIfAny(
                "SELECT fullharvests.harvest_id"
                + " FROM fullharvests, harvestdefinitions,"
                + "  harvestdefinitions AS currenthd"
                + " WHERE currenthd.harvest_id=?"
                + " AND fullharvests.harvest_id=harvestdefinitions.harvest_id"
                + " AND harvestdefinitions.submitted<currenthd.submitted"
                + " ORDER BY harvestdefinitions.submitted DESC", firstHarvest);
        //Follow the chain of orginating IDs back
        for (Long originatingHarvest = olderHarvest;
             originatingHarvest != null;
             originatingHarvest = DBConnect.selectFirstLongValueIfAny(
                     "SELECT previoushd FROM fullharvests"
                     + " WHERE fullharvests.harvest_id=?",
                     originatingHarvest)) {
            results.add(originatingHarvest);
        }
        return results;
    }

    /**
     * Returns the number of existing jobs.
     *
     * @return Number of jobs in 'jobs' table
     */
    public synchronized int getCountJobs() {
        return DBConnect.selectIntValue("SELECT COUNT(*) FROM jobs");
    }

    public synchronized long rescheduleJob(long oldJobID) {
        Connection c = DBConnect.getDBConnection();
        long newJobID = generateNextID();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT status FROM jobs WHERE job_id = ?");
            s.setLong(1, oldJobID);
            ResultSet res = s.executeQuery();
            if (!res.next()) {
                throw new UnknownID("No job with ID " + oldJobID + " to resubmit");
            }
            final JobStatus job_status = JobStatus.fromOrdinal(res.getInt(1));
            if (job_status != JobStatus.SUBMITTED &&
                job_status != JobStatus.FAILED) {
                throw new IllegalState("Job " + oldJobID + " is not ready to be copied.");
            }

            // Now do the actual copying.
            // Note that startdate, and enddate is not copied.
            // They must be null in JobStatus NEW.
            c.setAutoCommit(false);
            s = c.prepareStatement("INSERT INTO jobs "
                                   + " (job_id, harvest_id, priority, status,"
                                   + "  forcemaxcount, forcemaxbytes, orderxml,"
                                   + "  orderxmldoc, seedlist, harvest_num,"
                                   + "  num_configs, edition) "
                                   + " SELECT ?, harvest_id, priority, ?,"
                                   + "  forcemaxcount, forcemaxbytes, orderxml,"
                                   + "  orderxmldoc, seedlist, harvest_num,"
                                   + " num_configs, ?"
                                   + " FROM jobs WHERE job_id = ?");
            s.setLong(1, newJobID);
            s.setLong(2, JobStatus.NEW.ordinal());
            long initialEdition = 1;
            s.setLong(3, initialEdition);
            s.setLong(4, oldJobID);
            s.executeUpdate();
            s = c.prepareStatement("INSERT INTO job_configs "
                                   + "( job_id, config_id ) "
                                   + "SELECT ?, config_id "
                                   + "  FROM job_configs"
                                   + " WHERE job_id = ?");
            s.setLong(1, newJobID);
            s.setLong(2, oldJobID);
            s.executeUpdate();
            s = c.prepareStatement("UPDATE jobs SET status = ?"
                                   + " WHERE job_id = ?");
            s.setInt(1, JobStatus.RESUBMITTED.ordinal());
            s.setLong(2, oldJobID);
            s.executeUpdate();
            c.commit();
        } catch (SQLException e) {
            String message = "SQL error rescheduling job in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
            DBConnect.rollbackIfNeeded(c, "resubmit job", oldJobID);
        }
        return newJobID;
    }

}
