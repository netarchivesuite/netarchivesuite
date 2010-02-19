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

package dk.netarkivet.harvester.datamodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FilterIterator;

/**
 * A database-oriented implementation of the HarvestDefinitionDAO.
 *
 * Statements to create the tables are in
 * scripts/sql/createfullhddb.sql
 *
 */

public class HarvestDefinitionDBDAO extends HarvestDefinitionDAO {
    /** The logger. */
    private final Log log = LogFactory.getLog(getClass());
    /** The current version needed of the table 'fullharvests'. */
    static final int FULLHARVESTS_VERSION_NEEDED = 3;

    /** Create a new HarvestDefinitionDAO using database.
     */
    HarvestDefinitionDBDAO() {

        Connection connection = DBConnect.getDBConnection();
        int fullharvestsVersion = DBUtils.getTableVersion(connection,
                                                          "fullharvests"
        );
        
        if (fullharvestsVersion < FULLHARVESTS_VERSION_NEEDED) {
            log.info("Migrate table" + " 'fullharvests' to version "
                    + FULLHARVESTS_VERSION_NEEDED);
            DBSpecifics.getInstance().updateTable("fullharvests", 
                    FULLHARVESTS_VERSION_NEEDED);
        }
        
        DBUtils.checkTableVersion(connection,
                                  "harvestdefinitions", 2);
        DBUtils.checkTableVersion(connection, "fullharvests", 3);
        DBUtils.checkTableVersion(connection, "partialharvests", 1);
        DBUtils.checkTableVersion(connection, "harvest_configs", 1);
    }

    /**
     * Create a harvest definition in Database.
     * The harvest definition object should not have its ID set
     * unless we are in the middle of migrating.
     *
     * @param harvestDefinition A new harvest definition to store in
     * the database.
     * @return The harvestId for the just created harvest definition.
     * @see HarvestDefinitionDAO#create(HarvestDefinition)
     */
    public synchronized Long create(HarvestDefinition harvestDefinition) {
        Long id = harvestDefinition.getOid();
        if (id == null) {
            id = generateNextID();
        }

        Connection connection = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            connection.setAutoCommit(false);
            s = connection.prepareStatement("INSERT INTO harvestdefinitions "
                    + "( harvest_id, name, comments, numevents, submitted,"
                    + "  isactive, edition ) "
                    + "VALUES ( ?, ?, ?, ?, ?, ?, ? )");
            s.setLong(1, id);
            DBUtils.setName(s, 2, harvestDefinition, Constants.MAX_NAME_SIZE);
            DBUtils.setComments(s, 3, harvestDefinition,
                    Constants.MAX_COMMENT_SIZE);
            s.setLong(4, harvestDefinition.getNumEvents());
            Date submissiondate = new Date();
            // Don't set on object, as we may yet rollback
            s.setTimestamp(5, new Timestamp(submissiondate.getTime()));
            s.setBoolean(6, harvestDefinition.getActive());
            final int edition = 1;
            s.setLong(7, edition);
            s.executeUpdate();
            s.close();
            if (harvestDefinition instanceof FullHarvest) {
                FullHarvest fh = (FullHarvest) harvestDefinition;
                s = connection.prepareStatement("INSERT INTO fullharvests "
                        + "( harvest_id, maxobjects, maxbytes, previoushd )"
                        + "VALUES ( ?, ?, ?, ? )");
                s.setLong(1, id);
                s.setLong(2, fh.getMaxCountObjects());
                s.setLong(3, fh.getMaxBytes());
                if (fh.getPreviousHarvestDefinition() != null) {
                    s.setLong(4, fh.getPreviousHarvestDefinition().getOid());
                } else {
                    s.setNull(4, Types.BIGINT);
                }
                s.executeUpdate();
            } else if (harvestDefinition instanceof PartialHarvest) {
                PartialHarvest ph = (PartialHarvest) harvestDefinition;
                // Get schedule id
                long scheduleId = DBUtils.selectLongValue(
                        connection,
                        "SELECT schedule_id FROM schedules WHERE name = ?",
                        ph.getSchedule().getName());
                s = connection.prepareStatement("INSERT INTO partialharvests "
                        + "( harvest_id, schedule_id, nextdate ) "
                        + "VALUES ( ?, ?, ? )");
                s.setLong(1, id);
                s.setLong(2, scheduleId);
                DBUtils.setDateMaybeNull(s, 3, ph.getNextDate());
                s.executeUpdate();
                createHarvestConfigsEntries(connection, ph, id);
            } else {
                String message = "Harvest definition " + harvestDefinition
                        + " is of unknown class "
                        + harvestDefinition.getClass();
                log.warn(message);
                throw new ArgumentNotValid(message);
            }
            connection.commit();
            // Now that we have committed, set new data on object.
            harvestDefinition.setSubmissionDate(submissiondate);
            harvestDefinition.setEdition(edition);
            harvestDefinition.setOid(id);
            return id;
        } catch (SQLException e) {
            String message = "SQL error creating harvest definition "
                    + harvestDefinition + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.rollbackIfNeeded(connection, "creating", harvestDefinition);
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /** Create the entries in the harvest_configs table that connect
     * PartialHarvests and their configurations.
     *
     * @param c DB connection
     * @param ph The harvest to insert entries for.
     * @param id The id of the harvest -- this may not yet be set on ph
     * @throws SQLException If a database error occurs during the create
     * process.
     */
    private void createHarvestConfigsEntries(
            Connection c, PartialHarvest ph, long id) throws SQLException {
        PreparedStatement s = null;
        try {
            // Create harvest_configs entries
            s = c.prepareStatement("DELETE FROM harvest_configs "
                    + "WHERE harvest_id = ?");
            s.setLong(1, id);
            s.executeUpdate();
            s.close();
            s = c.prepareStatement("INSERT INTO harvest_configs "
                    + "( harvest_id, config_id ) "
                    + "SELECT ?, config_id FROM configurations, domains "
                    + "WHERE domains.name = ? AND configurations.name = ?"
                    + "  AND domains.domain_id = configurations.domain_id");
            for (Iterator<DomainConfiguration> dcs
                    = ph.getDomainConfigurations(); dcs.hasNext();) {
                DomainConfiguration dc = dcs.next();
                s.setLong(1, id);
                s.setString(2, dc.getDomain().getName());
                s.setString(3, dc.getName());
                s.executeUpdate();
            }
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Generates the next id of a harvest definition. this implementation
     * retrieves the maximum value of harvest_id in the DB, and
     * returns this value + 1.
     * @return The next available ID
     * @see HarvestDefinitionDAO#generateNextID()
     */
    protected Long generateNextID() {
        Long maxVal = DBUtils.selectLongValue(
                DBConnect.getDBConnection(),
                "SELECT max(harvest_id) FROM harvestdefinitions");
        if (maxVal == null) {
            maxVal = 0L;
        }
        return maxVal + 1L;
    }

    /**
     * Read the stored harvest definition for the given ID.
     * @see HarvestDefinitionDAO#read(Long)
     * @param harvestDefinitionID An ID number for a harvest definition
     * @return A harvest definition that has been read from persistent storage.
     * @throws UnknownID if no entry with that ID exists in the database
     * @throws IOFailure If DB-failure occurs?
     */
    public synchronized HarvestDefinition read(
            Long harvestDefinitionID) throws UnknownID, IOFailure {
        if (!exists(harvestDefinitionID)) {
            String message = "Unknown harvest definition "
                + harvestDefinitionID;
            log.debug(message);
            throw new UnknownID(message);
        }
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(
                                "SELECT name, comments, numevents, submitted, "
                                + "previoushd, maxobjects, maxbytes, isactive, "
                                + "edition "
                                + "FROM harvestdefinitions, fullharvests "
                                + "WHERE harvestdefinitions.harvest_id = ?"
                                + "  AND harvestdefinitions.harvest_id "
                                        + " = fullharvests.harvest_id");
            s.setLong(1, harvestDefinitionID);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                // Found full harvest
                final String name = res.getString(1);
                final String comments = res.getString(2);
                final int numEvents = res.getInt(3);
                final Date submissionDate = new Date(
                        res.getTimestamp(4).getTime());
                final long maxObjects = res.getLong(6);
                final long maxBytes = res.getLong(7);
                FullHarvest fh;
                long prevhd = res.getLong(5);
                if (!res.wasNull()) {
                    fh = new FullHarvest(name, comments, prevhd, maxObjects,
                                         maxBytes);
                } else {
                    fh = new FullHarvest(name, comments, null, maxObjects,
                                         maxBytes);
                }
                fh.setSubmissionDate(submissionDate);
                fh.setNumEvents(numEvents);
                fh.setActive(res.getBoolean(8));
                fh.setOid(harvestDefinitionID);
                fh.setEdition(res.getLong(9));
                // We found a FullHarvest object, just return it.
                return fh;
            }
            s.close();
            // No full harvest with that ID, try selective harvest
            s = c.prepareStatement(
                    "SELECT harvestdefinitions.name,"
                    + "       harvestdefinitions.comments,"
                    + "       harvestdefinitions.numevents,"
                    + "       harvestdefinitions.submitted,"
                    + "       harvestdefinitions.isactive,"
                    + "       harvestdefinitions.edition,"
                    + "       schedules.name,"
                    + "       partialharvests.nextdate "
                    + "FROM harvestdefinitions, partialharvests, schedules"
                    + " WHERE harvestdefinitions.harvest_id = ?"
                    + "   AND harvestdefinitions.harvest_id "
                            + "= partialharvests.harvest_id"
                    + "   AND schedules.schedule_id " 
                            + "= partialharvests.schedule_id");
            s.setLong(1, harvestDefinitionID);
            res = s.executeQuery();
            res.next();
            // Have to get configs before creating object, so storing data here.
            final String name = res.getString(1);
            final String comments = res.getString(2);
            final int numEvents = res.getInt(3);
            final Date submissionDate = new Date(res.getTimestamp(4).getTime());
            final boolean active = res.getBoolean(5);
            final long edition = res.getLong(6);
            final String scheduleName = res.getString(7);
            final Date nextDate = DBUtils.getDateMaybeNull(res, 8);
            s.close();
            // Found partial harvest -- have to find configurations.
            // To avoid holding on to the readlock while getting domains,
            // we grab the strings first, then look up domains and configs.
            final DomainDAO domainDao = DomainDAO.getInstance();
            /** Helper class that contain (domainName, configName) pairs.*/
            class DomainConfigPair {
                /** The domain name. */
                final String domainName;
                /** The config name. */
                final String configName;
                
                /** Constructor for the DomainConfigPair class. 
                 * 
                 * @param domainName A given domain name
                 * @param configName A name for a specific configuration
                 */
                public DomainConfigPair(String domainName, String configName) {
                    this.domainName = domainName;
                    this.configName = configName;
                }
            }
            List<DomainConfigPair> configs
                    = new ArrayList<DomainConfigPair>();
            s = c.prepareStatement("SELECT domains.name, configurations.name "
                    + "FROM domains, configurations, harvest_configs "
                    + "WHERE harvest_id = ?"
                    + "  AND configurations.config_id "
                            + "= harvest_configs.config_id"
                    + "  AND configurations.domain_id = domains.domain_id");
            s.setLong(1, harvestDefinitionID);
            res = s.executeQuery();
            while (res.next()) {
                configs.add(new DomainConfigPair(
                        res.getString(1), res.getString(2)));
            }
            s.close();
            List<DomainConfiguration> configurations =
                    new ArrayList<DomainConfiguration>();
            for (DomainConfigPair domainConfig : configs) {
                Domain d = domainDao.read(domainConfig.domainName);
                configurations.add(d.getConfiguration(domainConfig.configName));
            }

            Schedule schedule =
                    ScheduleDAO.getInstance().read(scheduleName);

            PartialHarvest ph = new PartialHarvest(configurations, schedule,
                    name, comments);

            ph.setNumEvents(numEvents);
            ph.setSubmissionDate(submissionDate);
            ph.setActive(active);
            ph.setEdition(edition);
            ph.setNextDate(nextDate);
            ph.setOid(harvestDefinitionID);
            return ph;
        } catch (SQLException e) {
            throw new IOFailure("SQL Error while reading harvest definition "
                    + harvestDefinitionID + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }
    
    /**
     * @see HarvestDefinitionDAO#describeUsages(Long)
     */
    public String describeUsages(Long oid) {
        Connection connection = DBConnect.getDBConnection();
        List<Long> usages = DBUtils.selectLongList(connection,
                                                   "SELECT job_id FROM jobs"
                + " WHERE jobs.harvest_id = ? ", oid);
        if (usages.size() != 0) {
            return "Harvested by jobs " + usages;
        } else {
            List<String> dependencies = DBUtils.selectStringList(
                    connection,
                    "SELECT name FROM harvestdefinitions, fullharvests"
                    + " WHERE fullharvests.harvest_id "
                                + "= harvestdefinitions.harvest_id"
                    + "   AND fullharvests.previoushd = ?",
                    oid);
            if (dependencies.size() != 0) {
                return "Is the basis of snapshot harvests " + dependencies;
            } else {
                return null;
            }
        }
    }

    /**
     * Delete a harvest definition from persistent storage.
     * You cannot delete a harvest definition that is referenced
     * by jobs or harvestinfo.
     *
     * @param oid The ID of a harvest definition to delete.
     * @see HarvestDefinitionDAO#delete(Long)
     */
    public synchronized void delete(Long oid) {
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            c.setAutoCommit(false);
            // First check that nobody's referencing it
            String usages = describeUsages(oid);
            if (usages != null) {
                String message = "Cannot delete harvest definition " + oid
                        + ":" + usages;
                log.debug(message);
                throw new ArgumentNotValid(message);
            }
            for (String table : new String[] {"harvestdefinitions",
                                           "fullharvests",
                                           "partialharvests",
                                           "harvest_configs"}) {
                s = c.prepareStatement("DELETE FROM " + table
                        + " WHERE harvest_id = ?");
                s.setLong(1, oid);
                s.executeUpdate();
            }
            log.debug("Deleting harvest definition " + oid);
            c.commit();
        } catch (SQLException e) {
            throw new IOFailure("SQL Error while deleting harvest definition "
                    + oid + "\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.rollbackIfNeeded(c, "deleting harvestdefinition", oid);
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Update an existing harvest definition with new info.
     *
     * @param hd An updated harvest definition
     * @see HarvestDefinitionDAO#update(HarvestDefinition)
     */
    public synchronized void update(HarvestDefinition hd) {
        ArgumentNotValid.checkNotNull(hd, "HarvestDefinition hd");
        if (hd.getOid() == null || !exists(hd.getOid())) {
            final String message = "Cannot update non-existing "
                    + "harvestdefinition '" + hd.getName() + "'";
            log.debug(message);
            throw new PermissionDenied(message);
        }
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            c.setAutoCommit(false);
            s = c.prepareStatement("UPDATE harvestdefinitions SET "
                    + "name = ?, "
                    + "comments = ?, "
                    + "numevents = ?, "
                    + "submitted = ?,"
                    + "isactive = ?,"
                    + "edition = ? "
                    + "WHERE harvest_id = ? AND edition = ?");
            DBUtils.setName(s, 1, hd, Constants.MAX_NAME_SIZE);
            DBUtils.setComments(s, 2, hd, Constants.MAX_COMMENT_SIZE);
            s.setInt(3, hd.getNumEvents());
            s.setTimestamp(4, new Timestamp(hd.getSubmissionDate().getTime()));
            s.setBoolean(5, hd.getActive());
            long nextEdition = hd.getEdition() + 1;
            s.setLong(6, nextEdition);
            s.setLong(7, hd.getOid());
            s.setLong(8, hd.getEdition());
            int rows = s.executeUpdate();
            // Since the HD exists, no rows indicates bad edition
            if (rows == 0) {
                String message = "Somebody else must have updated " + hd
                        + " since edition " + hd.getEdition()
                        + ", not updating";
                log.debug(message);
                throw new PermissionDenied(message);
            }
            s.close();
            if (hd instanceof FullHarvest) {
                FullHarvest fh = (FullHarvest) hd;
                s = c.prepareStatement("UPDATE fullharvests SET "
                        + "previoushd = ?, "
                        + "maxobjects = ?, "
                        + "maxbytes = ? "
                        + "WHERE harvest_id = ?");
                if (fh.getPreviousHarvestDefinition() != null) {
                    s.setLong(1, fh.getPreviousHarvestDefinition().getOid());
                } else {
                    s.setNull(1, Types.BIGINT);
                }
                s.setLong(2, fh.getMaxCountObjects());
                s.setLong(3, fh.getMaxBytes());
                s.setLong(4, fh.getOid());
                rows = s.executeUpdate();
            } else if (hd instanceof PartialHarvest) {
                PartialHarvest ph = (PartialHarvest) hd;
                s = c.prepareStatement("UPDATE partialharvests SET "
                        + "schedule_id = "
                        + "    (SELECT schedule_id FROM schedules "
                                    + "WHERE schedules.name = ?), "
                        + "nextdate = ? "
                        + "WHERE harvest_id = ?");
                s.setString(1, ph.getSchedule().getName());
                DBUtils.setDateMaybeNull(s, 2, ph.getNextDate());
                s.setLong(3, ph.getOid());
                rows = s.executeUpdate();
                s.close();
                createHarvestConfigsEntries(c, ph, ph.getOid());
            } else {
                String message = "Harvest definition " + hd
                        + " has unknown class " + hd.getClass();
                log.warn(message);
                throw new ArgumentNotValid(message);
            }
            c.commit();
            hd.setEdition(nextEdition);
        } catch (SQLException e) {
            throw new IOFailure("SQL error while updating harvest definition "
                    + hd + "\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.rollbackIfNeeded(c, "updating", hd);
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * @see HarvestDefinitionDAO#exists(Long)
     */
    public synchronized boolean exists(Long oid) {
        return 1 == DBUtils.selectIntValue(DBConnect.getDBConnection(),
                                           "SELECT COUNT(harvest_id) "
                + "FROM harvestdefinitions WHERE harvest_id = ?", oid);
    }

    /**
     * Get a list of all existing harvest definitions ordered by name.
     *
     * @return An iterator that give the existing harvest definitions in turn
     */
    public synchronized Iterator<HarvestDefinition> getAllHarvestDefinitions() {
        List<Long> hds = DBUtils.selectLongList(
                DBConnect.getDBConnection(),
                "SELECT harvest_id FROM harvestdefinitions "
                + "ORDER BY name");
        return new FilterIterator<Long, HarvestDefinition>(hds.iterator()) {
            public HarvestDefinition filter(Long id) {
                return read(id);
            }
        };
    }

    /**
     * Gets default configurations for all domains that are not aliases.
     *
     * This method currently gives an iterator that reads in all domains,
     * although only on demand, that is: when calling "hasNext".
     *
     * @return Iterator containing the default DomainConfiguration for all
     * domains that are not aliases
     */
    public synchronized Iterator<DomainConfiguration>
            getSnapShotConfigurations() {
        return new FilterIterator<Domain, DomainConfiguration>(
                DomainDAO.getInstance().getAllDomainsInSnapshotHarvestOrder()) {
            public DomainConfiguration filter(Domain domain) {
                if (domain.getAliasInfo() == null
                        || domain.getAliasInfo().isExpired()) {
                    return domain.getDefaultConfiguration();
                } else {
                    return null;
                }
            }
        };
    }

    /** Returns a list of IDs of harvest definitions that are ready to be
     * scheduled.
     * @param now The current date
     * @return List of ready harvest definitions.   No check is performed for
     * whether these are already in the middle of being scheduled.
     */
    public Iterable<Long> getReadyHarvestDefinitions(Date now) {
        ArgumentNotValid.checkNotNull(now, "Date now");
        Connection connection = DBConnect.getDBConnection();
        List<Long> ids = DBUtils.selectLongList(
                connection,
                "SELECT fullharvests.harvest_id"
                + " FROM fullharvests, harvestdefinitions"
                + " WHERE harvestdefinitions.harvest_id "
                        + "= fullharvests.harvest_id"
                + "   AND isactive = ? AND numevents < 1", true);
        ids.addAll(DBUtils.selectLongList(
                connection,
                "SELECT partialharvests.harvest_id"
                + " FROM partialharvests, harvestdefinitions"
                + " WHERE harvestdefinitions.harvest_id "
                        + "= partialharvests.harvest_id"
                + "   AND isactive = ?"
                + "   AND nextdate IS NOT NULL"
                + "   AND nextdate < ?", true, now));
        return ids;
    }

    /**
     * Get the harvest definition that has the given name, if any.
     *
     * @param name The name of a harvest definition.
     * @return The HarvestDefinition object with that name, or null if none
     *         has that name.
     */
    public synchronized HarvestDefinition getHarvestDefinition(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT harvest_id FROM harvestdefinitions "
                    + "WHERE name = ?");
            s.setString(1, name);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                long harvestDefinitionID = res.getLong(1);
                s.close();
                return read(harvestDefinitionID);
            }
            return null;
        } catch (SQLException e) {
            throw new IOFailure("SQL error while getting HD by name" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Returns an iterator of all snapshot harvest definitions ordered by name.
     *
     * @return An iterator (possibly empty) of FullHarvests
     * @see HarvestDefinitionDAO#getAllFullHarvestDefinitions()
     */
    public synchronized Iterator<FullHarvest> getAllFullHarvestDefinitions() {
        List<Long> hds = DBUtils.selectLongList(
                DBConnect.getDBConnection(),
                "SELECT fullharvests.harvest_id "
                + "FROM fullharvests, harvestdefinitions "
                + "WHERE fullharvests.harvest_id "
                        + "= harvestdefinitions.harvest_id "
                + "ORDER BY harvestdefinitions.name");
        return new FilterIterator<Long, FullHarvest>(hds.iterator()) {
            public FullHarvest filter(Long id) {
                return (FullHarvest) read(id);
            }
        };
    }


    /**
     * Returns an iterator of all non-snapshot harvest definitions ordered
     * by name.
     *
     * @return An iterator (possibly empty) of PartialHarvests
     * @see HarvestDefinitionDAO#getAllPartialHarvestDefinitions()
     */
    public synchronized Iterator<PartialHarvest>
    getAllPartialHarvestDefinitions() {
        List<Long> hds = DBUtils.selectLongList(
                DBConnect.getDBConnection(),
                "SELECT partialharvests.harvest_id "
                + "FROM partialharvests, harvestdefinitions "
                + "WHERE partialharvests.harvest_id "
                    + "= harvestdefinitions.harvest_id "
                + "ORDER BY harvestdefinitions.name");
        return new FilterIterator<Long, PartialHarvest>(hds.iterator()) {
            public PartialHarvest filter(Long id) {
                return (PartialHarvest) read(id);
            }
        };
    }
    /**
     * @see HarvestDefinitionDAO#getHarvestRunInfo(long)
     */
    public List<HarvestRunInfo> getHarvestRunInfo(long harvestID) {
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            ResultSet res = null;
            Map<Integer, HarvestRunInfo> runInfos =
                    new HashMap<Integer, HarvestRunInfo>();
            List<HarvestRunInfo> infoList = new ArrayList<HarvestRunInfo>();

            // Synchronize on this to make sure no new jobs are added between
            // two selects
            synchronized(this) {
                // Select dates and counts for all different statues
                // for each run
                s = c.prepareStatement(
                        "SELECT name, harvest_num, status, "
                        + "       MIN(startdate), MAX(enddate), COUNT(job_id)"
                        + "  FROM jobs, harvestdefinitions"
                        + " WHERE harvestdefinitions.harvest_id = ?"
                        + "   AND jobs.harvest_id "
                                    + "= harvestdefinitions.harvest_id"
                        + " GROUP BY name, harvest_num, status"
                        + " ORDER BY harvest_num");
                s.setLong(1, harvestID);
                res = s.executeQuery();
                while (res.next()) {
                    int runNr = res.getInt(2);
                    HarvestRunInfo info = runInfos.get(runNr);
                    if (info == null) {
                        String name = res.getString(1);
                        info = new HarvestRunInfo(harvestID, name, runNr);
                        // Put into hash for easy access when updating.
                        runInfos.put(runNr, info);
                        // Add to return list in order given by DB
                        infoList.add(info);
                    }
                    JobStatus status = JobStatus.fromOrdinal(res.getInt(3));
                    // For started stati, check start date
                    if (status != JobStatus.NEW
                        && status != JobStatus.SUBMITTED
                        && status != JobStatus.RESUBMITTED) {
                        Date startDate = DBUtils.getDateMaybeNull(res, 4);
                        if (info.getStartDate() == null
                            || (startDate != null
                                && startDate.before(info.getStartDate()))) {
                            info.setStartDate(startDate);
                        }
                    }
                    // For finished jobs, check end date
                    if (status == JobStatus.DONE
                        || status == JobStatus.FAILED) {
                        Date endDate = DBUtils.getDateMaybeNull(res, 5);
                        if (info.getEndDate() == null
                            || (endDate != null
                                && endDate.after(info.getEndDate()))) {
                            info.setEndDate(endDate);
                        }
                    }
                    int count = res.getInt(6);
                    info.setStatusCount(status, count);
                }

                s = c.prepareStatement(
                        "SELECT jobs.harvest_num,"
                        + "SUM(historyinfo.bytecount), "
                        + "SUM(historyinfo.objectcount),"
                        + "COUNT(jobs.status)"
                        + " FROM jobs, historyinfo "
                        + " WHERE jobs.harvest_id = ? "
                        + "   AND historyinfo.job_id = jobs.job_id"
                        + " GROUP BY jobs.harvest_num"
                        + " ORDER BY jobs.harvest_num");
                s.setLong(1, harvestID);
                res = s.executeQuery();
            }

            while (res.next()) {
                final int harvestNum = res.getInt(1);
                HarvestRunInfo info = runInfos.get(harvestNum);
                // TODO If missing?
                info.setBytesHarvested(res.getLong(2));
                info.setDocsHarvested(res.getLong(3));
            }

            // Make sure that jobs that aren't really done don't have end date.
            for (HarvestRunInfo info : infoList) {
                if (info.getJobCount(JobStatus.STARTED) != 0
                    || info.getJobCount(JobStatus.NEW) != 0
                    || info.getJobCount(JobStatus.SUBMITTED) != 0) {
                    info.setEndDate(null);
                }
            }
            return infoList;
        } catch (SQLException e) {
            String message = "SQL error asking for harvest run info on "
                    + harvestID + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }
    
    /**
     * @see HarvestDefinitionDAO#mayDelete(HarvestDefinition)
     */
    public boolean mayDelete(HarvestDefinition hd) {
        ArgumentNotValid.checkNotNull(hd, "HarvestDefinition hd");
        // May not delete if harvested
        Connection connection = DBConnect.getDBConnection();
        if (DBUtils.selectAny(connection, "SELECT job_id "
                + " FROM jobs WHERE harvest_id = ?",
                              hd.getOid())) {
            return false;
        }
        if (DBUtils.selectAny(connection,
                              "SELECT harvest_id FROM fullharvests"
                + " WHERE previoushd = ?", hd.getOid())) {
            return false;
        }
        return true;
    }

    /**
     * Get all domain,configuration pairs for a harvest definition in sparse
     * version for GUI purposes.
     *
     * @param harvestDefinitionID The ID of the harvest definition.
     * @return Domain,configuration pairs for that HD. Returns an empty iterable
     *         for unknown harvest definitions.
     * @throws ArgumentNotValid on null argument.
     */
    public Iterable<SparseDomainConfiguration> getSparseDomainConfigurations(
            Long harvestDefinitionID) {
        ArgumentNotValid.checkNotNull(harvestDefinitionID,
                                      "harvestDefinitionID");
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT domains.name, configurations.name "
                                   + "FROM domains, configurations,"
                                   + " harvest_configs "
                                   + "WHERE harvest_id = ?"
                                   + "  AND configurations.config_id "
                                   + "      = harvest_configs.config_id"
                                   + "  AND configurations.domain_id "
                                   + "      = domains.domain_id");
            s.setLong(1, harvestDefinitionID);
            ResultSet res = s.executeQuery();
            List<SparseDomainConfiguration> resultList
                    = new ArrayList<SparseDomainConfiguration>();
            while (res.next()) {
                SparseDomainConfiguration sdc = new SparseDomainConfiguration(
                        res.getString(1), res.getString(2));
                resultList.add(sdc);
            }
            return resultList;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse domains" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Get all sparse versions of partial harvests for GUI purposes.
     *
     * @return An iterable (possibly empty) of SparsePartialHarvests
     */
    public Iterable<SparsePartialHarvest>
            getAllSparsePartialHarvestDefinitions() {
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(
                    "SELECT harvestdefinitions.harvest_id,"
                    + "       harvestdefinitions.name,"
                    + "       harvestdefinitions.comments,"
                    + "       harvestdefinitions.numevents,"
                    + "       harvestdefinitions.submitted,"
                    + "       harvestdefinitions.isactive,"
                    + "       harvestdefinitions.edition,"
                    + "       schedules.name,"
                    + "       partialharvests.nextdate "
                    + "FROM harvestdefinitions, partialharvests, schedules"
                    + " WHERE harvestdefinitions.harvest_id "
                    + "       = partialharvests.harvest_id"
                    + "   AND schedules.schedule_id "
                    + "       = partialharvests.schedule_id "
                    + "ORDER BY harvestdefinitions.name");
            ResultSet res = s.executeQuery();
            List<SparsePartialHarvest> harvests
                    = new ArrayList<SparsePartialHarvest>();
            while (res.next()) {
                SparsePartialHarvest sph = new SparsePartialHarvest(
                        res.getLong(1),
                        res.getString(2), res.getString(3), res.getInt(4),
                        new Date(res.getTimestamp(5).getTime()),
                        res.getBoolean(6), res.getLong(7), res.getString(8),
                        DBUtils.getDateMaybeNull(res, 9));
                harvests.add(sph);
            }
            return harvests;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvests" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Get a sparse version of a partial harvest for GUI purposes.
     *
     * @param harvestName Name of harvest definition.
     * @return Sparse version of partial harvest or null for none.
     * @throws ArgumentNotValid on null or empty name.
     */
    public SparsePartialHarvest getSparsePartialHarvest(
            String harvestName) {
        ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(
                    "SELECT harvestdefinitions.harvest_id,"
                    + "       harvestdefinitions.comments,"
                    + "       harvestdefinitions.numevents,"
                    + "       harvestdefinitions.submitted,"
                    + "       harvestdefinitions.isactive,"
                    + "       harvestdefinitions.edition,"
                    + "       schedules.name,"
                    + "       partialharvests.nextdate "
                    + "FROM harvestdefinitions, partialharvests, schedules"
                    + " WHERE harvestdefinitions.name = ?"
                    + "   AND harvestdefinitions.harvest_id "
                                + "= partialharvests.harvest_id"
                    + "   AND schedules.schedule_id "
                                + "= partialharvests.schedule_id");
            s.setString(1, harvestName);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                return new SparsePartialHarvest(
                        res.getLong(1),
                        harvestName, res.getString(2), res.getInt(3),
                        new Date(res.getTimestamp(4).getTime()),
                        res.getBoolean(5), res.getLong(6), res.getString(7),
                        DBUtils.getDateMaybeNull(res, 8));
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvest" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Get all sparse versions of full harvests for GUI purposes.
     *
     * @return An iterable (possibly empty) of SparseFullHarvests
     */
    public Iterable<SparseFullHarvest> getAllSparseFullHarvestDefinitions() {
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(
                    "SELECT harvestdefinitions.harvest_id,"
                    + "       harvestdefinitions.name,"
                    + "       harvestdefinitions.comments,"
                    + "       harvestdefinitions.numevents,"
                    + "       harvestdefinitions.isactive,"
                    + "       harvestdefinitions.edition,"
                    + "       fullharvests.maxobjects,"
                    + "       fullharvests.maxbytes,"
                    + "       fullharvests.previoushd "
                    + "FROM harvestdefinitions, fullharvests"
                    + " WHERE harvestdefinitions.harvest_id "
                    + "       = fullharvests.harvest_id");
            ResultSet res = s.executeQuery();
            List<SparseFullHarvest> harvests
                    = new ArrayList<SparseFullHarvest>();
            while (res.next()) {
                SparseFullHarvest sfh = new SparseFullHarvest(
                        res.getLong(1),
                        res.getString(2), res.getString(3), res.getInt(4),
                        res.getBoolean(5), res.getLong(6), res.getLong(7),
                        res.getLong(8), DBUtils.getLongMaybeNull(res, 9));
                harvests.add(sfh);
            }
            return harvests;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvests" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Get the name of a harvest given its ID.
     *
     * @param harvestDefinitionID The ID of a harvest
     *
     * @return The name of the given harvest.
     *
     * @throws ArgumentNotValid on null argument
     * @throws UnknownID        if no harvest has the given ID.
     * @throws IOFailure        on any other error talking to the database
     */
    public String getHarvestName(Long harvestDefinitionID) {
        ArgumentNotValid.checkNotNull(harvestDefinitionID,
                                      "harvestDefinitionID");
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(
                    "SELECT name FROM harvestdefinitions WHERE harvest_id = ?");
            s.setLong(1, harvestDefinitionID);
            ResultSet res = s.executeQuery();
            String name = null;
            while (res.next()) {
                if (name != null) {
                    throw new IOFailure("Found more than one name for harvest"
                                        + " definition " + harvestDefinitionID
                                        + ": '" + name + "' and '"
                                        + res.getString(1) + "'");
                }
                name = res.getString(1);
            }
            if (name == null) {
                throw new UnknownID("No name found for harvest definition "
                                    + harvestDefinitionID);
            }
            return name;
        } catch (SQLException e) {
            throw new IOFailure("An error occurred finding the name for "
                                + "harvest definition " + harvestDefinitionID 
                                + "\n" 
                                + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Get whether a given harvest is a snapshot or selective harvest.
     *
     * @param harvestDefinitionID ID of a harvest
     *
     * @return True if the given harvest is a snapshot harvest, false
     *         otherwise.
     *
     * @throws ArgumentNotValid on null argument
     * @throws UnknownID        if no harvest has the given ID.
     */
    public boolean isSnapshot(Long harvestDefinitionID) {
        ArgumentNotValid.checkNotNull(harvestDefinitionID,
                                      "harvestDefinitionID");
        Connection connection = DBConnect.getDBConnection();
        boolean isSnapshot = DBUtils.selectAny(
                connection,
                "SELECT harvest_id FROM fullharvests WHERE harvest_id = ?",
                harvestDefinitionID);
        if (isSnapshot) {
            return true;
        }
        boolean isSelective = DBUtils.selectAny(
                connection,
                "SELECT harvest_id FROM partialharvests WHERE harvest_id = ?",
                harvestDefinitionID);
        if (isSelective) {
            return false;
        }
        throw new UnknownID("Failed to find harvest definition with id "
                            + harvestDefinitionID);
    }

    /**
     * Get a sparse version of a full harvest for GUI purposes.
     *
     * @param harvestName Name of harvest definition.
     * @return Sparse version of full harvest or null for none.
     * @throws ArgumentNotValid on null or empty name.
     * @throws UnknownID        if no harvest has the given ID.
     * @throws IOFailure        on any other error talking to the database
     */
    public SparseFullHarvest getSparseFullHarvest(
            String harvestName) {
        ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(
                    "SELECT harvestdefinitions.harvest_id,"
                    + "       harvestdefinitions.comments,"
                    + "       harvestdefinitions.numevents,"
                    + "       harvestdefinitions.isactive,"
                    + "       harvestdefinitions.edition,"
                    + "       fullharvests.maxobjects,"
                    + "       fullharvests.maxbytes,"
                    + "       fullharvests.previoushd "
                    + "FROM harvestdefinitions, fullharvests"
                    + " WHERE harvestdefinitions.name = ?"
                    + "   AND harvestdefinitions.harvest_id "
                    + "       = fullharvests.harvest_id");
            s.setString(1, harvestName);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                return new SparseFullHarvest(
                        res.getLong(1),
                        harvestName, res.getString(2), res.getInt(3),
                        res.getBoolean(4), res.getLong(5), res.getLong(6),
                        res.getLong(7), DBUtils.getLongMaybeNull(res, 8));
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvest" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }
    
    
    /** Get a sorted list of all domainnames of a HarvestDefinition.
    *
    * @param harvestName of HarvestDefinition
    * @return List of all domains of the HarvestDefinition.
    */
    public List<String> getListOfDomainsOfHarvestDefinition(
            String harvestName) {
        ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(
                    "SELECT DISTINCT do.name "
                    +" FROM     domains do,"
                    +"          configurations co,"
                    +"          harvest_configs haco,"
                    +"          harvestdefinitions hd"
                    +" WHERE    co.domain_id = do.domain_id"
                    +"          AND haco.config_id = co.config_id"
                    +"          AND haco.harvest_id = hd.harvest_id"
                    +"          AND hd.name = ?" 
                    +" ORDER BY do.name");
            s.setString(1, harvestName);
            ResultSet res = s.executeQuery();
            List<String> domains = new ArrayList<String>();

            while (res.next()) {
                domains.add(res.getString(1));
            }
            return domains;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting seeds of a domain of a "
                    + "harvest definition"
                    + "\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /** Get a sorted list of all seeds of a Domain in a HarvestDefinition.
    *
    * @param harvestName of HarvestDefinition
    * @param domainName of Domain
    * @return List of all seeds of the Domain in the HarvestDefinition.
    */
    public List<String> getListOfSeedsOfDomainOfHarvestDefinition(
            String harvestName, String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(
                    "SELECT sl.seeds"
                    +" FROM   configurations co,"
                    +"        harvest_configs haco,"
                    +"        harvestdefinitions hd,"
                    +"        seedlists sl,"
                    +"        config_seedlists cose,"
                    +"        domains do"
                    +" WHERE  cose.seedlist_id = sl.seedlist_id"
                    +"        AND co.config_id = cose.config_id"
                    +"        AND co.config_id = haco.config_id"
                    +"        AND haco.harvest_id = hd.harvest_id"
                    +"        AND co.domain_id = do.domain_id"
                    +"        AND do.name = ?" 
                    +"        AND hd.name = ?");
            s.setString(1, domainName);
            s.setString(2, harvestName);
            ResultSet res = s.executeQuery();
            List<String> seeds = new ArrayList<String>();

            while (res.next()) {
                String seedsOfDomain = res.getString(1);
                
                    StringTokenizer st
                        = new StringTokenizer(seedsOfDomain, "\n");
                    
                    while(st.hasMoreTokens()) {
                        String seed = st.nextToken();
                        
                        boolean bFound = false;
                        for (String entry: seeds) {
                            if (entry.equals(seed)) {
                                bFound = true;
                                break;
                            }
                        }
                        
                        // duplicates will not be added
                        if (!bFound) {
                            seeds.add(seed);
                        }
                    }
            }
            
            Collections.sort(seeds, Collator.getInstance());     
            
            return seeds;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting seeds of a domain" + "\n"
                                +ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }
    
}
