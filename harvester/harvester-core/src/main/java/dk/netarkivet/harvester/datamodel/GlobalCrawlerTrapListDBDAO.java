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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;

/**
 * A singleton giving access to global crawler traps.
 */
public class GlobalCrawlerTrapListDBDAO extends GlobalCrawlerTrapListDAO {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(GlobalCrawlerTrapListDBDAO.class);

    /**
     * protected constructor of this class. Checks if any migration are needed before operation starts.
     */
    protected GlobalCrawlerTrapListDBDAO() {
        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.GLOBALCRAWLERTRAPEXPRESSIONS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.GLOBALCRAWLERTRAPLISTS);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Statement to select all trap lists which are either active or inactive.
     */
    private static final String SELECT_BY_ACTIVITY = "SELECT global_crawler_trap_list_id FROM global_crawler_trap_lists WHERE isActive = ?";

    /**
     * Returns a list of either all active or all inactive trap lists.
     *
     * @param isActive whether to return active or inactive lists.
     * @return a list if global crawler trap lists.
     */
    private List<GlobalCrawlerTrapList> getAllByActivity(boolean isActive) {
        List<GlobalCrawlerTrapList> result = new ArrayList<GlobalCrawlerTrapList>();
        Connection conn = HarvestDBConnection.get();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(SELECT_BY_ACTIVITY);
            stmt.setBoolean(1, isActive);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(read(rs.getInt(1)));
            }
            return result;
        } catch (SQLException e) {
            String message = "Error reading trap list\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new UnknownID(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stmt);
            HarvestDBConnection.release(conn);
        }
    }

    @Override
    public List<GlobalCrawlerTrapList> getAllActive() {
        return getAllByActivity(true);
    }

    @Override
    public List<GlobalCrawlerTrapList> getAllInActive() {
        return getAllByActivity(false);
    }

    @Override
    public List<String> getAllActiveTrapExpressions() {
        Connection conn = HarvestDBConnection.get();
        List<String> result = new ArrayList<String>();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("SELECT DISTINCT trap_expression FROM global_crawler_trap_lists, "
                    + "global_crawler_trap_expressions " + "WHERE global_crawler_trap_list_id = "
                    + "crawler_trap_list_id " + "AND isActive = ?");
            stmt.setBoolean(1, true);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        } catch (SQLException e) {
            String message = "Error retrieving expressions.\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stmt);
            HarvestDBConnection.release(conn);
        }
    }

    /**
     * Statement to insert a new trap list.
     */
    private static final String INSERT_TRAPLIST_STMT = "INSERT INTO global_crawler_trap_lists (name, description, isActive)"
            + "VALUES (?,?,?)";

    /**
     * Statement to insert a new trap expression in a given list.
     */
    private static final String INSERT_TRAP_EXPR_STMT = "INSERT INTO global_crawler_trap_expressions (crawler_trap_list_id, trap_expression) VALUES (?,?) ";

    @Override
    public int create(GlobalCrawlerTrapList trapList) {
        ArgumentNotValid.checkNotNull(trapList, "trapList");
        // Check for existence of a trapList in the database with the same name
        // and throw argumentNotValid if not
        if (exists(trapList.getName())) {
            throw new ArgumentNotValid("Crawlertrap with name '" + trapList.getName() + "'already exists in database");
        }
        int trapId;
        Connection conn = HarvestDBConnection.get();
        PreparedStatement stmt = null;
        try {
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(INSERT_TRAPLIST_STMT, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, trapList.getName());
            stmt.setString(2, trapList.getDescription());
            stmt.setBoolean(3, trapList.isActive());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            rs.next();
            trapId = rs.getInt(1);
            trapList.setId(trapId);
            log.debug("Inserting {} traps into database for list {} (trapId={})", trapList.getTraps().size(), trapList.getName(), trapId);
            for (String expr : trapList.getTraps()) {
                stmt = conn.prepareStatement(INSERT_TRAP_EXPR_STMT);
                stmt.setInt(1, trapId);
                stmt.setString(2, expr);
                stmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            String message = "SQL error creating global crawler trap list \n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stmt);
            DBUtils.rollbackIfNeeded(conn, "create trap list", trapList);
            HarvestDBConnection.release(conn);
        }
        return trapId;
    }

    /**
     * Statement to delete a trap list.
     */
    private static final String DELETE_TRAPLIST_STMT = "DELETE from global_crawler_trap_lists WHERE global_crawler_trap_list_id = ?";

    /**
     * Statement to delete all expressions in a given trap list.
     */
    private static final String DELETE_EXPR_STMT = "DELETE FROM global_crawler_trap_expressions WHERE crawler_trap_list_id = ?";

    @Override
    public void delete(int id) {
        Connection conn = HarvestDBConnection.get();
        PreparedStatement stmt = null;
        try {
            conn.setAutoCommit(false);
            // First delete the list.
            stmt = conn.prepareStatement(DELETE_TRAPLIST_STMT);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            stmt.close();
            // Then delete all its expressions.
            stmt = conn.prepareStatement(DELETE_EXPR_STMT);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            String message = "Error deleting trap list: '" + id + "'\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new UnknownID(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stmt);
            DBUtils.rollbackIfNeeded(conn, "delete trap list", id);
            HarvestDBConnection.release(conn);
        }
    }

    /**
     * Statement to update the elementary properties of a trap list.
     */
    private static final String LIST_UPDATE_STMT = "UPDATE global_crawler_trap_lists SET name = ?, description = ?, isActive = ? WHERE global_crawler_trap_list_id = ?";

    /**
     * Update a trap list. In order to update the trap expressions for this list, we first delete all the existing trap
     * expressions for the list then add all those in the updated version.
     *
     * @param trapList the trap list to update
     */
    @Override
    public void update(GlobalCrawlerTrapList trapList) {
        ArgumentNotValid.checkNotNull(trapList, "trapList");
        Connection conn = HarvestDBConnection.get();
        PreparedStatement stmt = null;
        try {
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(LIST_UPDATE_STMT);
            stmt.setString(1, trapList.getName());
            stmt.setString(2, trapList.getDescription());
            stmt.setBoolean(3, trapList.isActive());
            stmt.setInt(4, trapList.getId());
            stmt.executeUpdate();
            stmt.close();

            // Delete all the trap expressions.
            stmt = conn.prepareStatement(DELETE_EXPR_STMT);
            stmt.setInt(1, trapList.getId());
            stmt.executeUpdate();
            stmt.close();
            // Add the new trap expressions one by one.
            for (String expr : trapList.getTraps()) {
                stmt = conn.prepareStatement(INSERT_TRAP_EXPR_STMT);
                stmt.setInt(1, trapList.getId());
                stmt.setString(2, expr);
                stmt.executeUpdate();
                stmt.close();
            }
            conn.commit();
        } catch (SQLException e) {
            String message = "Error updating trap list :'" + trapList.getId() + "'\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new UnknownID(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stmt);
            DBUtils.rollbackIfNeeded(conn, "update trap list", trapList);
            HarvestDBConnection.release(conn);
        }
    }

    /**
     * Statement to read the elementary properties of a trap list.
     */
    private static final String SELECT_TRAPLIST_STMT = "SELECT name, description, isActive FROM global_crawler_trap_lists WHERE global_crawler_trap_list_id = ?";

    /**
     * Statement to read the trap expressions for a trap list.
     */
    private static final String SELECT_TRAP_EXPRESSIONS_STMT = "SELECT trap_expression from global_crawler_trap_expressions WHERE crawler_trap_list_id = ?";

    @Override
    public GlobalCrawlerTrapList read(int id) {
        Connection conn = HarvestDBConnection.get();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(SELECT_TRAPLIST_STMT);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new UnknownID("No such GlobalCrawlerTrapList: '" + id + "'");
            }
            String name = rs.getString("name");
            String description = rs.getString("description");
            boolean isActive = rs.getBoolean("isActive");
            stmt.close();
            stmt = conn.prepareStatement(SELECT_TRAP_EXPRESSIONS_STMT);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            List<String> exprs = new ArrayList<String>();
            while (rs.next()) {
                exprs.add(rs.getString("trap_expression"));
            }
            return new GlobalCrawlerTrapList(id, exprs, name, description, isActive);
        } catch (SQLException e) {
            String message = "Error retrieving trap list for id '" + id + "'\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stmt);
            HarvestDBConnection.release(conn);
        }
    }

    /**
     * Statement to read the elementary properties of a trap list.
     */
    private static final String EXISTS_TRAPLIST_STMT = "SELECT * FROM global_crawler_trap_lists WHERE name = ?";

    /**
     * Does crawlertrap with this name already exist.
     *
     * @param name The name for a crawlertrap
     * @return true, if a crawlertrap with the given name already exists in the database; otherwise false
     */
    @Override
    public boolean exists(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        Connection conn = HarvestDBConnection.get();
        PreparedStatement stmt = null;
        boolean exists = false;
        try {
            stmt = conn.prepareStatement(EXISTS_TRAPLIST_STMT);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                exists = true;
            }

            return exists;
        } catch (SQLException e) {
            String message = "Error checking for existence of trap list " + "with name '" + name + "'\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stmt);
            HarvestDBConnection.release(conn);
        }
    }

    /**
     * Reads a list of all active global crawler trap expressions from the database and adds them to the crawl template
     * for this job.
     * @param orderXmlDoc The template to add crawlertraps to.
     */
    public void addGlobalCrawlerTraps(HeritrixTemplate orderXmlDoc) {
    
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDAO.getInstance();
        orderXmlDoc.insertCrawlerTraps(Constants.GLOBAL_CRAWLER_TRAPS_ELEMENT_NAME,
        		               dao.getAllActiveTrapExpressions());
    } 
}
