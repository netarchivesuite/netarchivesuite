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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FilterIterator;

/**
 * A database-oriented implementation of the HarvestDefinitionDAO.
 *
 * Statements to create the tables are in
 * scripts/sql/createfullhddb.sql
 *
 */

public class HarvestDefinitionDBDAO extends HarvestDefinitionDAO {
    private final Log log = LogFactory.getLog(getClass());

    /** Create a new HarvestDefinitionDAO using database.
     */
    HarvestDefinitionDBDAO() {
    	DBConnect.checkTableVersion("harvestdefinitions", 2);
        DBConnect.checkTableVersion("fullharvests", 2);
        DBConnect.checkTableVersion("partialharvests", 1);
        DBConnect.checkTableVersion("harvest_configs", 1);
    }

    /**
     * Create a harvest definition in Database.
     * The harvest definition object should not have its ID set
     * unless we are in the middle of migrating.
     *
     * @param harvestDefinition A new harvest definition to store in the database.
     * @return The harvestId for the just created harvest definition.
     * @see HarvestDefinitionDAO#create(HarvestDefinition)
     */
    public synchronized Long create(HarvestDefinition harvestDefinition) {
        Long id = harvestDefinition.getOid();
        if (id == null) {
            id = generateNextID();
        }

        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            c.setAutoCommit(false);
            s = c.prepareStatement("INSERT INTO harvestdefinitions "
                    + "( harvest_id, name, comments, numevents, submitted,"
                    + "  isactive, edition ) "
                    + "VALUES ( ?, ?, ?, ?, ?, ?, ? )");
            s.setLong(1, id);
            DBConnect.setName(s, 2, harvestDefinition);
            DBConnect.setComments(s, 3, harvestDefinition);
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
                s = c.prepareStatement("INSERT INTO fullharvests "
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
                long schedule_id = DBConnect.selectLongValue("SELECT schedule_id FROM schedules WHERE name = ?",
                        ph.getSchedule().getName());
                s = c.prepareStatement("INSERT INTO partialharvests "
                        + "( harvest_id, schedule_id, nextdate ) "
                        + "VALUES ( ?, ?, ? )");
                s.setLong(1, id);
                s.setLong(2, schedule_id);
                DBConnect.setDateMaybeNull(s, 3, ph.getNextDate());
                s.executeUpdate();
                createHarvestConfigsEntries(c, ph, id);
            } else {
                String message = "Harvest definition " + harvestDefinition
                        + " is of unknown class " + harvestDefinition.getClass();
                log.warn(message);
                throw new ArgumentNotValid(message);
            }
            c.commit();
            // Now that we have committed, set new data on object.
            harvestDefinition.setSubmissionDate(submissiondate);
            harvestDefinition.setEdition(edition);
            harvestDefinition.setOid(id);
            return id;
        } catch (SQLException e) {
            String message = "SQL error creating harvest definition "
                    + harvestDefinition + " in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBConnect.rollbackIfNeeded(c, "creating", harvestDefinition);
            DBConnect.closeStatementIfOpen(s);
        }
    }

    /** Create the entries in the harvest_configs table that connect
     * PartialHarvests and their configurations.
     *
     * @param c DB connection
     * @param ph The harvest to insert entries for.
     * @param id The id of the harvest -- this may not yet be set on ph
     * @throws SQLException
     */
    private void createHarvestConfigsEntries(Connection c, PartialHarvest ph, long id) throws SQLException {
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
            for (Iterator<DomainConfiguration> dcs = ph.getDomainConfigurations();
                 dcs.hasNext(); ) {
                DomainConfiguration dc = dcs.next();
                s.setLong(1, id);
                s.setString(2, dc.getDomain().getName());
                s.setString(3, dc.getName());
                s.executeUpdate();
            }
        } finally {
            DBConnect.closeStatementIfOpen(s);
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
        Long maxVal = DBConnect.selectLongValue("SELECT max(harvest_id) FROM harvestdefinitions");
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
    public synchronized HarvestDefinition read(Long harvestDefinitionID) throws UnknownID, IOFailure {
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
                                + "previoushd, maxobjects, maxbytes, isactive, edition "
                                + "FROM harvestdefinitions, fullharvests "
                                + "WHERE harvestdefinitions.harvest_id = ?"
                                + "  AND harvestdefinitions.harvest_id = fullharvests.harvest_id");
            s.setLong(1, harvestDefinitionID);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                // Found full harvest
                final String name = res.getString(1);
                final String comments = res.getString(2);
                final int numEvents = res.getInt(3);
                final Date submissionDate = new Date(res.getTimestamp(4).getTime());
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
                    + "   AND harvestdefinitions.harvest_id = partialharvests.harvest_id"
                    + "   AND schedules.schedule_id = partialharvests.schedule_id");
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
            final Date nextDate = DBConnect.getDateMaybeNull(res, 8);
            s.close();
            // Found partial harvest -- have to find configurations.
            // To avoid holding on to the readlock while getting domains,
            // we grab the strings first, then look up domains and configs.
            final DomainDAO domainDao = DomainDAO.getInstance();
            class DomainConfigPair {
                final String domainName;
                final String configName;
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
                    + "  AND configurations.config_id = harvest_configs.config_id"
                    + "  AND configurations.domain_id = domains.domain_id");
            s.setLong(1, harvestDefinitionID);
            res = s.executeQuery();
            while (res.next()) {
                configs.add(new DomainConfigPair
                        (res.getString(1), res.getString(2)));
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
                    + harvestDefinitionID, e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }
    }

    public String describeUsages(Long oid) {
        try {
            List<Long> usages = DBConnect.selectLongList("SELECT job_id FROM jobs"
                    + " WHERE jobs.harvest_id = ? ", oid);
            if (usages.size() != 0) {
                return "Harvested by jobs " + usages;
            } else {
                List<String> dependencies = DBConnect.selectStringlist(
                        "SELECT name FROM harvestdefinitions, fullharvests"
                        + " WHERE fullharvests.harvest_id = harvestdefinitions.harvest_id"
                        + "   AND fullharvests.previoushd = ?",
                        oid);
                if (dependencies.size() != 0) {
                    return "Is the basis of snapshot harvests " + dependencies;
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            String message = "SQL exception while checking for usages of "
                    + "harvest definition #" + oid;
            log.warn(message, e);
            throw new IOFailure(message, e);
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
            for (String table : new String[] { "harvestdefinitions",
                                           "fullharvests",
                                           "partialharvests",
                                           "harvest_configs"} ) {
                s = c.prepareStatement("DELETE FROM " + table +
                        " WHERE harvest_id = ?");
                s.setLong(1, oid);
                s.executeUpdate();
            }
            log.debug("Deleting harvest definition " + oid);
            c.commit();
        } catch (SQLException e) {
            throw new IOFailure("SQL Error while deleting harvest definition "
                    + oid, e);
        } finally {
            DBConnect.rollbackIfNeeded(c, "deleting harvestdefinition", oid);
            DBConnect.closeStatementIfOpen(s);
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
            final String message = "Cannot update non-existing harvestdefinition "
                    + hd.getName();
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
            DBConnect.setName(s, 1, hd);
            DBConnect.setComments(s, 2, hd);
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
                        + " since edition " + hd.getEdition() + ", not updating";
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
                        + "    (SELECT schedule_id FROM schedules WHERE schedules.name = ?), "
                        + "nextdate = ? "
                        + "WHERE harvest_id = ?");
                s.setString(1, ph.getSchedule().getName());
                DBConnect.setDateMaybeNull(s, 2, ph.getNextDate());
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
                    + hd, e);
        } finally {
            DBConnect.rollbackIfNeeded(c, "updating", hd);
            DBConnect.closeStatementIfOpen(s);
        }
    }

    /**
     * @see HarvestDefinitionDAO#exists(Long)
     */
    public synchronized boolean exists(Long oid) {
        return 1 == DBConnect.selectIntValue("SELECT COUNT(harvest_id) "
                + "FROM harvestdefinitions WHERE harvest_id = ?", oid);
    }

    /**
     * Get a list of all existing harvest definitions ordered by name.
     *
     * @return An iterator that give the existing harvest definitions in turn
     */
    public synchronized Iterator<HarvestDefinition> getAllHarvestDefinitions() {
        try {
            List<Long> hds = DBConnect.selectLongList("SELECT harvest_id FROM harvestdefinitions "
                    + "ORDER BY name");
            return new FilterIterator<Long, HarvestDefinition>(hds.iterator()) {
                public HarvestDefinition filter(Long id) {
                    return read(id);
                }
            };
        } catch (SQLException e) {
            throw new IOFailure("SQL Error while asking for all harvest definitions",
                    e);
        }
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
        return new FilterIterator<Domain, DomainConfiguration> (
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
     *
     * @return List of ready harvest definitions.   No check is performed for
     * whether these are already in the middle of being scheduled.
     */
    public Iterable<Long> getReadyHarvestDefinitions(Date now) {
        try {
            List<Long> ids = DBConnect.selectLongList
                    ("SELECT fullharvests.harvest_id"
                    + " FROM fullharvests, harvestdefinitions"
                    + " WHERE harvestdefinitions.harvest_id = fullharvests.harvest_id"
                    + "   AND isactive = ? AND numevents < 1", true);
            ids.addAll(DBConnect.selectLongList
                    ("SELECT partialharvests.harvest_id"
                    + " FROM partialharvests, harvestdefinitions"
                    + " WHERE harvestdefinitions.harvest_id = partialharvests.harvest_id"
                    + "   AND isactive = ?"
                    + "   AND nextdate IS NOT NULL"
                    + "   AND nextdate < ?", true, now));
            return ids;
        } catch (SQLException e) {
            throw new IOFailure("SQL error while getting ready harvests", e);
        }
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
            throw new IOFailure("SQL error while getting HD by name", e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }
    }

    /**
     * Returns an iterator of all snapshot harvest definitions ordered by name.
     *
     * @return An iterator (possibly empty) of FullHarvests
     * @see HarvestDefinitionDAO#getAllFullHarvestDefinitions()
     */
    public synchronized Iterator<FullHarvest> getAllFullHarvestDefinitions() {
        try {
            List<Long> hds = DBConnect.selectLongList("SELECT fullharvests.harvest_id "
                    + "FROM fullharvests, harvestdefinitions "
                    + "WHERE fullharvests.harvest_id = harvestdefinitions.harvest_id "
                    + "ORDER BY harvestdefinitions.name");
            return new FilterIterator<Long, FullHarvest>(hds.iterator()) {
                public FullHarvest filter(Long id) {
                    return (FullHarvest)read(id);
                }
            };
        } catch (SQLException e) {
            throw new IOFailure("SQL Error while asking for all full harvest definitions",
                    e);
        }
    }


    /**
     * Returns an iterator of all non-snapshot harvest definitions ordered by name.
     *
     * @return An iterator (possibly empty) of PartialHarvests
     * @see HarvestDefinitionDAO#getAllPartialHarvestDefinitions()
     */
    public synchronized Iterator<PartialHarvest> getAllPartialHarvestDefinitions() {
        try {
            List<Long> hds = DBConnect.selectLongList("SELECT partialharvests.harvest_id "
                    + "FROM partialharvests, harvestdefinitions "
                    + "WHERE partialharvests.harvest_id = harvestdefinitions.harvest_id "
                    + "ORDER BY harvestdefinitions.name");
            return new FilterIterator<Long, PartialHarvest>(hds.iterator()) {
                public PartialHarvest filter(Long id) {
                    return (PartialHarvest)read(id);
                }
            };
        } catch (SQLException e) {
            throw new IOFailure("SQL Error while asking for all partial harvest definitions",
                    e);
        }
    }

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
                s = c.prepareStatement("SELECT name, harvest_num, status, "
                                       + "       MIN(startdate), MAX(enddate), COUNT(job_id)"
                                       + "  FROM jobs, harvestdefinitions"
                                       + " WHERE harvestdefinitions.harvest_id = ?"
                                       + "   AND jobs.harvest_id = harvestdefinitions.harvest_id"
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
                        Date startDate = DBConnect.getDateMaybeNull(res, 4);
                        if (info.getStartDate() == null
                            || (startDate != null
                                && startDate.before(info.getStartDate()))) {
                            info.setStartDate(startDate);
                        }
                    }
                    // For finished jobs, check end date
                    if (status == JobStatus.DONE
                        || status == JobStatus.FAILED) {
                        Date endDate = DBConnect.getDateMaybeNull(res, 5);
                        if (info.getEndDate() == null
                            || (endDate != null
                                && endDate.after(info.getEndDate()))) {
                            info.setEndDate(endDate);
                        }
                    }
                    int count = res.getInt(6);
                    info.setStatusCount(status, count);
                }

                s = c.prepareStatement("SELECT jobs.harvest_num,"
                                       + "SUM(historyinfo.bytecount), SUM(historyinfo.objectcount),"
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
                // TODO: If missing?
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
                             + harvestID + " in database";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }
    }

    public boolean mayDelete(HarvestDefinition hd) {
        // May not delete if harvested
        if (DBConnect.selectAny("SELECT job_id "
                + " FROM jobs WHERE harvest_id = ?",
                hd.getOid())) {
            return false;
        }
        if (DBConnect.selectAny("SELECT harvest_id FROM fullharvests"
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
            throw new IOFailure("SQL error getting sparse domains", e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
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
                        DBConnect.getDateMaybeNull(res, 9));
                harvests.add(sph);
            }
            return harvests;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvests", e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
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
                    + "   AND harvestdefinitions.harvest_id = partialharvests.harvest_id"
                    + "   AND schedules.schedule_id = partialharvests.schedule_id");
            s.setString(1, harvestName);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                return new SparsePartialHarvest(
                        res.getLong(1),
                        harvestName, res.getString(2), res.getInt(3),
                        new Date(res.getTimestamp(4).getTime()),
                        res.getBoolean(5), res.getLong(6), res.getString(7),
                        DBConnect.getDateMaybeNull(res, 8));
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvest", e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
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
                        res.getLong(8), DBConnect.getLongMaybeNull(res, 9));
                harvests.add(sfh);
            }
            return harvests;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvests", e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
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
            s = c.prepareStatement("SELECT name FROM harvestdefinitions WHERE harvest_id = ?");
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
                                + "harvest definition " + harvestDefinitionID, e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
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
        boolean isSnapshot = DBConnect.selectAny(
                "SELECT harvest_id FROM fullharvests WHERE harvest_id = ?",
                harvestDefinitionID);
        if (isSnapshot) {
            return true;
        }
        boolean isSelective = DBConnect.selectAny(
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
                        res.getLong(7), DBConnect.getLongMaybeNull(res, 8));
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvest", e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }
    }
}
