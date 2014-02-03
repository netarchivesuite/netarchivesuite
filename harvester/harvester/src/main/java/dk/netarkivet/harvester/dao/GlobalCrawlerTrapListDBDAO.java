/* File:        $Id: GlobalCrawlerTrapListDBDAO.java 2225 2012-01-06 14:31:20Z svc $
 * Revision:    $Revision: 2225 $
 * Author:      $Author: svc $
 * Date:        $Date: 2012-01-06 15:31:20 +0100 (Fri, 06 Jan 2012) $
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.harvester.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList;

/**
 * A singleton giving access to global crawler traps.
 *
 */
public class GlobalCrawlerTrapListDBDAO extends GlobalCrawlerTrapListDAO {
	
	private final class GlobalCrawlerTrapListExtractor 
		implements ResultSetExtractor<GlobalCrawlerTrapList> {
		
		private final long id;

		public GlobalCrawlerTrapListExtractor(long id) {
			this.id = id;
		}

		@Override
		public GlobalCrawlerTrapList extractData(ResultSet rs)
				throws SQLException, DataAccessException {
			if (!rs.next()){
                throw new UnknownID("No such GlobalCrawlerTrapList: '" + id + "'");
            }
			return new GlobalCrawlerTrapList(
					id, 
					new ArrayList<String>(),
					rs.getString("name"), 
					rs.getString("description"), 
					rs.getBoolean("isActive"));
		}
		
	}

    /**
     * protected constructor of this class. Checks if any migration
     * are needed before operation starts.
     */
    protected GlobalCrawlerTrapListDBDAO() {
    	super();
    }

	@Override
	protected Collection<HarvesterDatabaseTables> getRequiredTables() {
		ArrayList<HarvesterDatabaseTables> tables = new ArrayList<HarvesterDatabaseTables>();
		tables.add(HarvesterDatabaseTables.GLOBALCRAWLERTRAPEXPRESSIONS);
		tables.add(HarvesterDatabaseTables.GLOBALCRAWLERTRAPLISTS);
		return tables;
	}

    /**
     * Returns a list of either all active or all inactive trap lists.
     * @param isActive whether to return active or inactive lists.
     * @return  a list if global crawler trap lists.
     */
    private List<GlobalCrawlerTrapList> getAllByActivity(final boolean isActive) {
    	List<Long> listIds = queryLongList(
    			"SELECT global_crawler_trap_list_id"
    			+ " FROM global_crawler_trap_lists WHERE isActive=:isActive",
    			new ParameterMap("isActive", isActive));
    	List<GlobalCrawlerTrapList> trapLists = new ArrayList<GlobalCrawlerTrapList>();
    	for (long id : listIds) {
    		trapLists.add(read(id));
    	}
    	return trapLists;
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
    	return queryStringList(
    			"SELECT DISTINCT trap_expression"
                + " FROM global_crawler_trap_lists, global_crawler_trap_expressions"
                + " WHERE global_crawler_trap_list_id=crawler_trap_list_id"
                + " AND isActive=:isActive",
                new ParameterMap("isActive", true));
    }

    @Override
    public long create(final GlobalCrawlerTrapList trapList) {
        ArgumentNotValid.checkNotNull(trapList, "trapList");
        
        // Check for existence of a trapList in the database with the same name
        // and throw argumentNotValid if not
        if (exists(trapList.getName())) {
            throw new ArgumentNotValid("Crawlertrap with name '"
                    + trapList.getName() + "'already exists in database");
        }
        
        return (Long) executeTransaction("doCreate", GlobalCrawlerTrapList.class, trapList);
    }
    
    @SuppressWarnings("unused")
	private long doCreate(final GlobalCrawlerTrapList trapList) {    	
    	return executeUpdate(
    			 "INSERT INTO global_crawler_trap_lists"
                     + " (name, description, isActive)"
                     + " VALUES (:name,:desc,:isActive)", 
                     getParameterMap(trapList), 
                     "global_crawler_trap_list_id");
    }

    @Override
    public void delete(final long id) {
    	executeTransaction("doDelete", Long.class, id);
    }
    
    @SuppressWarnings("unused")
	private void doDelete(final Long id) {    	
    	executeUpdate(
    			"DELETE from global_crawler_trap_lists WHERE global_crawler_trap_list_id=:id", 
    			new ParameterMap("id", id));
    	
    	deleteTrapExpressions(id);
    }

    /**
     * Update a trap list. In order to update the trap expressions for this
     * list, we first delete all the existing trap expressions for the list
     * then add all those in the updated version.
     * @param trapList the trap list to update
     */
    @Override
    public void update(final GlobalCrawlerTrapList trapList) {
        ArgumentNotValid.checkNotNull(trapList, "trapList");
    }
    
    @SuppressWarnings("unused")
	private void doUpdate(final GlobalCrawlerTrapList trapList) {
    	executeUpdate(
    			"UPDATE global_crawler_trap_lists"
    			+ " SET name=:name, description=:desc, isActive=:isActive"
    		    + " WHERE global_crawler_trap_list_id=:id", 
    		    getParameterMap(trapList));
    	
    	long trapListId = trapList.getId();
    	
    	//Delete all the trap expressions.
    	deleteTrapExpressions(trapListId);
    	
    	// Add the new trap expressions one by one.
        for (String expr: trapList.getTraps()) {
        	executeUpdate(
        			"INSERT INTO global_crawler_trap_expressions"
		            + " (crawler_trap_list_id, trap_expression)"
		            + " VALUES (:id,:expr)",
		            new ParameterMap(
		            		"id", trapList,
		            		"expr", expr));
        }
    }

    @Override
    public GlobalCrawlerTrapList read(final long id) {
    	
    	GlobalCrawlerTrapList trapList = query(
    			"SELECT name, description, isActive"
    			+ " FROM global_crawler_trap_lists"
    			+ " WHERE global_crawler_trap_list_id=:id",
    			new ParameterMap("id", id),
    			new GlobalCrawlerTrapListExtractor(id));
    	
    	// Now get expressions in
    	HashSet<String> expressions = new HashSet<String>();
    	expressions.addAll(queryStringList(
    			"SELECT trap_expression FROM global_crawler_trap_expressions"
    			+ " WHERE crawler_trap_list_id=:id",
    			new ParameterMap("id", id)));
    	trapList.setTraps(expressions);
    	
    	return trapList;
    }

    /**
     * Does crawlertrap with this name already exist.
     * @param name The name for a crawlertrap
     * @return true, if a crawlertrap with the given name already exists
     * in the database; otherwise false
     */
    @Override
    public boolean exists(final String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");        
        return 1 == queryIntValue(
        		"SELECT COUNT(*) FROM global_crawler_trap_lists WHERE name=:name",
        		new ParameterMap("name", name));
    }
    
    private ParameterMap getParameterMap(final GlobalCrawlerTrapList trapList) {
    	return new ParameterMap(
    			"id", trapList.getId(),
    			"name", trapList.getName(),
    			"desc", trapList.getDescription(),
    			"isActive", trapList.isActive());
    }
    
    private final void deleteTrapExpressions(final long trapListId) {
    	executeUpdate(
    			"DELETE FROM global_crawler_trap_expressions WHERE crawler_trap_list_id =:id", 
    			new ParameterMap("id", trapListId));
    }

}
