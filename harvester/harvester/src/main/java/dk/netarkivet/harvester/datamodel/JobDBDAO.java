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

package dk.netarkivet.harvester.datamodel;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FilterIterator;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.XmlUtils;
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
    
    /** Version of jobs table needed by our code. */
    private static final int JOBS_VERSION_NEEDED = 5;
    
    /** Version of job_configs table needed by our code. */
    private static final int JOB_CONFIGS_VERSION_NEEDED = 1;

    /**
     * Create a new JobDAO implemented using database.
     * This constructor also tries to upgrade the jobs and jobs_configs tables
     * in the current database.
     * throws and IllegalState exception, if it is impossible to
     * make the necessary updates.
     */
    protected JobDBDAO() {
        Connection connection = DBConnect.getDBConnection();
        int jobVersion = DBUtils.getTableVersion(connection,
                                                 "jobs");
        
        // Try to upgrade the jobs table to version JOBS_VERSION_NEEDED
        if (jobVersion < JOBS_VERSION_NEEDED) {
            log.info("Migrate table" + " 'jobs' to version "
                    + JOBS_VERSION_NEEDED);
            DBSpecifics.getInstance().updateTable("jobs", JOBS_VERSION_NEEDED);
        }
        
        DBUtils.checkTableVersion(connection, "jobs", JOBS_VERSION_NEEDED);
        DBUtils.checkTableVersion(connection,
                                  "job_configs", JOB_CONFIGS_VERSION_NEEDED
        );
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
    public synchronized void create(Job job) {
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
        
        log.debug("Creating " + job.toString());
        
        Connection dbconnection = DBConnect.getDBConnection();
        PreparedStatement statement = null;
        try {
            dbconnection.setAutoCommit(false);
            statement = dbconnection.prepareStatement(
                    "INSERT INTO jobs "
                    + "(job_id, harvest_id, status, priority, forcemaxcount, "
                    + "forcemaxbytes, orderxml, orderxmldoc, seedlist, "
                    + "harvest_num, startdate, enddate, submitteddate, "
                    + "num_configs, edition, resubmitted_as_job) "
                    + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                    + "?, ? )");
            

            statement.setLong(1, job.getJobID());
            statement.setLong(2, job.getOrigHarvestDefinitionID());
            statement.setInt(3, job.getStatus().ordinal());
            statement.setInt(4, job.getPriority().ordinal());
            statement.setLong(5, job.getForceMaxObjectsPerDomain());
            statement.setLong(6, job.getMaxBytesPerDomain());
            DBUtils.setStringMaxLength(statement, 7, job.getOrderXMLName(),
                                         Constants.MAX_NAME_SIZE, job, 
                                         "order.xml name");
            final String orderreader = job.getOrderXMLdoc().asXML();
            DBUtils.setClobMaxLength(statement, 8, orderreader,
                                       Constants.MAX_ORDERXML_SIZE, job,
                                       "order.xml");
            DBUtils.setClobMaxLength(statement, 9, job.getSeedListAsString(),
                                       Constants.MAX_COMBINED_SEED_LIST_SIZE,
                                       job, "seedlist");
            statement.setInt(10, job.getHarvestNum());
            DBUtils.setDateMaybeNull(statement, 11, job.getActualStart());
            DBUtils.setDateMaybeNull(statement, 12, job.getActualStop());
            DBUtils.setDateMaybeNull(statement, 13, job.getSubmittedDate());
            
            // The size of the configuration map == number of configurations
            statement.setInt(14, job.getDomainConfigurationMap().size());
            long initialEdition = 1;
            statement.setLong(15, initialEdition);
            DBUtils.setLongMaybeNull(statement, 16, job.getResubmittedAsJob());
            
            statement.executeUpdate();
            createJobConfigsEntries(dbconnection, job);
            dbconnection.commit();
            job.setEdition(initialEdition);
        } catch (SQLException e) {
            String message = "SQL error creating job " + job + " in database"
                             + "\n"+ ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
            DBUtils.rollbackIfNeeded(dbconnection, "create job", job);
        }
    }

    /** Create the entries in the job_configs table for this job.
     * Since some jobs have up to 10000 configs, this must be optimized.
     * The entries are only created, if job.configsChanged is true.
     *
     * @param dbconnection A connection to work on
     * @param job The job to store entries for
     * @throws SQLException If any problems occur during creation of the 
     * new entries in the job_configs table.
     */
    private void createJobConfigsEntries(Connection dbconnection, Job job)
    throws SQLException {
        if (job.configsChanged) {
            PreparedStatement statement = null;
            String tmpTable = null;
            Long jobID = job.getJobID();
            try {
                statement = dbconnection.prepareStatement(
                        "DELETE FROM job_configs WHERE job_id = ?");
                statement.setLong(1, jobID);
                statement.executeUpdate();
                statement.close();
                tmpTable = DBSpecifics.getInstance().getJobConfigsTmpTable(
                        dbconnection);
                final Map<String, String> domainConfigurationMap
                        = job.getDomainConfigurationMap();
                statement = dbconnection.prepareStatement(
                        "INSERT INTO " + tmpTable
                        + " ( domain_name, config_name ) "
                        + " VALUES ( ?, ?)");
                for (Map.Entry <String, String> entry
                        : domainConfigurationMap.entrySet()) {
                    statement.setString(1, entry.getKey());
                    statement.setString(2, entry.getValue());
                    statement.executeUpdate();
                    statement.clearParameters();
                    }
                statement.close();
                // Now we have a temp table with all the domains and configs
                statement = dbconnection.prepareStatement(
                        "INSERT INTO job_configs "
                        + "( job_id, config_id ) "
                        + "SELECT ?, configurations.config_id "
                        + "  FROM domains, configurations, " + tmpTable
                        + " WHERE domains.name = " + tmpTable 
                        + ".domain_name"
                        + "   AND domains.domain_id = configurations.domain_id"
                        + "   AND configurations.name = "
                        + tmpTable + ".config_name");
                statement.setLong(1, jobID);
                int rows = statement.executeUpdate();
                if (rows != domainConfigurationMap.size()) {
                    log.debug("Domain or configuration in table for "
                              + job + " missing: Should have "
                              + domainConfigurationMap.size()
                              + ", got " + rows);
                }
                dbconnection.commit();
            } finally {
                DBUtils.closeStatementIfOpen(statement);
                if (tmpTable != null) {
                    DBSpecifics.getInstance().dropJobConfigsTmpTable(
                            dbconnection, tmpTable);
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
        return 1 == DBUtils.selectLongValue(
                DBConnect.getDBConnection(),
                "SELECT COUNT(*) FROM jobs WHERE job_id = ?", jobID);
    }
    
    /** 
     * @see JobDAO#generateNextID()
     */
    synchronized Long generateNextID() {
        // Set to zero original, can be set after admin machine breakdown,
        // and the use this as the point of reference.
        Long restoreId = Settings.getLong(Constants.NEXT_JOB_ID);
        Long maxVal = DBUtils.selectLongValue(DBConnect.getDBConnection(),
                                              "SELECT MAX(job_id) FROM jobs"
        );
        if (maxVal == null) {
            maxVal = 0L;
        }
        // return the largest number of the two numbers: the NEXT_JOB_ID
        // declared in settings and max value of job_id used in the jobs table.
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
    public synchronized void update(Job job) {
        ArgumentNotValid.checkNotNull(job, "job");
        final Long jobID = job.getJobID();
        if (!exists(jobID)) {
            throw new UnknownID("Job id " + jobID
                                + " is not known in persistent storage");
        }
        Connection dbconnection = DBConnect.getDBConnection();
        // Not done as a transaction as it's awfully big.
        // TODO Make sure that a failed job update does... what?
        PreparedStatement statement = null;
        try {
            dbconnection.setAutoCommit(false);
            statement = dbconnection.prepareStatement(
                    "UPDATE jobs SET "
                    + "harvest_id = ?, status = ?, priority = ?, "
                    + "forcemaxcount = ?, forcemaxbytes = ?, "
                    + "orderxml = ?, "
                    + "orderxmldoc = ?, seedlist = ?, "
                    + "harvest_num = ?, harvest_errors = ?, "
                    + "harvest_error_details = ?, upload_errors = ?, "
                    + "upload_error_details = ?, startdate = ?,"
                    + "enddate = ?, num_configs = ?, edition = ?, "
                    + "submitteddate = ?, resubmitted_as_job = ?"
                    + " WHERE job_id = ? AND edition = ?");
            statement.setLong(1, job.getOrigHarvestDefinitionID());
            statement.setInt(2, job.getStatus().ordinal());
            statement.setInt(3, job.getPriority().ordinal());
            statement.setLong(4, job.getForceMaxObjectsPerDomain());
            statement.setLong(5, job.getMaxBytesPerDomain());
            DBUtils.setStringMaxLength(statement, 6, job.getOrderXMLName(),
                                         Constants.MAX_NAME_SIZE, job, 
                                         "order.xml name");
            final String orderreader = job.getOrderXMLdoc().asXML();
            DBUtils.setClobMaxLength(statement, 7, orderreader,
                                       Constants.MAX_ORDERXML_SIZE, job, 
                                       "order.xml");
            DBUtils.setClobMaxLength(statement, 8, job.getSeedListAsString(),
                                       Constants.MAX_COMBINED_SEED_LIST_SIZE, 
                                       job, "seedlist");
            statement.setInt(9, job.getHarvestNum()); // Not in job yet
            DBUtils.setStringMaxLength(statement, 10, job.getHarvestErrors(),
                                         Constants.MAX_ERROR_SIZE, job, 
                                         "harvest_error");
            DBUtils.setStringMaxLength(
                    statement, 11, job.getHarvestErrorDetails(),
                    Constants.MAX_ERROR_DETAIL_SIZE, job, 
                    "harvest_error_details");
            DBUtils.setStringMaxLength(statement, 12, job.getUploadErrors(),
                                         Constants.MAX_ERROR_SIZE, job,
                                         "upload_error");
            DBUtils.setStringMaxLength(
                    statement, 13, job.getUploadErrorDetails(),
                    Constants.MAX_ERROR_DETAIL_SIZE, job, 
                    "upload_error_details");
            long edition = job.getEdition() + 1;
            DBUtils.setDateMaybeNull(statement, 14, job.getActualStart());
            DBUtils.setDateMaybeNull(statement, 15, job.getActualStop());
            statement.setInt(16, job.getDomainConfigurationMap().size());
            statement.setLong(17, edition);
            DBUtils.setDateMaybeNull(statement, 18, job.getSubmittedDate());
            DBUtils.setLongMaybeNull(statement, 19, job.getResubmittedAsJob());
            
            statement.setLong(20, job.getJobID());
            statement.setLong(21, job.getEdition());
            final int rows = statement.executeUpdate();
            if (rows == 0) {
                String message = "Edition " + job.getEdition() 
                    + " has expired, not updating";
                log.debug(message);
                throw new PermissionDenied(message);
            }
            createJobConfigsEntries(dbconnection, job);
            dbconnection.commit();
            job.setEdition(edition);
        } catch (SQLException e) {
            String message = "SQL error updating job " + job + " in database"
                + "\n"+ ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.rollbackIfNeeded(dbconnection, "update job", job);
            DBUtils.closeStatementIfOpen(statement);
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
                                + jobID
                                + " is not known in persistent storage");
        }
        Connection dbconnection = DBConnect.getDBConnection();
        PreparedStatement statement = null;
        try {
            statement = dbconnection.prepareStatement("SELECT "
                                   + "harvest_id, status, priority, "
                                   + "forcemaxcount, forcemaxbytes, orderxml, "
                                   + "orderxmldoc, seedlist, harvest_num,"
                                   + "harvest_errors, harvest_error_details, "
                                   + "upload_errors, upload_error_details, "
                                   + "startdate, enddate, submitteddate, "
                                   + "edition, resubmitted_as_job "
                                   + "FROM jobs WHERE job_id = ?");
            statement.setLong(1, jobID);
            ResultSet result = statement.executeQuery();
            result.next();
            long harvestID = result.getLong(1);
            JobStatus status = JobStatus.fromOrdinal(result.getInt(2));
            JobPriority pri = JobPriority.fromOrdinal(result.getInt(3));
            long forceMaxCount = result.getLong(4);
            long forceMaxBytes = result.getLong(5);
            String orderxml = result.getString(6);
            
            Document orderXMLdoc = null;
            
            boolean useClobs = DBSpecifics.getInstance().supportsClob();
            if (useClobs) {
                Clob clob = result.getClob(7);
                orderXMLdoc = getOrderXMLdocFromClob(clob);
            } else {
                orderXMLdoc = XmlUtils.documentFromString(result.getString(7));
            }
            String seedlist = "";
            if (useClobs) {
                Clob clob = result.getClob(8);
                seedlist = clob.getSubString(1, (int) clob.length());
            } else {
                seedlist = result.getString(8);
            }            
            
            int harvestNum = result.getInt(9);
            String harvestErrors = result.getString(10);
            String harvestErrorDetails = result.getString(11);
            String uploadErrors = result.getString(12);
            String uploadErrorDetails = result.getString(13);
            Date startdate = DBUtils.getDateMaybeNull(result, 14);
            Date stopdate = DBUtils.getDateMaybeNull(result, 15);
            Date submittedDate = DBUtils.getDateMaybeNull(result, 16);
            Long edition = result.getLong(17);
            Long resubmittedAsJob = DBUtils.getLongMaybeNull(result, 18);
            
            statement.close();
            // IDs should match up in a natural join
            // The following if-block is an attempt to fix Bug 1856, an
            // unexplained derby deadlock, by making this statement a dirty
            // read.
            String domainStatement = 
                    "SELECT domains.name, configurations.name "
                    + "FROM domains, configurations, job_configs "
                    + "WHERE job_configs.job_id = ?"
                    + "  AND job_configs.config_id = configurations.config_id"
                    + "  AND domains.domain_id = configurations.domain_id";
            if (Settings.get(CommonSettings.DB_SPECIFICS_CLASS).
                    contains(CommonSettings.DB_IS_DERBY_IF_CONTAINS)) {
                statement = dbconnection.prepareStatement(domainStatement
                        + " WITH UR");
            } else {
                statement = dbconnection.prepareStatement(domainStatement); 
            }
            statement.setLong(1, jobID);
            result = statement.executeQuery();
            Map<String, String> configurationMap 
                = new HashMap<String, String>();
            while (result.next()) {
                String domainName = result.getString(1);
                String configName = result.getString(2);
                configurationMap.put(domainName, configName);
            }
            final Job job = new Job(harvestID, configurationMap, 
                    pri, forceMaxCount, forceMaxBytes, status, orderxml,
                    orderXMLdoc, seedlist, harvestNum);
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
            
            if (submittedDate != null) {
                job.setSubmittedDate(submittedDate);
            }
            
            job.configsChanged = false;
            job.setJobID(jobID);
            job.setEdition(edition);
            
            if (resubmittedAsJob != null) {
                job.setResubmittedAsJob(resubmittedAsJob);
            }
            return job;
        } catch (SQLException e) {
            String message = "SQL error reading job " + jobID + " in database"
                + "\n"+ ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } catch (DocumentException e) {
            String message = "XML error reading job " + jobID + " in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /** Try to extract an orderxmldoc from a given Clob.
     * This method is used by the read() method, which catches the
     * thrown DocumentException.
     * @param clob a given Clob returned from the database
     * @return a Document object based on the data in the Clob
     * @throws SQLException If data from the clob cannot be fetched.
     * @throws DocumentException If unable to create a Document object based on
     * the data in the Clob
     */
    private Document getOrderXMLdocFromClob(Clob clob) throws SQLException,
        DocumentException {
        Document doc;
        try {
            SAXReader reader = new SAXReader();
            doc = reader.read(clob.getCharacterStream());
        } catch (DocumentException e) {
            log.warn("Failed to read the contents of the clob as XML:"
                    +  clob.getSubString(1, (int) clob.length()));
            throw e;
        }
        return doc;
    }

    /**
     * Return a list of all jobs with the given status, ordered by id.
     *
     * @param status A given status.
     * @return A list of all job with given status
     */
    public synchronized Iterator<Job> getAll(JobStatus status) {
        ArgumentNotValid.checkNotNull(status, "JobStatus status");
        List<Long> idList = DBUtils.selectLongList(
                DBConnect.getDBConnection(),
                "SELECT job_id FROM jobs WHERE status = ? "
                + "ORDER BY job_id", status.ordinal());
        return new FilterIterator<Long, Job>(idList.iterator()) {
            public Job filter(Long aLong) {
                return read(aLong);
            }
        };
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
        ArgumentNotValid.checkNotNull(status, "JobStatus status");
        List<Long> idList = DBUtils.selectLongList(
                DBConnect.getDBConnection(),
                "SELECT job_id FROM jobs WHERE status = ? "
                + "ORDER BY job_id", status.ordinal());
        return idList.iterator();
    }

    /**
     * Return a list of all jobs.
     *
     * @return A list of all jobs
     */
    public synchronized Iterator<Job> getAll() {
        List<Long> idList = DBUtils.selectLongList(
                DBConnect.getDBConnection(),
                "SELECT job_id FROM jobs ORDER BY job_id");
        return new FilterIterator<Long, Job>(idList.iterator()) {
            public Job filter(Long aLong) {
                return read(aLong);
            }
        };
    }

    /**
     * Return a list of all job_ids .
     *
     * @return A list of all job_ids
     */
    public synchronized Iterator<Long> getAllJobIds(){
        List<Long> idList = DBUtils.selectLongList(
                DBConnect.getDBConnection(),
                "SELECT job_id FROM jobs ORDER BY job_id");
        return idList.iterator();
    }

    /**
     * Get a list of small and immediately usable status information
     * for given status and in given order. Is used by getStatusInfo
     * functions in order to share code (and SQL)
     * TODO should also include given harvest run
     *
     * @param jobStatusCode code for jobstatus, -1 if all
     * @param asc true if it is to be sorted in ascending order,
     *        false if it is to be sorted in descending order
     * @return List of JobStatusInfo objects for all jobs.
     * @throws ArgumentNotValid for invalid jobStatusCode
     * @throws IOFailure on trouble getting data from database
     */
    private List<JobStatusInfo> getStatusInfo(int jobStatusCode, boolean asc) {
        // Validate jobStatusCode
        // Throws ArgumentNotValid if it is an invalid job status
        if (jobStatusCode != JobStatus.ALL_STATUS_CODE) {
            JobStatus.fromOrdinal(jobStatusCode);
        }

        StringBuffer sqlBuffer = new StringBuffer(
                "SELECT jobs.job_id, status, jobs.harvest_id, "
            + "harvestdefinitions.name, harvest_num, harvest_errors,"
            + " upload_errors, orderxml, num_configs, submitteddate, startdate,"
            + " enddate, resubmitted_as_job"
            + " FROM jobs, harvestdefinitions "
            + " WHERE harvestdefinitions.harvest_id = jobs.harvest_id ");
        
        if (jobStatusCode != JobStatus.ALL_STATUS_CODE)  {
            sqlBuffer.append(" AND status = ").append(jobStatusCode);
        }
        sqlBuffer.append(" ORDER BY jobs.job_id");
        if (!asc)  { // Assume default is ASCENDING
            sqlBuffer.append(" " + HarvestStatusQuery.SORT_ORDER.DESC.name());
        }

        Connection dbconnection = DBConnect.getDBConnection();
        PreparedStatement statement = null;
        try {
            statement = dbconnection.prepareStatement(sqlBuffer.toString());
            ResultSet res = statement.executeQuery();
            return makeJobStatusInfoListFromResultset(res);
        } catch (SQLException e) {
            String message
                       = "SQL error asking for job status list in database"
                           + "\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
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
    public List<JobStatusInfo> getStatusInfo(JobStatus status) {
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
    public HarvestStatus getStatusInfo(HarvestStatusQuery query) {
    
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        
        // Obtain total count without limit
        // NB this will be a performance bottleneck if the table gets big
        long totalRowsCount = 0;
        try {
            s = c.prepareStatement(buildSqlQuery(query, true));
            ResultSet res = s.executeQuery();
            res.next();
            totalRowsCount = res.getLong(1);
        } catch (SQLException e) {
            String message
                       = "SQL error asking for job status list in database"
                           + "\n"+ ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
        
        List<JobStatusInfo> jobs = null;
        try {
            s = c.prepareStatement(buildSqlQuery(query, false));
            ResultSet res = s.executeQuery();
            jobs = makeJobStatusInfoListFromResultset(res);
        } catch (SQLException e) {
            String message
                       = "SQL error asking for job status list in database"
                           + "\n"+ ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
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
    public synchronized List<Long> getJobIDsForDuplicateReduction(long jobID)
            throws UnknownID {
        if (!exists(jobID)) {
            throw new UnknownID("Job ID '" + jobID
                                + "' does not exist in database");
        }

        List<Long> jobs;
        //Select the previous harvest from the same harvestdefinition
        Connection connection = DBConnect.getDBConnection();
        jobs = DBUtils.selectLongList(
                connection,
                "SELECT jobs.job_id FROM jobs, jobs AS original_jobs"
                + " WHERE original_jobs.job_id=?"
                + " AND jobs.harvest_id=original_jobs.harvest_id"
                + " AND jobs.harvest_num=original_jobs.harvest_num-1",
                jobID);
        List<Long> harvestDefinitions = getPreviousFullHarvests(jobID);
        if (!harvestDefinitions.isEmpty()) {
            //Select all jobs from a given list of harvest definitions
            jobs.addAll(DBUtils.selectLongList(
                    connection,
                    "SELECT jobs.job_id FROM jobs"
                    + " WHERE jobs.harvest_id IN ("
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
    private List<Long> getPreviousFullHarvests(long jobID) {
        List<Long> results = new ArrayList<Long>();
        //Find the jobs' fullharvest id
        Connection connection = DBConnect.getDBConnection();
        Long thisHarvest = DBUtils.selectFirstLongValueIfAny(
                connection,
                "SELECT jobs.harvest_id FROM jobs, fullharvests"
                + " WHERE jobs.harvest_id=fullharvests.harvest_id"
                + " AND jobs.job_id=?",
                jobID);

        if (thisHarvest == null) {
            //Not a full harvest
            return results;
        }

        //Follow the chain of orginating IDs back
        for (Long originatingHarvest = thisHarvest;
             originatingHarvest != null;
             originatingHarvest = DBUtils.selectFirstLongValueIfAny(
                     connection,
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
        Long olderHarvest = DBUtils.selectFirstLongValueIfAny(
                connection, "SELECT fullharvests.harvest_id"
                + " FROM fullharvests, harvestdefinitions,"
                + "  harvestdefinitions AS currenthd"
                + " WHERE currenthd.harvest_id=?"
                + " AND fullharvests.harvest_id=harvestdefinitions.harvest_id"
                + " AND harvestdefinitions.submitted<currenthd.submitted"
                + " ORDER BY harvestdefinitions.submitted "
                + HarvestStatusQuery.SORT_ORDER.DESC.name(), firstHarvest);
        //Follow the chain of orginating IDs back
        for (Long originatingHarvest = olderHarvest;
             originatingHarvest != null;
             originatingHarvest = DBUtils.selectFirstLongValueIfAny(
                     connection,
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
        return DBUtils.selectIntValue(DBConnect.getDBConnection(),
                                      "SELECT COUNT(*) FROM jobs");
    }
    
    /**
     * @see JobDAO#rescheduleJob(long)
     */
    public synchronized long rescheduleJob(long oldJobID) {
        Connection dbconnection = DBConnect.getDBConnection();
        long newJobID = generateNextID();
        PreparedStatement statement = null;
        try {
            statement = dbconnection.prepareStatement(
                    "SELECT status FROM jobs WHERE job_id = ?");
            statement.setLong(1, oldJobID);
            ResultSet res = statement.executeQuery();
            if (!res.next()) {
                throw new UnknownID("No job with ID " + oldJobID
                        + " to resubmit");
            }
            final JobStatus currentJobStatus
                = JobStatus.fromOrdinal(res.getInt(1));
            if (currentJobStatus != JobStatus.SUBMITTED 
                    && currentJobStatus != JobStatus.FAILED) {
                        throw new IllegalState("Job " + oldJobID
                        + " is not ready to be copied.");
            }

            // Now do the actual copying.
            // Note that startdate, and enddate is not copied.
            // They must be null in JobStatus NEW.
            dbconnection.setAutoCommit(false);
            statement = dbconnection.prepareStatement("INSERT INTO jobs "
                                   + " (job_id, harvest_id, priority, status,"
                                   + "  forcemaxcount, forcemaxbytes, orderxml,"
                                   + "  orderxmldoc, seedlist, harvest_num,"
                                   + "  num_configs, edition) "
                                   + " SELECT ?, harvest_id, priority, ?,"
                                   + "  forcemaxcount, forcemaxbytes, orderxml,"
                                   + "  orderxmldoc, seedlist, harvest_num,"
                                   + " num_configs, ?"
                                   + " FROM jobs WHERE job_id = ?");
            statement.setLong(1, newJobID);
            statement.setLong(2, JobStatus.NEW.ordinal());
            long initialEdition = 1;
            statement.setLong(3, initialEdition);
            statement.setLong(4, oldJobID);
            statement.executeUpdate();
            statement = dbconnection.prepareStatement("INSERT INTO job_configs "
                                   + "( job_id, config_id ) "
                                   + "SELECT ?, config_id "
                                   + "  FROM job_configs"
                                   + " WHERE job_id = ?");
            statement.setLong(1, newJobID);
            statement.setLong(2, oldJobID);
            statement.executeUpdate();
            statement = dbconnection.prepareStatement(
                    "UPDATE jobs SET status = ?, resubmitted_as_job = ? "
                  + " WHERE job_id = ?");
            statement.setInt(1, JobStatus.RESUBMITTED.ordinal());
            statement.setLong(2, newJobID);
            statement.setLong(3, oldJobID);
            statement.executeUpdate();
            dbconnection.commit();
        } catch (SQLException e) {
            String message = "SQL error rescheduling job in database"
                + "\n"+ ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
            DBUtils.rollbackIfNeeded(dbconnection, "resubmit job", oldJobID);
        }
        return newJobID;
    }
    
    /**
     * @see JobDAO#getStatusInfo(long, long, boolean)
     */
    public List<JobStatusInfo> getStatusInfo(long harvestId, long harvestNum,
            boolean asc) {
        ArgumentNotValid.checkNotNegative(harvestId, "long harvestId");
        ArgumentNotValid.checkNotNegative(harvestNum, "long harvestNum");
       
        return getStatusInfo(harvestId, harvestNum, asc, getSetWithAllStates());
    }
    
    /** 
     * Helper method that returns a set with all JobStatus objects.
     * 
     * @return a set with all JobStatus objects.
     */
    private Set<JobStatus> getSetWithAllStates() {
        Set<JobStatus>statusSet = new HashSet<JobStatus>();
        statusSet.addAll(Arrays.asList(JobStatus.values()));
        return statusSet;
    }
    
    
    /**
     * Get statusInfo.
     * @see JobDAO#getStatusInfo(long, long, boolean, Set)
     */
    public List<JobStatusInfo> getStatusInfo(long harvestId, long harvestNum,
            boolean asc, Set<JobStatus> selectedStatusSet) {
        ArgumentNotValid.checkNotNegative(harvestId, "harvestId");
        ArgumentNotValid.checkNotNegative(harvestNum, "harvestNum");
        ArgumentNotValid.checkNotNullOrEmpty(selectedStatusSet, 
                "selectedStatusSet");
        
        String ascdescString = (asc)? HarvestStatusQuery.SORT_ORDER.ASC.name()
                    : HarvestStatusQuery.SORT_ORDER.DESC.name();
        StringBuffer statusSortBuffer = new StringBuffer();
        
        boolean selectAllJobStates = (selectedStatusSet.size() 
                    == JobStatus.values().length);
        
        if (!selectAllJobStates) {
           if (selectedStatusSet.size() == 1) {
               int theWantedStatus = selectedStatusSet.iterator()
                   .next().ordinal();
               statusSortBuffer.append(" AND status = ")
                       .append(theWantedStatus);
           } else {
               Iterator<JobStatus> it = selectedStatusSet.iterator();
               int nextInt = it.next().ordinal();
               StringBuffer res = new StringBuffer("AND (status = " + nextInt);
               while (it.hasNext()) {
                   nextInt = it.next().ordinal();
                   res.append(" OR status = ").append(nextInt);
               }
               res.append(") ");
               statusSortBuffer.append(res);
           }
        }
   
        Connection dbconnection = DBConnect.getDBConnection();
        PreparedStatement statement = null;
        try {
            statement = dbconnection.prepareStatement(
                    "SELECT jobs.job_id, status, jobs.harvest_id, "
                    + "harvestdefinitions.name, harvest_num, harvest_errors,"
                    + " upload_errors, orderxml, num_configs, submitteddate,"
                    +   " startdate, enddate, resubmitted_as_job"
                    + " FROM jobs, harvestdefinitions "
                    + " WHERE harvestdefinitions.harvest_id = jobs.harvest_id "
                    + " AND jobs.harvest_id = "
                    + harvestId
                    + " AND jobs.harvest_num = "
                    + harvestNum
                    + statusSortBuffer.toString()
                    + " ORDER BY jobs.job_id " + ascdescString);
            ResultSet res = statement.executeQuery();
            return makeJobStatusInfoListFromResultset(res);
        } catch (SQLException e) {
            String message = "SQL error asking for job status list in database"
                + "\n"+ ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }
    
    /** Helper-method that constructs a list of JobStatusInfo objects
     * from the given resultset.
     * @param res a given resultset
     * @return a list of JobStatusInfo objects
     * @throws SQLException If any problem with accessing the data in
     * the ResultSet
     */
    private List<JobStatusInfo> makeJobStatusInfoListFromResultset(
            ResultSet res)
    throws SQLException{
        List<JobStatusInfo> joblist = new ArrayList<JobStatusInfo>();
        while (res.next()) {
            final long jobId = res.getLong(1);
            joblist.add(
                    new JobStatusInfo(
                            jobId, 
                            JobStatus.fromOrdinal(res.getInt(2)),
                            res.getLong(3), res.getString(4), res.getInt(5),
                            res.getString(6), res.getString(7),
                            res.getString(8), res.getInt(9),
                            
                            DBUtils.getDateMaybeNull(res, 10),
                            DBUtils.getDateMaybeNull(res, 11),
                            DBUtils.getDateMaybeNull(res, 12),
                            DBUtils.getLongMaybeNull(res, 13)
                            ));
        }
        return joblist;
    }
    
    /**
     * Builds a query to fetch jobs according to selection criteria.
     * @param query the selection criteria.
     * @param count build a count query instead of selecting columns.
     * @return the proper SQL query.
     */
    private String buildSqlQuery(HarvestStatusQuery query, boolean count) {

        StringBuffer sql = new StringBuffer("SELECT");
        if (count) {
            sql.append(" count(*)");
        } else {
            sql.append(" jobs.job_id, status, jobs.harvest_id,");
            sql.append(" harvestdefinitions.name, harvest_num,");
            sql.append(" harvest_errors, upload_errors, orderxml,");
            sql.append(" num_configs, submitteddate, startdate, enddate,");
            sql.append(" resubmitted_as_job");
        }

        sql.append(" FROM jobs, harvestdefinitions ");
        sql.append(" WHERE harvestdefinitions.harvest_id = jobs.harvest_id ");

        JobStatus[] jobStatuses = query.getSelectedJobStatuses();
        if (jobStatuses.length > 0) {
            if (jobStatuses.length == 1) {
                int statusOrdinal = jobStatuses[0].ordinal();
                sql.append(" AND status = ");
                sql.append(statusOrdinal);
            } else {
                sql.append("AND (status = ");
                sql.append(jobStatuses[0].ordinal());
                for (int i = 1; i < jobStatuses.length; i++) {
                    sql.append(" OR status = ");
                    sql.append(jobStatuses[i].ordinal());
                }
                sql.append(")");
            }
        }

        String harvestName = query.getHarvestName();
        if (!harvestName.isEmpty()) {
            if (harvestName.indexOf(
                    HarvestStatusQuery.HARVEST_NAME_WILDCARD) == -1) {
                // No wildcard, exact match
                sql.append(" AND harvestdefinitions.name='");
                sql.append(harvestName);
                sql.append("'");
            } else {
                String harvestNamePattern = harvestName.replaceAll("\\*", "%");
                sql.append(" AND harvestdefinitions.name LIKE '");
                sql.append(harvestNamePattern);
                sql.append("'");
            }
        }

        Long harvestRun = query.getHarvestRunNumber();
        if (harvestRun != null) {
            sql.append(" AND jobs.harvest_num = " + harvestRun);
        }

        Long harvestId = query.getHarvestId();
        if (harvestId != null) {
            sql.append(" AND harvestdefinitions.harvest_id = " + harvestId);
        }

        long startDate = query.getStartDate();
        if (startDate != HarvestStatusQuery.DATE_NONE) {
            sql.append(" AND startdate >= '");
            sql.append(new java.sql.Date(startDate).toString());
            sql.append("'");
        }

        long endDate = query.getEndDate();
        if (endDate != HarvestStatusQuery.DATE_NONE) {
            sql.append(" AND enddate <= '");
            sql.append(new java.sql.Date(endDate).toString());
            sql.append("'");
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
                        + DBSpecifics.getInstance()
                                .getOrderByLimitAndOffsetSubClause(
                                        pagesize,
                                        (query.getStartPageIndex() - 1)
                                                * pagesize));
            }
        }

        return sql.toString();
    }
}
