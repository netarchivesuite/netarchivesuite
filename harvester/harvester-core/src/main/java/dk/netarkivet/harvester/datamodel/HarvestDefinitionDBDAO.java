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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FilterIterator;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.datamodel.dao.DAOProviderFactory;
import dk.netarkivet.harvester.datamodel.eav.EAV;
import dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDefaultValue;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValue;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDBDAO;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery;

/**
 * A database-oriented implementation of the HarvestDefinitionDAO.
 * <p>
 * The statements to create the tables are located in:
 * <ul>
 * <li><em>Derby:</em> scripts/sql/createfullhddb.sql</li>
 * <li><em>MySQL:</em> scripts/sql/createfullhddb.mysql</li>
 * <li><em>PostgreSQL:</em> scripts/postgresql/netarchivesuite_init.sql</li>
 * </ul>
 */
public class HarvestDefinitionDBDAO extends HarvestDefinitionDAO {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(HarvestDefinitionDBDAO.class);

    /**
     * Comparator used for sorting the UI list of {@link SparseDomainConfiguration}s. Sorts first by domain name
     * alphabetical order, next by configuration name.
     */
    private static class SparseDomainConfigurationComparator implements Comparator<SparseDomainConfiguration> {

        @Override
        public int compare(SparseDomainConfiguration sdc1, SparseDomainConfiguration sdc2) {
            int domComp = sdc1.getDomainName().compareTo(sdc2.getDomainName());
            if (0 == domComp) {
                return sdc1.getConfigurationName().compareTo(sdc2.getConfigurationName());
            }
            return domComp;
        }
    }

    /** Create a new HarvestDefinitionDAO using database. */
    HarvestDefinitionDBDAO() {
        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.FULLHARVESTS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.HARVESTDEFINITIONS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.PARTIALHARVESTS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.HARVESTCONFIGS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELDTYPE);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELD);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELDVALUE);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Create a harvest definition in Database. The harvest definition object should not have its ID set unless we are
     * in the middle of migrating.
     *
     * @param harvestDefinition A new harvest definition to store in the database.
     * @return The harvestId for the just created harvest definition.
     * @see HarvestDefinitionDAO#create(HarvestDefinition)
     */
    @Override
    public synchronized Long create(HarvestDefinition harvestDefinition) {
        Long id = harvestDefinition.getOid();
        try (Connection connection = HarvestDBConnection.get();) {
            Date submissiondate = new Date();
            final int edition = 1;
            try {
                if (id == null) {
                    id = generateNextID(connection);
                }

                connection.setAutoCommit(false);
                try ( PreparedStatement s = connection.prepareStatement("INSERT INTO harvestdefinitions "
                                + "( harvest_id, name, comments, numevents, submitted,  isactive, edition, audience ) "
                                + "VALUES ( ?, ?, ?, ?, ?, ?, ?,? )");) {
                    s.setLong(1, id);
                    DBUtils.setName(s, 2, harvestDefinition, Constants.MAX_NAME_SIZE);
                    DBUtils.setComments(s, 3, harvestDefinition, Constants.MAX_COMMENT_SIZE);
                    s.setLong(4, harvestDefinition.getNumEvents());
                    // Don't set on object, as we may yet rollback
                    s.setTimestamp(5, new Timestamp(submissiondate.getTime()));
                    s.setBoolean(6, harvestDefinition.getActive());
                    s.setLong(7, edition);
                    s.setString(8, harvestDefinition.getAudience());
                    s.executeUpdate();
                }
                if (harvestDefinition instanceof FullHarvest) {
                    FullHarvest fh = (FullHarvest) harvestDefinition;
                    try ( PreparedStatement s = connection.prepareStatement(
                            "INSERT INTO fullharvests "
                            + "( harvest_id, maxobjects, maxbytes, maxjobrunningtime, previoushd, isindexready)"
                            + "VALUES ( ?, ?, ?, ?, ?, ? )"); ) {
                    s.setLong(1, id);
                    s.setLong(2, fh.getMaxCountObjects());
                    s.setLong(3, fh.getMaxBytes());
                    s.setLong(4, fh.getMaxJobRunningTime());
                    if (fh.getPreviousHarvestDefinition() != null) {
                        s.setLong(5, fh.getPreviousHarvestDefinition().getOid());
                    } else {
                        s.setNull(5, Types.BIGINT);
                    }
                    s.setBoolean(6, fh.getIndexReady());
                    s.executeUpdate();
                }
                } else if (harvestDefinition instanceof PartialHarvest) {
                    PartialHarvest ph = (PartialHarvest) harvestDefinition;
                    // Get schedule id
                    long scheduleId = DBUtils.selectLongValue(connection,
                            "SELECT schedule_id FROM schedules WHERE name = ?", ph.getSchedule().getName());
                    try ( PreparedStatement s = connection.prepareStatement("INSERT INTO partialharvests ( harvest_id, schedule_id, nextdate ) "
                            + "VALUES ( ?, ?, ? )"); ) {
                        s.setLong(1, id);
                        s.setLong(2, scheduleId);
                        DBUtils.setDateMaybeNull(s, 3, ph.getNextDate());
                        s.executeUpdate();
                        createHarvestConfigsEntries(connection, ph, id);
                    }
                } else {
                    String message = "Harvest definition " + harvestDefinition + " is of unknown class "
                            + harvestDefinition.getClass();
                    log.warn(message);
                    throw new ArgumentNotValid(message);
                }
                connection.commit();

                // Now that we have committed, set new data on object.
                harvestDefinition.setSubmissionDate(submissiondate);
                harvestDefinition.setEdition(edition);
                harvestDefinition.setOid(id);

                // saving after receiving id
                saveExtendedFieldValues(connection, harvestDefinition);

            } catch (SQLException e) {
                String message = "SQL error creating harvest definition " + harvestDefinition + " in database" + "\n"
                        + ExceptionUtils.getSQLExceptionCause(e);
                log.warn(message, e);
                throw new IOFailure(message, e);
            } finally {
                DBUtils.rollbackIfNeeded(connection, "creating", harvestDefinition);
            }
        } catch (SQLException e) {
            log.error("Unable to close db resources", e);
        }

        return id;
    }

    /**
     * Create the entries in the harvest_configs table that connect PartialHarvests and their configurations.
     *
     * @param c harvest definition DB connection
     * @param ph The harvest to insert entries for.
     * @param id The id of the harvest -- this may not yet be set on ph
     * @throws SQLException If a database error occurs during the create process.
     */
    private void createHarvestConfigsEntries(Connection c, PartialHarvest ph, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("DELETE FROM harvest_configs WHERE harvest_id = ?");) {
            s.setLong(1, id);
            s.executeUpdate();
        }
        try (PreparedStatement s = c.prepareStatement("INSERT INTO harvest_configs " + "( harvest_id, config_id ) "
                + "SELECT ?, config_id FROM configurations, domains "
                + "WHERE domains.name = ? AND configurations.name = ?"
                + "  AND domains.domain_id = configurations.domain_id");
        ) {
            Iterator<DomainConfiguration> dcs = ph.getDomainConfigurations();
            while (dcs.hasNext()) {
                DomainConfiguration dc = dcs.next();
                s.setLong(1, id);
                s.setString(2, dc.getDomainName());
                s.setString(3, dc.getName());
                s.executeUpdate();
            }
        }
    }

    /**
     * Generates the next id of a harvest definition. this implementation retrieves the maximum value of harvest_id in
     * the DB, and returns this value + 1.
     *
     * @param c An open connection to the harvestDatabase
     * @return The next available ID
     */
    private synchronized Long generateNextID(Connection c) {
        Long maxVal = DBUtils.selectLongValue(c, "SELECT max(harvest_id) FROM harvestdefinitions");
        if (maxVal == null) {
            maxVal = 0L;
        }
        return maxVal + 1L;
    }

    /**
     * Read the stored harvest definition for the given ID.
     *
     * @param harvestDefinitionID An ID number for a harvest definition
     * @return A harvest definition that has been read from persistent storage.
     * @throws UnknownID if no entry with that ID exists in the database
     * @throws IOFailure If DB-failure occurs?
     * @see HarvestDefinitionDAO#read(Long)
     */
    @Override
    public synchronized HarvestDefinition read(Long harvestDefinitionID) throws UnknownID, IOFailure {
        Connection c = HarvestDBConnection.get();
        try {
            return read(c, harvestDefinitionID);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Read the stored harvest definition for the given ID.
     *
     * @param c The used database connection
     * @param harvestDefinitionID ID number for a harvest definition
     * @return A harvest definition that has been read from persistent storage.
     * @throws UnknownID if no entry with that ID exists in the database
     * @throws IOFailure If DB-failure occurs?
     * @see HarvestDefinitionDAO#read(Long)
     */
    private HarvestDefinition read(Connection c, Long harvestDefinitionID) throws UnknownID, IOFailure {
        if (!exists(c, harvestDefinitionID)) {
            String message = "Unknown harvest definition " + harvestDefinitionID;
            log.debug(message);
            throw new UnknownID(message);
        }
        log.debug("Reading harvestdefinition w/ id {}", harvestDefinitionID);
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT name, comments, numevents, submitted, "
                    + "previoushd, maxobjects, maxbytes, "
                    + "maxjobrunningtime, isindexready, isactive, edition, audience "
                    + "FROM harvestdefinitions, fullharvests " + "WHERE harvestdefinitions.harvest_id = ?"
                    + "  AND harvestdefinitions.harvest_id " + " = fullharvests.harvest_id");
            s.setLong(1, harvestDefinitionID);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                // Found full harvest
                log.debug("fullharvest found w/id " + harvestDefinitionID);
                final String name = res.getString(1);
                final String comments = res.getString(2);
                final int numEvents = res.getInt(3);
                final Date submissionDate = new Date(res.getTimestamp(4).getTime());
                final long maxObjects = res.getLong(6);
                final long maxBytes = res.getLong(7);
                final long maxJobRunningtime = res.getLong(8);
                final boolean isIndexReady = res.getBoolean(9);
                FullHarvest fh;
                final long prevhd = res.getLong(5);
                if (!res.wasNull()) {
                    fh = new FullHarvest(name, comments, prevhd, maxObjects, maxBytes, maxJobRunningtime, isIndexReady,
                            DAOProviderFactory.getHarvestDefinitionDAOProvider(),
                            DAOProviderFactory.getJobDAOProvider(), DAOProviderFactory.getExtendedFieldDAOProvider(),
                            DAOProviderFactory.getDomainDAOProvider());
                } else {
                    fh = new FullHarvest(name, comments, null, maxObjects, maxBytes, maxJobRunningtime, isIndexReady,
                            DAOProviderFactory.getHarvestDefinitionDAOProvider(),
                            DAOProviderFactory.getJobDAOProvider(), DAOProviderFactory.getExtendedFieldDAOProvider(),
                            DAOProviderFactory.getDomainDAOProvider());
                }
                fh.setSubmissionDate(submissionDate);
                fh.setNumEvents(numEvents);
                fh.setActive(res.getBoolean(10));
                fh.setOid(harvestDefinitionID);
                fh.setEdition(res.getLong(11));
                fh.setAudience(res.getString(12));

                readExtendedFieldValues(fh);

                // We found a FullHarvest object, just return it.
                log.debug("Returned FullHarvest object w/ id {}", harvestDefinitionID);
                return fh;
            }
            s.close();
            // No full harvest with that ID, try selective harvest
            s = c.prepareStatement("SELECT harvestdefinitions.name," + "       harvestdefinitions.comments,"
                    + "       harvestdefinitions.numevents," + "       harvestdefinitions.submitted,"
                    + "       harvestdefinitions.isactive," + "       harvestdefinitions.edition,"
                    + "       harvestdefinitions.audience," + "       schedules.name,"
                    + "       partialharvests.nextdate, " + "       harvestdefinitions.channel_id "
                    + "FROM harvestdefinitions, partialharvests, schedules"
                    + " WHERE harvestdefinitions.harvest_id = ?" + "   AND harvestdefinitions.harvest_id "
                    + "= partialharvests.harvest_id" + "   AND schedules.schedule_id "
                    + "= partialharvests.schedule_id");
            s.setLong(1, harvestDefinitionID);
            res = s.executeQuery();
            boolean foundPartialHarvest = res.next();
            if (foundPartialHarvest) {
                log.debug("Partialharvest found w/ id " + harvestDefinitionID);
                // Have to get configs before creating object, so storing data
                // here.
                final String name = res.getString(1);
                final String comments = res.getString(2);
                final int numEvents = res.getInt(3);
                final Date submissionDate = new Date(res.getTimestamp(4).getTime());
                final boolean active = res.getBoolean(5);
                final long edition = res.getLong(6);
                final String audience = res.getString(7);
                final String scheduleName = res.getString(8);
                final Date nextDate = DBUtils.getDateMaybeNull(res, 9);
                final Long channelId = DBUtils.getLongMaybeNull(res, 10);
                s.close();
                // Found partial harvest -- have to find configurations.
                // To avoid holding on to the readlock while getting domains,
                // we grab the strings first, then look up domains and configs.
                final DomainDAO domainDao = DomainDAO.getInstance();
                List<SparseDomainConfiguration> configs = new ArrayList<SparseDomainConfiguration>();
                s = c.prepareStatement("SELECT domains.name, configurations.name "
                        + "FROM domains, configurations, harvest_configs " + "WHERE harvest_id = ?"
                        + "  AND configurations.config_id " + "= harvest_configs.config_id"
                        + "  AND configurations.domain_id = domains.domain_id");
                s.setLong(1, harvestDefinitionID);
                res = s.executeQuery();
                while (res.next()) {
                    configs.add(new SparseDomainConfiguration(res.getString(1), res.getString(2)));
                }
                s.close();
                List<DomainConfiguration> configurations = new ArrayList<DomainConfiguration>();
                for (SparseDomainConfiguration domainConfig : configs) {
                    configurations.add(domainDao.getDomainConfiguration(domainConfig.getDomainName(),
                            domainConfig.getConfigurationName()));
                }

                Schedule schedule = ScheduleDAO.getInstance().read(scheduleName);

                PartialHarvest ph = new PartialHarvest(configurations, schedule, name, comments, audience);

                ph.setNumEvents(numEvents);
                ph.setSubmissionDate(submissionDate);
                ph.setActive(active);
                ph.setEdition(edition);
                ph.setNextDate(nextDate);
                ph.setOid(harvestDefinitionID);
                if (channelId != null) {
                    ph.setChannelId(channelId);
                }

                readExtendedFieldValues(ph);

                return ph;
            } else {
                throw new IllegalState("No entries in fullharvests or partialharvests found for id "
                        + harvestDefinitionID);
            }
        } catch (SQLException e) {
            throw new IOFailure("SQL Error while reading harvest definition " + harvestDefinitionID + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
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
            final String message = "Cannot update non-existing " + "harvestdefinition '" + hd.getName() + "'";
            log.debug(message);
            throw new PermissionDenied(message);
        }
        HarvestDefinition preHD = null;
        if (hd instanceof FullHarvest) {
            preHD = ((FullHarvest) hd).getPreviousHarvestDefinition();
        }

        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            c.setAutoCommit(false);
            s = c.prepareStatement("UPDATE harvestdefinitions SET " + "name = ?, " + "comments = ?, "
                    + "numevents = ?, " + "submitted = ?," + "isactive = ?," + "edition = ?, audience = ? "
                    + "WHERE harvest_id = ? AND edition = ?");
            DBUtils.setName(s, 1, hd, Constants.MAX_NAME_SIZE);
            DBUtils.setComments(s, 2, hd, Constants.MAX_COMMENT_SIZE);
            s.setInt(3, hd.getNumEvents());
            s.setTimestamp(4, new Timestamp(hd.getSubmissionDate().getTime()));
            s.setBoolean(5, hd.getActive());
            long nextEdition = hd.getEdition() + 1;
            s.setLong(6, nextEdition);
            s.setString(7, hd.getAudience());
            s.setLong(8, hd.getOid());
            s.setLong(9, hd.getEdition());

            int rows = s.executeUpdate();
            // Since the HD exists, no rows indicates bad edition
            if (rows == 0) {
                String message = "Somebody else must have updated " + hd + " since edition " + hd.getEdition()
                        + ", not updating";
                log.debug(message);
                throw new PermissionDenied(message);
            }
            s.close();
            if (hd instanceof FullHarvest) {
                FullHarvest fh = (FullHarvest) hd;
                s = c.prepareStatement("UPDATE fullharvests SET previoushd = ?, " + "maxobjects = ?, "
                        + "maxbytes = ?, " + "maxjobrunningtime = ?, isindexready = ? " + "WHERE harvest_id = ?");
                if (preHD != null) {
                    s.setLong(1, preHD.getOid());
                } else {
                    s.setNull(1, Types.BIGINT);
                }
                s.setLong(2, fh.getMaxCountObjects());
                s.setLong(3, fh.getMaxBytes());
                s.setLong(4, fh.getMaxJobRunningTime());
                s.setBoolean(5, fh.getIndexReady());
                s.setLong(6, fh.getOid());

                rows = s.executeUpdate();
                log.debug("{} fullharvests records updated", rows);
            } else if (hd instanceof PartialHarvest) {
                PartialHarvest ph = (PartialHarvest) hd;
                s = c.prepareStatement("UPDATE partialharvests SET " + "schedule_id = "
                        + "(SELECT schedule_id FROM schedules WHERE schedules.name = ?), " + "nextdate = ? "
                        + "WHERE harvest_id = ?");
                s.setString(1, ph.getSchedule().getName());
                DBUtils.setDateMaybeNull(s, 2, ph.getNextDate());
                s.setLong(3, ph.getOid());
                rows = s.executeUpdate();
                log.debug("{} partialharvests records updated", rows);
                s.close();
                // FIXME The updates to harvest_configs table should be done
                // in method removeDomainConfiguration(), and not here.
                // The following deletes ALL harvest_configs entries for
                // this PartialHarvest, and creates the entries for the
                // PartialHarvest again!!
                createHarvestConfigsEntries(c, ph, ph.getOid());
            } else {
                String message = "Harvest definition " + hd + " has unknown class " + hd.getClass();
                log.warn(message);
                throw new ArgumentNotValid(message);
            }
            saveExtendedFieldValues(c, hd);

            c.commit();
            hd.setEdition(nextEdition);
        } catch (SQLException e) {
            throw new IOFailure("SQL error while updating harvest definition " + hd + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            DBUtils.rollbackIfNeeded(c, "updating", hd);
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Activates or deactivates a partial harvest definition. This method is actually to be used not to have to read
     * from the DB big harvest definitions and optimize the activation / deactivation, it is sort of a lightweight
     * version of update.
     *
     * @param harvestDefinition the harvest definition object.
     */
    @Override
    public synchronized void flipActive(SparsePartialHarvest harvestDefinition) {
        ArgumentNotValid.checkNotNull(harvestDefinition, "HarvestDefinition harvestDefinition");

        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            if (harvestDefinition.getOid() == null || !exists(c, harvestDefinition.getOid())) {
                final String message = "Cannot update non-existing " + "harvestdefinition '"
                        + harvestDefinition.getName() + "'";
                log.debug(message);
                throw new PermissionDenied(message);
            }

            c.setAutoCommit(false);
            s = c.prepareStatement("UPDATE harvestdefinitions SET " + "name = ?, " + "comments = ?, "
                    + "numevents = ?, " + "submitted = ?," + "isactive = ?," + "edition = ?, audience = ? "
                    + "WHERE harvest_id = ? AND edition = ?");
            DBUtils.setName(s, 1, harvestDefinition, Constants.MAX_NAME_SIZE);
            DBUtils.setComments(s, 2, harvestDefinition, Constants.MAX_COMMENT_SIZE);
            s.setInt(3, harvestDefinition.getNumEvents());
            s.setTimestamp(4, new Timestamp(harvestDefinition.getSubmissionDate().getTime()));
            s.setBoolean(5, !harvestDefinition.isActive());
            long nextEdition = harvestDefinition.getEdition() + 1;
            s.setLong(6, nextEdition);
            s.setString(7, harvestDefinition.getAudience());
            s.setLong(8, harvestDefinition.getOid());
            s.setLong(9, harvestDefinition.getEdition());
            int rows = s.executeUpdate();
            // Since the HD exists, no rows indicates bad edition
            if (rows == 0) {
                String message = "Somebody else must have updated " + harvestDefinition + " since edition "
                        + harvestDefinition.getEdition() + ", not updating";
                log.debug(message);
                throw new PermissionDenied(message);
            }
            s.close();

            // Now pull more strings
            s = c.prepareStatement("UPDATE partialharvests SET schedule_id = "
                    + "(SELECT schedule_id FROM schedules WHERE schedules.name = ?), " + "nextdate = ? "
                    + "WHERE harvest_id = ?");
            s.setString(1, harvestDefinition.getScheduleName());
            DBUtils.setDateMaybeNull(s, 2, harvestDefinition.getNextDate());
            s.setLong(3, harvestDefinition.getOid());
            rows = s.executeUpdate();
            log.debug("{} partialharvests records updated", rows);
            s.close();
            c.commit();
        } catch (SQLException e) {
            throw new IOFailure("SQL error while updating harvest definition " + harvestDefinition + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.rollbackIfNeeded(c, "updating", harvestDefinition);
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public synchronized boolean exists(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        Connection c = HarvestDBConnection.get();
        try {
            return 1 == DBUtils.selectIntValue(c, "SELECT COUNT(harvest_id) "
                    + "FROM harvestdefinitions WHERE name = ?", name);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public synchronized boolean exists(Long oid) {
        ArgumentNotValid.checkNotNull(oid, "Long oid");
        Connection c = HarvestDBConnection.get();
        try {
            return exists(c, oid);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Check if a harvestdefinition exists with the given id.
     *
     * @param c An open connection to the harvestDatabase
     * @param oid A potential identifier for a harvestdefinition
     * @return true If a harvestdefinition exists with the given id.
     * @see HarvestDefinitionDAO#exists(Long)
     */
    private boolean exists(Connection c, Long oid) {
        return 1 == DBUtils.selectIntValue(c, "SELECT COUNT(harvest_id) "
                + "FROM harvestdefinitions WHERE harvest_id = ?", oid);
    }

    /**
     * Get a list of all existing harvest definitions ordered by name.
     *
     * @return An iterator that give the existing harvest definitions in turn
     */
    @Override
    public synchronized Iterator<HarvestDefinition> getAllHarvestDefinitions() {
        Connection c = HarvestDBConnection.get();
        try {
            List<Long> hds = DBUtils.selectLongList(c, "SELECT harvest_id FROM harvestdefinitions ORDER BY name");
            log.debug("Getting an iterator for all stored harvestdefinitions.");

            List<HarvestDefinition> orderedList = new LinkedList<HarvestDefinition>();
            for (Long id : hds) {
                orderedList.add(read(c, id));
            }
            return orderedList.iterator();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Gets default configurations for all domains that are not aliases.
     * <p>
     * This method currently gives an iterator that reads in all domains, although only on demand, that is: when calling
     * "hasNext".
     *
     * @return Iterator containing the default DomainConfiguration for all domains that are not aliases
     */
    @Override
    public synchronized Iterator<DomainConfiguration> getSnapShotConfigurations() {
        return new FilterIterator<Domain, DomainConfiguration>(DomainDAO.getInstance()
                .getAllDomainsInSnapshotHarvestOrder()) {
            public DomainConfiguration filter(Domain domain) {
                if (domain.getAliasInfo() == null || domain.getAliasInfo().isExpired()) {
                    return domain.getDefaultConfiguration();
                } else {
                    return null;
                }
            }
        };
    }

    /**
     * Returns a list of IDs of harvest definitions that are ready to be scheduled.
     *
     * @param now The current date
     * @return List of ready harvest definitions. No check is performed for whether these are already in the middle of
     * being scheduled.
     */
    @Override
    public Iterable<Long> getReadyHarvestDefinitions(Date now) {
        ArgumentNotValid.checkNotNull(now, "Date now");
        Connection connection = HarvestDBConnection.get();
        try {
            List<Long> ids = DBUtils.selectLongList(connection, "SELECT fullharvests.harvest_id"
                    + " FROM fullharvests, harvestdefinitions"
                    + " WHERE harvestdefinitions.harvest_id = fullharvests.harvest_id" + " AND isactive = ? "
                    + " AND numevents < 1  AND isindexready = ?", true, true);
            ids.addAll(DBUtils.selectLongList(connection, "SELECT partialharvests.harvest_id"
                    + " FROM partialharvests, harvestdefinitions"
                    + " WHERE harvestdefinitions.harvest_id = partialharvests.harvest_id"
                    + " AND isactive = ? AND nextdate IS NOT NULL AND nextdate < ?", true, now));
            Set<Long> distinctIds = new HashSet<>();
            distinctIds.addAll(ids);
            if (distinctIds.size() != ids.size()) {
                log.warn("Query returned multiple identical ids {}. These have been sanitized.", ids);
                return distinctIds;
            } else {
                return ids;
            }
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Get the harvest definition that has the given name, if any.
     *
     * @param name The name of a harvest definition.
     * @return The HarvestDefinition object with that name, or null if none has that name.
     */
    @Override
    public synchronized HarvestDefinition getHarvestDefinition(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        log.debug("Reading harvestdefinition w/ name '{}'", name);
        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT harvest_id FROM harvestdefinitions WHERE name = ?");
            s.setString(1, name);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                long harvestDefinitionID = res.getLong(1);
                s.close();
                return read(c, harvestDefinitionID);
            }
            return null;
        } catch (SQLException e) {
            throw new IOFailure("SQL error while getting HD by name" + "\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public List<HarvestRunInfo> getHarvestRunInfo(long harvestID) {
        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            ResultSet res = null;
            Map<Integer, HarvestRunInfo> runInfos = new HashMap<Integer, HarvestRunInfo>();
            List<HarvestRunInfo> infoList = new ArrayList<HarvestRunInfo>();

            // Select dates and counts for all different statues
            // for each run
            s = c.prepareStatement("SELECT name, harvest_num, status, MIN(startdate), MAX(enddate), COUNT(job_id)"
                    + "  FROM jobs, harvestdefinitions"
                    + " WHERE harvestdefinitions.harvest_id = ?   AND jobs.harvest_id = harvestdefinitions.harvest_id"
                    + " GROUP BY name, harvest_num, status ORDER BY harvest_num DESC");
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
                // For started states, check start date
                if (status != JobStatus.NEW && status != JobStatus.SUBMITTED && status != JobStatus.RESUBMITTED) {
                    Date startDate = DBUtils.getDateMaybeNull(res, 4);
                    if (info.getStartDate() == null || (startDate != null && startDate.before(info.getStartDate()))) {
                        info.setStartDate(startDate);
                    }
                }
                // For finished jobs, check end date
                if (status == JobStatus.DONE || status == JobStatus.FAILED) {
                    Date endDate = DBUtils.getDateMaybeNull(res, 5);
                    if (info.getEndDate() == null || (endDate != null && endDate.after(info.getEndDate()))) {
                        info.setEndDate(endDate);
                    }
                }
                int count = res.getInt(6);
                info.setStatusCount(status, count);
            }
            s.close();
            s = c.prepareStatement("SELECT jobs.harvest_num, SUM(historyinfo.bytecount), "
                    + "SUM(historyinfo.objectcount)," + "COUNT(jobs.status)" + " FROM jobs, historyinfo "
                    + " WHERE jobs.harvest_id = ? AND historyinfo.job_id = jobs.job_id" + " GROUP BY jobs.harvest_num"
                    + " ORDER BY jobs.harvest_num");
            s.setLong(1, harvestID);
            res = s.executeQuery();

            while (res.next()) {
                final int harvestNum = res.getInt(1);
                HarvestRunInfo info = runInfos.get(harvestNum);
                if (info != null) {
                    info.setBytesHarvested(res.getLong(2));
                    info.setDocsHarvested(res.getLong(3));
                } else {
                    log.debug("Harvestnum {} for harvestID {} is skipped. Must have arrived between selects",
                            harvestNum, harvestID);
                }
            }

            // Make sure that jobs that aren't really done don't have end date.
            for (HarvestRunInfo info : infoList) {
                if (info.getJobCount(JobStatus.STARTED) != 0 || info.getJobCount(JobStatus.NEW) != 0
                        || info.getJobCount(JobStatus.SUBMITTED) != 0) {
                    info.setEndDate(null);
                }
            }
            return infoList;
        } catch (SQLException e) {
            String message = "SQL error asking for harvest run info on " + harvestID + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Get all domain,configuration pairs for a harvest definition in sparse version for GUI purposes.
     *
     * @param harvestDefinitionID The ID of the harvest definition.
     * @return Domain, configuration pairs for that HD. Returns an empty iterable for unknown harvest definitions.
     * @throws ArgumentNotValid on null argument.
     */
    @Override
    public List<SparseDomainConfiguration> getSparseDomainConfigurations(Long harvestDefinitionID) {
        ArgumentNotValid.checkNotNull(harvestDefinitionID, "harvestDefinitionID");
        Connection c = HarvestDBConnection.get();
        try {
            return getSparseDomainConfigurations(c, harvestDefinitionID);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Get all domain,configuration pairs for a harvest definition in sparse version.
     *
     * @param c a connection to the harvest database
     * @param harvestDefinitionID The ID of the harvest definition.
     * @return Domain, configuration pairs for that HD. Returns an empty iterable for unknown harvest definitions.
     */
    private List<SparseDomainConfiguration> getSparseDomainConfigurations(Connection c, Long harvestDefinitionID) {
        try (PreparedStatement s = c
                .prepareStatement("SELECT domains.name, configurations.name " + "FROM domains, configurations,"
                        + " harvest_configs "
                        + "WHERE harvest_id = ?  AND configurations.config_id = harvest_configs.config_id"
                        + " AND configurations.domain_id = domains.domain_id");
        ) {
            s.setLong(1, harvestDefinitionID);
            ResultSet res = s.executeQuery();
            List<SparseDomainConfiguration> resultList = new ArrayList<SparseDomainConfiguration>();
            while (res.next()) {
                SparseDomainConfiguration sdc = new SparseDomainConfiguration(res.getString(1), res.getString(2));
                resultList.add(sdc);
            }

            Collections.sort(resultList, new SparseDomainConfigurationComparator());
            return resultList;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse domains" + "\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Get all sparse versions of partial harvests for GUI purposes ordered by name.
     *
     * @return An iterable (possibly empty) of SparsePartialHarvests
     */
    public Iterable<SparsePartialHarvest> getSparsePartialHarvestDefinitions(boolean excludeInactive) {
        String query = "SELECT harvestdefinitions.harvest_id," + "       harvestdefinitions.name,"
                + "       harvestdefinitions.comments," + "       harvestdefinitions.numevents,"
                + "       harvestdefinitions.submitted," + "       harvestdefinitions.isactive,"
                + "       harvestdefinitions.edition," + "       schedules.name," + "       partialharvests.nextdate, "
                + "       harvestdefinitions.audience, " + "       harvestdefinitions.channel_id "
                + "FROM harvestdefinitions, partialharvests, schedules" + " WHERE harvestdefinitions.harvest_id "
                + "       = partialharvests.harvest_id" + " AND (harvestdefinitions.isactive "
                + " = ?"
                // This linie is duplicated to allow to select both active
                // and inactive HD's.
                + " OR harvestdefinitions" + ".isactive " + " = ?)" + "   AND schedules.schedule_id "
                + "       = partialharvests.schedule_id " + "ORDER BY harvestdefinitions.name";
        try (
                Connection c = HarvestDBConnection.get();
                PreparedStatement s = DBUtils.prepareStatement(c, query, true, excludeInactive);
        ) {
            ResultSet res = s.executeQuery();
            List<SparsePartialHarvest> harvests = new ArrayList<SparsePartialHarvest>();
            while (res.next()) {
                SparsePartialHarvest sph = new SparsePartialHarvest(res.getLong(1), res.getString(2), res.getString(3),
                        res.getInt(4), new Date(res.getTimestamp(5).getTime()), res.getBoolean(6), res.getLong(7),
                        res.getString(8), DBUtils.getDateMaybeNull(res, 9), res.getString(10),
                        DBUtils.getLongMaybeNull(res, 11));
                harvests.add(sph);
            }
            return harvests;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvests\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Get a sparse version of a partial harvest for GUI purposes.
     *
     * @param harvestName Name of harvest definition.
     * @return Sparse version of partial harvest or null for none.
     * @throws ArgumentNotValid on null or empty name.
     */
    @Override
    public SparsePartialHarvest getSparsePartialHarvest(String harvestName) {
        ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
        try (
                Connection c = HarvestDBConnection.get();
                PreparedStatement s = c
                        .prepareStatement("SELECT harvestdefinitions.harvest_id," + "       harvestdefinitions.comments,"
                                + "       harvestdefinitions.numevents," + "       harvestdefinitions.submitted,"
                                + "       harvestdefinitions.isactive," + "       harvestdefinitions.edition,"
                                + "       schedules.name," + "       partialharvests.nextdate, "
                                + "       harvestdefinitions.audience, " + "       harvestdefinitions.channel_id "
                                + "FROM harvestdefinitions, partialharvests, schedules"
                                + " WHERE harvestdefinitions.name = ?"
                                + "   AND harvestdefinitions.harvest_id " + "= partialharvests.harvest_id"
                                + "   AND schedules.schedule_id " + "= partialharvests.schedule_id");
        ) {
            s.setString(1, harvestName);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                SparsePartialHarvest sph = new SparsePartialHarvest(res.getLong(1), harvestName, res.getString(2),
                        res.getInt(3), new Date(res.getTimestamp(4).getTime()), res.getBoolean(5), res.getLong(6),
                        res.getString(7), DBUtils.getDateMaybeNull(res, 8), res.getString(9), DBUtils.getLongMaybeNull(
                        res, 10));
                sph.setExtendedFieldValues(getExtendedFieldValues(sph.getOid()));
                return sph;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvest\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Get all sparse versions of full harvests for GUI purposes.
     *
     * @return An iterable (possibly empty) of SparseFullHarvests
     */
    public Iterable<SparseFullHarvest> getAllSparseFullHarvestDefinitions() {
        try (
                Connection c = HarvestDBConnection.get();
                PreparedStatement s = c
                        .prepareStatement("SELECT harvestdefinitions.harvest_id," + "       harvestdefinitions.name,"
                                + "       harvestdefinitions.comments," + "       harvestdefinitions.numevents,"
                                + "       harvestdefinitions.isactive," + "       harvestdefinitions.edition,"
                                + "       fullharvests.maxobjects," + "       fullharvests.maxbytes,"
                                + "       fullharvests.maxjobrunningtime," + "       fullharvests.previoushd, "
                                + "       harvestdefinitions.channel_id " + "FROM harvestdefinitions, fullharvests"
                                + " WHERE harvestdefinitions.harvest_id " + "       = fullharvests.harvest_id"
                                + " ORDER BY harvestdefinitions.name");
        ) {
            ResultSet res = s.executeQuery();
            List<SparseFullHarvest> harvests = new ArrayList<SparseFullHarvest>();
            while (res.next()) {
                SparseFullHarvest sfh = new SparseFullHarvest(res.getLong(1), res.getString(2), res.getString(3),
                        res.getInt(4), res.getBoolean(5), res.getLong(6), res.getLong(7), res.getLong(8),
                        res.getLong(9), DBUtils.getLongMaybeNull(res, 10), DBUtils.getLongMaybeNull(res, 11));
                // EAV
                long oid = sfh.getOid();
                List<AttributeAndType> attributesAndTypes = EAV.getInstance().getAttributesAndTypes(EAV.SNAPSHOT_TREE_ID, (int)oid);
                sfh.setAttributesAndTypes(attributesAndTypes);
                harvests.add(sfh);
            }
            return harvests;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvests\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Get the name of a harvest given its ID.
     *
     * @param harvestDefinitionID The ID of a harvest
     * @return The name of the given harvest.
     * @throws ArgumentNotValid on null argument
     * @throws UnknownID if no harvest has the given ID.
     * @throws IOFailure on any other error talking to the database
     */
    @Override
    public String getHarvestName(Long harvestDefinitionID) {
        ArgumentNotValid.checkNotNull(harvestDefinitionID, "harvestDefinitionID");
        try (
                Connection c = HarvestDBConnection.get();
                PreparedStatement s = c.prepareStatement("SELECT name FROM harvestdefinitions WHERE harvest_id = ?");
        ) {
            s.setLong(1, harvestDefinitionID);
            ResultSet res = s.executeQuery();
            String name = null;
            while (res.next()) {
                if (name != null) {
                    throw new IOFailure("Found more than one name for harvest definition " + harvestDefinitionID
                            + ": '" + name + "' and '" + res.getString(1) + "'");
                }
                name = res.getString(1);
            }
            if (name == null) {
                throw new UnknownID("No name found for harvest definition " + harvestDefinitionID);
            }
            return name;
        } catch (SQLException e) {
            throw new IOFailure("An error occurred finding the name for " + "harvest definition " + harvestDefinitionID
                    + "\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Get whether a given harvest is a snapshot or selective harvest.
     *
     * @param harvestDefinitionID ID of a harvest
     * @return True if the given harvest is a snapshot harvest, false otherwise.
     * @throws ArgumentNotValid on null argument
     * @throws UnknownID if no harvest has the given ID.
     */
    @Override
    public boolean isSnapshot(Long harvestDefinitionID) {
        ArgumentNotValid.checkNotNull(harvestDefinitionID, "harvestDefinitionID");
        try (Connection connection = HarvestDBConnection.get();) {
            boolean isSnapshot = DBUtils.selectAny(connection,
                    "SELECT harvest_id FROM fullharvests WHERE harvest_id = ?", harvestDefinitionID);
            if (isSnapshot) {
                return true;
            }
            boolean isSelective = DBUtils.selectAny(connection, "SELECT harvest_id FROM partialharvests "
                    + "WHERE harvest_id = ?", harvestDefinitionID);
            if (isSelective) {
                return false;
            }
            throw new UnknownID("Failed to find harvest definition with id " + harvestDefinitionID);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to close DB connection");
        }
    }

    /**
     * Get a sparse version of a full harvest for GUI purposes.
     *
     * @param harvestName Name of harvest definition.
     * @return Sparse version of full harvest or null for none.
     * @throws ArgumentNotValid on null or empty name.
     * @throws UnknownID if no harvest has the given ID.
     * @throws IOFailure on any other error talking to the database
     */
    @Override
    public SparseFullHarvest getSparseFullHarvest(String harvestName) {
        ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
        try (Connection c = HarvestDBConnection.get();
                PreparedStatement s = c
                        .prepareStatement("SELECT harvestdefinitions.harvest_id," + "       harvestdefinitions.comments,"
                                + "       harvestdefinitions.numevents," + "       harvestdefinitions.isactive,"
                                + "       harvestdefinitions.edition," + "       fullharvests.maxobjects,"
                                + "       fullharvests.maxbytes," + "       fullharvests.maxjobrunningtime,"
                                + "       fullharvests.previoushd, " + "       harvestdefinitions.channel_id "
                                + "FROM harvestdefinitions, fullharvests" + " WHERE harvestdefinitions.name = ?"
                                + "   AND harvestdefinitions.harvest_id " + "       = fullharvests.harvest_id");
        ) {
            s.setString(1, harvestName);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                SparseFullHarvest sfh = new SparseFullHarvest(res.getLong(1), harvestName, res.getString(2),
                        res.getInt(3), res.getBoolean(4), res.getLong(5), res.getLong(6), res.getLong(7),
                        res.getLong(8), DBUtils.getLongMaybeNull(res, 9), DBUtils.getLongMaybeNull(res, 10));
                // EAV
                long oid = sfh.getOid();
                List<AttributeAndType> attributesAndTypes = EAV.getInstance().getAttributesAndTypes(EAV.SNAPSHOT_TREE_ID, (int)oid);
                sfh.setAttributesAndTypes(attributesAndTypes);
                sfh.setExtendedFieldValues(getExtendedFieldValues(sfh.getOid()));
                return sfh;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting sparse harvest\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Get a sorted list of all domain names of a HarvestDefinition.
     *
     * @param harvestName of HarvestDefinition
     * @return List of all domains of the HarvestDefinition.
     */
    @Override
    public List<String> getListOfDomainsOfHarvestDefinition(String harvestName) {
        ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
        try (Connection c = HarvestDBConnection.get();
                PreparedStatement s = c.prepareStatement(
                        // Note: the DISTINCT below is put in deliberately to fix
                        // bug 1878: Seeds for domain is shown twice on page
                        // History/Harveststatus-seeds.jsp
                        "SELECT DISTINCT domains.name" + " FROM     domains," + "          configurations,"
                                + "          harvest_configs," + "          harvestdefinitions"
                                + " WHERE    configurations.domain_id = domains.domain_id"
                                + " AND harvest_configs.config_id = "
                                + "configurations.config_id" + " AND harvest_configs.harvest_id = "
                                + "harvestdefinitions.harvest_id" + " AND harvestdefinitions.name = ?"
                                + " ORDER BY domains.name");
        ) {
            s.setString(1, harvestName);
            ResultSet res = s.executeQuery();
            List<String> domains = new ArrayList<String>();

            while (res.next()) {
                domains.add(res.getString(1));
            }
            return domains;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting seeds of a domain of a harvest definition" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Get a sorted list of all seeds of a Domain in a HarvestDefinition.
     *
     * @param harvestName of HarvestDefinition
     * @param domainName of Domain
     * @return List of all seeds of the Domain in the HarvestDefinition.
     */
    @Override
    public List<String> getListOfSeedsOfDomainOfHarvestDefinition(String harvestName, String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        try (Connection c = HarvestDBConnection.get();
                PreparedStatement s = c.prepareStatement(
                        "SELECT seedlists.seeds" + " FROM   configurations," + "        harvest_configs,"
                                + "        harvestdefinitions," + "        seedlists," + "        config_seedlists,"
                                + "        domains" + " WHERE  config_seedlists.seedlist_id " + "= seedlists.seedlist_id"
                                + " AND configurations.config_id " + "= config_seedlists.config_id"
                                + " AND configurations.config_id " + "= harvest_configs.config_id"
                                + " AND harvest_configs.harvest_id " + "= harvestdefinitions.harvest_id"
                                + " AND configurations.domain_id = domains.domain_id" + " AND domains.name = ?"
                                + " AND harvestdefinitions.name = ?");
        ) {
            s.setString(1, domainName);
            s.setString(2, harvestName);
            ResultSet res = s.executeQuery();
            List<String> seeds = new ArrayList<String>();

            while (res.next()) {
                String seedsOfDomain = res.getString(1);

                StringTokenizer st = new StringTokenizer(seedsOfDomain, "\n");

                while (st.hasMoreTokens()) {
                    String seed = st.nextToken();

                    boolean isDuplicate = false;
                    for (String entry : seeds) {
                        if (entry.equals(seed)) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    if (!isDuplicate) { // duplicates will not be added
                        seeds.add(seed);
                    }
                }
            }

            Collections.sort(seeds, Collator.getInstance());

            return seeds;
        } catch (SQLException e) {
            throw new IOFailure("SQL error getting seeds of a domain\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    @Override
    public Set<Long> getJobIdsForSnapshotDeduplicationIndex(Long harvestId) {
        ArgumentNotValid.checkNotNull(harvestId, "Long harvestId");
        Set<Long> jobIds = new HashSet<Long>();
        if (!isSnapshot(harvestId)) {
            throw new NotImplementedException("This functionality only works for snapshot harvests");
        }
        List<Long> harvestDefinitions = getPreviousFullHarvests(harvestId);
        try (Connection c = HarvestDBConnection.get();) {
            List<Long> jobs = new ArrayList<Long>();
            if (!harvestDefinitions.isEmpty()) {
                // Select all jobs from a given list of harvest definitions
                jobs.addAll(DBUtils.selectLongList(c, "SELECT jobs.job_id FROM jobs WHERE jobs.harvest_id IN ("
                        + StringUtils.conjoin(",", harvestDefinitions) + ")"));
            }
            jobIds.addAll(jobs);
        } catch (SQLException e) {
            log.error("Unable to close DB connection", e);
        }

        return jobIds;
    }

    /**
     * Get list of harvests previous to this one.
     *
     * @param thisHarvest The id of this harvestdefinition
     * @return a list of IDs belonging to harvests previous to this one.
     */
    private List<Long> getPreviousFullHarvests(Long thisHarvest) {
        List<Long> results = new ArrayList<Long>();
        try (Connection c = HarvestDBConnection.get();) {
            // Follow the chain of originating IDs back
            for (Long originatingHarvest = thisHarvest; originatingHarvest != null;
                // Compute next originatingHarvest
                 originatingHarvest = DBUtils.selectFirstLongValueIfAny(c, "SELECT previoushd FROM fullharvests"
                         + " WHERE fullharvests.harvest_id=?", originatingHarvest)) {
                if (!originatingHarvest.equals(thisHarvest)) {
                    results.add(originatingHarvest);
                }
            }

            // Find the first harvest in the chain (but last in the list).
            Long firstHarvest = thisHarvest;
            if (!results.isEmpty()) {
                firstHarvest = results.get(results.size() - 1);
            }

            // Find the last harvest in the chain before
            Long olderHarvest = DBUtils.selectFirstLongValueIfAny(c, "SELECT fullharvests.harvest_id"
                            + " FROM fullharvests, harvestdefinitions," + "  harvestdefinitions AS currenthd"
                            + " WHERE currenthd.harvest_id=?" + " AND fullharvests.harvest_id "
                            + "= harvestdefinitions.harvest_id"
                            + " AND harvestdefinitions.submitted " + "< currenthd.submitted"
                            + " ORDER BY harvestdefinitions.submitted " + HarvestStatusQuery.SORT_ORDER.DESC.name(),
                    firstHarvest);
            // Follow the chain of originating IDs back
            for (Long originatingHarvest = olderHarvest; originatingHarvest != null; originatingHarvest = DBUtils
                    .selectFirstLongValueIfAny(c, "SELECT previoushd FROM fullharvests"
                            + " WHERE fullharvests.harvest_id=?", originatingHarvest)) {
                results.add(originatingHarvest);
            }
        } catch (SQLException e) {
            log.warn("Exception thrown while updating fullharvests.isindexready field: {}",
                    ExceptionUtils.getSQLExceptionCause(e), e);
        }
        return results;
    }

    @Override
    public void setIndexIsReady(Long harvestId, boolean newValue) {
        if (!isSnapshot(harvestId)) {
            throw new NotImplementedException("Not implemented for non snapshot harvests");
        } else {
            try (
                    Connection c = HarvestDBConnection.get();
                    PreparedStatement s = c.prepareStatement(
                            "UPDATE fullharvests SET isindexready=? WHERE harvest_id=?");
            ) {
                s.setBoolean(1, newValue);
                s.setLong(2, harvestId);
                int rows = s.executeUpdate();
                log.debug(rows + " entries of table fullharvests updated");
            } catch (SQLException e) {
                log.warn("Exception thrown while updating fullharvests.isindexready field: {}",
                        ExceptionUtils.getSQLExceptionCause(e), e);
            }
        }
    }

    /*
     * Removes the entry in harvest_configs, that binds a certain domainconfiguration to this PartialHarvest. TODO maybe
     * update the edition as well.
     */
    @Override
    public void removeDomainConfiguration(Long harvestId, SparseDomainConfiguration key) {
        ArgumentNotValid.checkNotNull(key, "DomainConfigurationKey key");
        if (harvestId == null) {
            // Don't need to do anything, if PartialHarvest is not
            // yet stored in database
            log.warn("No removal of domainConfiguration, " + "as harvestId is null");
            return;
        }
        Connection connection = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            s = connection.prepareStatement("DELETE FROM harvest_configs WHERE harvest_id = ? "
                    + "AND config_id = (SELECT config_id " + " FROM configurations, domains "
                    + "WHERE domains.name = ? AND configurations.name = ?"
                    + "  AND domains.domain_id = configurations.domain_id)");
            s.setLong(1, harvestId);
            s.setString(2, key.getDomainName());
            s.setString(3, key.getConfigurationName());
            s.executeUpdate();
        } catch (SQLException e) {
            log.warn("Exception thrown while removing domainconfiguration: {}", ExceptionUtils.getSQLExceptionCause(e),
                    e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            DBUtils.rollbackIfNeeded(connection, "removing DomainConfiguration from harvest w/id " + harvestId
                    + " failed", harvestId);
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public void updateNextdate(long harvestId, Date nextdate) {
        ArgumentNotValid.checkNotNull(harvestId, "Long harvest ID");
        ArgumentNotValid.checkNotNull(nextdate, "Date nextdate");
        if (harvestId < 0) {
            // Don't need to do anything, if PartialHarvest is not
            // yet stored in database
            return;
        }
        Connection connection = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            connection.setAutoCommit(false);
            s = connection.prepareStatement("UPDATE partialharvests SET nextdate = ? " + "WHERE harvest_id = ?");
            DBUtils.setDateMaybeNull(s, 1, nextdate);
            s.setLong(2, harvestId);
            s.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            log.warn("Exception thrown while updating nextdate: {}", ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            DBUtils.rollbackIfNeeded(connection, "Updating nextdate from", harvestId);
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public void addDomainConfiguration(PartialHarvest ph, SparseDomainConfiguration dcKey) {
        ArgumentNotValid.checkNotNull(ph, "PartialHarvest ph");
        ArgumentNotValid.checkNotNull(dcKey, "DomainConfigurationKey dcKey");

        try (
                Connection connection = HarvestDBConnection.get();
                PreparedStatement s = connection
                        .prepareStatement("INSERT INTO harvest_configs " + "( harvest_id, config_id ) "
                                + "SELECT ?, config_id FROM configurations, domains "
                                + "WHERE domains.name = ? AND configurations.name = ?"
                                + "  AND domains.domain_id = configurations.domain_id");

        ) {
            s.setLong(1, ph.getOid());
            s.setString(2, dcKey.getDomainName());
            s.setString(3, dcKey.getConfigurationName());
            s.executeUpdate();
            s.close();
        } catch (SQLException e) {
            log.warn("Exception thrown while adding domainConfiguration: {}", ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    @Override
    public void resetDomainConfigurations(PartialHarvest ph, List<DomainConfiguration> dcList) {
        ArgumentNotValid.checkNotNull(ph, "PartialHarvest ph");
        ArgumentNotValid.checkNotNull(dcList, "List<DomainConfiguration> dcList");

        try (Connection connection = HarvestDBConnection.get()) {
            createHarvestConfigsEntries(connection, ph, ph.getOid());
        } catch (SQLException e) {
            log.warn("Exception thrown while resetting domainConfigurations: {}",
                    ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    @Override
    public void mapToHarvestChannel(long harvestDefinitionId, HarvestChannel channel) {
        ArgumentNotValid.checkNotNull(channel, "HarvestChannel channel");

        try (
                Connection connection = HarvestDBConnection.get();
                PreparedStatement s = connection.prepareStatement(
                        "UPDATE harvestdefinitions SET channel_id=? WHERE harvest_id=?");
        ) {
            s.setLong(1, channel.getId());
            s.setLong(2, harvestDefinitionId);
            if (s.executeUpdate() != 1) {
                throw new IOFailure("Could not map harvest channel " + channel.getId() + " to harvest definition "
                        + harvestDefinitionId);
            }
            s.close();
        } catch (SQLException e) {
            log.warn("Exception thrown while mapping to harvest channel: {}", ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Saves all extended Field values for a HarvestDefinition in the Database.
     *
     * @param c Connection to Database
     * @param h HarvestDefinition where loaded extended Field Values will be set
     * @throws SQLException If database errors occur.
     */
    private void saveExtendedFieldValues(Connection c, HarvestDefinition h) throws SQLException {
        List<ExtendedFieldValue> list = h.getExtendedFieldValues();
        for (int i = 0; i < list.size(); i++) {
            ExtendedFieldValue efv = list.get(i);
            efv.setInstanceID(h.getOid());

            ExtendedFieldValueDBDAO dao = (ExtendedFieldValueDBDAO) ExtendedFieldValueDAO.getInstance();
            if (efv.getExtendedFieldValueID() != null) {
                dao.update(c, efv, false);
            } else {
                dao.create(c, efv, false);
            }
        }
    }

    /**
     * Reads all extended Field values from the database for a HarvestDefinition.
     *
     * @param h HarvestDefinition where loaded extended Field Values will be set
     * @throws SQLException If database errors occur.
     */
    private void readExtendedFieldValues(HarvestDefinition h) throws SQLException {
        h.setExtendedFieldValues(getExtendedFieldValues(h.getOid()));
    }

    /**
     * Reads all extended Field values from the database for a HarvestDefinitionOid.
     *
     * @param aOid HarvestDefinition where loaded extended Field Values will be set
     * @return a list of ExtendedFieldValues belonging to the given harvest oid
     * @throws SQLException If database errors occur.
     */
    private List<ExtendedFieldValue> getExtendedFieldValues(Long aOid) throws SQLException {
        List<ExtendedFieldValue> extendedFieldValues = new ArrayList<ExtendedFieldValue>();

        ExtendedFieldDAO dao = ExtendedFieldDAO.getInstance();
        List<ExtendedField> list = dao.getAll(ExtendedFieldTypes.HARVESTDEFINITION);

        for (int i = 0; i < list.size(); i++) {
            ExtendedField ef = list.get(i);

            ExtendedFieldValueDAO dao2 = ExtendedFieldValueDAO.getInstance();
            ExtendedFieldValue efv = dao2.read(ef.getExtendedFieldID(), aOid);

            if (efv == null) {
                efv = new ExtendedFieldValue();
                efv.setExtendedFieldID(ef.getExtendedFieldID());
                efv.setInstanceID(aOid);
                efv.setContent(new ExtendedFieldDefaultValue(ef.getDefaultValue(), ef.getFormattingPattern(), ef
                        .getDatatype()).getDBValue());
            }

            extendedFieldValues.add(efv);
        }

        return extendedFieldValues;
    }
}
