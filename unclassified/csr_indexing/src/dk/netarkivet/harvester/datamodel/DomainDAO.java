/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

import java.util.Iterator;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FilterIterator;

/**
 * Persistent storage for Domain objects.
 * Configuration information and seeds are stored as well.
 *
 */
public abstract class DomainDAO implements Iterable<Domain> {

    /**
     * The Singleton DomainDAO.
     */
    private static DomainDAO instance;

    /**
     * protected constructor for singleton class.
     */
    protected DomainDAO() {
    }

    /**
     * Get the singleton DomainDAO instance.
     *
     * @return the singleton DomainDAO
     */
    public synchronized static DomainDAO getInstance() {
        if (instance == null) {
            instance = new DomainDBDAO();
        }

        return instance;
    }

    /**
     * Create a domain in persistent storage.
     * @param domain a given Domain object
     */
    public abstract void create(Domain domain);

    /**
     * Read a domain from the persistent storage.
     *
     * @param domainName the name of the domain to retrieve
     * @return the retrieved Domain
     */
    public abstract Domain read(String domainName);

    /**
     * Check existence of a domain with the given domainName.
     *
     * @param domainName A given domain name.
     * @return true if the domain exists, false otherwise.
     * @throws ArgumentNotValid if domainName is null or empty.
     */
    public abstract boolean exists(String domainName);

    /** Check where a domain is being used.  This method is this way to
     * check if a domain can be deleted.
     *
     * @param domainName A domain to check for usages of.
     * @return A string explaining (in human-readable format) where
     * the domain has been used.  If it hasn't been used in any place
     * that would prevent it from being deleted, returns null.
     */
    public abstract String describeUsages(String domainName);

    /**
     * Delete domain from persistent storage.
     *
     * @param domainName name of the domain to delete
     * @throws ArgumentNotValid if null or empty domainName supplied
     * @throws UnknownID        if domainName does not match an existing domain
     * @throws PermissionDenied if the domain can not be deleted
     */
    public abstract void delete(String domainName);

    /**
     * Update information about existing domain information.
     *
     * @param domain the domain to update
     * @throws ArgumentNotValid if domain is null
     * @throws UnknownID        if the Domain domain has not been added previously to persistent storage.
     */
    public abstract void update(Domain domain);

    /**
     * Get the total number of domains available.
     *
     * @return the total number of registered domanis.
     */
    public abstract int getCountDomains();


    /**
     * Gets list of all domains.
     *
     * @return List of all added domains
     */
    public abstract Iterator<Domain> getAllDomains();

    /** Gets an iterator of all domains.
     * Implements the Iterable interface.
     *
     * @return Iterator of all presently known domains.
     */
    public Iterator<Domain> iterator() {
        return getAllDomains();
    }

    /**
     * Gets list of all domains in the order expected by snapshot harvest
     * job generation, that is order by
     * template name, then byte limit (descending), then domain name.
     *
     * @return List of all added domains
     */
    public abstract Iterator<Domain> getAllDomainsInSnapshotHarvestOrder();

    /**
     * Reset the singleton.  Only for use in tests!
     */
    static void resetSingleton() {
        instance = null;
    }

    /**
     * Find all info about results of a harvest definition.
     *
     * @param previousHarvestDefinition A harvest definition that has already
     *                                  been run.
     * @return An array of information for all domainconfigurations
     *         which were harvested by the given harvest definition.
     */
    public Iterator<HarvestInfo> getHarvestInfoBasedOnPreviousHarvestDefinition(
            final HarvestDefinition previousHarvestDefinition) {
        ArgumentNotValid.checkNotNull(previousHarvestDefinition,
                                      "previousHarvestDefinition");
        // For each domainConfig, get harvest infos if there is any for the
        // previous harvest definition
        return new FilterIterator<DomainConfiguration,HarvestInfo>(
                previousHarvestDefinition.getDomainConfigurations()) {
            /**
             * @see FilterIterator#filter(Object)
             */
            protected HarvestInfo filter(DomainConfiguration o){
                DomainConfiguration config = o;
                DomainHistory domainHistory
                        = config.getDomain().getHistory();
                HarvestInfo hi = domainHistory.getSpecifiedHarvestInfo(
                        previousHarvestDefinition.getOid(),
                        config.getName());
                return hi;
            }
        }; // Here ends the above return-statement
    }

    /** Close down any connections used by the DAO. */
    public abstract void close();

    /** Use a glob-like matcher to find a subset of domains.
     *
     * In this simple matcher, * stands for any number of arbitrary characters,
     * and ? stands for one arbitrary character.  Including these, the
     * given string must match the entire domain name.
     *
     * @param glob A domain name with * and ? wildcards
     * @return List of domain names matching the glob, sorted by name.
     */
    public abstract List<String> getDomains(String glob);

    /**
     * Find the number of domains matching a given glob.
     * @param glob A domain name with * and ? wildcards
     * @return the number of domains matching the glob
     * @see DomainDAO#getDomains(String)
     */
    public abstract int getCountDomains(String glob);


    /** Get a list of info about harvests performed on a given domain.
     *
     * Note that harvest info from before the DB DAOs are unreliable, as
     * harvests cannot be told apart and no dates are available.
     *
     * @param domainName Domain to get info for.
     * @return List of DomainHarvestInfo objects with information on that domain.
     *
     */
    public abstract List<DomainHarvestInfo> getDomainHarvestInfo(String domainName);

    /** Return whether the given configuration can be deleted.
     * This should be a fairly lightweight method, but is not likely to be
     * instantaneous.
     * Note that to increase speed, this method may rely on underlying systems
     * to enforce transitive invariants.  This means that if this method says
     * a configuration can be deleted, the dao may still reject a delete
     * request.  If this method returns false, deletion will however
     * definitely not be allowed.
     * @param config the given configuration
     * @return true if the he given configuration can be deleted, false otherwise
     */
    public abstract boolean mayDelete(DomainConfiguration config);

    /** Return whether the given seedlist can be deleted.
     * This should be a fairly lightweight method, but is not likely to be
     * instantaneous.
     * Note that to increase speed, this method may rely on underlying systems
     * to enforce transitive invariants.  This means that if this method says
     * a seedlist can be deleted, the dao may still reject a delete
     * request.  If this method returns false, deletion will however
     * definitely not be allowed.
     * @param seedlist the given seedlist
     * @return true, if the given seedlist can be deleted, otherwise false.
     */
    public abstract boolean mayDelete(SeedList seedlist);

    /** Return whether the given password can be deleted.
     * This should be a fairly lightweight method, but is not likely to be
     * instantaneous.
     * Note that to increase speed, this method may rely on underlying systems
     * to enforce transitive invariants.  This means that if this method says
     * a password can be deleted, the dao may still reject a delete
     * request.  If this method returns false, deletion will however
     * definitely not be allowed.
     * @param password the given password
     * @return true, if the given password can be deleted, otherwise false.
     */
    public abstract boolean mayDelete(Password password);

    /** Return whether the given domain can be deleted.
     * This should be a fairly lightweight method, but is not likely to be
     * instantaneous.
     * Note that to increase speed, this method may rely on underlying systems
     * to enforce transitive invariants.  This means that if this method says
     * a domain can be deleted, the dao may still reject a delete
     * request.  If this method returns false, deletion will however
     * definitely not be allowed.
     * @param domainName the given domain
     * @return true, if  the given domain can be deleted, otherwise false.
     */
    public abstract boolean mayDelete(Domain domainName);

    /**
     * Read a Domain from Database, and return the domain information
     * as a SparseDomain object.
     * We only read information relevant for the GUI listing.
     * @param domainName a given domain
     * @return a SparseDomain.
     * @throws ArgumentNotValid if domainName is null or empty.
     * @throws UnknownID if domain does not exist
     */
    public abstract SparseDomain readSparse(String domainName);

    /**
     * Return a list of AliasInfo objects.
     * If the given domain is not-null, it should return AliasInfo objects
     * where AliasInfo.aliasOf == domain
     *
     * @param domain a given domain
     * @return a list of AliasInfo objects.
     * @throws UnknownID If the given domain does not exist. (!DomainDAO.exists(domain))
     * @throws ArgumentNotValid if domainName is null
     */
    public abstract List<AliasInfo> getAliases(String domain);

    /**
     * Get a list of all current alias-relations.  The list should be sorted
     * by increasing last-update.  This means any expired aliases will be at
     * the start of the list, while un-expired aliases will be at the end.
     *
     * @return a list of all current alias-relations.
     */
    public abstract List<AliasInfo> getAllAliases();

    /**
     * Get a list of all TLDs present in the domains table.
     * IP-numbers registered are counted together.
     *
     * @return a list of all TLDs present in the domains table, sorted
     * alphabetically.
     */
    public abstract List<TLDInfo> getTLDs();
    
    /**
     * Get the HarvestInfo object for a certain job and DomainConfiguration
     * defined by domainName and configName.
     * @param domainName the name of a given domain
     * @param configName the name of a given configuration
     * @param job the job
     * @return The HarvestInfo object for a certain job and DomainConfiguration 
     *  or null, if job has not yet been started.
     */
    public abstract HarvestInfo getDomainJobInfo(Job job, String domainName, String configName);
}
