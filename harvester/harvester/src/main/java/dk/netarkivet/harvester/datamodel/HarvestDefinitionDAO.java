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


import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * A Data Access Object for harvest definitions.
 * This object is a singleton to ensure thread-safety. It
 * handles the transformation from harvest definitions to persistent storage.
 *
 */
public abstract class HarvestDefinitionDAO implements Iterable<HarvestDefinition> {

    /** The one and only instance of the HarvestDefinitionDAO class to ensure
      * thread-safety.
      */
    private static HarvestDefinitionDAO instance;
    /** The log. */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Default constructor.
     * Does not do anything, however.
     */
    protected HarvestDefinitionDAO() {
    }

    /**
     * Creates the singleton.
     * @return the HarvestDefinitionDAO singleton.
     * @throws IOFailure if unable to create the singleton.
     */
    public static synchronized HarvestDefinitionDAO getInstance() {
        if (instance == null) {
            instance = new HarvestDefinitionDBDAO();
        }
        return instance;
    }

    /**
     * Create a harvest definition in persistent storage.
     *
     * @param harvestDefinition A new harvest definition to write out.
     * @return The harvestId for the just created harvest definition.
     */
    public abstract Long create(HarvestDefinition harvestDefinition);

    /**
     * Generates the next id of a harvest definition.
     * TODO: Maybe this method is not needed in this superclass as an abstract method.
     * It is really only a helper method for the create() method.
     * @return id The next available id.
     */
    protected abstract Long generateNextID();

    /**
     * Read the stored harvest definition for the given ID.
     *
     * @param harvestDefinitionID An ID number for a harvest definition
     * @return A harvest definition that has been read from persistent storage.
     * @throws UnknownID if no file with that ID exists
     * @throws IOFailure if the File does not exist, does not have the
     *                   correct ID, or
     *                   otherwise fails to load correctly.
     */
    public abstract HarvestDefinition read(Long harvestDefinitionID)
            throws UnknownID, IOFailure;

    /** Return a string describing the current uses of a harvest definition,
     * or null if the harvest definition is safe to delete (i.e. has never been
     * run).
     *
     * @param oid a given identifier designating a harvest definition.
     * @return the above mentioned usage-string.
     */
    public abstract String describeUsages(Long oid);

    /**
     * Delete a harvest definition from persistent storage.
     *
     * @param oid The ID of a harvest definition to delete.
     */
    public abstract void delete(Long oid);

    /**
     * Update an existing harvest definition with new info
     * in persistent storage.
     *
     * @param harvestDefinition An updated harvest definition
     *  object to be persisted.
     */
    public abstract void update(HarvestDefinition harvestDefinition);

    /**
     * Check, if there exists a HarvestDefinition identified by a given OID.
     * @param oid a given OID
     * @return true, if such a harvestdefinition exists.
     */
    public abstract boolean exists(Long oid);

    /**
     * Get a list of all existing harvest definitions.
     *
     * @return An iterator that give the existing harvest definitions in turn
     */
    public abstract Iterator<HarvestDefinition> getAllHarvestDefinitions();

    /** Get an iterator of all harvest definitions.
     * Implements the Iterable interface.
     *
     * @return Iterator of all harvest definitions, Selective and Full both.
     */
    public Iterator<HarvestDefinition> iterator() {
        return getAllHarvestDefinitions();
    }

    /**
     * Gets default configurations for all domains.
     *
     * @return Iterator containing the default DomainConfiguration
     * for all domains
     */
    public abstract Iterator<DomainConfiguration> getSnapShotConfigurations();



    /**
     * Edit the harvestdefinition.
     * @param harvestDefinitionID The ID for the harvestdefintion to be updated
     * @param harvestDefinition the HarvestDefinition object to used to update
     *        harvestdefinition with the above ID
     * @return the ID for the updated harvestdefinition
     * (this should be equal to harvestDefinitionID?)
     *
     */
    public Long editHarvestDefinition(Long harvestDefinitionID,
                                      HarvestDefinition harvestDefinition) {
        ArgumentNotValid.checkNotNull(harvestDefinitionID,
                "harvestDefinitionID");
        ArgumentNotValid.checkNotNull(harvestDefinition, "harvestDefinition");

        HarvestDefinition oldhd = read(harvestDefinitionID);
        harvestDefinition.setOid(oldhd.getOid());
        harvestDefinition.setSubmissionDate(oldhd.getSubmissionDate());
        update(harvestDefinition);
        return harvestDefinition.getOid();
    }

    /** Get the IDs of the harvest definitions that are ready to run.
     *
     * @param now
     * @return IDs of the harvest definitions that are currently ready to
     * be scheduled.  Some of these might already be in the process of being
     * scheduled.
     */
    public abstract Iterable<Long> getReadyHarvestDefinitions(Date now);

    /**
     * Get the harvest definition that has the given name, or null,
     * if no harvestdefinition exist with this name.
     * @param name The name of a harvest definition.
     * @return The HarvestDefinition object with that name, or null if none
     * has that name.
     */
    public abstract HarvestDefinition getHarvestDefinition(String name);

    /** Returns an iterator of all snapshot harvest definitions.
     *
     * @return An iterator (possibly empty) of FullHarvests
     */
    public abstract Iterator<FullHarvest> getAllFullHarvestDefinitions();

    /** Returns an iterator of all non-snapshot harvest definitions.
     *
     * @return An iterator (possibly empty) of PartialHarvests
     */
    public abstract Iterator<PartialHarvest> getAllPartialHarvestDefinitions();

    /** Returns a list with information on the runs of a particular harvest.
     *
     * @param harvestID ID of an existing harvest
     * @return List of objects with selected information.
     */
    public abstract List<HarvestRunInfo> getHarvestRunInfo(long harvestID);

    /**
     * Reset the DAO instance. Only for use in tests.
     */
    static void reset() {
        instance = null;
    }

    /** Return whether the given harvestdefinition can be deleted.
     * This should be a fairly lightweight method, but is not likely to be
     * instantaneous.
     * Note that to increase speed, this method may rely on underlying systems
     * to enforce transitive invariants.  This means that if this method says
     * a harvestdefinition can be deleted, the dao may still reject a delete
     * request.  If this method returns false, deletion will however
     * definitely not be allowed.
     * @param hd A given harvestdefinition
     * @return true, if this HarvestDefinition can be deleted without problems.
     */
    public abstract boolean mayDelete(HarvestDefinition hd);

    /**
     * Get all domain,configuration pairs for a harvest definition in sparse
     * version for GUI purposes.
     *
     * @param harvestDefinitionID The ID of the harvest definition.
     * @return Domain,configuration pairs for that HD. Returns an empty iterable
     *         for unknown harvest definitions.
     * @throws ArgumentNotValid on null argument.
     */
    public abstract Iterable<SparseDomainConfiguration>
            getSparseDomainConfigurations(Long harvestDefinitionID);

    /**
     * Get a sparse version of a partial harvest for GUI purposes.
     *
     * @param harvestName Name of harvest definition.
     * @return Sparse version of partial harvest or null for none.
     * @throws ArgumentNotValid on null or empty name.
     */
    public abstract SparsePartialHarvest getSparsePartialHarvest(
            String harvestName);

    /**
     * Get all sparse versions of partial harvests for GUI purposes.
     *
     * @return An iterable (possibly empty) of SparsePartialHarvests
     */
    public abstract Iterable<SparsePartialHarvest>
            getAllSparsePartialHarvestDefinitions();

    /**
     * Get a sparse version of a full harvest for GUI purposes.
     *
     * @param harvestName Name of harvest definition.
     * @return Sparse version of full harvest or null for none.
     * @throws ArgumentNotValid on null or empty name.
     */
    public abstract SparseFullHarvest getSparseFullHarvest(String harvestName);

    /**
     * Get all sparse versions of full harvests for GUI purposes.
     *
     * @return An iterable (possibly empty) of SparseFullHarvests
     */
    public abstract Iterable<SparseFullHarvest>
            getAllSparseFullHarvestDefinitions();

    /** Get the name of a harvest given its ID.
     *
     * @param harvestDefinitionID The ID of a harvest
     * @return The name of the given harvest.
     * @throws ArgumentNotValid on null argument
     * @throws UnknownID if no harvest has the given ID.
     * @throws IOFailure        on any other error talking to the database
     */
    public abstract String getHarvestName(Long harvestDefinitionID);

    /** Get whether a given harvest is a snapshot or selective harvest.
     *
     * @param harvestDefinitionID ID of a harvest
     * @return True if the given harvest is a snapshot harvest, false
     * otherwise.
     * @throws ArgumentNotValid on null argument
     * @throws UnknownID if no harvest has the given ID.
     * @throws IOFailure        on any other error talking to the database
     */
    public abstract boolean isSnapshot(Long harvestDefinitionID);
    
    /** Get a sorted list of all domainnames of a HarvestDefintion
    *
    * @param harvestName of HarvestDefintion
    * @return List of all domains of the HarvestDefinition.
    * @throws ArgumentNotValid on null argument
    * @throws IOFailure        on any other error talking to the database
    */
    public abstract List<String> getListOfDomainsOfHarvestDefinition(String harvestName);
    
    /** Get a sorted list of all seeds of a Domain in a HarvestDefinition.
    *
    * @param harvestName of HarvestDefintion
    * @param domainName of Domain
    * @return List of all seeds of the Domain in the HarvestDefinition.
    * @throws ArgumentNotValid on null argument
    * @throws IOFailure        on any other error talking to the database
    */
    public abstract List<String> getListOfSeedsOfDomainOfHarvestDefinition(String harvestName, String domainName);
}
