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

import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * A Data Access Object for managing persistent collections of global crawler traps.
 */
public abstract class GlobalCrawlerTrapListDAO implements DAO {

    /** The database singleton model. */
    private static GlobalCrawlerTrapListDAO instance;

    /**
     * Factory method to return the singleton instance of this class.
     *
     * @return the singleton instance of this class.
     */
    public static synchronized GlobalCrawlerTrapListDAO getInstance() {
        if (instance == null) {
            instance = new GlobalCrawlerTrapListDBDAO();
        }
        return instance;
    }

    /**
     * Resets the singleton instance of this class. Mostly for testing.
     */
    public static void reset() {
        instance = null;
    }

    /**
     * Get all active crawler traps.
     *
     * @return a list of all active crawler traps.
     */
    public abstract List<GlobalCrawlerTrapList> getAllActive();

    /**
     * Get all inactive crawler traps.
     *
     * @return a list of all inactive crawler traps.
     */
    public abstract List<GlobalCrawlerTrapList> getAllInActive();

    /**
     * Get a merged list (without duplicates) of all currently-active crawler trap expressions.
     *
     * @return a list os all active crawler trap expressions.
     */
    public abstract List<String> getAllActiveTrapExpressions();

    // CRUD methods for this DAO.

    /**
     * This method creates the object in the database and has the side effect of setting the trapLists id field to the
     * auto-generated id in the database.
     *
     * @param trapList The list to persist
     * @return the id of the created list
     * @throws ArgumentNotValid if the trapList is null.
     */
    public abstract int create(GlobalCrawlerTrapList trapList) throws ArgumentNotValid;

    /**
     * Deletes a crawler trap list from the database.
     *
     * @param id the id of the list to be deleted
     * @throws UnknownID if the argument doesn not correspond to a known trap list.
     */
    public abstract void delete(int id) throws UnknownID;

    /**
     * Updates a given global crawler trap list.
     *
     * @param trapList the trap list to update
     * @throws UnknownID if the id of the trapList argument does not correspond to an existing trap list in the
     * database.
     */
    public abstract void update(GlobalCrawlerTrapList trapList) throws UnknownID;

    /**
     * Get a traplist from the database.
     *
     * @param id the id of the traplist to be read.
     * @return the trap list.
     * @throws UnknownID if the id does not correspond to a known traplist in the database.
     */
    public abstract GlobalCrawlerTrapList read(int id) throws UnknownID;

    /**
     * Does crawlertrap with this name already exist.
     *
     * @param name The name for a crawlertrap
     * @return true, if a crawlertrap with the given name already exists in the database; otherwise false
     */
    public abstract boolean exists(String name);

    /**
     * Reads a list of all active global crawler trap expressions from the database and adds them to the crawl template.
     */
    public abstract void addGlobalCrawlerTraps(HeritrixTemplate orderXmlDoc);
}
