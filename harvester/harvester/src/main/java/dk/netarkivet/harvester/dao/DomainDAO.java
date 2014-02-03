/* File:    $Id: DomainDAO.java 2716 2013-07-10 17:15:34Z mss $
 * Version: $Revision: 2716 $
 * Date:    $Date: 2013-07-10 19:15:34 +0200 (Wed, 10 Jul 2013) $
 * Author:  $Author: mss $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.dao;

import java.util.Iterator;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.AliasInfo;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainHarvestInfo;
import dk.netarkivet.harvester.datamodel.DomainHistory;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.SparseDomain;
import dk.netarkivet.harvester.datamodel.TLDInfo;

/**
 * Persistent storage for Domain objects.
 * Configuration information and seeds are stored as well.
 *
 */
public abstract class DomainDAO extends HarvestDatabaseDAO implements Iterable<Domain> {

    /**
     * The Singleton DomainDAO.
     */
    private static DomainDAO instance;
    
    /**
     * protected constructor for singleton class.
     */
    protected DomainDAO() {
    	super();
    }

    /**
     * Get the singleton DomainDAO instance.
     *
     * @return the singleton DomainDAO
     */
    public static synchronized DomainDAO getInstance() {
        if (instance == null) {
            instance = new DomainDBDAO();
        }

        return instance;
    }

    /**
     * Create a domain in persistent storage.
     * @param domain a given {@link Domain} object.
     */
    public abstract void create(final Domain domain);

    /**
     * Create a list of domains in persistent storage.
     * @param domains a list of {@link Domain} objects.
     */
    public synchronized void create(final List<Domain> domains) {
    	for (Domain d : domains) {
            create(d);
        }
    }

    /**
     * Read a domain from the persistent storage.
     *
     * @param domainName the name of the domain to retrieve
     * @return the retrieved Domain
     */
    public abstract Domain read(final String domainName);

    /**
     * Read a domain from the persistent storage known to exist.
     *
     * @param domainName the name of the domain to retrieve
     * @return the retrieved Domain
     */
    public abstract Domain readKnown(final String domainName);
    
    /**
     * Check existence of a domain with the given domainName.
     *
     * @param domainName A given domain name.
     * @return true if the domain exists, false otherwise.
     * @throws ArgumentNotValid if domainName is null or empty.
     */
    public abstract boolean exists(final String domainName);

    /**
     * Update information about existing domain information.
     *
     * @param domain the domain to update
     * @throws ArgumentNotValid if domain is null
     * @throws UnknownID        if the Domain domain has not been added
     * previously to persistent storage.
     */
    public abstract void update(final Domain domain);

    /**
     * Get the total number of domains available.
     *
     * @return the total number of registered domains.
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
     * TODO remove this, no test methods in business classes!
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
    public abstract Iterator<HarvestInfo> getHarvestInfoBasedOnPreviousHarvestDefinition(
            final HarvestDefinition previousHarvestDefinition);

    /** Use a glob-like matcher to find a subset of domains.
    *
    * In this simple matcher, * stands for any number of arbitrary characters,
    * and ? stands for one arbitrary character.  Including these, the
    * given string must match the entire domain name.
    *
    * @param glob A domain name with * and ? wildcards
    * @return List of domain names matching the glob, sorted by name.
    */
   public abstract List<String> getDomains(final String glob);

    /** Return whether the given configuration can be deleted.
     * This should be a fairly lightweight method, but is not likely to be
     * instantaneous.
     * Note that to increase speed, this method may rely on underlying systems
     * to enforce transitive invariants.  This means that if this method says
     * a configuration can be deleted, the dao may still reject a delete
     * request.  If this method returns false, deletion will however
     * definitely not be allowed.
     * @param config the given configuration
     * @return true if the he given configuration can be deleted,
     * false otherwise
     */
    public abstract boolean mayDelete(final DomainConfiguration config);

    /**
     * Read a Domain from Database, and return the domain information
     * as a SparseDomain object.
     * We only read information relevant for the GUI listing.
     * @param domainName a given domain
     * @return a SparseDomain.
     * @throws ArgumentNotValid if domainName is null or empty.
     * @throws UnknownID if domain does not exist
     */
    public abstract SparseDomain readSparse(final String domainName);

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
    public abstract List<AliasInfo> getAliases(final String domain);

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
     * @param level maximum level of TLD
     * @return a list of all TLDs present in the domains table, sorted
     * alphabetically.
     */
    public abstract List<TLDInfo> getTLDs(final int level);

    /**
     * Get the HarvestInfo object for a certain job and DomainConfiguration
     * defined by domainName and configName.
     * @param domainName the name of a given domain
     * @param configName the name of a given configuration
     * @param job the job
     * @return The HarvestInfo object for a certain job and DomainConfiguration
     *  or null, if job has not yet been started.
     */
    public abstract HarvestInfo getDomainJobInfo(
    		final Job job, 
    		final String domainName, 
    		final String configName);


    /** Get a list of info about harvests performed on a given domain.
     *
     * Note that harvest info from before the DB DAOs are unreliable, as
     * harvests cannot be told apart and no dates are available.
     *
     *
     * @param domainName Domain to get info for.
     * @param orderBy The column attribute to order by.
     * @param asc true if the results should be ordered according to the natural order,
     *            false if they are to be sorted in reverse.
     * @return List of DomainHarvestInfo objects with information on that
     * domain.
     *
     */
    public abstract List<DomainHarvestInfo> listDomainHarvestInfo(
    		final String domainName, 
    		final String orderBy, 
    		final boolean asc);

    /**
     * Get the DomainConfiguration given a specific domainName and a
     * configurationName.
     * @param domainName The name of a domain
     * @param configName The name of a configuration for this domain
     * @return the DomainConfiguration, if the specified configuration exists;
     * otherwise throws UnknownID
     */
    public abstract DomainConfiguration getDomainConfiguration(
    		final String domainName, 
    		final String configName);

    /**
     * Get the domainHistory for a specific domain.
     *
     * @param domainName
     *            A name of a specific domain.
     * @return the domainHistory for a specific domain.
     */
    public abstract DomainHistory getDomainHistory(final String domainName);

    /** Use a glob-like matcher to find a subset of domains.
    *
    * In this simple matcher, * stands for any number of arbitrary characters,
    * and ? stands for one arbitrary character.  Including these, the
    * given string must match the entire domain name.
    *
    * @param glob A domain name with * and ? wildcards
    * @param searchField The field in the Domain table to search
    * @return List of domain names matching the glob, sorted by name.
    */
   public abstract List<String> getDomains(
		   final String glob, 
		   final String searchField);

    /**
     * Read the used configurations name + seedslists for the domain. Note that even
     * though a list of <code>DomainConfiguration</code> object are returned,
     * only the name + seeds lists are set.
     *
     * A used configuration is the default configuration + configurations used
     * in a active harvest definition.
     *
     * @param domainID The domain to find the configurations for.
     * @return The list of ID for the used configurations.
     */
    public abstract List<Long> findUsedConfigurations(final Long domainID);
}
