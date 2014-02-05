/* File:        $Id: DomainDBDAO.java 2716 2013-07-10 17:15:34Z mss $
 * Revision:    $Revision: 2716 $
 * Author:      $Author: mss $
 * Date:        $Date: 2013-07-10 19:15:34 +0200 (Wed, 10 Jul 2013) $
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

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FilterIterator;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.dao.spec.DBSpecifics;
import dk.netarkivet.harvester.datamodel.AliasInfo;
import dk.netarkivet.harvester.datamodel.Constants;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainHarvestInfo;
import dk.netarkivet.harvester.datamodel.DomainHistory;
import dk.netarkivet.harvester.datamodel.DomainOwnerInfo;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.Password;
import dk.netarkivet.harvester.datamodel.SeedList;
import dk.netarkivet.harvester.datamodel.SparseDomain;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.datamodel.TLDInfo;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValue;

/**
 * A database-based implementation of the DomainDAO.
 *
 * The statements to create the tables are located in:
 * <ul>
 * <li><em>Derby:</em> scripts/sql/createfullhddb.sql</li>
 * <li><em>MySQL:</em> scripts/sql/createfullhddb.mysql</li>
 * <li><em>PostgreSQL:</em> scripts/postgresql/netarchivesuite_init.sql</li>
 * </ul>
 *
 */
public class DomainDBDAO extends DomainDAO {

	/** The log. */
	private static final Log log = LogFactory.getLog(DomainDBDAO.class);

	/**
	 * Builds a bare bones {@link Domain} instance from the results set
	 * @see DomainDBDAO#readKnown(String)
	 */
	private final class DomainResultSetExtractor implements ResultSetExtractor<Domain> {

		private final String domainName;

		private String defaultConfigName;

		public DomainResultSetExtractor(String domainName) {
			this.domainName = domainName;
		}

		@Override
		public Domain extractData(ResultSet rs)
				throws SQLException, DataAccessException {
			if (!rs.next()) {
				final String message = "Error reading existing domain '"
						+ domainName + "' due to database inconsistency. " 
						+ "Note that this should never happen. Please ask your "
						+ "database admin to check " 
						+ "your 'domains' and 'configurations' tables for any inconsistencies.";
				log.warn(message);
				throw new IOFailure(message);
			}

			Domain d = new Domain(domainName);
			d.setID(rs.getInt("domain_id"));
			d.setComments(rs.getString("comments"));
			// don't throw exception if illegal regexps are found.
			d.setCrawlerTraps(
					Arrays.asList(rs.getString("crawlertraps").split("\n")), 
					false);
			d.setEdition(rs.getLong("edition"));

			this.defaultConfigName = rs.getString("name");

			String alias = rs.getString(6);
			if (alias != null) {
				d.setAliasInfo(new AliasInfo(
						domainName, 
						alias, 
						getDateKeepNull(rs, "lastaliasupdate")));
			}

			return d;
		}

		/**
		 * @return the defaultConfigName
		 */
		public final String getDefaultConfigName() {
			return defaultConfigName;
		}

	}

	/**
	 * Builds {@link DomainConfiguration}, and uses nested queries to populate 
	 * seed lists and passwords.
	 * @see DomainDBDAO#getDomainConfiguration(String, String)
	 */
	private class GetDomainConfigurationMapper implements RowMapper<DomainConfiguration> {

		private final String domainName;		
		private final DomainHistory history;
		private final List<String> crawlertraps;

		public GetDomainConfigurationMapper(		
				final String domainName,
				final DomainHistory history, 
				final List<String> crawlertraps) {
			super();
			this.domainName = domainName;
			this.history = history;
			this.crawlertraps = crawlertraps;
		}

		@Override
		public DomainConfiguration mapRow(ResultSet res, int pos)
				throws SQLException {

			long configId = res.getLong("config_id");

			List<SeedList> seedLists = query(
					"SELECT seedlists.seedlist_id, seedlists.name"
							+ ", seedlists.comments, seedlists.seeds"
							+ " FROM seedlists, config_seedlists"
							+ " WHERE config_seedlists.config_id=:configId"
							+ " AND config_seedlists.seedlist_id=seedlists.seedlist_id",
							new ParameterMap("configId", configId),
							new RowMapper<SeedList>() {
								@Override
								public SeedList mapRow(ResultSet rs, int pos) throws SQLException {								
									String seedlistContents = "";
									if (DBSpecifics.getInstance().supportsClob()) {
										Clob clob = rs.getClob("seeds");
										seedlistContents = clob.getSubString(1, (int) clob.length());
									} else {
										seedlistContents = rs.getString("seeds");
									}

									SeedList seedlist = new SeedList(
											rs.getString("name"), 
											seedlistContents);
									seedlist.setComments(rs.getString("comments"));
									seedlist.setID(rs.getLong("seedlist_id"));
									return seedlist;
								}
							});

			List<Password> passwords = query(
					"SELECT passwords.password_id, passwords.name, passwords.comments"
							+ ", passwords.url, passwords.realm, passwords.username"
							+ ", passwords.password"
							+ " FROM passwords, config_passwords"
							+ " WHERE config_passwords.config_id=:configId"
							+ " AND config_passwords.password_id=passwords.password_id",
							new ParameterMap("configId", configId),
							new RowMapper<Password>() {
								@Override
								public Password mapRow(ResultSet rs, int pos) throws SQLException {
									return new Password(
											rs.getString("name"), 
											rs.getString("comments"), 
											rs.getString("url"), 
											rs.getString("realm"), 
											rs.getString("username"), 
											rs.getString("password"));							
								}
							});

			DomainConfiguration dc = new DomainConfiguration(
					res.getString("configName"), 
					domainName, 
					history, 
					crawlertraps,
					seedLists, 
					passwords);

			dc.setID(configId);
			dc.setComments(res.getString("comments"));
			dc.setOrderXmlName(res.getString("orderXmlName"));
			dc.setMaxObjects(res.getLong("maxobjects"));
			dc.setMaxRequestRate(res.getInt("maxrate"));
			dc.setMaxBytes(res.getLong("maxbytes"));

			return dc;
		}
	}

	/**
	 * Builds {@link DomainConfiguration}, and uses nested queries to populate 
	 * seed lists and passwords.
	 * @see DomainDBDAO#readConfigurations(Domain)
	 */
	private class ReadDomainConfigurationMapper implements RowMapper<DomainConfiguration> {

		private final Domain d;

		public ReadDomainConfigurationMapper(Domain d) {
			super();
			this.d = d;
		}

		@Override
		public DomainConfiguration mapRow(ResultSet res, int pos)
				throws SQLException {

			long configId = res.getLong("config_id");
			String configName = res.getString("configName");

			List<SeedList> seedlists = new ArrayList<SeedList>();
			List<String> slNames = queryStringList(
					"SELECT seedlists.name"
							+ " FROM seedlists, config_seedlists"
							+ " WHERE config_seedlists.config_id=:configId"
							+ " AND config_seedlists.seedlist_id=seedlists.seedlist_id",
							new ParameterMap("configId", configId));
			for (String name : slNames) {
				seedlists.add(d.getSeedList(name));
			}
			if (seedlists.isEmpty()) {
				String message = "Configuration " + configName
						+ " of " + d + " has no seedlists";
				log.warn(message);
				throw new IOFailure(message);
			}

			List<Password> passwords = new ArrayList<Password>();
			List<String> pwdNames = queryStringList(
					"SELECT passwords.name"
							+ " FROM passwords, config_passwords"	                   
							+ " WHERE config_passwords.config_id=:configId"
							+ " AND config_passwords.password_id=passwords.password_id",
							new ParameterMap("configId", configId));
			for (String name : pwdNames) {
				passwords.add(d.getPassword(name));
			}

			DomainConfiguration dc = new DomainConfiguration(
					configName, 
					d,
					seedlists, 
					passwords);

			dc.setID(configId);
			dc.setComments(res.getString("comments"));
			dc.setOrderXmlName(res.getString("orderXmlName"));
			dc.setMaxObjects(res.getLong("maxobjects"));
			dc.setMaxRequestRate(res.getInt("maxrate"));
			dc.setMaxBytes(res.getLong("maxbytes"));

			return dc;
		}
	}

	/**
	 * Builds a {@link HarvestInfo} instance from a result set row.
	 * @see DomainDBDAO#readHistoryInfo(Domain)
	 */
	private final class HarvestInfoMapper implements RowMapper<HarvestInfo> {

		private final String domainName;

		public HarvestInfoMapper(final String domainName) {
			this.domainName = domainName;
		}

		@Override
		public HarvestInfo mapRow(ResultSet rs, int pos)
				throws SQLException {
			HarvestInfo hi = new HarvestInfo(
					rs.getLong("harvest_id"), 
					domainName, 
					rs.getString("name"), 
					new Date(rs.getTimestamp("harvest_time").getTime()), 
					rs.getLong("bytecount"), 
					rs.getLong("objectcount"), 
					StopReason.getStopReason(rs.getInt("stopreason")));
			hi.setID(rs.getLong("historyinfo_id"));
			return hi;
		}
	}

	/**
	 * Builds an {@link AliasInfo} instance from a result set row.
	 * @see DomainDBDAO#getAliases(String)
	 */
	private final class AliasInfoMapper implements RowMapper<AliasInfo> {

		private final String domainName;

		public AliasInfoMapper(String domainName) {
			this.domainName = domainName;
		}

		@Override
		public AliasInfo mapRow(ResultSet rs, int pos)
				throws SQLException {
			return new AliasInfo(
					rs.getString("domains.name"), 
					domainName, 
					getDateKeepNull(rs, "lastaliasupdate"));
		}
	}

	/**
	 * Specific extractor that maps a {@link TLDInfo} instance to the domain name.
	 * Expects a result set containing domain names only.
	 * @see DomainDBDAO#getTLDs(int)
	 */
	private final class TLDInfoExtractor 
	implements ResultSetExtractor<Map<String, TLDInfo>> {

		private final int level;

		public TLDInfoExtractor(int level) {
			this.level = level;
		}

		@Override
		public Map<String, TLDInfo> extractData(ResultSet rs)
				throws SQLException, DataAccessException {
			Map<String, TLDInfo> map = new HashMap<String, TLDInfo>();
			while (rs.next()) {
				String domain = rs.getString(1);
				//getting the TLD level of the domain
				int domainTLDLevel = TLDInfo.getTLDLevel(domain);

				//restraining to max level
				if(domainTLDLevel > level) { domainTLDLevel = level; }

				//looping from level 1 to level max of the domain
				for(int currentLevel = 1; currentLevel <= domainTLDLevel;
						currentLevel++){
					//getting the tld of the domain by level
					String tld = TLDInfo.getMultiLevelTLD(domain, currentLevel);
					TLDInfo i = map.get(tld);
					if (i == null) {
						i = new TLDInfo(tld);
						map.put(tld, i);
					}
					i.addSubdomain(domain);
				}
			}
			return map;
		}

	}

	/**
	 * Specific extractor that builds a {@link HarvestInfo} instance 
	 * related to a given {@link Job}
	 * @see DomainDBDAO#getDomainJobInfo(Job, String, String)
	 */
	private final class HarvestInfoExtractor 
	implements ResultSetExtractor<HarvestInfo> {

		private final Job j;

		private final String domainName;

		private final String configName;

		public HarvestInfoExtractor(
				final Job j, 
				final String domainName, 
				final String configName) {
			super();
			this.j = j;
			this.domainName = domainName;
			this.configName = configName;
		}

		@Override
		public HarvestInfo extractData(ResultSet rs) 
				throws SQLException, DataAccessException {
			if (!rs.next()) {
				// If no result, the job may not have been run yet
				// return null HarvestInfo
				return null;
			}
			return new HarvestInfo(j.getOrigHarvestDefinitionID(),
					j.getJobID(), 
					domainName, 
					configName, 
					rs.getDate("harvest_time"),
					rs.getLong("bytecount"), 
					rs.getLong("objectcount"), 
					StopReason.getStopReason(rs.getInt("stopreason")));
		}

	}

	/**
	 * Specific mapper that builds a list of {@link DomainHarvestInfo} instances.
	 * @see DomainDBDAO#listDomainHarvestInfo(String, String, boolean)
	 *
	 */
	private final class DomainHarvestInfoMapper implements RowMapper<DomainHarvestInfo> {

		private final String domainName;

		public DomainHarvestInfoMapper(String domainName) {
			this.domainName = domainName;
		}

		@Override
		public DomainHarvestInfo mapRow(ResultSet rs, int pos)
				throws SQLException {
			return new DomainHarvestInfo(
					domainName, 
					rs.getInt("jobs.job_id"),
					rs.getString("hdname"), 
					rs.getInt("hdid"), 
					rs.getInt("harvest_num"), 
					rs.getString("configname"),
					getDateKeepNull(rs, "startdate"), 
					getDateKeepNull(rs, "enddate"),
					rs.getLong("objectcount"), 
					rs.getLong("bytecount"), 
					StopReason.getStopReason(rs.getInt("stopreason")));
		}
	}

	/**
	 * Creates a database-based implementation of the DomainDAO. Will check that
	 * all schemas have correct versions, and update the ones that haven't.
	 *
	 * @throws IOFailure
	 *             on trouble updating tables to new versions, or on tables with
	 *             wrong versions that we don't know how to change to expected
	 *             version.
	 */
	protected DomainDBDAO() {
		super();
	}

	@Override
	public synchronized void create(Domain d) {
		ArgumentNotValid.checkNotNull(d, "d");
		ArgumentNotValid.checkNotNullOrEmpty(d.getName(), "d.getName()");

		if (exists(d.getName())) {
			String msg = "Cannot create already existing domain " + d;
			log.debug(msg);
			throw new PermissionDenied(msg);
		}

		AliasInfo aliasInfo = d.getAliasInfo();
		Long aliasDomainId = aliasInfo == null 
				? null : queryLongValue(
						"SELECT domain_id FROM domains WHERE name=:name", 
						new ParameterMap("name", aliasInfo.getAliasOf()));

		log.debug("trying to create domain with name: " + d.getName());
		executeTransaction(
				"doCreate", 
				Domain.class, d,
				AliasInfo.class, d.getAliasInfo(),
				Long.class, aliasDomainId);
	}

	@SuppressWarnings("unused")
	private void doCreate(
			final Domain d,
			final AliasInfo aliasInfo,
			final Long aliasDomainId) {

		// Id is autogenerated
		// defaultconfig cannot exist yet, so we put in -1
		// until we have configs
		long initialEdition = 1;

		Long domainId = executeUpdate(
				"INSERT INTO domains (name, comments,"
						+ " defaultconfig, crawlertraps, edition,"
						+ " alias, lastaliasupdate)"
						+ " VALUES (:name,:comments,-1,:crawlerTraps,:edition,:alias,:aliasDate)", 
						new ParameterMap( 
								"name", getStorableName(d),
								"comments", getStorableComments(d),
								"crawlerTraps", StringUtils.conjoin("\n", d.getCrawlerTraps()),
								"edition", initialEdition,
								"alias", aliasDomainId,
								"aliasDate", aliasInfo == null ? null : aliasInfo.getLastChange()
								),
				"domain_id");            
		d.setID(domainId);

		Iterator<Password> passwords = d.getAllPasswords();
		while (passwords.hasNext()) {
			Password p = passwords.next();
			insertPassword(d.getID(), p);
		}

		Iterator<SeedList> seedlists = d.getAllSeedLists();
		if (!seedlists.hasNext()) {
			String msg = "No seedlists for domain " + d;
			log.debug(msg);
			throw new ArgumentNotValid(msg);
		}
		while (seedlists.hasNext()) {
			SeedList sl = seedlists.next();
			insertSeedlist(domainId, sl);
		}

		Iterator<DomainConfiguration> dcs = d.getAllConfigurations();
		if (!dcs.hasNext()) {
			String msg = "No configurations for domain " + d;
			log.debug(msg);
			throw new ArgumentNotValid(msg);
		}
		while (dcs.hasNext()) {
			DomainConfiguration dc = dcs.next();
			insertConfiguration(domainId, dc);

			// Create xref tables for seedlists referenced by this config
			createConfigSeedlistsEntries(domainId, dc);

			// Create xref tables for passwords referenced by this config
			createConfigPasswordsEntries(domainId, dc);
		}

		// Now that configs are defined, set the default config.
		DomainConfiguration defaultConf = d.getDefaultConfiguration();
		executeUpdate(
				"UPDATE domains SET defaultconfig = "
						+ "(SELECT config_id FROM configurations"
						+ " WHERE configurations.name=:dftConfName"
						+ " AND configurations.domain_id=:domainId)"
						+ " WHERE domain_id=:domainId",
						new ParameterMap(
								"dftConfName", getMaxLengthStringValue(
										defaultConf, 
										"name", 
										defaultConf.getName(), 
										Constants.MAX_NAME_SIZE),
								"domainId", domainId
							));

		for (Iterator<HarvestInfo> hi = d.getHistory().getHarvestInfo();
				hi.hasNext();) {
			insertHarvestInfo(d, hi.next());
		}

		for (DomainOwnerInfo doi : d.getAllDomainOwnerInfo()) {
			insertOwnerInfo(domainId, doi);
		}

		addExtendedFieldValues(d);
		saveExtendedFieldValues(d);

		d.setEdition(initialEdition);
	}

	@Override
	public synchronized void update(Domain d) {
		ArgumentNotValid.checkNotNull(d, "domain");

		if (!exists(d.getName())) {
			throw new UnknownID("No domain named " + d.getName() + " exists");
		}

		// Domain object may not have ID yet, so get it from the DB
		long domainID = queryLongValue(
				"SELECT domain_id FROM domains WHERE name=:name",
				new ParameterMap("name", d.getName()));
		if (d.hasID() && d.getID() != domainID) {
			String message = "Domain " + d + " has wrong id: Has "
					+ d.getID() + ", but persistent store claims "
					+ domainID;
			log.warn(message);
			throw new ArgumentNotValid(message);
		}
		d.setID(domainID);

		// The alias field is now updated using a separate select request
		// rather than embedding the select inside the update statement.
		// This change was needed to accommodate MySQL, and may lower
		// performance.		

		AliasInfo aliasInfo = d.getAliasInfo();
		Long aliasDomainId = aliasInfo == null 
				? null : queryLongValue(
						"SELECT domain_id FROM domains WHERE name=:name", 
						new ParameterMap("name", aliasInfo.getAliasOf()));

		executeTransaction(
				"doUpdate", 
				Domain.class, d,
				AliasInfo.class, d.getAliasInfo(),
				Long.class, aliasDomainId);
	}

	@SuppressWarnings("unused")
	private void doUpdate(
			final Domain d,
			final AliasInfo aliasInfo,
			final Long aliasDomainId) {

		long domainId = d.getID();
		long edition = d.getEdition();
		String cookedDomainComments =
				getMaxLengthStringValue(
						d, "comments", d.getComments(), Constants.MAX_COMMENT_SIZE);
		executeUpdate(
				"UPDATE domains SET comments=:comments, crawlertraps=:traps,"
						+ " edition=:nextEdition, alias=:alias, lastAliasUpdate=:aliasDate"
						+ " WHERE domain_id=:domainId AND edition=:currentEdition",
						new ParameterMap(
								"domainId", domainId,
								"comments", cookedDomainComments,
								"traps", StringUtils.conjoin("\n", d.getCrawlerTraps()),
								"currentEdition", edition,
								"nextEdition", edition + 1,
								"alias", aliasDomainId,
								"aliasDate", aliasInfo == null 
								? null : aliasInfo.getLastChange()
								));

		updatePasswords(d);

		updateSeedlists(d);

		updateConfigurations(d);

		updateOwnerInfo(d);

		updateHarvestInfo(d);

		saveExtendedFieldValues(d);            

		// Now that configs are updated, we can set default_config
		long defaultConfigId = queryLongValue(
				"SELECT config_id FROM configurations"
						+ " WHERE domain_id=:id AND name=:name",
						new ParameterMap(
								"id", domainId,
								"name", d.getDefaultConfiguration().getName()));			

		executeUpdate(
				"UPDATE domains SET defaultconfig=:defaultConfigId"
						+ " WHERE domain_id=:id",
						new ParameterMap(
								"id", domainId,
								"defaultConfigId", defaultConfigId));

		d.setEdition(edition + 1);
	}

	/**
	 * Update the list of passwords for the given domain, keeping IDs where
	 * applicable.
	 * @param d A domain to update.
	 */
	private void updatePasswords(Domain d) {
		Map<String, Long> oldNames = query(
				"SELECT name, password_id FROM passwords WHERE domain_id=:id",
				new ParameterMap("id", d.getID()),
				new StringToLongMapExtractor("name", "password_id"));

		String paramSql = "UPDATE passwords SET comments=:comments, url=:domain"
				+ ", realm=:realm, username=:user, password=:password"
				+ " WHERE name=:name AND domain_id=:domainId";

		for (Iterator<Password> pwds =
				d.getAllPasswords(); pwds.hasNext();) {

			Password p = pwds.next();			
			if (oldNames.containsKey(p.getName())) {
				executeUpdate(paramSql, getParameterMap(p, d.getID()));
				p.setID(oldNames.get(p.getName()));
				oldNames.remove(p.getName());
			} else {
				insertPassword(d.getID(), p);
			}
		}		

		// Delete the removed ones if they're not in use.
		paramSql = "DELETE FROM passwords WHERE password_id=:id";
		for (Long gone : oldNames.values()) {
			ParameterMap goneId = new ParameterMap("id", gone);
			// Check that we're not deleting something that's in use
			// Since deletion is very rare, this is allowed to take
			// some time.
			List<String> usages = queryStringList(
					"SELECT configurations.name FROM configurations, config_passwords"
							+ " WHERE configurations.config_id=config_passwords.config_id"
							+ " AND config_passwords.password_id=:id",
							goneId);
			if (!usages.isEmpty()) {
				String name = queryStringValue(
						"SELECT name FROM passwords WHERE password_id=:id",
						goneId);
				String message = "Cannot delete password " + name
						+ " as it is used in " + usages;
				log.debug(message);
				throw new PermissionDenied(message);
			}
			// Ok proceed to delete
			executeUpdate(paramSql, goneId);
		}
	}

	/**
	 * Update the list of seedlists for the given domain, keeping IDs where
	 * applicable.
	 * @param d A domain to update.
	 */
	private void updateSeedlists(Domain d) {
		Map<String, Long> oldNames = query(
				"SELECT name, seedlist_id FROM seedlists WHERE domain_id=:id",
				new ParameterMap("id", d.getID()),
				new StringToLongMapExtractor("name", "seedlist_id"));

		String paramSql = "UPDATE seedlists SET comments=:comments, seeds=:seeds"
				+ " WHERE name=:name AND domain_id=:domainId";

		for (Iterator<SeedList> sls = d.getAllSeedLists(); sls.hasNext();) {
			SeedList sl = sls.next();
			if (oldNames.containsKey(sl.getName())) {
				executeUpdate(paramSql, getParameterMap(sl, d.getID()));
				sl.setID(oldNames.get(sl.getName()));
				oldNames.remove(sl.getName());
			} else {
				insertSeedlist(d.getID(), sl);
			}
		}

		// Delete the removed ones if they're not in use.
		paramSql = "DELETE FROM seedlists WHERE seedlist_id=:id";
		for (Long gone : oldNames.values()) {
			ParameterMap goneId = new ParameterMap("id", gone);
			// Check that we're not deleting something that's in use
			// Since deletion is very rare, this is allowed to take
			// some time.
			List<String> usages = queryStringList(
					"SELECT configurations.name FROM configurations, config_seedlists"
							+ " WHERE configurations.config_id=config_seedlists.config_id"
							+ " AND config_seedlists.seedlist_id=:id",
							goneId);
			if (!usages.isEmpty()) {
				String name = queryStringValue(
						"SELECT name FROM seedlists WHERE seedlist_id=:id",
						goneId);
				String message = "Cannot delete seedlist " + name
						+ " as it is used in " + usages;
				log.debug(message);
				throw new PermissionDenied(message);
			}

			// Ok proceed to delete
			executeUpdate(paramSql, goneId);
		}		
	}

	/**
	 * Update the list of configurations for the given domain, keeping IDs where
	 * applicable. This also builds the xref tables for passwords and seedlists
	 * used in configurations, and so should be run after those are updated.
	 * @param d A domain to update.
	 */
	private void updateConfigurations(Domain d) {

		long domainId = d.getID();

		Map<String, Long> oldNames = query(
				"SELECT name, config_id FROM configurations WHERE domain_id=:id",
				new ParameterMap("id", d.getID()),
				new StringToLongMapExtractor("name", "config_id"));

		String paramSql = "UPDATE configurations SET comments=:comments"
				+ ", template_id=:templateId, maxobjects=:maxObjects"
				+ ", maxrate=:maxRate, maxbytes=:maxBytes"
				+ " WHERE name=:name AND domain_id=:domainId";
		for (Iterator<DomainConfiguration> dcs = d.getAllConfigurations(); dcs.hasNext();) {

			DomainConfiguration dc = dcs.next();

			if (oldNames.containsKey(dc.getName())) {

				long templateId = queryLongValue(
						"SELECT template_id FROM ordertemplates WHERE name=:name",
						new ParameterMap("name", dc.getOrderXmlName()));

				// Update
				executeUpdate(paramSql, getParameterMap(dc, domainId, templateId));

				dc.setID(oldNames.get(dc.getName()));
				oldNames.remove(dc.getName());
			} else {
				insertConfiguration(domainId, dc);
			}

			updateConfigPasswordsEntries(domainId, dc);
			updateConfigSeedlistsEntries(domainId, dc);
		}// Delete the removed ones if they're not in use.

		// Delete the removed ones if they're not in use.
		paramSql = "DELETE FROM configurations WHERE config_id=:configId";
		for (Long gone : oldNames.values()) {
			ParameterMap goneId = new ParameterMap("id", gone);
			// Before deleting, check if this is unused. Since deletion is
			// rare, this is allowed to take some time to give good output
			List<String> usages = queryStringList(
					"SELECT harvestdefinitions.name FROM harvestdefinitions, harvest_configs"
							+ " WHERE harvestdefinitions.harvest_id=harvest_configs.harvest_id"
							+ " AND harvest_configs.config_id=:id",
							goneId);
			if (usages != null) {
				String name = queryStringValue(
						"SELECT name FROM configurations WHERE config_id=:id", 
						goneId);
				String message = "Cannot delete configuration " + name
						+ " as it is used in " + usages;
				log.debug(message);
				throw new PermissionDenied(message);
			}

			// Proceed to delete
			executeUpdate(paramSql, goneId);
		}
	}

	/**
	 * Update the list of owner info for the given domain, keeping IDs where
	 * applicable.
	 * @param d A domain to update.
	 */
	private void updateOwnerInfo(Domain d) {

		long domainId = d.getID();

		List<Long> oldIDs = queryLongList(
				"SELECT ownerinfo_id FROM ownerinfo WHERE domain_id=:id", 
				new ParameterMap("id", d.getID()));

		String paramSql = "UPDATE ownerinfo SET created=:created, info=:info"
				+ " WHERE ownerinfo_id=:ownerInfoId";

		for (DomainOwnerInfo doi : d.getAllDomainOwnerInfo()) {
			if (doi.hasID() && oldIDs.remove(doi.getID())) {				
				ParameterMap paramMap = getParameterMap(doi, domainId);
				paramMap.put("ownerInfoId", doi.getID());				
				executeUpdate(paramSql, paramMap);				
			} else {
				insertOwnerInfo(domainId, doi);
			}
		}
		if (oldIDs.size() != 0) {
			String message = "Not allowed to delete ownerinfo " + oldIDs
					+ " on " + d;
			log.debug(message);
			throw new IOFailure(message);
		}
	}

	/**
	 * Update the list of harvest info for the given domain, keeping IDs where
	 * applicable.
	 * @param d A domain to update.
	 */
	private void updateHarvestInfo(Domain d) {
		long domainId = d.getID();
		List<Long> oldIDs = queryLongList(
				"SELECT historyinfo.historyinfo_id FROM historyinfo, configurations"
						+ " WHERE historyinfo.config_id=configurations.config_id"
						+ " AND configurations.domain_id=:domainId", 
						new ParameterMap("domainId", domainId));

		Iterator<HarvestInfo> his = d.getHistory().getHarvestInfo();
		while (his.hasNext()) {
			HarvestInfo hi = his.next();
			if (hi.hasID() && oldIDs.remove(hi.getID())) {

				String configName = 
						d.getConfiguration(hi.getDomainConfigurationName()).getName();

				long configId = queryLongValue("SELECT config_id FROM configurations, domains"
						+ " WHERE domains.domain_id=:domainId"
						+ " AND configurations.name=:configName"
						+ " AND configurations.domain_id=domains.domain_id",
						new ParameterMap(
								"domainId", domainId,
								"configName", configName
								));
				executeUpdate(
						"UPDATE historyinfo SET stopreason=:stopReason" 
								+ ", objectcount=:objectCount, bytecount=:byteCount"
								+ ", config_id=:configId"
								+ ", harvest_id=:harvestId, job_id=:jobId"
								+ " WHERE historyinfo_id=:id", 
								new ParameterMap(
										getParameterMap(hi, d, configId),
										"id", hi.getID(),
										"configName", configName
										));
			} else {
				insertHarvestInfo(d, hi);
			}
		}
		if (oldIDs.size() != 0) {
			String message = "Not allowed to delete historyinfo " + oldIDs
					+ " on " + d;
			log.debug(message);
			throw new IOFailure(message);
		}
	}

	/**
	 * Insert new harvest info for a domain.
	 * @param d
	 *            A domain to insert on. The domains ID must be correct.
	 * @param harvestInfo
	 *            Harvest info to insert.
	 */
	private void insertHarvestInfo(
			Domain d,
			HarvestInfo harvestInfo) {
		// Note that the config_id is grabbed from the configurations table.
		// TODO More stable way to get IDs, use a select
		long configId = d.getConfiguration(harvestInfo.getDomainConfigurationName()).getID();
		long harvestInfoId = executeUpdate(
				"INSERT INTO historyinfo (stopreason, objectcount, bytecount, config_id,"
						+ " job_id, harvest_id, harvest_time )"
						+ " VALUES (:stopReason,:objCount,:byteCount,:configId,"
						+ ":jobId,:harvestId,:time)",
						getParameterMap(harvestInfo, d, configId),
				"historyinfo_id");
		harvestInfo.setID(harvestInfoId);
	}

	/**
	 * Insert new owner info for a domain.
	 * @param domainId the ID of the associated domain.
	 * @param doi Owner info to insert.
	 */
	private void insertOwnerInfo(
			long domainId,
			DomainOwnerInfo doi) {

		long domainOwnerInfoId = executeUpdate(
				"INSERT INTO ownerinfo (domain_id, created, info)"
						+ " VALUES (:domainId,;created,:info)",
						getParameterMap(doi, domainId),
				"ownerinfo_id");
		doi.setID(domainOwnerInfoId);
	}

	/**
	 * Insert new seedlist for a domain.
	 * @param domainId the domain id
	 * @param sl Seedlist to insert.
	 */
	private void insertSeedlist(
			Long domainId,
			SeedList sl) {

		// ID is autogenerated
		long seedListId = executeUpdate(
				"INSERT INTO seedlists (name,comments,domain_id,seeds)"
						+ " VALUES (:name,:comments,:domainId,:seeds)",
						getParameterMap(sl, domainId),
				"seedlist_id");
		sl.setID(seedListId);
	}

	/**
	 * Inserts a new password entry into the database.
	 * @param domainId the domain id
	 * @param p A password entry to insert.
	 */
	private void insertPassword(
			Long domainId,
			Password p) {

		// ID is autogenerated
		long passwordId = executeUpdate(
				"INSERT INTO passwords (name, comments,"
						+ " domain_id, url, realm, username,"
						+ " password) " + "VALUES (:domainId,:name,:comments,"
						+ ":domain,:realm,:user,:password)",
						getParameterMap(p, domainId),
				"password_id");
		p.setID(passwordId);
	}

	/**
	 * Insert the basic configuration info into the DB. This does not establish
	 * the connections with seedlists and passwords, use
	 * {create,update}Config{Passwords,Seedlists}Entries for that.
	 * @param domainId the domain id
	 * @param dc a domain configuration
	 */
	private void insertConfiguration(
			final Long domainId,
			final DomainConfiguration dc) {

		long templateId = queryLongValue(
				"SELECT template_id FROM ordertemplates WHERE name=:name",
				new ParameterMap("name",  dc.getOrderXmlName()));

		// ID is autogenerated
		long confId = executeUpdate(
				"INSERT INTO configurations (name, comments, domain_id,"
						+ " template_id, maxobjects, maxrate, maxbytes)"
						+ " VALUES (:name,:comments,:domainId,:templateId,:maxObjects,:maxRate,:maxBytes)",
						getParameterMap(dc, domainId, templateId),
				"config_id");
		dc.setID(confId);
	}

	/**
	 * Delete all entries from the config_passwords table that refer to the
	 * given configuration and insert the current ones.
	 * @param domainId A domain ID
	 * @param dc Configuration to update.
	 */
	private void updateConfigPasswordsEntries(long domainId, DomainConfiguration dc) {		
		// Remove xrefs
		executeUpdate(
				"DELETE FROM config_passwords WHERE config_id=:id",
				new ParameterMap("id", dc.getID()));

		createConfigPasswordsEntries(domainId, dc);
	}

	/**
	 * Create the xref table for passwords used by configurations.
	 * @param domainId the domain ID to operate on.
	 * @param dc A configuration to create xref table for.
	 */
	private void createConfigPasswordsEntries(
			long domainId,
			DomainConfiguration dc) {
		for (Iterator<Password> passwords = dc.getPasswords(); passwords.hasNext();) {
			Password p = passwords.next();
			executeUpdate(
					"INSERT INTO config_passwords "
							+ "(config_id, password_id) "
							+ "SELECT config_id, password_id "
							+ "  FROM configurations, passwords"
							+ " WHERE configurations.domain_id=:domainId"
							+ "   AND configurations.name=:configName"
							+ "   AND passwords.name=:passwordName"
							+ "   AND passwords.domain_id=configurations.domain_id", 
							new ParameterMap(
									"domainId", domainId,
									"configName", dc.getName(),
									"passwordName", p.getName()));
		}
	}

	/**
	 * Delete all entries from the config_seedlists table that refer to the
	 * given configuration and insert the current ones.
	 * @param domainId a domain ID
	 * @param dc Configuration to update.
	 */
	private void updateConfigSeedlistsEntries(
			final long domainId,
			final DomainConfiguration dc) {
		// Remove xrefs
		executeUpdate(
				"DELETE FROM config_seedlists WHERE config_id=:id",
				new ParameterMap("id", dc.getID()));

		createConfigSeedlistsEntries(domainId, dc);
	}

	/**
	 * Create the xref table for seedlists used by configurations.
	 * @param domainId the domain ID to operate on.
	 * @param dc A configuration to create xref table for.
	 */
	private void createConfigSeedlistsEntries(
			long domainId,
			DomainConfiguration dc) {

		for (Iterator<SeedList> seedlists = dc.getSeedLists(); seedlists.hasNext();) {
			SeedList sl = seedlists.next();            
			executeUpdate(
					"INSERT INTO config_seedlists (config_id, seedlist_id)"
							+ " SELECT configurations.config_id, seedlists.seedlist_id"
							+ "  FROM configurations, seedlists"
							+ " WHERE configurations.name=:configName"
							+ "   AND seedlists.name=:seedListName"
							+ "   AND configurations.domain_id=:domainId"
							+ "   AND seedlists.domain_id=:domainId", 
							new ParameterMap(
									"domainId", domainId,
									"configName", dc.getName(),
									"seedListName", sl.getName()
									));
		}
	}

	@Override 
	public synchronized Domain read(String domainName) {
		ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
		if (!exists(domainName)) {
			throw new UnknownID("No domain by the name '" + domainName + "'");
		}
		return readKnown(domainName); 
	}

	@Override
	public synchronized Domain readKnown(String domainName) {
		ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");

		DomainResultSetExtractor domainExt = new DomainResultSetExtractor(domainName); 
		Domain d = query(
				"SELECT domains.domain_id, domains.comments, domains.crawlertraps"
						+ ", domains.edition, configurations.name"
						+ ", (SELECT name FROM domains as aliasdomains"
						+ " WHERE aliasdomains.domain_id=domains.alias)"
						+ ", domains.lastaliasupdate"
						+ " FROM domains, configurations"
						+ " WHERE domains.name=:name"
						+ " AND domains.defaultconfig=configurations.config_id",
						new ParameterMap("name", domainName),
						domainExt);

		readSeedlists(d);
		readPasswords(d);
		readConfigurations(d);

		// Now that configs are in, we can set the default
		d.setDefaultConfiguration(domainExt.getDefaultConfigName());

		readOwnerInfo(d);
		readHistoryInfo(d);
		readExtendedFieldValues(d);

		return d;
	}

	/**
	 * Read the configurations for the domain. This should not be called until
	 * after passwords and seedlists are read.
	 * @param d The domain being read. Its ID must be set.
	 */
	private void readConfigurations(Domain d) {

		// Read the configurations now that passwords and seedlists exist
		List<DomainConfiguration> confList = query(
				"SELECT config_id, configurations.name as configName, comments"
						+ ", ordertemplates.name as orderXmlName, maxobjects, maxrate, maxbytes"
						+ " FROM configurations, ordertemplates"
						+ " WHERE domain_id=:id"
						+ " AND configurations.template_id=ordertemplates.template_id",
						new ParameterMap("id", d.getID()),
						new ReadDomainConfigurationMapper(d));		

		for (DomainConfiguration dc : confList) {
			d.addConfiguration(dc);
		}

		if (!d.getAllConfigurations().hasNext()) {
			String message = "Loaded domain " + d
					+ " with no configurations";
			log.warn(message);
			throw new IOFailure(message);
		}
	}

	@Override
	public List<Long> findUsedConfigurations(Long domainID) {
		return queryLongList(
				"SELECT configurations.config_id, configurations.name" +
						" FROM configurations" +
						" JOIN harvest_configs USING (config_id)" +
						" JOIN harvestdefinitions USING (harvest_id) " +
						" WHERE configurations.domain_id=:domainId" +
						" AND harvestdefinitions.isactive=:active",
						new ParameterMap(
								"domainId", domainID,
								"active", true
								));
	}

	/**
	 * Read owner info entries for the domain.
	 * @param d The domain being read. Its ID must be set.
	 */
	private void readOwnerInfo(final Domain d) {
		// Read owner info
		List<DomainOwnerInfo> doiList = query(
				"SELECT ownerinfo_id, created, info FROM ownerinfo WHERE domain_id=:id",
				new ParameterMap("id", d.getID()),
				new RowMapper<DomainOwnerInfo>() {
					@Override
					public DomainOwnerInfo mapRow(ResultSet res, int pos)
							throws SQLException {
						DomainOwnerInfo doi = new DomainOwnerInfo(
								new Date(res.getTimestamp("created").getTime()), 
								res.getString("info"));
						doi.setID(res.getLong("ownerinfo_id"));
						return doi;
					}

				});

		for (DomainOwnerInfo doi : doiList) {
			d.addOwnerInfo(doi);
		}
	}

	/**
	 * Read history info entries for the domain.
	 * @param d The domain being read. Its ID must be set.
	 */
	private void readHistoryInfo(final Domain d) {
		// Read history info
		List<HarvestInfo> historyList = query(
				"SELECT historyinfo_id, stopreason, objectcount, bytecount"
						+ ", name, job_id, harvest_id, harvest_time"
						+ " FROM historyinfo, configurations"
						+ " WHERE configurations.domain_id=:id"
						+ " AND historyinfo.config_id=configurations.config_id",
						new ParameterMap("id", d.getID()),
						new HarvestInfoMapper(d.getName()));
		for (HarvestInfo hi : historyList) {
			d.getHistory().addHarvestInfo(hi);
		}
	}

	/**
	 * Read passwords for the domain.
	 * @param d The domain being read. Its ID must be set.
	 */
	private void readPasswords(Domain d) {

		List<Password> pwdList = query(
				"SELECT password_id, name, comments, url, realm, username, password"
						+ " FROM passwords WHERE domain_id=:id",
						new ParameterMap("id", d.getID()),
						new RowMapper<Password>() {
							@Override
							public Password mapRow(ResultSet res, int pos) 
									throws SQLException {
								Password pwd = new Password(
										res.getString("name"), 
										res.getString("comments"), 
										res.getString("url"), 
										res.getString("realm"), 
										res.getString("username"), 
										res.getString("password"));
								pwd.setID(res.getLong("password_id"));
								return pwd;
							}

						});

		for (Password pwd : pwdList) {
			d.addPassword(pwd);
		}
	}

	/**
	 * Read seedlists for the domain.
	 * @param d The domain being read. Its ID must be set.
	 */
	private void readSeedlists(final Domain d) {

		List<SeedList> seedLists = query(
				"SELECT seedlist_id, name, comments, seeds"
						+ " FROM seedlists WHERE domain_id=:id",
						new ParameterMap("id", d.getID()),
						new RowMapper<SeedList>() {
							@Override
							public SeedList mapRow(ResultSet rs, int pos) throws SQLException {								
								String seedlistContents = "";
								if (DBSpecifics.getInstance().supportsClob()) {
									Clob clob = rs.getClob("seeds");
									seedlistContents = clob.getSubString(1, (int) clob.length());
								} else {
									seedlistContents = rs.getString("seeds");
								}

								SeedList seedlist = new SeedList(
										rs.getString("name"), 
										seedlistContents);
								seedlist.setComments(rs.getString("comments"));
								seedlist.setID(rs.getLong("seedlist_id"));
								return seedlist;
							}
						});

		for (SeedList sl : seedLists) {
			d.addSeedList(sl);
		}

		if (!d.getAllSeedLists().hasNext()) {
			final String msg = "Domain " + d + " loaded with no seedlists";
			log.warn(msg);
			throw new IOFailure(msg);
		}
	}

	/**
	 * Return true if a domain with the given name exists.
	 *
	 * @param c an open connection to the harvestDatabase
	 * @param domainName a name of a domain
	 * @return true if a domain with the given name exists, otherwise false.
	 */
	public synchronized boolean exists(String domainName) {
		return 1 == queryIntValue(
				"SELECT COUNT(*) FROM domains WHERE name=:name", 
				new ParameterMap("name", domainName));
	}


	@Override
	public synchronized int getCountDomains() {
		return queryIntValue("SELECT COUNT(*) FROM domains");
	}

	@Override
	public synchronized Iterator<Domain> getAllDomains() {
		List<String> domainNames = queryStringList(
				"SELECT name FROM domains ORDER BY name");
		List<Domain> orderedDomains = new LinkedList<Domain>();
		for (String name : domainNames) {
			orderedDomains.add(read(name));
		}
		return orderedDomains.iterator();
	}

	@Override
	public Iterator<Domain> getAllDomainsInSnapshotHarvestOrder() {
		// Note: maxbytes are ordered with largest first for symmetry
		// with HarvestDefinition.CompareConfigDesc
		List<String> domainNames = queryStringList(
				"SELECT domains.name FROM domains, configurations, ordertemplates"
						+ " WHERE domains.defaultconfig=configurations.config_id"
						+ " AND configurations.template_id=ordertemplates.template_id"
						+ " ORDER BY ordertemplates.name,"
						+ " configurations.maxbytes DESC domains.name");
		return new FilterIterator<String, Domain>(domainNames.iterator()) {
			public Domain filter(String s) {
				return readKnown(s);
			}
		};
	}


	@Override
	public List<String> getDomains(String glob) {
		ArgumentNotValid.checkNotNullOrEmpty(glob, "glob");
		// SQL uses % and _ instead of * and ?
		String sqlGlob = makeSQLGlob(glob);
		return queryStringList(
				"SELECT name FROM domains WHERE name LIKE :pattern", 
				new ParameterMap("pattern", sqlGlob));
	}

	@Override
	public boolean mayDelete(DomainConfiguration config) {
		ArgumentNotValid.checkNotNull(config, "config");
		String defaultConfigName = 
				this.getDefaultDomainConfigurationName(config.getDomainName());
		// Never delete default config and don't delete configs being used.
		int configCount = queryIntValue(
				"SELECT count(config_id) FROM harvest_configs WHERE config_id=:id",
				new ParameterMap("id", config.getID()));
		return !config.getName().equals(defaultConfigName) && 0 == configCount;
	}

	/**
	 * Get the name of the default configuration for the given domain.
	 * @param domainName a name of a domain 
	 * @return the name of the default configuration for the given domain.
	 */
	private String getDefaultDomainConfigurationName(String domainName) {
		return queryStringValue(
				"SELECT configurations.name FROM domains, configurations"
						+ " WHERE domains.defaultconfig=configurations.config_id"
						+ " AND domains.name=:name", 
						new ParameterMap("name", domainName));
	}

	@Override
	public synchronized SparseDomain readSparse(String domainName) {
		ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");

		List<String> domainConfigurationNames = queryStringList(
				"SELECT configurations.name FROM configurations, domains"
						+ " WHERE domains.domain_id=configurations.domain_id"
						+ " AND domains.name=:name",
						new ParameterMap("name", domainName));
		if (domainConfigurationNames.size() == 0) {
			throw new UnknownID("No domain exists with name '" + domainName
					+ "'");
		}
		return new SparseDomain(domainName, domainConfigurationNames);
	}

	@Override
	public List<AliasInfo> getAliases(String domainName) {
		ArgumentNotValid.checkNotNullOrEmpty(domainName, "String domain");

		// return all <domain, alias, lastaliasupdate> tuples
		// where alias = domain
		if (!exists(domainName)) {
			log.debug("domain named '" + domainName
					+ "' does not exist. Returning empty result set");
			return new ArrayList<AliasInfo>();
		}

		return query(
				"SELECT domains.name, domains.lastaliasupdate"
						+ " FROM domains, domains as fatherDomains "
						+ " WHERE domains.alias=fatherDomains.domain_id"
						+ " AND fatherDomains.name=:name"
						+ " ORDER BY domains.name",
						new ParameterMap("name", domainName),
						new AliasInfoMapper(domainName));
	}

	@Override
	public List<AliasInfo> getAllAliases() {
		// return all <domain, alias, lastaliasupdate> tuples
		// where alias is not-null
		return query(
				"SELECT domains.name, "
						+ "(SELECT name FROM domains as aliasdomains"
						+ " WHERE aliasdomains.domain_id= domains.alias), domains.lastaliasupdate"
						+ " FROM domains"
						+ " WHERE domains.alias IS NOT NULL ORDER BY lastaliasupdate ASC",
						new RowMapper<AliasInfo>() {						@Override
							public AliasInfo mapRow(ResultSet rs, int pos)
									throws SQLException {
							return new AliasInfo(
									rs.getString("domains.name"), 
									rs.getString("name"), 
									getDateKeepNull(rs, "lastaliasupdate"));
						}

						});
	}

	/**
	 * Return all TLDs represented by the domains in the domains table.
	 * it was asked that a level X TLD belong appear in TLD list where
	 * the level is <=X for example bidule.bnf.fr belong to .bnf.fr and to .fr
	 * it appear in the level 1 list of TLD and in the level 2 list
	 * @param level maximum level of TLD
	 * @return a list of TLDs
	 * @see DomainDAO#getTLDs(int)
	 */
	@Override
	public List<TLDInfo> getTLDs(int level) {
		Map<String, TLDInfo> resultMap = query(
				"SELECT name FROM domains",
				new TLDInfoExtractor(level));

		List<TLDInfo> resultSet = new ArrayList<TLDInfo>(resultMap.values());
		Collections.sort(resultSet);
		return resultSet;
	}

	@Override
	public HarvestInfo getDomainJobInfo(
			final Job j, 
			final String domainName, 
			final String configName) {
		ArgumentNotValid.checkNotNull(j, "j");
		ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
		ArgumentNotValid.checkNotNullOrEmpty(configName, "configName");

		// Get domain_id for domainName
		long domainId = queryLongValue(
				"SELECT domain_id FROM domains WHERE name=:name",
				new ParameterMap("name", domainName));

		long configId = queryLongValue(
				"SELECT config_id FROM configurations WHERE name=:name AND domain_id=:domainId",
				new ParameterMap(
						"name", configName, 
						"domainId", domainId
						));

		return query(
				"SELECT stopreason, objectcount, bytecount"
						+ ", harvest_time FROM historyinfo"
						+ " WHERE job_id=:jobId"
						+ " AND config_id=:configId"
						+ " AND harvest_id=:harvestId",
						new ParameterMap(
								"jobId", j.getJobID(),
								"configId", configId,
								"harvestId", j.getOrigHarvestDefinitionID()),
								new HarvestInfoExtractor(j, domainName, configName));
	}

	@Override
	public List<DomainHarvestInfo> listDomainHarvestInfo(String domainName,
			String orderBy,
			boolean asc) {
		ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");

		final String ascOrDesc = asc ? "ASC" : "DESC";
		log.debug("Using ascOrDesc=" + ascOrDesc + " after receiving " + asc);

		// For historical reasons, not all historyinfo objects have the
		// information required to find the job that made them. Therefore,
		// we must left outer join them onto the jobs list to get the
		// start date and end date for those where they can be found.
		return query(
				"SELECT jobs.job_id, hdname, hdid, harvest_num, configname, startdate"
						+ ", enddate, objectcount, bytecount, stopreason"
						+ " FROM ( "
						+ "  SELECT harvestdefinitions.name AS hdname,"
						+ "         harvestdefinitions.harvest_id AS hdid,"
						+ "         configurations.name AS configname,"
						+ "         objectcount, bytecount, job_id, stopreason"
						+ "    FROM domains, configurations, historyinfo, "
						+ "         harvestdefinitions"
						+ "   WHERE domains.name=:domainName"
						+ "     AND domains.domain_id=configurations.domain_id"
						+ "     AND historyinfo.config_id=configurations.config_id"
						+ "     AND historyinfo.harvest_id=harvestdefinitions.harvest_id) AS hist"
						+ " LEFT OUTER JOIN jobs ON hist.job_id=jobs.job_id"
						+ " ORDER BY " + orderBy + " " + ascOrDesc,
						new ParameterMap("domainName", domainName),
						new DomainHarvestInfoMapper(domainName));					
	}

	/**
	 * Adds Defaultvalues for all extended fields of this entity.
	 * @param d the domain to which to add the values
	 */
	private void addExtendedFieldValues(Domain d) {
		ExtendedFieldDAO extendedFieldDAO = ExtendedFieldDAO.getInstance();
		List<ExtendedField> list = extendedFieldDAO
				.getAll(ExtendedFieldTypes.DOMAIN);

		Iterator<ExtendedField> it = list.iterator();
		while (it.hasNext()) {
			ExtendedField ef = it.next();

			ExtendedFieldValue efv = new ExtendedFieldValue();
			efv.setContent(ef.getDefaultValue());
			efv.setExtendedFieldID(ef.getExtendedFieldID());

			d.getExtendedFieldValues().add(efv);
		}
	}

	/**
	 * Saves all extended Field values for a Domain in the Database.
	 * @param d Domain where loaded extended Field Values will be set
	 */
	private void saveExtendedFieldValues(Domain d) {
		List<ExtendedFieldValue> list = d.getExtendedFieldValues();
		for (int i = 0; i < list.size(); i++) {
			ExtendedFieldValue efv = list.get(i);
			efv.setInstanceID(d.getID());

			ExtendedFieldValueDBDAO dao  = 
					(ExtendedFieldValueDBDAO) ExtendedFieldValueDAO.getInstance();
			if (efv.getExtendedFieldValueID() != null) {
				dao.update(efv);
			} else {
				dao.create(efv);
			}
		}
	}


	/**
	 * Reads all extended Field values from the database for a domain.
	 * @param d Domain where loaded extended Field Values will be set
	 * 
	 */
	private void readExtendedFieldValues(final Domain d) {
		ExtendedFieldDAO dao = ExtendedFieldDAO.getInstance();
		List<ExtendedField> list = dao.getAll(ExtendedFieldTypes.DOMAIN);

		for (int i = 0; i < list.size(); i++) {
			ExtendedField ef = list.get(i);

			ExtendedFieldValueDAO dao2 = ExtendedFieldValueDAO.getInstance();
			ExtendedFieldValue efv = dao2.read(ef.getExtendedFieldID(),
					d.getID());
			if (efv == null) {
				efv = new ExtendedFieldValue();
				efv.setExtendedFieldID(ef.getExtendedFieldID());
				efv.setInstanceID(d.getID());
				efv.setContent(ef.getDefaultValue());
			}

			d.addExtendedFieldValue(efv);
		}
	}

	@Override
	public DomainConfiguration getDomainConfiguration(
			final String domainName,
			final String configName) {
		DomainHistory history = getDomainHistory(domainName);
		List<String> crawlertraps = getCrawlertraps(domainName);

		List<DomainConfiguration> foundConfigs = query(
				"SELECT config_id, configurations.name as configName"
						+ ", comments, ordertemplates.name as orderXmlName"
						+ ", maxobjects, maxrate, maxbytes"
						+ " FROM configurations, ordertemplates"
						+ " WHERE domain_id=(SELECT domain_id FROM domains"
						+ "  WHERE name=:domainName) AND configurations.name=:configName"
						+ "  AND configurations.template_id=ordertemplates.template_id",
						new ParameterMap(
								"domainName", domainName,
								"configName", configName),
								new GetDomainConfigurationMapper(domainName, history, crawlertraps));
		return foundConfigs.get(0);
	}

	/**
	 * Retrieve the crawlertraps for a specific domain.
	 * TODO should this method be public?
	 * @param domainName the name of a domain.
	 * @return the crawlertraps for given domain.
	 */
	private List<String> getCrawlertraps(String domainName) {
		try {
			return queryStringList(
					"SELECT crawlertraps FROM domains WHERE name=:domainName",
					new ParameterMap("domainName", domainName));
		} catch (DataAccessException e) {
			throw new IOFailure("Unable to find crawlertraps for domain '"
					+ domainName + "'. The domain doesn't seem to exist.");
		}
	}

	@Override
	public Iterator<HarvestInfo> getHarvestInfoBasedOnPreviousHarvestDefinition(
			final HarvestDefinition previousHarvestDefinition) {
		ArgumentNotValid.checkNotNull(previousHarvestDefinition,
				"previousHarvestDefinition");
		// For each domainConfig, get harvest infos if there is any for the
		// previous harvest definition
		return new FilterIterator<DomainConfiguration, HarvestInfo>(
				previousHarvestDefinition.getDomainConfigurations()) {
			/**
			 * @see FilterIterator#filter(Object)
			 */
			protected HarvestInfo filter(DomainConfiguration o){
				DomainConfiguration config = o;
				DomainHistory domainHistory 
				= getDomainHistory(config.getDomainName());
				HarvestInfo hi = domainHistory.getSpecifiedHarvestInfo(
						previousHarvestDefinition.getOid(),
						config.getName());
				return hi;
			}
		}; // Here ends the above return-statement
	}

	@Override
	public DomainHistory getDomainHistory(String domainName) {
		ArgumentNotValid.checkNotNullOrEmpty(domainName, "String domainName");

		List<HarvestInfo> hiList = query(
				"SELECT historyinfo_id, stopreason, objectcount, bytecount"
						+ ", name, job_id, harvest_id, harvest_time"
						+ " FROM historyinfo, configurations"
						+ " WHERE configurations.domain_id="
						+ " (SELECT domain_id FROM domains WHERE name=:domainName)"
						+ " AND historyinfo.config_id=configurations.config_id",
						new ParameterMap("domainName", domainName),
						new HarvestInfoMapper(domainName));

		DomainHistory history = new DomainHistory();
		for (HarvestInfo hi : hiList) {
			history.addHarvestInfo(hi);
		}

		return history;
	}

	@Override
	public List<String> getDomains(String glob, String searchField) {
		ArgumentNotValid.checkNotNullOrEmpty(glob, "glob");
		ArgumentNotValid.checkNotNullOrEmpty(searchField, "searchField");
		// SQL uses % and _ instead of * and ?
		String sqlGlob = makeSQLGlob(glob);
		return queryStringList(
				"SELECT name FROM domains"
						+ " WHERE " + searchField.toLowerCase() + " LIKE :pattern",
						new ParameterMap("pattern", sqlGlob));
	}

	private ParameterMap getParameterMap(
			final SeedList sl,
			final long domainId) {
		return new ParameterMap(
				"domainId", domainId,
				"name", getStorableName(sl),
				"comments", getStorableComments(sl),
				"seeds", getMaxLengthTextValue(
						sl, "seedlists", sl.getSeedsAsString(), Constants.MAX_SEED_LIST_SIZE)
				);
	}

	private ParameterMap getParameterMap(
			final Password p,
			final long domainId) {
		return new ParameterMap(
				"domainId", domainId,
				"name", getStorableName(p),
				"comments", getStorableComments(p),
				"domain", getMaxLengthStringValue(
						p, 
						"domain", 
						p.getPasswordDomain(), 
						Constants.MAX_URL_SIZE),
				"realm", getMaxLengthStringValue(
						p, "realm", 
						p.getRealm(), 
						Constants.MAX_REALM_NAME_SIZE),
				"user", getMaxLengthStringValue(
						p, 
						"user", 
						p.getUsername(), 
						Constants.MAX_USER_NAME_SIZE),
				"password", getMaxLengthStringValue(
						p, 
						"password", 
						p.getPassword(), 
						Constants.MAX_PASSWORD_SIZE)
				);
	}

	private ParameterMap getParameterMap(
			final DomainConfiguration dc,
			final long domainId,
			final long templateId) {
		return new ParameterMap(
				"domainId", domainId,
				"name", getStorableName(dc),
				"comments", getStorableComments(dc),
				"templateId", templateId,
				"maxObjects", dc.getMaxObjects(),
				"maxBytes", dc.getMaxBytes(),
				"maxRate", dc.getMaxRequestRate()
				);
	}

	private ParameterMap getParameterMap(
			final DomainOwnerInfo doi,
			final long domainId) {
		return new ParameterMap(
				"domainId", domainId,
				"created", doi.getDate(),
				"info", doi.getInfo()
				);
	}

	private ParameterMap getParameterMap(
			final HarvestInfo harvestInfo,
			final Domain d,
			final long configId) {
		return new ParameterMap(
				"configId", configId,
				"stopReason", harvestInfo.getStopReason().ordinal(),
				"objectCount", harvestInfo.getCountObjectRetrieved(),
				"byteCount", harvestInfo.getSizeDataRetrieved(),
				"jobId", harvestInfo.getJobID() != null ? harvestInfo.getJobID() : null,
						"harvestId", harvestInfo.getHarvestID(),
						"time", harvestInfo.getDate()
				);
	}

}
