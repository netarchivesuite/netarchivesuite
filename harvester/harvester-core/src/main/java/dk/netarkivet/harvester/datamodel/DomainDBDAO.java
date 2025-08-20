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

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.FilterIterator;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.datamodel.eav.EAV;
import dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValue;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDBDAO;

/**
 * A database-based implementation of the DomainDAO.
 * <p>
 * The statements to create the tables are located in:
 * <ul>
 * <li><em>Derby:</em> scripts/sql/createfullhddb.sql</li>
 * <li><em>MySQL:</em> scripts/sql/createfullhddb.mysql</li>
 * <li><em>PostgreSQL:</em> scripts/postgresql/netarchivesuite_init.sql</li>
 * </ul>
 */
public class DomainDBDAO extends DomainDAO {

    /** The log. */
    private static final Logger log = LoggerFactory.getLogger(DomainDBDAO.class);

    /**
     * Creates a database-based implementation of the DomainDAO. Will check that all schemas have correct versions, and
     * update the ones that haven't.
     *
     * @throws IOFailure on trouble updating tables to new versions, or on tables with wrong versions that we don't know
     * how to change to expected version.
     */
    protected DomainDBDAO() {
        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.CONFIGURATIONS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.DOMAINS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.CONFIGPASSWORDS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.CONFIGSEEDLISTS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.SEEDLISTS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.PASSWORDS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.OWNERINFO);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.HISTORYINFO);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELDTYPE);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELD);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELDVALUE);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    protected void create(Connection connection, Domain d) {
        ArgumentNotValid.checkNotNull(d, "d");
        ArgumentNotValid.checkNotNullOrEmpty(d.getName(), "d.getName()");
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(d.getName()),"Not creating domain wth invalid name " + d.getName());

        if (exists(connection, d.getName())) {
            String msg = "Cannot create already existing domain " + d;
            log.debug(msg);
            throw new PermissionDenied(msg);
        }

        PreparedStatement s = null;
        log.debug("trying to create domain with name: " + d.getName());
        try {
            connection.setAutoCommit(false);
            s = connection.prepareStatement("INSERT INTO domains "
                    + "(name, comments, defaultconfig, crawlertraps, edition, alias, lastaliasupdate ) "
                    + "VALUES ( ?, ?, -1, ?, ?, ?, ? )", Statement.RETURN_GENERATED_KEYS);
            // Id is autogenerated
            // defaultconfig cannot exist yet, so we put in -1
            // until we have configs
            DBUtils.setName(s, 1, d, Constants.MAX_NAME_SIZE);
            DBUtils.setComments(s, 2, d, Constants.MAX_COMMENT_SIZE);
            s.setString(3, StringUtils.conjoin("\n", d.getCrawlerTraps()));
            long initialEdition = 1;
            s.setLong(4, initialEdition);
            AliasInfo aliasInfo = d.getAliasInfo();
            DBUtils.setLongMaybeNull(
                    s,
                    5,
                    aliasInfo == null ? null : DBUtils.selectLongValue(connection,
                            "SELECT domain_id FROM domains WHERE name = ?", aliasInfo.getAliasOf()));
            DBUtils.setDateMaybeNull(s, 6, aliasInfo == null ? null : aliasInfo.getLastChange());
            s.executeUpdate();

            d.setID(DBUtils.getGeneratedID(s));
            s.close();

            Iterator<Password> passwords = d.getAllPasswords();
            while (passwords.hasNext()) {
                Password p = passwords.next();
                insertPassword(connection, d, p);
            }

            Iterator<SeedList> seedlists = d.getAllSeedLists();
            if (!seedlists.hasNext()) {
                String msg = "No seedlists for domain " + d;
                log.debug(msg);
                throw new ArgumentNotValid(msg);
            }
            while (seedlists.hasNext()) {
                SeedList sl = seedlists.next();
                insertSeedlist(connection, d, sl);
            }

            Iterator<DomainConfiguration> dcs = d.getAllConfigurations();
            if (!dcs.hasNext()) {
                String msg = "No configurations for domain " + d;
                log.debug(msg);
                throw new ArgumentNotValid(msg);
            }
            while (dcs.hasNext()) {
                DomainConfiguration dc = dcs.next();
                insertConfiguration(connection, d, dc);

                // Create xref tables for seedlists referenced by this config
                createConfigSeedlistsEntries(connection, d, dc);

                // Create xref tables for passwords referenced by this config
                createConfigPasswordsEntries(connection, d, dc);
            }

            // Now that configs are defined, set the default config.
            s = connection.prepareStatement("UPDATE domains SET defaultconfig = (SELECT config_id FROM configurations "
                    + "WHERE configurations.name = ? AND configurations.domain_id = ?) WHERE domain_id = ?");
            DBUtils.setName(s, 1, d.getDefaultConfiguration(), Constants.MAX_NAME_SIZE);
            s.setLong(2, d.getID());
            s.setLong(3, d.getID());
            s.executeUpdate();
            s.close();
            for (Iterator<HarvestInfo> hi = d.getHistory().getHarvestInfo(); hi.hasNext();) {
                insertHarvestInfo(connection, d, hi.next());
            }

            for (DomainOwnerInfo doi : d.getAllDomainOwnerInfo()) {
                insertOwnerInfo(connection, d, doi);
            }

            saveExtendedFieldValues(connection, d);

            connection.commit();
            d.setEdition(initialEdition);
        } catch (SQLException e) {
            String message = "SQL error creating domain " + d + " in database" + "\n";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.rollbackIfNeeded(connection, "creating", d);
        }
    }

    @Override
    public synchronized void update(Domain d) {
        ArgumentNotValid.checkNotNull(d, "domain");

        if (!exists(d.getName())) {
            throw new UnknownID("No domain named " + d.getName() + " exists");
        }
        Connection connection = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            connection.setAutoCommit(false);
            // Domain object may not have ID yet, so get it from the DB
            long domainID = DBUtils.selectLongValue(connection, "SELECT domain_id FROM domains WHERE name = ?",
                    d.getName());
            if (d.hasID() && d.getID() != domainID) {
                String message = "Domain " + d + " has wrong id: Has " + d.getID() + ", but persistent store claims "
                        + domainID;
                log.warn(message);
                throw new ArgumentNotValid(message);
            }
            d.setID(domainID);

            // The alias field is now updated using a separate select request
            // rather than embedding the select inside the update statement.
            // This change was needed to accommodate MySQL, and may lower
            // performance.
            s = connection.prepareStatement("UPDATE domains SET "
                    + "comments = ?, crawlertraps = ?, edition = ?, alias = ?, lastAliasUpdate = ? "
                    + "WHERE domain_id = ? AND edition = ?");
            DBUtils.setComments(s, 1, d, Constants.MAX_COMMENT_SIZE);
            s.setString(2, StringUtils.conjoin("\n", d.getCrawlerTraps()));
            final long newEdition = d.getEdition() + 1;
            s.setLong(3, newEdition);
            AliasInfo aliasInfo = d.getAliasInfo();
            DBUtils.setLongMaybeNull(
                    s,
                    4,
                    aliasInfo == null ? null : DBUtils.selectLongValue(connection,
                            "SELECT domain_id FROM domains WHERE name = ?", aliasInfo.getAliasOf()));
            DBUtils.setDateMaybeNull(s, 5, aliasInfo == null ? null : aliasInfo.getLastChange());
            s.setLong(6, d.getID());
            s.setLong(7, d.getEdition());
            int rows = s.executeUpdate();
            if (rows == 0) {
                String message = "Edition " + d.getEdition() + " has expired, cannot update " + d;
                log.debug(message);
                throw new PermissionDenied(message);
            }
            s.close();

            updatePasswords(connection, d);

            updateSeedlists(connection, d);

            updateConfigurations(connection, d);

            updateOwnerInfo(connection, d);

            updateHarvestInfo(connection, d);

            saveExtendedFieldValues(connection, d);

            // Now that configs are updated, we can set default_config
            s = connection.prepareStatement("UPDATE domains SET defaultconfig = (SELECT config_id "
                    + "FROM configurations WHERE domain_id = ? AND name = ?) WHERE domain_id = ?");
            s.setLong(1, d.getID());
            s.setString(2, d.getDefaultConfiguration().getName());
            s.setLong(3, d.getID());
            s.executeUpdate();
            connection.commit();
            d.setEdition(newEdition);
        } catch (SQLException e) {
            String message = "SQL error updating domain " + d + " in database" + "\n";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            DBUtils.rollbackIfNeeded(connection, "updating", d);
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Update the list of passwords for the given domain, keeping IDs where applicable.
     *
     * @param c A connection to the database
     * @param d A domain to update.
     * @throws SQLException If any database problems occur during the update process.
     */
    private void updatePasswords(Connection c, Domain d) throws SQLException {
        Map<String, Long> oldNames = DBUtils.selectStringLongMap(c,
                "SELECT name, password_id FROM passwords WHERE domain_id = ?", d.getID());
        PreparedStatement s = c.prepareStatement("UPDATE passwords SET " + "comments = ?, " + "url = ?, "
                + "realm = ?, username = ?, " + "password = ? " + "WHERE name = ? AND domain_id = ?");
        for (Iterator<Password> pwds = d.getAllPasswords(); pwds.hasNext();) {
            Password pwd = pwds.next();
            if (oldNames.containsKey(pwd.getName())) {
                DBUtils.setComments(s, 1, pwd, Constants.MAX_COMMENT_SIZE);
                DBUtils.setStringMaxLength(s, 2, pwd.getPasswordDomain(), Constants.MAX_URL_SIZE, pwd, "password url");
                DBUtils.setStringMaxLength(s, 3, pwd.getRealm(), Constants.MAX_REALM_NAME_SIZE, pwd, "password realm");
                DBUtils.setStringMaxLength(s, 4, pwd.getUsername(), Constants.MAX_USER_NAME_SIZE, pwd,
                        "password username");
                DBUtils.setStringMaxLength(s, 5, pwd.getPassword(), Constants.MAX_PASSWORD_SIZE, pwd, "password");
                s.setString(6, pwd.getName());
                s.setLong(7, d.getID());
                s.executeUpdate();
                s.clearParameters();
                pwd.setID(oldNames.get(pwd.getName()));
                oldNames.remove(pwd.getName());
            } else {
                insertPassword(c, d, pwd);
            }
        }
        s.close();
        s = c.prepareStatement("DELETE FROM passwords WHERE password_id = ?");
        for (Long gone : oldNames.values()) {
            // Check that we're not deleting something that's in use
            // Since deletion is very rare, this is allowed to take
            // some time.
            String usages = DBUtils.getUsages(c, "SELECT configurations.name  FROM configurations, config_passwords"
                    + " WHERE configurations.config_id = config_passwords.config_id "
                    + "AND config_passwords.password_id = ?", gone, gone);
            if (usages != null) {
                String name = DBUtils.selectStringValue(c, "SELECT name FROM passwords WHERE password_id = ?", gone);
                String message = "Cannot delete password " + name + " as it is used in " + usages;
                log.debug(message);
                throw new PermissionDenied(message);
            }
            s.setLong(1, gone);
            s.executeUpdate();
            s.clearParameters();
        }
    }

    /**
     * Update the list of seedlists for the given domain, keeping IDs where applicable.
     *
     * @param c A connection to the database
     * @param d A domain to update.
     * @throws SQLException If any database problems occur during the update process.
     */
    private void updateSeedlists(Connection c, Domain d) throws SQLException {
        Map<String, Long> oldNames = DBUtils.selectStringLongMap(c, "SELECT name, seedlist_id FROM seedlists "
                + "WHERE domain_id = ?", d.getID());
        PreparedStatement s = c.prepareStatement("UPDATE seedlists SET comments = ?, " + "seeds = ? "
                + "WHERE name = ? AND domain_id = ?");
        for (Iterator<SeedList> sls = d.getAllSeedLists(); sls.hasNext();) {
            SeedList sl = sls.next();
            if (oldNames.containsKey(sl.getName())) {
                DBUtils.setComments(s, 1, sl, Constants.MAX_COMMENT_SIZE);
                DBUtils.setClobMaxLength(s, 2, sl.getSeedsAsString(), Constants.MAX_SEED_LIST_SIZE, sl, "seedlist");
                s.setString(3, sl.getName());
                s.setLong(4, d.getID());
                s.executeUpdate();
                s.clearParameters();
                sl.setID(oldNames.get(sl.getName()));
                oldNames.remove(sl.getName());
            } else {
                insertSeedlist(c, d, sl);
            }
        }
        s.close();
        s = c.prepareStatement("DELETE FROM seedlists WHERE seedlist_id = ?");
        for (Long gone : oldNames.values()) {
            // Check that we're not deleting something that's in use
            // Since deletion is very rare, this is allowed to take
            // some time.
            String usages = DBUtils.getUsages(c, "SELECT configurations.name FROM configurations, config_seedlists"
                    + " WHERE configurations.config_id = config_seedlists.config_id "
                    + "AND config_seedlists.seedlist_id = ?", gone, gone);
            if (usages != null) {
                String name = DBUtils.selectStringValue(c, "SELECT name FROM seedlists WHERE seedlist_id = ?", gone);
                String message = "Cannot delete seedlist " + name + " as it is used in " + usages;
                log.debug(message);
                throw new PermissionDenied(message);
            }
            s.setLong(1, gone);
            s.executeUpdate();
            s.clearParameters();
        }
    }

    /**
     * Update the list of configurations for the given domain, keeping IDs where applicable. This also builds the xref
     * tables for passwords and seedlists used in configurations, and so should be run after those are updated.
     *
     * @param connection A connection to the database
     * @param d A domain to update.
     * @throws SQLException If any database problems occur during the update process.
     */
    private void updateConfigurations(Connection connection, Domain d) throws SQLException {
        Map<String, Long> oldNames = DBUtils.selectStringLongMap(connection,
                "SELECT name, config_id FROM configurations WHERE domain_id = ?", d.getID());
        PreparedStatement s = connection.prepareStatement("UPDATE configurations SET comments = ?, "
                + "template_id = ( SELECT template_id FROM ordertemplates " + "WHERE name = ? ), " + "maxobjects = ?, "
                + "maxrate = ?, " + "maxbytes = ? " + "WHERE name = ? AND domain_id = ?");
        for (Iterator<DomainConfiguration> dcs = d.getAllConfigurations(); dcs.hasNext();) {
            DomainConfiguration dc = dcs.next();

            if (oldNames.containsKey(dc.getName())) {
                // Update
                DBUtils.setComments(s, 1, dc, Constants.MAX_COMMENT_SIZE);
                s.setString(2, dc.getOrderXmlName());
                s.setLong(3, dc.getMaxObjects());
                s.setInt(4, dc.getMaxRequestRate());
                s.setLong(5, dc.getMaxBytes());
                s.setString(6, dc.getName());
                s.setLong(7, d.getID());
                s.executeUpdate();
                s.clearParameters();
                dc.setID(oldNames.get(dc.getName()));
                oldNames.remove(dc.getName());
            } else {
                insertConfiguration(connection, d, dc);
            }

            updateConfigPasswordsEntries(connection, d, dc);
            updateConfigSeedlistsEntries(connection, d, dc);
        }
        s.close();
        s = connection.prepareStatement("DELETE FROM configurations WHERE config_id = ?");
        for (Long gone : oldNames.values()) {
            // Before deleting, check if this is unused. Since deletion is
            // rare, this is allowed to take some time to give good output
            String usages = DBUtils.getUsages(connection, "SELECT harvestdefinitions.name"
                    + "  FROM harvestdefinitions, harvest_configs WHERE harvestdefinitions.harvest_id = "
                    + "harvest_configs.harvest_id AND harvest_configs.config_id = ?", gone, gone);
            if (usages != null) {
                String name = DBUtils.selectStringValue(connection, "SELECT name FROM configurations "
                        + "WHERE config_id = ?", gone);
                String message = "Cannot delete configuration " + name + " as it is used in " + usages;
                log.debug(message);
                throw new PermissionDenied(message);
            }
            s.setLong(1, gone);
            s.executeUpdate();
            s.clearParameters();
        }
    }

    /**
     * Update the list of owner info for the given domain, keeping IDs where applicable.
     *
     * @param c A connection to the database
     * @param d A domain to update.
     * @throws SQLException If any database problems occur during the update process.
     */
    private void updateOwnerInfo(Connection c, Domain d) throws SQLException {
        List<Long> oldIDs = DBUtils.selectLongList(c, "SELECT ownerinfo_id FROM ownerinfo " + "WHERE domain_id = ?",
                d.getID());
        PreparedStatement s = c.prepareStatement("UPDATE ownerinfo SET " + "created = ?, " + "info = ? "
                + "WHERE ownerinfo_id = ?");
        for (DomainOwnerInfo doi : d.getAllDomainOwnerInfo()) {
            if (doi.hasID() && oldIDs.remove(doi.getID())) {
                s.setTimestamp(1, new Timestamp(doi.getDate().getTime()));
                DBUtils.setStringMaxLength(s, 2, doi.getInfo(), Constants.MAX_OWNERINFO_SIZE, doi, "owner info");
                s.setLong(3, doi.getID());
                s.executeUpdate();
                s.clearParameters();
            } else {
                insertOwnerInfo(c, d, doi);
            }
        }
        if (oldIDs.size() != 0) {
            String message = "Not allowed to delete ownerinfo " + oldIDs + " on " + d;
            log.debug(message);
            throw new IOFailure(message);
        }
    }

    /**
     * Update the list of harvest info for the given domain, keeping IDs where applicable.
     *
     * @param c A connection to the database
     * @param d A domain to update.
     * @throws SQLException If any database problems occur during the update process.
     */
    private void updateHarvestInfo(Connection c, Domain d) throws SQLException {
        List<Long> oldIDs = DBUtils.selectLongList(c, "SELECT historyinfo.historyinfo_id "
                + "FROM historyinfo, configurations WHERE historyinfo.config_id = configurations.config_id"
                + "  AND configurations.domain_id = ?", d.getID());
        PreparedStatement s = c.prepareStatement("UPDATE historyinfo SET " + "stopreason = ?, " + "objectcount = ?, "
                + "bytecount = ?, " + "config_id = " + " (SELECT config_id FROM configurations, domains"
                + "  WHERE domains.domain_id = ?" + "    AND configurations.name = ?"
                + "    AND configurations.domain_id = domains.domain_id), " + "harvest_id = ?, " + "job_id = ? "
                + "WHERE historyinfo_id = ?");
        Iterator<HarvestInfo> his = d.getHistory().getHarvestInfo();
        while (his.hasNext()) {
            HarvestInfo hi = his.next();
            if (hi.hasID() && oldIDs.remove(hi.getID())) {
                s.setInt(1, hi.getStopReason().ordinal());
                s.setLong(2, hi.getCountObjectRetrieved());
                s.setLong(3, hi.getSizeDataRetrieved());
                s.setLong(4, d.getID());
                s.setString(5, d.getConfiguration(hi.getDomainConfigurationName()).getName());
                s.setLong(6, hi.getHarvestID());
                if (hi.getJobID() != null) {
                    s.setLong(7, hi.getJobID());
                } else {
                    s.setNull(7, Types.BIGINT);
                }
                s.setLong(8, hi.getID());
                s.executeUpdate();
                s.clearParameters();
            } else {
                insertHarvestInfo(c, d, hi);
            }
        }
        if (oldIDs.size() != 0) {
            String message = "Not allowed to delete historyinfo " + oldIDs + " on " + d;
            log.debug(message);
            throw new IOFailure(message);
        }
    }

    /**
     * Insert new harvest info for a domain.
     *
     * @param c A connection to the database
     * @param d A domain to insert on. The domains ID must be correct.
     * @param harvestInfo Harvest info to insert.
     */
    private void insertHarvestInfo(Connection c, Domain d, HarvestInfo harvestInfo) {
        PreparedStatement s = null;
        try {
            // Note that the config_id is grabbed from the configurations table.
            s = c.prepareStatement("INSERT INTO historyinfo " + "( stopreason, objectcount, bytecount, config_id, "
                    + "job_id, harvest_id, harvest_time ) " + "VALUES ( ?, ?, ?, ?, ?, ?, ? )",
                    Statement.RETURN_GENERATED_KEYS);
            s.setInt(1, harvestInfo.getStopReason().ordinal());
            s.setLong(2, harvestInfo.getCountObjectRetrieved());
            s.setLong(3, harvestInfo.getSizeDataRetrieved());
            // TODO More stable way to get IDs, use a select
            s.setLong(4, d.getConfiguration(harvestInfo.getDomainConfigurationName()).getID());
            if (harvestInfo.getJobID() != null) {
                s.setLong(5, harvestInfo.getJobID());
            } else {
                s.setNull(5, Types.BIGINT);
            }
            s.setLong(6, harvestInfo.getHarvestID());
            s.setTimestamp(7, new Timestamp(harvestInfo.getDate().getTime()));
            s.executeUpdate();
            harvestInfo.setID(DBUtils.getGeneratedID(s));
        } catch (SQLException e) {
            throw new IOFailure("SQL error while inserting harvest info " + harvestInfo + " for " + d + "\n", e);
        }
    }

    /**
     * Insert new owner info for a domain.
     *
     * @param c A connection to the database
     * @param d A domain to insert on. The domains ID must be correct.
     * @param doi Owner info to insert.
     * @throws SQLException If any database problems occur during the insertion process.
     */
    private void insertOwnerInfo(Connection c, Domain d, DomainOwnerInfo doi) throws SQLException {
        PreparedStatement s = c.prepareStatement("INSERT INTO ownerinfo ( domain_id, created, info ) "
                + "VALUES ( ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        s.setLong(1, d.getID());
        s.setTimestamp(2, new Timestamp(doi.getDate().getTime()));
        s.setString(3, doi.getInfo());
        s.executeUpdate();
        doi.setID(DBUtils.getGeneratedID(s));
    }

    /**
     * Insert new seedlist for a domain.
     *
     * @param c A connection to the database
     * @param d A domain to insert on. The domains ID must be correct.
     * @param sl Seedlist to insert.
     * @throws SQLException If some database error occurs during the insertion process.
     */
    private void insertSeedlist(Connection c, Domain d, SeedList sl) throws SQLException {
        PreparedStatement s = c.prepareStatement("INSERT INTO seedlists ( name, comments, domain_id, seeds ) "
                + "VALUES ( ?, ?, ?, ? )", Statement.RETURN_GENERATED_KEYS);
        // ID is autogenerated
        DBUtils.setName(s, 1, sl, Constants.MAX_NAME_SIZE);
        DBUtils.setComments(s, 2, sl, Constants.MAX_COMMENT_SIZE);
        s.setLong(3, d.getID());
        DBUtils.setClobMaxLength(s, 4, sl.getSeedsAsString(), Constants.MAX_SEED_LIST_SIZE, sl, "seedlist");
        s.executeUpdate();
        sl.setID(DBUtils.getGeneratedID(s));
    }

    /**
     * Inserts a new password entry into the database.
     *
     * @param c A connection to the database
     * @param d A domain to insert on. The domains ID must be correct.
     * @param p A password entry to insert.
     * @throws SQLException If some database error occurs during the insertion process.
     */
    private void insertPassword(Connection c, Domain d, Password p) throws SQLException {
        PreparedStatement s = c.prepareStatement("INSERT INTO passwords "
                + "( name, comments, domain_id, url, realm, username, " + "password ) "
                + "VALUES ( ?, ?, ?, ?, ?, ?, ? )", Statement.RETURN_GENERATED_KEYS);
        // ID is autogenerated
        DBUtils.setName(s, 1, p, Constants.MAX_NAME_SIZE);
        DBUtils.setComments(s, 2, p, Constants.MAX_COMMENT_SIZE);
        s.setLong(3, d.getID());
        DBUtils.setStringMaxLength(s, 4, p.getPasswordDomain(), Constants.MAX_URL_SIZE, p, "password url");
        DBUtils.setStringMaxLength(s, 5, p.getRealm(), Constants.MAX_REALM_NAME_SIZE, p, "password realm");
        DBUtils.setStringMaxLength(s, 6, p.getUsername(), Constants.MAX_USER_NAME_SIZE, p, "password username");
        DBUtils.setStringMaxLength(s, 7, p.getPassword(), Constants.MAX_PASSWORD_SIZE, p, "password");
        s.executeUpdate();
        p.setID(DBUtils.getGeneratedID(s));
    }

    /**
     * Insert the basic configuration info into the DB. This does not establish the connections with seedlists and
     * passwords, use {create,update}Config{Passwords,Seedlists}Entries for that.
     *
     * @param connection A connection to the database
     * @param d a domain
     * @param dc a domainconfiguration
     * @throws SQLException If some database error occurs during the insertion process.
     */
    private void insertConfiguration(Connection connection, Domain d, DomainConfiguration dc) throws SQLException {
        long templateId = DBUtils.selectLongValue(connection, "SELECT template_id FROM ordertemplates WHERE name = ?",
                dc.getOrderXmlName());
        PreparedStatement s = connection.prepareStatement("INSERT INTO configurations "
                + "( name, comments, domain_id, template_id, maxobjects, " + "maxrate, maxbytes ) "
                + "VALUES ( ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        // Id is autogenerated
        DBUtils.setName(s, 1, dc, Constants.MAX_NAME_SIZE);
        DBUtils.setComments(s, 2, dc, Constants.MAX_COMMENT_SIZE);
        s.setLong(3, d.getID());
        s.setLong(4, templateId);
        s.setLong(5, dc.getMaxObjects());
        s.setInt(6, dc.getMaxRequestRate());
        s.setLong(7, dc.getMaxBytes());
        int rows = s.executeUpdate();
        if (rows != 1) {
            String message = "Error inserting configuration " + dc;
            log.warn(message);
            throw new IOFailure(message);
        }
        dc.setID(DBUtils.getGeneratedID(s));
    }

    /**
     * Delete all entries in the given crossref table that belong to the configuration.
     *
     * @param c A connection to the database
     * @param configId The domain configuration to remove entries for.
     * @param table One of "config_passwords" or "config_seedlists"
     * @throws SQLException If any database problems occur during the delete process.
     */
    private void deleteConfigFromTable(Connection c, long configId, String table) throws SQLException {
        PreparedStatement s = c.prepareStatement("DELETE FROM " + table + " WHERE " + table + ".config_id = ?");
        s.setLong(1, configId);
        s.executeUpdate();
    }

    /**
     * Delete all entries from the config_passwords table that refer to the given configuration and insert the current
     * ones.
     *
     * @param c A connection to the database
     * @param d A domain to operate on
     * @param dc Configuration to update.
     * @throws SQLException If any database problems occur during the update process.
     */
    private void updateConfigPasswordsEntries(Connection c, Domain d, DomainConfiguration dc) throws SQLException {
        deleteConfigFromTable(c, dc.getID(), "config_passwords");
        createConfigPasswordsEntries(c, d, dc);
    }

    /**
     * Create the xref table for passwords used by configurations.
     *
     * @param c A connection to the database
     * @param d A domain to operate on.
     * @param dc A configuration to create xref table for.
     * @throws SQLException If any database problems occur during the insertion of password entries for the given domain
     * configuration
     */
    private void createConfigPasswordsEntries(Connection c, Domain d, DomainConfiguration dc) throws SQLException {
        PreparedStatement s = c.prepareStatement("INSERT INTO config_passwords " + "( config_id, password_id ) "
                + "SELECT config_id, password_id " + "  FROM configurations, passwords"
                + " WHERE configurations.domain_id = ?" + "   AND configurations.name = ?"
                + "   AND passwords.name = ?" + "   AND passwords.domain_id = configurations.domain_id");
        for (Iterator<Password> passwords = dc.getPasswords(); passwords.hasNext();) {
            Password p = passwords.next();
            s.setLong(1, d.getID());
            s.setString(2, dc.getName());
            s.setString(3, p.getName());
            s.executeUpdate();
            s.clearParameters();
        }
    }

    /**
     * Delete all entries from the config_seedlists table that refer to the given configuration and insert the current
     * ones.
     *
     * @param c An open connection to the harvestDatabase.
     * @param d A domain to operate on
     * @param dc Configuration to update.
     * @throws SQLException If any database problems occur during the update process.
     */
    private void updateConfigSeedlistsEntries(Connection c, Domain d, DomainConfiguration dc) throws SQLException {
        deleteConfigFromTable(c, dc.getID(), "config_seedlists");
        createConfigSeedlistsEntries(c, d, dc);
    }

    /**
     * Create the xref table for seedlists used by configurations.
     *
     * @param c A connection to the database
     * @param d A domain to operate on.
     * @param dc A configuration to create xref table for.
     * @throws SQLException If any database problems occur during the insertion of seedlist entries for the given domain
     * configuration
     */
    private void createConfigSeedlistsEntries(Connection c, Domain d, DomainConfiguration dc) throws SQLException {
        PreparedStatement s = c.prepareStatement("INSERT INTO config_seedlists " + " ( config_id, seedlist_id ) "
                + "SELECT configurations.config_id, seedlists.seedlist_id" + "  FROM configurations, seedlists"
                + " WHERE configurations.name = ?" + "   AND seedlists.name = ?"
                + "   AND configurations.domain_id = ?" + "   AND seedlists.domain_id = ?");
        for (Iterator<SeedList> seedlists = dc.getSeedLists(); seedlists.hasNext();) {
            SeedList sl = seedlists.next();
            s.setString(1, dc.getName());
            s.setString(2, sl.getName());
            s.setLong(3, d.getID());
            s.setLong(4, d.getID());
            s.executeUpdate();
            s.clearParameters();
        }
    }

    @Override
    protected synchronized Domain read(Connection c, String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(domainName), "Invalid domain name " + domainName);
        if (!exists(c, domainName)) {
            throw new UnknownID("No domain by the name '" + domainName + "'");
        }
        return readKnown(c, domainName);
    }

    @Override
    protected synchronized Domain readKnown(Connection c, String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(domainName), "Invalid domain name " + domainName);
        Domain result;
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT domains.domain_id, " + "domains.comments, " + "domains.crawlertraps, "
                    + "domains.edition, " + "configurations.name, " + " (SELECT name FROM domains as aliasdomains"
                    + "  WHERE aliasdomains.domain_id = domains.alias), " + "domains.lastaliasupdate "
                    + "FROM domains, configurations " + "WHERE domains.name = ?"
                    + "  AND domains.defaultconfig = configurations.config_id");
            s.setString(1, domainName);
            ResultSet res = s.executeQuery();
            if (!res.next()) {
                final String message = "Error reading existing domain '" + domainName
                        + "' due to database inconsistency. "
                        + "Note that this should never happen. Please ask your database admin to check "
                        + "your 'domains' and 'configurations' tables for any inconsistencies.";
                log.warn(message);
                throw new IOFailure(message);
            }
            int domainId = res.getInt(1);
            String comments = res.getString(2);
            String crawlertraps = res.getString(3);
            long edition = res.getLong(4);
            String defaultconfig = res.getString(5);
            String alias = res.getString(6);
            Date lastAliasUpdate = DBUtils.getDateMaybeNull(res, 7);
            s.close();
            Domain d = new Domain(domainName);
            d.setComments(comments);
            // don't throw exception if illegal regexps are found.
            boolean strictMode = false; 
            String[] traps = crawlertraps.split("\n");
            List<String> insertList = new ArrayList<String>();
            for (String trap: traps) {
                if (!trap.isEmpty()) { // Ignore empty traps (NAS-2480)
                    insertList.add(trap);
                }
            }
            log.trace("Found {} crawlertraps for domain '{}' in database", insertList.size(), domainName);
            d.setCrawlerTraps(insertList, strictMode);
            d.setID(domainId);
            d.setEdition(edition);
            if (alias != null) {
                d.setAliasInfo(new AliasInfo(domainName, alias, lastAliasUpdate));
            }

            readSeedlists(c, d);
            readPasswords(c, d);
            readConfigurations(c, d);
            // Now that configs are in, we can set the default
            d.setDefaultConfiguration(defaultconfig);
            readOwnerInfo(c, d);
            readHistoryInfo(c, d);

            result = d;
        } catch (SQLException e) {
            throw new IOFailure("SQL Error while reading domain " + domainName + "\n", e);
        }

        return result;
    }

    /**
     * Read the configurations for the domain. This should not be called until after passwords and seedlists are read.
     *
     * @param c A connection to the database
     * @param d The domain being read. Its ID must be set.
     * @throws SQLException If database errors occur.
     */
    private void readConfigurations(Connection c, Domain d) throws SQLException {
        // Read the configurations now that passwords and seedlists exist
        PreparedStatement s = c.prepareStatement("SELECT " + "config_id, " + "configurations.name, " + "comments, "
                + "ordertemplates.name, " + "maxobjects, " + "maxrate, " + "maxbytes"
                + " FROM configurations, ordertemplates " + "WHERE domain_id = ?"
                + "  AND configurations.template_id = " + "ordertemplates.template_id");
        s.setLong(1, d.getID());
        ResultSet res = s.executeQuery();
        while (res.next()) {
            long domainconfigId = res.getLong(1);
            String domainconfigName = res.getString(2);
            String domainConfigComments = res.getString(3);
            String order = res.getString(4);
            long maxobjects = res.getLong(5);
            int maxrate = res.getInt(6);
            long maxbytes = res.getLong(7);
            PreparedStatement s1 = c.prepareStatement("SELECT seedlists.name " + "FROM seedlists, config_seedlists "
                    + "WHERE config_seedlists.config_id = ? " + "AND config_seedlists.seedlist_id = "
                    + "seedlists.seedlist_id");
            s1.setLong(1, domainconfigId);
            ResultSet seedlistResultset = s1.executeQuery();
            List<SeedList> seedlists = new ArrayList<SeedList>();
            while (seedlistResultset.next()) {
                seedlists.add(d.getSeedList(seedlistResultset.getString(1)));
            }
            s1.close();
            if (seedlists.isEmpty()) {
                String message = "Configuration " + domainconfigName + " of " + d + " has no seedlists";
                log.warn(message);
                throw new IOFailure(message);
            }

            s1 = c.prepareStatement("SELECT passwords.name FROM passwords, config_passwords "
                    + "WHERE config_passwords.config_id = ? AND config_passwords.password_id = passwords.password_id");
            s1.setLong(1, domainconfigId);
            ResultSet passwordResultset = s1.executeQuery();
            List<Password> passwords = new ArrayList<Password>();
            while (passwordResultset.next()) {
                passwords.add(d.getPassword(passwordResultset.getString(1)));
            }
            DomainConfiguration dc = new DomainConfiguration(domainconfigName, d, seedlists, passwords);
            dc.setOrderXmlName(order);
            dc.setMaxObjects(maxobjects);
            dc.setMaxRequestRate(maxrate);
            dc.setComments(domainConfigComments);
            dc.setMaxBytes(maxbytes);
            dc.setID(domainconfigId);
            d.addConfiguration(dc);
            s1.close();

            // EAV
            List<AttributeAndType> attributesAndTypes = EAV.getInstance().getAttributesAndTypes(EAV.DOMAIN_TREE_ID, (int)domainconfigId);
            dc.setAttributesAndTypes(attributesAndTypes);
        }
        if (!d.getAllConfigurations().hasNext()) {
            String message = "Loaded domain " + d + " with no configurations";
            log.warn(message);
            throw new IOFailure(message);
        }
    }

    @Override
    public List<Long> findUsedConfigurations(Long domainID) {
        Connection connection = HarvestDBConnection.get();
        try {
            List<Long> usedConfigurations = new LinkedList<Long>();

            PreparedStatement readUsedConfigurations = connection
                    .prepareStatement(" SELECT configurations.config_id, configurations.name" + " FROM configurations "
                            + " JOIN harvest_configs USING (config_id) "
                            + " JOIN harvestdefinitions USING (harvest_id) " + " WHERE configurations.domain_id = ? "
                            + "AND harvestdefinitions.isactive = ?");
            readUsedConfigurations.setLong(1, domainID);
            readUsedConfigurations.setBoolean(2, true);
            ResultSet res = readUsedConfigurations.executeQuery();
            while (res.next()) {
                usedConfigurations.add(res.getLong(1));
            }
            readUsedConfigurations.close();

            return usedConfigurations;
        } catch (SQLException e) {
            throw new IOFailure("SQL Error while reading configuration + seeds lists", e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Read owner info entries for the domain.
     *
     * @param c A connection to the database
     * @param d The domain being read. Its ID must be set.
     * @throws SQLException If database errors occur.
     */
    private void readOwnerInfo(Connection c, Domain d) throws SQLException {
        // Read owner info
        PreparedStatement s = c.prepareStatement("SELECT ownerinfo_id, created, info"
                + " FROM ownerinfo WHERE domain_id = ?");
        s.setLong(1, d.getID());
        ResultSet res = s.executeQuery();
        while (res.next()) {
            final DomainOwnerInfo ownerinfo = new DomainOwnerInfo(new Date(res.getTimestamp(2).getTime()),
                    res.getString(3));
            ownerinfo.setID(res.getLong(1));
            d.addOwnerInfo(ownerinfo);
        }
    }

    /**
     * Read history info entries for the domain.
     *
     * @param c A connection to the database
     * @param d The domain being read. Its ID must be set.
     * @throws SQLException If database errors occur.
     */
    private void readHistoryInfo(Connection c, Domain d) throws SQLException {
        // Read history info
        PreparedStatement s = c.prepareStatement("SELECT historyinfo_id, stopreason, " + "objectcount, bytecount, "
                + "name, job_id, harvest_id, harvest_time " + "FROM historyinfo, configurations "
                + "WHERE configurations.domain_id = ?" + "  AND historyinfo.config_id = configurations.config_id");
        s.setLong(1, d.getID());
        ResultSet res = s.executeQuery();
        while (res.next()) {
            long hiID = res.getLong(1);
            int stopreasonNum = res.getInt(2);
            StopReason stopreason = StopReason.getStopReason(stopreasonNum);
            long objectCount = res.getLong(3);
            long byteCount = res.getLong(4);
            String configName = res.getString(5);
            Long jobId = res.getLong(6);
            if (res.wasNull()) {
                jobId = null;
            }
            long harvestId = res.getLong(7);
            Date harvestTime = new Date(res.getTimestamp(8).getTime());
            HarvestInfo hi;
            // XML DAOs didn't keep the job id in harvestinfo, so some
            // entries will be null.
            hi = new HarvestInfo(harvestId, jobId, d.getName(), configName, harvestTime, byteCount, objectCount,
                    stopreason);
            hi.setID(hiID);
            d.getHistory().addHarvestInfo(hi);
        }
    }

    /**
     * Read passwords for the domain.
     *
     * @param c A connection to the database
     * @param d The domain being read. Its ID must be set.
     * @throws SQLException If database errors occur.
     */
    private void readPasswords(Connection c, Domain d) throws SQLException {
        PreparedStatement s = c.prepareStatement("SELECT password_id, name, comments, url, "
                + "realm, username, password " + "FROM passwords WHERE domain_id = ?");
        s.setLong(1, d.getID());
        ResultSet res = s.executeQuery();
        while (res.next()) {
            final Password pwd = new Password(res.getString(2), res.getString(3), res.getString(4), res.getString(5),
                    res.getString(6), res.getString(7));
            pwd.setID(res.getLong(1));
            d.addPassword(pwd);
        }
    }

    /**
     * Read seedlists for the domain.
     *
     * @param c A connection to the database
     * @param d The domain being read. Its ID must be set.
     * @throws SQLException If database errors occur.
     */
    private void readSeedlists(Connection c, Domain d) throws SQLException {
        PreparedStatement s = c.prepareStatement("SELECT seedlist_id, name, comments, seeds"
                + " FROM seedlists WHERE domain_id = ?");
        s.setLong(1, d.getID());
        ResultSet res = s.executeQuery();
        while (res.next()) {
            final SeedList seedlist = getSeedListFromResultset(res);
            d.addSeedList(seedlist);
        }
        s.close();
        if (!d.getAllSeedLists().hasNext()) {
            final String msg = "Domain " + d + " loaded with no seedlists";
            log.warn(msg);
            throw new IOFailure(msg);
        }
    }

    /**
     * Make SeedList based on entry from seedlists (id, name, comments, seeds).
     *
     * @param res a Resultset
     * @return a SeedList based on ResultSet entry.
     * @throws SQLException if unable to get data from database
     */
    private SeedList getSeedListFromResultset(ResultSet res) throws SQLException {
        final long seedlistId = res.getLong(1);
        final String seedlistName = res.getString(2);
        String seedlistComments = res.getString(3);

        String seedlistContents = "";
        if (DBSpecifics.getInstance().supportsClob()) {
            Clob clob = res.getClob(4);
            seedlistContents = clob.getSubString(1, (int) clob.length());
        } else {
            seedlistContents = res.getString(4);
        }
        final SeedList seedlist = new SeedList(seedlistName, seedlistContents);
        seedlist.setComments(seedlistComments);
        seedlist.setID(seedlistId);
        return seedlist;
    }

    @Override
    public synchronized boolean exists(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        if (!DomainUtils.isValidDomainName(domainName)) {
            return false;
        }
        Connection c = HarvestDBConnection.get();
        try {
            return exists(c, domainName);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Return true if a domain with the given name exists.
     *
     * @param c an open connection to the harvestDatabase
     * @param domainName a name of a domain
     * @return true if a domain with the given name exists, otherwise false.
     */
    private synchronized boolean exists(Connection c, String domainName) {
        if (!DomainUtils.isValidDomainName(domainName)) {
            return false;
        }
        return 1 == DBUtils.selectIntValue(c, "SELECT COUNT(*) FROM domains WHERE name = ?", domainName);
    }

    @Override
    public synchronized int getCountDomains() {
        Connection c = HarvestDBConnection.get();
        try {
            return DBUtils.selectIntValue(c, "SELECT COUNT(*) FROM domains");
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public synchronized Iterator<Domain> getAllDomains() {
        Connection c = HarvestDBConnection.get();
        try {
            List<String> domainNames = DBUtils.selectStringList(c, "SELECT name FROM domains ORDER BY name");
            List<Domain> orderedDomains = new LinkedList<Domain>();
            for (String name : domainNames) {
                if (DomainUtils.isValidDomainName(name)) {
                    orderedDomains.add(read(c, name));
                }
            }
            return orderedDomains.iterator();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public Iterator<Domain> getAllDomainsInSnapshotHarvestOrder() {
        return getDomainsInSnapshotHarvestOrder(null);
    }
    
    @Override
    public Iterator<Domain> getDomainsInSnapshotHarvestOrder(Long hid) {
        Connection c = HarvestDBConnection.get();
        List<String> domainNames = null;
        List<String> domainNamesWithAttributes = null;
        try {
            if (hid==null) {
                log.info("Starting a select of all domains used for Snapshot harvesting");
                // Note: maxbytes are ordered with largest first for symmetry
                // with HarvestDefinition.CompareConfigDesc
                domainNames = DBUtils.selectStringList(c, "SELECT domains.name"
                        + " FROM domains, configurations, ordertemplates"
                        + " WHERE domains.defaultconfig=configurations.config_id" + " AND configurations.template_id"
                        + "=ordertemplates.template_id" + " ORDER BY" + " ordertemplates.name,"
                        + " configurations.maxbytes DESC," + " domains.name");
                log.info("Retrieved all {} domains used for Snapshot harvesting without searching for attributes for their default configs", domainNames.size());
                domainNamesWithAttributes = DBUtils.selectStringList(c, // Don't order this - it will be ordered later
                        "SELECT DISTINCT domains.name"
                        + " FROM domains, configurations, eav_attribute"
                        + " WHERE domains.defaultconfig=configurations.config_id"
                        + " AND configurations.config_id=eav_attribute.entity_id");
                log.info("Retrieved all {} domains used for Snapshot harvesting that has attributes for their default configs", domainNamesWithAttributes.size());
                domainNames = domainNames.stream().filter(DomainUtils::isValidDomainName).collect(Collectors.toList());
                //  Remove the content of domainNamesWithAttributes from domainNames
                domainNames.removeAll(domainNamesWithAttributes);
                log.info("Removed all {} domains with attributes from the total list, reducing total-list to {}", domainNamesWithAttributes.size(), domainNames.size());
                // Add the remainder of domainNames to domainNamesWithAttributes, so the domain configs with attributes will be handled first.
                domainNamesWithAttributes.addAll(domainNames);
                log.info("Remainder of total list merged with list of domains w/ attributes is size {}", domainNamesWithAttributes.size()); 
            } else {
                log.info("Starting a select of all domains harvested in previous snapshot harvest #{}", hid);
                domainNames = DBUtils.selectStringList(c, "SELECT DISTINCT domains.name"
                        + " FROM domains, configurations, ordertemplates, historyinfo"
                        + " WHERE domains.defaultconfig=configurations.config_id" + " AND configurations.template_id"
                        + "=ordertemplates.template_id" 
                        + " AND configurations.config_id=historyinfo.config_id "
                        + " AND historyinfo.harvest_id=" + hid);
                        // NOTE: the ordering has now been skipped to prevent duplicates
                        //  + " ORDER BY" + " ordertemplates.name," 
                        //  + " configurations.maxbytes DESC");
                        // "," + " domains.name");
                log.info("Retrieved all {} domains harvested in previous snapshot harvest #{}", domainNames.size(), hid);
                domainNamesWithAttributes = DBUtils.selectStringList(c, // Don't order this - it will be ordered later
                        "SELECT DISTINCT domains.name"
                        + " FROM domains, configurations, eav_attribute, historyinfo"
                        + " WHERE domains.defaultconfig=configurations.config_id"
                        + " AND configurations.config_id=eav_attribute.entity_id"
                        + " AND historyinfo.config_id=configurations.config_id"
                        + " AND historyinfo.harvest_id=" + hid
                        );
                log.info("Retrieved all {} domains harvested in previous snapshot harvest that has attributes for their default configs", domainNamesWithAttributes.size());
                domainNames = domainNames.stream().filter(DomainUtils::isValidDomainName).collect(Collectors.toList());
                //  Remove the content of domainNamesWithAttributes from domainNames
                domainNames.removeAll(domainNamesWithAttributes);
                log.info("Removed all {} domains with attributes from the total list, reducing total-list to {}", domainNamesWithAttributes.size(), domainNames.size());
                // Add the remainder of domainNames to domainNamesWithAttributes, so the domain configs with attributes will be handled first.
                domainNamesWithAttributes.addAll(domainNames);
                log.info("Remainder of total list merged with list of domains w/ attributes is size {}", domainNamesWithAttributes.size());   
            }

            return new FilterIterator<String, Domain>(domainNamesWithAttributes.iterator()) {
                public Domain filter(String s) {
                    return readKnown(s);
                }
            };
        } finally {
            HarvestDBConnection.release(c);
        }   
    }

    @Override
    public List<String> getDomains(String glob) {
        ArgumentNotValid.checkNotNullOrEmpty(glob, "glob");
        // SQL uses % and _ instead of * and ?
        String sqlGlob = DBUtils.makeSQLGlob(glob);
        Connection c = HarvestDBConnection.get();
        try {
            List<String> names = DBUtils.selectStringList(c, "SELECT name FROM domains WHERE name LIKE ? ORDER BY name", sqlGlob);
            return names.stream().filter(DomainUtils::isValidDomainName).collect(Collectors.toList());
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public boolean mayDelete(DomainConfiguration config) {
        ArgumentNotValid.checkNotNull(config, "config");
        String defaultConfigName = this.getDefaultDomainConfigurationName(config.getDomainName());
        Connection c = HarvestDBConnection.get();
        try {
            // Never delete default config and don't delete configs being used.
            return !config.getName().equals(defaultConfigName)
                    && !DBUtils.selectAny(c, "SELECT config_id" + " FROM harvest_configs WHERE config_id = ?",
                            config.getID());
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public String getDefaultDomainConfigurationName(String domainName) {
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(domainName), "Cannot read invalid domain name " + domainName);
        Connection c = HarvestDBConnection.get();
        try {
            return DBUtils.selectStringValue(c, "SELECT configurations.name " + "FROM domains, configurations "
                    + "WHERE domains.defaultconfig = configurations.config_id" + " AND domains.name = ?", domainName);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public synchronized SparseDomain readSparse(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(domainName), "Cannot read invalid domain name " + domainName);
        Connection c = HarvestDBConnection.get();
        try {
            List<String> domainConfigurationNames = DBUtils.selectStringList(c, "SELECT configurations.name "
                    + " FROM configurations, domains " + "WHERE domains.domain_id = configurations.domain_id "
                    + " AND domains.name = ? " + "ORDER by configurations.name" , domainName);
            if (domainConfigurationNames.size() == 0) {
                throw new UnknownID("No domain exists with name '" + domainName + "'");
            }
            return new SparseDomain(domainName, domainConfigurationNames);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public List<AliasInfo> getAliases(String domain) {
        ArgumentNotValid.checkNotNullOrEmpty(domain, "String domain");
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(domain), "Cannot read invalid domain name " + domain);
        List<AliasInfo> resultSet = new ArrayList<AliasInfo>();
        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        // return all <domain, alias, lastaliasupdate> tuples
        // where alias = domain
        if (!exists(c, domain)) {
            log.debug("domain named '{}' does not exist. Returning empty result set", domain);
            return resultSet;
        }
        try {
            s = c.prepareStatement("SELECT domains.name, " + "domains.lastaliasupdate "
                    + " FROM domains, domains as fatherDomains " + " WHERE domains.alias = fatherDomains.domain_id AND"
                    + "       fatherDomains.name = ?" + " ORDER BY domains.name");
            s.setString(1, domain);
            ResultSet res = s.executeQuery();
            while (res.next()) {
                AliasInfo ai = new AliasInfo(res.getString(1), domain, DBUtils.getDateMaybeNull(res, 2));
                resultSet.add(ai);
            }

            return resultSet;
        } catch (SQLException e) {
            throw new IOFailure("Failure getting alias-information" + "\n", e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public List<AliasInfo> getAllAliases() {
        List<AliasInfo> resultSet = new ArrayList<AliasInfo>();
        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        // return all <domain, alias, lastaliasupdate> tuples
        // where alias is not-null
        try {
            s = c.prepareStatement("SELECT domains.name, " + "(SELECT name FROM domains as aliasdomains"
                    + " WHERE aliasdomains.domain_id " + "= domains.alias), " + " domains.lastaliasupdate "
                    + " FROM domains " + " WHERE domains.alias IS NOT NULL" + " ORDER BY " + " lastaliasupdate ASC");
            ResultSet res = s.executeQuery();
            while (res.next()) {
                String domainName = res.getString(1);
                String aliasOf = res.getString(2);
                Date lastchanged = DBUtils.getDateMaybeNull(res, 3);
                AliasInfo ai = new AliasInfo(domainName, aliasOf, lastchanged);
                if (DomainUtils.isValidDomainName(domainName) && DomainUtils.isValidDomainName(aliasOf)) {
                    resultSet.add(ai);
                }
            }
            return resultSet;
        } catch (SQLException e) {
            throw new IOFailure("Failure getting alias-information" + "\n", e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Return all TLDs represented by the domains in the domains table. it was asked that a level X TLD belong appear in
     * TLD list where the level is <=X for example bidule.bnf.fr belong to .bnf.fr and to .fr it appear in the level 1
     * list of TLD and in the level 2 list
     *
     * @param level maximum level of TLD
     * @return a list of TLDs
     * @see DomainDAO#getTLDs(int)
     */
    @Override
    public List<TLDInfo> getTLDs(int level) {
        Map<String, TLDInfo> resultMap = new HashMap<String, TLDInfo>();
        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT name FROM domains");
            ResultSet res = s.executeQuery();
            while (res.next()) {
                String domain = res.getString(1);
                if (DomainUtils.isValidDomainName(domain)) {
                    // getting the TLD level of the domain
                    int domainTLDLevel = TLDInfo.getTLDLevel(domain);

                    // restraining to max level
                    if (domainTLDLevel > level) {
                        domainTLDLevel = level;
                    }

                    // looping from level 1 to level max of the domain
                    for (int currentLevel = 1; currentLevel <= domainTLDLevel; currentLevel++) {
                        // getting the tld of the domain by level
                        String tld = TLDInfo.getMultiLevelTLD(domain, currentLevel);
                        TLDInfo i = resultMap.get(tld);
                        if (i == null) {
                            i = new TLDInfo(tld);
                            resultMap.put(tld, i);
                        }
                        i.addSubdomain(domain);
                    }
                }
            }

            List<TLDInfo> resultSet = new ArrayList<TLDInfo>(resultMap.values());
            Collections.sort(resultSet);
            return resultSet;

        } catch (SQLException e) {
            throw new IOFailure("Failure getting TLD-information" + "\n", e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public HarvestInfo getDomainJobInfo(Job j, String domainName, String configName) {
        ArgumentNotValid.checkNotNull(j, "j");
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(domainName), "Cannot read invalid domain name " + domainName);
        ArgumentNotValid.checkNotNullOrEmpty(configName, "configName");
        HarvestInfo resultInfo = null;

        Connection connection = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            // Get domain_id for domainName
            long domainId = DBUtils.selectLongValue(connection, "SELECT domain_id FROM domains WHERE name=?",
                    domainName);

            s = connection.prepareStatement("SELECT stopreason, " + "objectcount, bytecount, "
                    + "harvest_time FROM historyinfo WHERE " + "job_id = ? AND " + "config_id = ? AND "
                    + "harvest_id = ?");
            s.setLong(1, j.getJobID());
            s.setLong(2, DBUtils.selectLongValue(connection, "SELECT config_id FROM configurations "
                    + "WHERE name = ? AND domain_id=?", configName, domainId));
            s.setLong(3, j.getOrigHarvestDefinitionID());
            ResultSet res = s.executeQuery();
            // If no result, the job may not have been run yet
            // return null HarvestInfo
            if (res.next()) {
                StopReason reason = StopReason.getStopReason(res.getInt(1));
                long objectCount = res.getLong(2);
                long byteCount = res.getLong(3);
                Date harvestTime = res.getDate(4);
                resultInfo = new HarvestInfo(j.getOrigHarvestDefinitionID(), j.getJobID(), domainName, configName,
                        harvestTime, byteCount, objectCount, reason);
            }

            return resultInfo;

        } catch (SQLException e) {
            throw new IOFailure("Failure getting DomainJobInfo" + "\n", e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public List<DomainHarvestInfo> listDomainHarvestInfo(String domainName, String orderBy, boolean asc) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(domainName), "Cannot read invalid domain name " + domainName);
        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        final ArrayList<DomainHarvestInfo> domainHarvestInfos = new ArrayList<DomainHarvestInfo>();
        final String ascOrDesc = asc ? "ASC" : "DESC";
        log.debug("Using ascOrDesc=" + ascOrDesc + " after receiving " + asc);
        try {
            // For historical reasons, not all historyinfo objects have the
            // information required to find the job that made them. Therefore,
            // we must left outer join them onto the jobs list to get the
            // start date and end date for those where they can be found.
            s = c.prepareStatement("SELECT jobs.job_id, hdname, hdid," + " harvest_num," + " configname, startdate,"
                    + " enddate, objectcount, bytecount, stopreason" + " FROM ( "
                    + "  SELECT harvestdefinitions.name AS hdname," + "         harvestdefinitions.harvest_id AS hdid,"
                    + "         configurations.name AS configname,"
                    + "         objectcount, bytecount, job_id, stopreason"
                    + "    FROM domains, configurations, historyinfo, " + "         harvestdefinitions"
                    + "   WHERE domains.name = ? " + "     AND domains.domain_id = configurations.domain_id"
                    + "     AND historyinfo.config_id = " + "configurations.config_id"
                    + "     AND historyinfo.harvest_id = " + "harvestdefinitions.harvest_id" + "  ) AS hist"
                    + " LEFT OUTER JOIN jobs" + "   ON hist.job_id = jobs.job_id ORDER BY " + orderBy + " " + ascOrDesc);
            s.setString(1, domainName);
            ResultSet res = s.executeQuery();
            while (res.next()) {
                final int jobID = res.getInt(1);
                final String harvestName = res.getString(2);
                final int harvestID = res.getInt(3);
                final int harvestNum = res.getInt(4);
                final String configName = res.getString(5);
                final Date startDate = DBUtils.getDateMaybeNull(res, 6);
                final Date endDate = DBUtils.getDateMaybeNull(res, 7);
                final long objectCount = res.getLong(8);
                final long byteCount = res.getLong(9);
                final StopReason reason = StopReason.getStopReason(res.getInt(10));
                domainHarvestInfos.add(new DomainHarvestInfo(domainName, jobID, harvestName, harvestID, harvestNum,
                        configName, startDate, endDate, byteCount, objectCount, reason));
            }
            return domainHarvestInfos;
        } catch (SQLException e) {
            String message = "SQL error getting domain harvest info for " + domainName + "\n";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Saves all extended Field values for a Domain in the Database.
     *
     * @param c Connection to Database
     * @param d Domain where loaded extended Field Values will be set
     * @throws SQLException If database errors occur.
     */
    private void saveExtendedFieldValues(Connection c, Domain d) throws SQLException {
        List<ExtendedFieldValue> list = d.getExtendedFieldValues();
        for (int i = 0; i < list.size(); i++) {
            ExtendedFieldValue efv = list.get(i);
            efv.setInstanceID(d.getID());

            ExtendedFieldValueDBDAO dao = (ExtendedFieldValueDBDAO) ExtendedFieldValueDAO.getInstance();
            if (efv.getExtendedFieldValueID() != null) {
                dao.update(c, efv, false);
            } else {
                dao.create(c, efv, false);
            }
        }
    }

    @Override
    public DomainConfiguration getDomainConfiguration(String domainName, String configName) {
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(domainName), "Cannot read invalid domain name " + domainName);
        DomainHistory history = getDomainHistory(domainName);
        List<String> crawlertraps = getCrawlertraps(domainName);

        Connection c = HarvestDBConnection.get();
        List<DomainConfiguration> foundConfigs = new ArrayList<DomainConfiguration>();
        PreparedStatement s = null;
        try {
            // Read the configurations now that passwords and seedlists exist
        	// TODO Seriously? Use a join.
            s = c.prepareStatement("SELECT config_id, " + "configurations.name, " + "comments, "
                    + "ordertemplates.name, " + "maxobjects, " + "maxrate, " + "maxbytes"
                    + " FROM configurations, ordertemplates " + "WHERE domain_id = (SELECT domain_id FROM domains "
                    + "  WHERE name=?)" + "  AND configurations.name = ?" + "  AND configurations.template_id = "
                    + "ordertemplates.template_id");
            s.setString(1, domainName);
            s.setString(2, configName);
            ResultSet res = s.executeQuery();
            while (res.next()) {
                long domainconfigId = res.getLong(1);
                String domainconfigName = res.getString(2);
                String domainConfigComments = res.getString(3);
                final String order = res.getString(4);
                long maxobjects = res.getLong(5);
                int maxrate = res.getInt(6);
                long maxbytes = res.getLong(7);
                PreparedStatement s1 = c.prepareStatement("SELECT seedlists.seedlist_id, seedlists.name,  "
                        + " seedlists.comments, seedlists.seeds " + "FROM seedlists, config_seedlists "
                        + "WHERE config_seedlists.config_id = ? " + "AND config_seedlists.seedlist_id = "
                        + "seedlists.seedlist_id");
                s1.setLong(1, domainconfigId);
                ResultSet seedlistResultset = s1.executeQuery();
                List<SeedList> seedlists = new ArrayList<SeedList>();
                while (seedlistResultset.next()) {
                    SeedList seedlist = getSeedListFromResultset(seedlistResultset);
                    seedlists.add(seedlist);
                }
                s1.close();
                if (seedlists.isEmpty()) {
                    String message = "Configuration " + domainconfigName + " of domain '" + domainName
                            + " has no seedlists";
                    log.warn(message);
                    throw new IOFailure(message);
                }

                PreparedStatement s2 = c.prepareStatement("SELECT passwords.password_id, "
                        + "passwords.name, passwords.comments, " + "passwords.url, passwords.realm, "
                        + "passwords.username, passwords.password " + "FROM passwords, config_passwords "
                        + "WHERE config_passwords.config_id = ? " + "AND config_passwords.password_id = "
                        + "passwords.password_id");
                s2.setLong(1, domainconfigId);
                ResultSet passwordResultset = s2.executeQuery();
                List<Password> passwords = new ArrayList<Password>();
                while (passwordResultset.next()) {
                    final Password pwd = new Password(passwordResultset.getString(2), passwordResultset.getString(3),
                            passwordResultset.getString(4), passwordResultset.getString(5),
                            passwordResultset.getString(6), passwordResultset.getString(7));
                    pwd.setID(passwordResultset.getLong(1));
                    passwords.add(pwd);
                }

                DomainConfiguration dc = new DomainConfiguration(domainconfigName, domainName, history, crawlertraps,
                        seedlists, passwords);
                dc.setOrderXmlName(order);
                dc.setMaxObjects(maxobjects);
                dc.setMaxRequestRate(maxrate);
                dc.setComments(domainConfigComments);
                dc.setMaxBytes(maxbytes);
                dc.setID(domainconfigId);
                foundConfigs.add(dc);
                s2.close();

                // EAV
                List<AttributeAndType> attributesAndTypes = EAV.getInstance().getAttributesAndTypes(EAV.DOMAIN_TREE_ID, (int)domainconfigId);
                dc.setAttributesAndTypes(attributesAndTypes);
            } // While
        } catch (SQLException e) {
            throw new IOFailure("Error while fetching DomainConfigration: ", e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
        return foundConfigs.get(0);
    }

    /**
     * Retrieve the crawlertraps for a specific domain. TODO should this method be public?
     *
     * @param domainName the name of a domain.
     * @return the crawlertraps for given domain.
     */
    private List<String> getCrawlertraps(String domainName) {
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(domainName), "Cannot read invalid domain name " + domainName);
        Connection c = HarvestDBConnection.get();
        String traps = null;
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT crawlertraps FROM domains WHERE name = ?");
            s.setString(1, domainName);
            ResultSet crawlertrapsResultset = s.executeQuery();
            if (crawlertrapsResultset.next()) {
                traps = crawlertrapsResultset.getString(1);
            } else {
                throw new IOFailure("Unable to find crawlertraps for domain '" + domainName + "'. "
                        + "The domain doesn't seem to exist.");
            }
        } catch (SQLException e) {
            throw new IOFailure("Error while fetching crawlertraps  for domain '" + domainName + "': ", e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
        return Arrays.asList(traps.split("\n"));
    }

    @Override
    public Iterator<HarvestInfo> getHarvestInfoBasedOnPreviousHarvestDefinition(
            final HarvestDefinition previousHarvestDefinition) {
        ArgumentNotValid.checkNotNull(previousHarvestDefinition, "previousHarvestDefinition");
        // For each domainConfig, get harvest infos if there is any for the
        // previous harvest definition
        log.debug("We start the Iterator<HarvestInfo> process with getting an iterator of DomainConfigs in the previous HD#{}", previousHarvestDefinition.getOid());
        Iterator<DomainConfiguration> previousDomainConfigs = previousHarvestDefinition.getDomainConfigurations();
        log.debug("Now finished getting an iterator of DomainConfigs in the previous HD#{}. We can now return the FilterIterator<DomainConfiguration, HarvestInfo>", previousHarvestDefinition.getOid());
        return new FilterIterator<DomainConfiguration, HarvestInfo>(previousDomainConfigs) {
            /**
             * @see FilterIterator#filter(Object)
             */
            protected HarvestInfo filter(DomainConfiguration o) {
                DomainConfiguration config = o;
                DomainHistory domainHistory = getDomainHistory(config.getDomainName());
                HarvestInfo hi = domainHistory.getSpecifiedHarvestInfo(previousHarvestDefinition.getOid(),
                        config.getName());
                return hi;
            }
        }; // Here ends the above return-statement
    }
    
    /**
     * Retrieve HarvestInfo for a given harvestdefinition and domain combination.
     * @param harvestDefinition a given harvestdefinition
     * @param domain a given domain
     * @return null, if no HarvestInfo found for the given harvestdefinition and domain combination, otherwise it returns the first matching HarvestInfo found and gives a warning if more than one match exist.
     */
    @Override
    public HarvestInfo getHarvestInfoForDomainInHarvest(final HarvestDefinition harvestDefinition, final Domain domain) {
        PreparedStatement s = null;
        Connection c = HarvestDBConnection.get();
        try {
            s = c.prepareStatement("SELECT h.stopreason, h.objectcount, h.bytecount, c.name, h.job_id, h.harvest_time FROM historyinfo as h, configurations as c WHERE "
                + " c.config_id=h.config_id AND c.domain_id=? AND h.harvest_id=?");
            s.setLong(1, domain.getID());
            s.setLong(2, harvestDefinition.getOid());
            ResultSet res = s.executeQuery();
            List<HarvestInfo> infoFoundForDomain = new ArrayList<HarvestInfo>();
            while (res.next()) {
                int stopreasonNum = res.getInt(1);
                StopReason stopreason = StopReason.getStopReason(stopreasonNum);
                long objectCount = res.getLong(2);
                long byteCount = res.getLong(3);
                String configName = res.getString(4);
                Long jobId = res.getLong(5);
                if (res.wasNull()) {
                    jobId = null;
                }
                long harvestId = harvestDefinition.getOid();
                Date harvestTime = new Date(res.getTimestamp(6).getTime());

                HarvestInfo hi = new HarvestInfo(harvestId, jobId, domain.getName(), configName, harvestTime, byteCount, objectCount, stopreason);
                infoFoundForDomain.add(hi);
            }
            if (infoFoundForDomain.isEmpty()) {
                return null;
            } else if (infoFoundForDomain.size() == 1) {
                return infoFoundForDomain.get(0);
            } else {
                HarvestInfo selected = infoFoundForDomain.get(0);
                Long latest = selected.getDate().getTime();
                for (int i=1; i < infoFoundForDomain.size(); i++) {
                    if (infoFoundForDomain.get(i).getDate().getTime() > latest) {
                        latest = infoFoundForDomain.get(i).getDate().getTime();
                        selected = infoFoundForDomain.get(i);
                    }
                }
                log.warn("Found {} harvestInfo entries for domain '{}' and harvestdefinition '{}'. Selecting the latest entry: {}", infoFoundForDomain.size(), domain.getName(), 
                        harvestDefinition.getName(), selected);
                return selected;
            }
        } catch (SQLException e) {
            throw new IOFailure("Error while fetching HarvestInfo for domain '" + domain.getName() + "' in harvest '" + harvestDefinition.getName() + "':", e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
    }
    
    

    @Override
    public DomainHistory getDomainHistory(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "String domainName");
        ArgumentNotValid.checkTrue(DomainUtils.isValidDomainName(domainName), "Cannot read invalid domain name " + domainName);
        Connection c = HarvestDBConnection.get();
        DomainHistory history = new DomainHistory();
        // Read history info
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT historyinfo_id, stopreason, " + "objectcount, bytecount, "
                    + "name, job_id, harvest_id, harvest_time " + "FROM historyinfo, configurations "
                    + "WHERE configurations.domain_id = " + "(SELECT domain_id FROM domains WHERE name=?)"
                    + "  AND historyinfo.config_id " + " = configurations.config_id");
            s.setString(1, domainName);
            ResultSet res = s.executeQuery();
            while (res.next()) {
                long hiID = res.getLong(1);
                int stopreasonNum = res.getInt(2);
                StopReason stopreason = StopReason.getStopReason(stopreasonNum);
                long objectCount = res.getLong(3);
                long byteCount = res.getLong(4);
                String configName = res.getString(5);
                Long jobId = res.getLong(6);
                if (res.wasNull()) {
                    jobId = null;
                }
                long harvestId = res.getLong(7);
                Date harvestTime = new Date(res.getTimestamp(8).getTime());
                HarvestInfo hi;

                hi = new HarvestInfo(harvestId, jobId, domainName, configName, harvestTime, byteCount, objectCount,
                        stopreason);
                hi.setID(hiID);
                history.addHarvestInfo(hi);
            }
        } catch (SQLException e) {
            throw new IOFailure("Error while fetching DomainHistory for domain '" + domainName + "': ", e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }

        return history;
    }

    @Override
    public List<String> getDomains(String glob, String searchField) {
        ArgumentNotValid.checkNotNullOrEmpty(glob, "glob");
        ArgumentNotValid.checkNotNullOrEmpty(searchField, "searchField");
        // SQL uses % and _ instead of * and ?
        String sqlGlob = DBUtils.makeSQLGlob(glob);

        Connection c = HarvestDBConnection.get();
        try {
            return DBUtils.selectStringList(c, "SELECT name FROM domains WHERE " + searchField.toLowerCase()
                    + " LIKE ?", sqlGlob).stream().filter(DomainUtils::isValidDomainName).collect(Collectors.toList());
        } finally {
            HarvestDBConnection.release(c);
        }
    }

	@Override
	public void renameAndUpdateConfig(Domain domain, DomainConfiguration domainConf,
			String configOldName) {
		Connection connection = HarvestDBConnection.get();
		Long configId = DBUtils.selectLongValue(connection,
                "SELECT config_id FROM configurations WHERE domain_id = ? and name = ?", domain.getID(), configOldName);
		
        try {
			PreparedStatement s = connection.prepareStatement("UPDATE configurations SET name = ?, comments = ?, "
			        + "template_id = ( SELECT template_id FROM ordertemplates " + "WHERE name = ? ), " + "maxobjects = ?, "
			        + "maxrate = ?, " + "maxbytes = ? " + "WHERE config_id = ? AND domain_id = ?");
					s.setString(1, domainConf.getName());
	                DBUtils.setComments(s, 2, domainConf, Constants.MAX_COMMENT_SIZE);
	                s.setString(3, domainConf.getOrderXmlName());
	                s.setLong(4, domainConf.getMaxObjects());
	                s.setInt(5, domainConf.getMaxRequestRate());
	                s.setLong(6, domainConf.getMaxBytes());
	                s.setLong(7, configId);
	                s.setLong(8, domain.getID());
	                s.executeUpdate();
	                s.clearParameters();
	            updateConfigPasswordsEntries(connection, domain, domainConf);
	            updateConfigSeedlistsEntries(connection, domain, domainConf);
	        s.close();
		} catch (SQLException e) {
			throw new IOFailure("Error while renaming configuration '" + configOldName + "' to: " + domainConf.getName(), e);
		}  finally {
            HarvestDBConnection.release(connection);
        }
	}

    @Override
    public List<String> getAllDomainNames() {
        Connection c = HarvestDBConnection.get();
        try {
            return DBUtils.selectStringList(c, "SELECT name FROM domains").stream().filter(DomainUtils::isValidDomainName).collect(
                    Collectors.toList());
        } finally {
            HarvestDBConnection.release(c);
        }   
    }
}
