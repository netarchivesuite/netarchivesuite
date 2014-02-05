/* File:        $Id: HarvestDefinitionDBDAO.java 2803 2013-10-29 15:42:35Z ngiraud $
 * Revision:    $Revision: 2803 $
 * Author:      $Author: ngiraud $
 * Date:        $Date: 2013-10-29 16:42:35 +0100 (Tue, 29 Oct 2013) $
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FilterIterator;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.FullHarvest;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestRunInfo;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.harvester.datamodel.Schedule;
import dk.netarkivet.harvester.datamodel.SparseDomainConfiguration;
import dk.netarkivet.harvester.datamodel.SparseFullHarvest;
import dk.netarkivet.harvester.datamodel.SparsePartialHarvest;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery;

/**
 * A database-oriented implementation of the HarvestDefinitionDAO.
 * 
 * The statements to create the tables are located in:
 * <ul>
 * <li><em>Derby:</em> scripts/sql/createfullhddb.sql</li>
 * <li><em>MySQL:</em> scripts/sql/createfullhddb.mysql</li>
 * <li><em>PostgreSQL:</em> scripts/postgresql/netarchivesuite_init.sql</li>
 * </ul>
 * 
 */
public class HarvestDefinitionDBDAO extends HarvestDefinitionDAO {

	/** The logger. */
	private final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Specific row mapper used to build a list of {@link HarvestRunInfo}s.
	 * @see HarvestDefinitionDBDAO#getHarvestRunInfo(long)
	 */
	private final class HarvestRunInfoMapper implements RowMapper<HarvestRunInfo> {
		
		private final long harvestId;
		
		Map<Integer, HarvestRunInfo> runInfos;
		
		public HarvestRunInfoMapper(final long harvestId) {
			super();
			this.harvestId = harvestId;
			this.runInfos = new HashMap<Integer, HarvestRunInfo>();
		}

		@Override
		public HarvestRunInfo mapRow(ResultSet rs, int pos) throws SQLException {
			int runNr = rs.getInt("harvest_num");
			HarvestRunInfo info = runInfos.get(runNr);
			if (info == null) {
				String name = rs.getString("name");
				info = new HarvestRunInfo(harvestId, name, runNr);
				// Put into hash for easy access when updating.
				runInfos.put(runNr, info);
			}
			JobStatus status = JobStatus.fromOrdinal(rs.getInt("status"));
			// For started states, check start date
			if (status != JobStatus.NEW && status != JobStatus.SUBMITTED
					&& status != JobStatus.RESUBMITTED) {
				Date startDate = getDateKeepNull(rs, "minStart");
				if (info.getStartDate() == null
						|| (startDate != null && startDate.before(info
								.getStartDate()))) {
					info.setStartDate(startDate);
				}
			}
			// For finished jobs, check end date
			if (status == JobStatus.DONE || status == JobStatus.FAILED) {
				Date endDate = getDateKeepNull(rs, "minEnd");
				if (info.getEndDate() == null
						|| (endDate != null && endDate.after(info
								.getEndDate()))) {
					info.setEndDate(endDate);
				}
			}
			info.setStatusCount(status, rs.getInt("jobCount"));
			return info;
		}

		/**
		 * Return harvest run infos mapped by harvest run
		 * @return the runInfos
		 */
		public final Map<Integer, HarvestRunInfo> getAsMap() {
			return runInfos;
		}
		
	}
	
	/**
	 * Specific extractor used to complement {@link HarvestRunInfo} instances.
	 * @see HarvestDefinitionDBDAO#getHarvestRunInfo(long)
	 */
	private final class HarvestRunInfoComplementer implements ResultSetExtractor<Integer> {

		private Map<Integer, HarvestRunInfo> runInfos;
		
		private final long harvestId;
		
		public HarvestRunInfoComplementer(
				final long harvestId,
				Map<Integer, HarvestRunInfo> runInfos) {
			super();
			this.harvestId = harvestId;
			this.runInfos = runInfos;
		}

		@Override
		public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
			int updateCount = 0;
			while (rs.next()) {
				final int harvestNum = rs.getInt("harvest_num");
				HarvestRunInfo info = runInfos.get(harvestNum);
				if (info != null) {
					info.setBytesHarvested(rs.getLong("byteCountSum"));
					info.setDocsHarvested(rs.getLong("objectCountSum"));
					updateCount++;
				} else {
					log.debug("Harvestnum " + harvestNum + " for harvestID " 
							+ harvestId 
							+ " is skipped. Must have arrived between selects");
				}
			}
			return updateCount;
		}
		
	}
	
	/**
	 * Comparator used for sorting the UI list of
	 * {@link SparseDomainConfiguration}s. Sorts first by domain name
	 * alphabetical order, next by configuration name.
	 * 
	 */
	private static class SparseDomainConfigurationComparator implements
	Comparator<SparseDomainConfiguration> {

		@Override
		public int compare(SparseDomainConfiguration sdc1,
				SparseDomainConfiguration sdc2) {
			int domComp = sdc1.getDomainName().compareTo(sdc2.getDomainName());
			if (0 == domComp) {
				return sdc1.getConfigurationName().compareTo(
						sdc2.getConfigurationName());
			}
			return domComp;
		}

	}

	/** Create a new HarvestDefinitionDAO using database. */
	HarvestDefinitionDBDAO() {
		super();
	}

	/**
	 * Create a harvest definition in Database. The harvest definition object
	 * should not have its ID set unless we are in the middle of migrating.
	 * 
	 * @param harvestDefinition
	 *            A new harvest definition to store in the database.
	 * @return The harvestId for the just created harvest definition.
	 * @see HarvestDefinitionDAO#create(HarvestDefinition)
	 */
	@Override
	public synchronized Long create(final HarvestDefinition harvestDefinition) {

		Long id = harvestDefinition.getOid();
		if (id == null) {
			id = generateNextID();
		}

		return (Long) executeTransaction(
				"doCreate", 
				HarvestDefinition.class, harvestDefinition,
				Long.class, id);
	}

	@SuppressWarnings("unused")
	private synchronized Long doCreate(
			final HarvestDefinition harvestDefinition,
			final Long id) {

		final int edition = 1;
		Date submissiondate = new Date();

		// Make base insert
		executeUpdate(
				"INSERT INTO harvestdefinitions (harvest_id, name, comments, numevents, submitted"
						+ ", isactive, edition, audience)"
						+ " VALUES (:id,:name,:comments,:numEvents"
						+ ",:submitted,:isActive,:edition,:audience)",
						new ParameterMap(
								"id", id,
								"name", getStorableName(harvestDefinition),
								"comments", getStorableComments(harvestDefinition),
								"numEvents", harvestDefinition.getNumEvents(),
								"submitted", submissiondate,
								"isActive", harvestDefinition.getActive(),
								"edition", edition,
								"audience", harvestDefinition.getAudience()
								));

		// Then complement depending on harvest type
		if (harvestDefinition instanceof FullHarvest) {
			FullHarvest fh = (FullHarvest) harvestDefinition;
			HarvestDefinition previousHd = fh.getPreviousHarvestDefinition();
			executeUpdate(
					"INSERT INTO fullharvests (harvest_id, maxobjects, maxbytes"
							+ ", maxjobrunningtime, previoushd, isindexready)"
							+ " VALUES (:harvestId,:maxObjects,:maxBytes,:maxJobDuration"
							+ ",:previousHd,:isIndexReady)",
							new ParameterMap(
									"harvestId", id,
									"maxObjects", fh.getMaxCountObjects(),
									"maxBytes", fh.getMaxBytes(),
									"maxJobDuration", fh.getMaxJobRunningTime(),
									"previousHd", previousHd != null ? previousHd.getOid() : null,
											"isIndexReady", fh.getIndexReady()));
		}  else if (harvestDefinition instanceof PartialHarvest) {
			PartialHarvest ph = (PartialHarvest) harvestDefinition;

			// Get schedule id
			long scheduleId = queryLongValue(
					"SELECT schedule_id FROM schedules WHERE name=:name", 
					new ParameterMap("name", ph.getSchedule().getName()));

			executeUpdate(
					"INSERT INTO partialharvests (harvest_id, schedule_id, nextdate)"
							+ " VALUES (:harvestId,:scheduleId,:nextDate)",
							new ParameterMap(
									"harvestId", id,
									"scheduleId", scheduleId,
									"nexDate", ph.getNextDate()));
		}

		// Now that we have committed, set new data on object.
		harvestDefinition.setSubmissionDate(submissiondate);
		harvestDefinition.setEdition(edition);
		harvestDefinition.setOid(id);
		return id;
	}

	/**
	 * Create the entries in the harvest_configs table that connect
	 * PartialHarvests and their configurations.
	 * @param ph The harvest to insert entries for.
	 * @param id The id of the harvest -- this may not yet be set on ph
	 */
	private void createHarvestConfigsEntries(
			final PartialHarvest ph,
			final long id) {

		// First delete xrefs
		executeUpdate(
				"DELETE FROM harvest_configs WHERE harvest_id=:id",
				new ParameterMap("id", id));

		// Then insert new ones
		Iterator<DomainConfiguration> dcs = ph.getDomainConfigurations();
		while (dcs.hasNext()) {
			DomainConfiguration dc = dcs.next();
			executeUpdate(
					"INSERT INTO harvest_configs (harvest_id, config_id)"
							+ " SELECT :id, config_id FROM configurations, domains"
							+ " WHERE domains.name=:domainName"
							+ " AND configurations.name=:confName"
							+ " AND domains.domain_id=configurations.domain_id",
							new ParameterMap(
									"id", id,
									"domainName", dc.getDomainName(),
									"confName", dc.getName()));
		}
	}

	/**
	 * Generates the next id of a harvest definition. this implementation
	 * retrieves the maximum value of harvest_id in the DB, and returns this
	 * value + 1.
	 * @return The next available ID
	 */
	private synchronized Long generateNextID() {
		Long maxVal = queryLongValue("SELECT max(harvest_id) FROM harvestdefinitions");
		if (maxVal == null) {
			maxVal = 0L;
		}
		return maxVal + 1L;
	}

	/**
	 * Read the stored harvest definition for the given ID.
	 * 
	 * @see HarvestDefinitionDAO#read(Long)
	 * @param harvestDefinitionID
	 *            An ID number for a harvest definition
	 * @return A harvest definition that has been read from persistent storage.
	 * @throws UnknownID
	 *             if no entry with that ID exists in the database
	 * @throws IOFailure
	 *             If DB-failure occurs?
	 */
	@Override
	public synchronized HarvestDefinition read(final Long harvestDefinitionID)
			throws UnknownID, IOFailure {

		if (!exists(harvestDefinitionID)) {
			String message = "Unknown harvest definition "
					+ harvestDefinitionID;
			log.debug(message);
			throw new UnknownID(message);
		}

		log.debug("Reading harvestdefinition w/ id " + harvestDefinitionID);

		// First look for a full harvest
		FullHarvest fh = query(
				"SELECT name, comments, numevents, submitted"
						+ ", previoushd, maxobjects, maxbytes"
						+ ", maxjobrunningtime, isindexready, isactive, edition, audience"
						+ " FROM harvestdefinitions, fullharvests"
						+ " WHERE harvestdefinitions.harvest_id=:harvestId"
						+ " AND harvestdefinitions.harvest_id=fullharvests.harvest_id",
						new ParameterMap("harvestId", harvestDefinitionID),
						new ResultSetExtractor<FullHarvest>() {					@Override
							public FullHarvest extractData(ResultSet rs)
									throws SQLException, DataAccessException {
							if (!rs.next()) {
								return null;
							}
							// Found full harvest
							log.debug("fullharvest found w/id " + harvestDefinitionID);
							final String name = rs.getString("name");
							final String comments = rs.getString("comments");
							final int numEvents = rs.getInt("numevents");
							final Date submissionDate = 
									new Date(rs.getTimestamp("submitted").getTime());
							final long maxObjects = rs.getLong("maxobjects");
							final long maxBytes = rs.getLong("maxbytes");
							final long maxJobRunningtime = rs.getLong("maxjobrunningtime");
							final boolean isIndexReady = rs.getBoolean("isindexready");
							FullHarvest fh;
							final long prevhd = rs.getLong("previoushd");
							if (!rs.wasNull()) {
								fh = new FullHarvest(name, comments, prevhd, maxObjects,
										maxBytes, maxJobRunningtime, isIndexReady);
							} else {
								fh = new FullHarvest(name, comments, null, maxObjects,
										maxBytes, maxJobRunningtime, isIndexReady);
							}
							fh.setSubmissionDate(submissionDate);
							fh.setNumEvents(numEvents);
							fh.setActive(rs.getBoolean("isactive"));
							fh.setOid(harvestDefinitionID);
							fh.setEdition(rs.getLong("edition"));
							fh.setAudience(rs.getString("audience"));

							// We found a FullHarvest object, just return it.
							log.debug("Returned FullHarvest object w/ id " + harvestDefinitionID);
							return fh;
						}        			
						});

		if (fh != null) {
			return fh;
		}

		// No full harvest with that ID, try selective harvest
		PartialHarvest ph = query(
				"SELECT H.name as harvestName, H.comments, H.numevents, H.submitted, H.isactive"
						+ ", H.edition, H.audience, S.name as scheduleName, P.nextdate"
						+ " FROM harvestdefinitions H, partialharvests P, schedules S"
						+ " WHERE H.harvest_id=:harvestId"
						+ " AND H.harvest_id=P.harvest_id"
						+ " AND S.schedule_id=P.schedule_id",
						new ParameterMap("harvestId", harvestDefinitionID),
						new ResultSetExtractor<PartialHarvest>() {					@Override
							public PartialHarvest extractData(ResultSet rs)
									throws SQLException, DataAccessException {
							if (!rs.next()) {
								return null;
							}
							log.debug("Partialharvest found w/ id " + harvestDefinitionID);
							// Have to get configs before creating object, so storing data
							// here.
							final String name = rs.getString("harvestName");
							final String comments = rs.getString("comments");
							final int numEvents = rs.getInt("numevents");
							final Date submissionDate = 
									new Date(rs.getTimestamp("submitted").getTime());
							final boolean active = rs.getBoolean("isactive");
							final long edition = rs.getLong("edition");
							final String audience = rs.getString("audience");
							final String scheduleName = rs.getString("scheduleName");
							final Date nextDate = getDateKeepNull(rs, "nextDate");
							// Found partial harvest -- have to find configurations.
							// To avoid holding on to the readlock while getting domains,
							// we grab the strings first, then look up domains and configs.
							final DomainDAO domainDao = DomainDAO.getInstance();
							List<SparseDomainConfiguration> configs = query(
									"SELECT domains.name as domainName"
											+ ", configurations.name as configName"
											+ " FROM domains, configurations, harvest_configs"
											+ " WHERE harvest_id=:harvestId"
											+ " AND configurations.config_id=harvest_configs.config_id"
											+ " AND configurations.domain_id=domains.domain_id",
											new ParameterMap("harvestId", harvestDefinitionID),
											new RowMapper<SparseDomainConfiguration>() {
												@Override
												public SparseDomainConfiguration mapRow(
														ResultSet rs, int pos) throws SQLException {
													return new SparseDomainConfiguration(
															rs.getString("domainName"),
															rs.getString("configName"));
												}		                        	
											});
							List<DomainConfiguration> configurations 
							= new ArrayList<DomainConfiguration>();
							for (SparseDomainConfiguration domainConfig : configs) {
								configurations.add(domainDao.getDomainConfiguration(
										domainConfig.getDomainName(),
										domainConfig.getConfigurationName()));
							}

							Schedule schedule = 
									ScheduleDAO.getInstance().read(scheduleName);

							PartialHarvest ph = new PartialHarvest(configurations,
									schedule, name, comments, audience);

							ph.setNumEvents(numEvents);
							ph.setSubmissionDate(submissionDate);
							ph.setActive(active);
							ph.setEdition(edition);
							ph.setNextDate(nextDate);
							ph.setOid(harvestDefinitionID);
							return ph;
						}        			
						});

		if (ph != null) {
			return ph;
		} else {
			throw new IllegalState(
					"No entries in fullharvests or partialharvests found for id " 
							+ harvestDefinitionID);
		}
	}

	/**
	 * Update an existing harvest definition with new info.
	 * 
	 * @param hd An updated harvest definition
	 * @see HarvestDefinitionDAO#update(HarvestDefinition)
	 */
	public synchronized void update(final HarvestDefinition hd) {
		ArgumentNotValid.checkNotNull(hd, "HarvestDefinition hd");
		if (hd.getOid() == null || !exists(hd.getOid())) {
			final String message = "Cannot update non-existing "
					+ "harvestdefinition '" + hd.getName() + "'";
			log.debug(message);
			throw new PermissionDenied(message);
		}

		executeTransaction("doUpdate", HarvestDefinition.class, hd);
	}

	@SuppressWarnings("unused")
	private synchronized void doUpdate(final HarvestDefinition hd) {

		HarvestDefinition preHD = null;
		if (hd instanceof FullHarvest) {
			preHD = ((FullHarvest) hd).getPreviousHarvestDefinition();
		}

		// Execute base update        
		long nextEdition = hd.getEdition() + 1;
		int updateCount = executeUpdate(
				"UPDATE harvestdefinitions"
						+ " SET name=:name, comments=:comments, numevents=:numEvents"
						+ ", submitted=:submitted, isactive=isActive, edition=:nextEdition"
						+ ", audience=:audience"
						+ " WHERE harvest_id=:harvestId AND edition=:edition",
						new ParameterMap(
								"name", getStorableName(hd),
								"comments", getStorableComments(hd),
								"numEvents", hd.getNumEvents(),
								"submitted", new Timestamp(hd.getSubmissionDate().getTime()),
								"isActive", hd.getActive(),
								"nextEdition", nextEdition,
								"edition", hd.getEdition(),
								"audience", hd.getAudience(),
								"harvestId", hd.getOid()));
		// Since the HD exists, no rows indicates bad edition
		if (updateCount == 0) {
			String message = "Somebody else must have updated " + hd
					+ " since edition " + hd.getEdition()
					+ ", not updating";
			log.debug(message);
			throw new PermissionDenied(message);
		}

		// Next complement based on harvest type
		if (hd instanceof FullHarvest) {
			FullHarvest fh = (FullHarvest) hd;
			int rows = executeUpdate(
					"UPDATE fullharvests"
							+ " SET previoushd=:previousHd, maxobjects=:maxObjects"
							+ ", maxbytes=:maxBytes, maxjobrunningtime=:maxJobDuration"
							+ ", isindexready=:indexReady"
							+ " WHERE harvest_id=:harvestId",
							new ParameterMap(
									"previousHd", preHD != null ? preHD.getOid() : null,
									"maxObjects", fh.getMaxCountObjects(),
									"maxBytes", fh.getMaxBytes(),
									"maxJobDuration", fh.getMaxJobRunningTime(),
									"indexReady", fh.getIndexReady(),
									"harvestId", hd.getOid()));
			log.debug(rows + " fullharvests records updated");
		} else if (hd instanceof PartialHarvest) {
			PartialHarvest ph = (PartialHarvest) hd;
			int rows = executeUpdate(
					"UPDATE partialharvests"
							+ " SET schedule_id="
							+ " (SELECT schedule_id FROM schedules"
							+ " WHERE schedules.name=:scheduleName)"
							+ ", nextdate=:nextDate"
							+ " WHERE harvest_id=:harvestId",
							new ParameterMap(
									"scheduleName", ph.getSchedule().getName(),
									"nextDate", ph.getNextDate(),
									"harvestId", hd.getOid()));
			log.debug(rows + " partialharvests records updated");
			// FIXME The updates to harvest_configs table should be done
			// in method removeDomainConfiguration(), and not here.
			// The following deletes ALL harvest_configs entries for
			// this PartialHarvest, and creates the entries for the
			// PartialHarvest again!!
			createHarvestConfigsEntries(ph, ph.getOid());
		} else {
			String message = "Harvest definition " + hd
					+ " has unknown class " + hd.getClass();
			log.warn(message);
			throw new ArgumentNotValid(message);
		}
		hd.setEdition(nextEdition);
	}

	/**
	 * Activates or deactivates a partial harvest definition. This method is
	 * actually to be used not to have to read from the DB big harvest
	 * definitions and optimize the activation / deactivation, it is sort of a
	 * lightweight version of update.
	 * 
	 * @param harvestDefinition the harvest definition object.
	 */
	@Override
	public synchronized void flipActive(final SparsePartialHarvest harvestDefinition) {
		ArgumentNotValid.checkNotNull(
				harvestDefinition,
				"HarvestDefinition harvestDefinition");

		if (harvestDefinition.getOid() == null
				|| !exists(harvestDefinition.getOid())) {
			final String message = "Cannot update non-existing "
					+ "harvestdefinition '" + harvestDefinition.getName()
					+ "'";
			log.debug(message);
			throw new PermissionDenied(message);
		}
		
		executeTransaction("doFlipActive", SparsePartialHarvest.class, harvestDefinition);
	}
	
	@SuppressWarnings("unused")
	private synchronized void doFlipActive(final SparsePartialHarvest hd) {

		long nextEdition = hd.getEdition() + 1;
		int rows = executeUpdate(
				"UPDATE harvestdefinitions"
				+ " SET name=:name, comments=:comments, numevents=:numEvents"
				+ ", submitted=:submitted, isactive=:isActive, edition=:nextEdition"
				+ ", audience=:audience"
				+ " WHERE harvest_id=:harvestId AND edition=:edition",
				new ParameterMap(
						"name", getStorableName(hd),
						"comments", getStorableComments(hd),
						"numEvents", hd.getNumEvents(),
						"submitted", new Timestamp(hd.getSubmissionDate().getTime()),
						"isActive", !hd.isActive(),
						"nextEdition", nextEdition,
						"edition", hd.getEdition(),
						"audience", hd.getAudience(),
						"harvestId", hd.getOid()
						));
		// Since the HD exists, no rows indicates bad edition
		if (rows == 0) {
			String message = "Somebody else must have updated "
					+ hd + " since edition "
					+ hd.getEdition() + ", not updating";
			log.debug(message);
			throw new PermissionDenied(message);
		}

		// Now pull more strings
		rows = executeUpdate(
				"UPDATE partialharvests"
				+ " SET schedule_id="
				+ " (SELECT schedule_id FROM schedules"
				+ " WHERE schedules.name=:scheduleName)"
				+ ", nextdate=:nextDate"
				+ " WHERE harvest_id=:harvestId",
				new ParameterMap(
						"scheduleName", hd.getScheduleName(),
						"nextDate", hd.getNextDate(),
						"harvestId", hd.getOid()));
		log.debug(rows + " partialharvests records updated");
	}

	@Override
	public synchronized boolean exists(String name) {
		ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
		return 1 == queryIntValue(
				"SELECT COUNT(harvest_id) FROM harvestdefinitions WHERE name=:name", 
				new ParameterMap("name", name));
	}

	@Override
	public synchronized boolean exists(final Long oid) {
		ArgumentNotValid.checkNotNull(oid, "Long oid");        
		return 1 == queryIntValue( 
				"SELECT COUNT(harvest_id) FROM harvestdefinitions WHERE harvest_id=:id", 
				new ParameterMap("id", oid));
	}

	/**
	 * Get a list of all existing harvest definitions ordered by name.
	 * 
	 * @return An iterator that give the existing harvest definitions in turn
	 */
	@Override
	public synchronized Iterator<HarvestDefinition> getAllHarvestDefinitions() {
		List<Long> hds = queryLongList(
				"SELECT harvest_id FROM harvestdefinitions ORDER BY name");
		if (log.isDebugEnabled()) {
			log.debug("Getting an iterator for all stored harvestdefinitions.");
		}

		List<HarvestDefinition> orderedList = new LinkedList<HarvestDefinition>();
		for (Long id : hds) {
			orderedList.add(read(id));
		}
		return orderedList.iterator();
	}

	/**
	 * Gets default configurations for all domains that are not aliases.
	 * 
	 * This method currently gives an iterator that reads in all domains,
	 * although only on demand, that is: when calling "hasNext".
	 * 
	 * @return Iterator containing the default DomainConfiguration for all
	 *         domains that are not aliases
	 */
	@Override
	public synchronized Iterator<DomainConfiguration> getSnapShotConfigurations() {
		return new FilterIterator<Domain, DomainConfiguration>(DomainDAO
				.getInstance().getAllDomainsInSnapshotHarvestOrder()) {
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

	/**
	 * Returns a list of IDs of harvest definitions that are ready to be
	 * scheduled.
	 * 
	 * @param now The current date
	 * @return List of ready harvest definitions. No check is performed for
	 *         whether these are already in the middle of being scheduled.
	 */
	@Override
	public Iterable<Long> getReadyHarvestDefinitions(final Date now) {
		ArgumentNotValid.checkNotNull(now, "Date now");
		List<Long> ids = queryLongList(
				"SELECT fullharvests.harvest_id"
						+ " FROM fullharvests, harvestdefinitions"
						+ " WHERE harvestdefinitions.harvest_id=fullharvests.harvest_id"
						+ " AND isactive=true"
						+ " AND numevents < 1 "
						+ " AND isindexready=true");
		ids.addAll(queryLongList(
				"SELECT partialharvests.harvest_id"
						+ " FROM partialharvests, harvestdefinitions"
						+ " WHERE harvestdefinitions.harvest_id=partialharvests.harvest_id"
						+ " AND isactive=true"
						+ " AND nextdate IS NOT NULL"
						+ " AND nextdate < :nextDate",
						new ParameterMap("nextDate", now)));
		return ids;
	}

	/**
	 * Get the harvest definition that has the given name, if any.
	 * 
	 * @param name The name of a harvest definition.
	 * @return The HarvestDefinition object with that name, or null if none has
	 *         that name.
	 */
	@Override
	public synchronized HarvestDefinition getHarvestDefinition(String name) {
		ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
		log.debug("Reading harvestdefinition w/ name '" + name + "'");
		Long harvestId = queryLongValue(
				"SELECT harvest_id FROM harvestdefinitions WHERE name=:name",
				new ParameterMap("name", name));
		if (harvestId == null) {
			return null;
		}
		return read(harvestId);
	}

	@Override
	public List<HarvestRunInfo> getHarvestRunInfo(final long harvestId) {
		// Select dates and counts for all different statues
		// for each run
		ParameterMap idMap = new ParameterMap("harvestId", harvestId);
		HarvestRunInfoMapper hriMapper = new HarvestRunInfoMapper(harvestId);
		List<HarvestRunInfo> infoList = query(
				"SELECT name, harvest_num, status"
				+ ", MIN(startdate) as minStart"
				+ ", MAX(enddate) as maxEnd"
				+ ", COUNT(job_id) as jobCount"
				+ " FROM jobs, harvestdefinitions"
				+ " WHERE harvestdefinitions.harvest_id=:harvestId"
				+ " AND jobs.harvest_id=harvestdefinitions.harvest_id"
				+ " GROUP BY name, harvest_num, status"
				+ " ORDER BY harvest_num DESC",
				idMap,
				hriMapper);
		
		Map<Integer, HarvestRunInfo> runInfos = hriMapper.getAsMap();		
		query("SELECT jobs.harvest_num"
				+ ", SUM(historyinfo.bytecount) as byteCountSum"
				+ ", SUM(historyinfo.objectcount) as objectCountSum"
				+ ", COUNT(jobs.status) as statusCount"
				+ " FROM jobs, historyinfo "
				+ " WHERE jobs.harvest_id=:harvestId"
				+ " AND historyinfo.job_id=jobs.job_id"
				+ " GROUP BY jobs.harvest_num"
				+ " ORDER BY jobs.harvest_num",
				idMap,
				new HarvestRunInfoComplementer(harvestId, runInfos));

		// Make sure that jobs that aren't really done don't have end date.
		for (HarvestRunInfo info : infoList) {
			if (info.getJobCount(JobStatus.STARTED) != 0
					|| info.getJobCount(JobStatus.NEW) != 0
					|| info.getJobCount(JobStatus.SUBMITTED) != 0) {
				info.setEndDate(null);
			}
		}
		return infoList;
	}

	/**
	 * Get all domain,configuration pairs for a harvest definition in sparse
	 * version for GUI purposes.
	 * 
	 * @param harvestDefinitionID
	 *            The ID of the harvest definition.
	 * @return Domain,configuration pairs for that HD. Returns an empty iterable
	 *         for unknown harvest definitions.
	 * @throws ArgumentNotValid
	 *             on null argument.
	 */
	@Override
	public List<SparseDomainConfiguration> getSparseDomainConfigurations(
			final Long harvestDefinitionID) {
		ArgumentNotValid.checkNotNull(harvestDefinitionID, "harvestDefinitionID");
		List<SparseDomainConfiguration> resultList = query(
				"SELECT domains.name, configurations.name"
				+ " FROM domains, configurations, harvest_configs"
				+ " WHERE harvest_id=:harvestId"
				+ " AND configurations.config_id=harvest_configs.config_id"
				+ " AND configurations.domain_id=domains.domain_id",
				new ParameterMap("harvestId", harvestDefinitionID),
				new RowMapper<SparseDomainConfiguration>() {
					@Override
					public SparseDomainConfiguration mapRow(ResultSet rs, int pos)
							throws SQLException {
						return new SparseDomainConfiguration(
								rs.getString(1), rs.getString(2));
					}
					
				});
		Collections.sort(resultList,
				new SparseDomainConfigurationComparator());
		return resultList;
	}

	/**
	 * Get all sparse versions of partial harvests for GUI purposes ordered by
	 * name.
	 * 
	 * @return An iterable (possibly empty) of SparsePartialHarvests
	 */
	public Iterable<SparsePartialHarvest> getSparsePartialHarvestDefinitions(
			final boolean excludeInactive) {
		return query(
				"SELECT harvestdefinitions.harvest_id"
				+ ", harvestdefinitions.name as harvestName"
				+ ", harvestdefinitions.comments, harvestdefinitions.numevents"
				+ ", harvestdefinitions.submitted, harvestdefinitions.isactive"
				+ ", harvestdefinitions.edition"
				+ ", schedules.name as scheduleName, partialharvests.nextdate"
				+ ", harvestdefinitions.audience, harvestdefinitions.channel_id"
				+ " FROM harvestdefinitions, partialharvests, schedules"
				+ " WHERE harvestdefinitions.harvest_id=partialharvests.harvest_id"
				+ " AND (harvestdefinitions.isactive=true"
				// This line is duplicated to allow to select both active
				// and inactive HD's.
				+ " OR harvestdefinitions.isactive=:excludeInactive)"
				+ " AND schedules.schedule_id=partialharvests.schedule_id"
				+ " ORDER BY harvestdefinitions.name",
				new ParameterMap("excludeInactive", excludeInactive),
				new RowMapper<SparsePartialHarvest>() {
					@Override
					public SparsePartialHarvest mapRow(ResultSet rs, int pos)
							throws SQLException {
						return mapSparsePartialHarvest(rs);
					}					
				});
	}

	/**
	 * Get a sparse version of a partial harvest for GUI purposes.
	 * 
	 * @param harvestName
	 *            Name of harvest definition.
	 * @return Sparse version of partial harvest or null for none.
	 * @throws ArgumentNotValid
	 *             on null or empty name.
	 */
	@Override
	public SparsePartialHarvest getSparsePartialHarvest(final String harvestName) {
		ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
		
		return query(
				"SELECT harvestdefinitions.harvest_id, harvestdefinitions.comments"
				+ ", harvestdefinitions.name as harvestName"
				+ ", harvestdefinitions.numevents, harvestdefinitions.submitted"
				+ ", harvestdefinitions.isactive, harvestdefinitions.edition"
				+ ", schedules.name as scheduleName"
				+ ", partialharvests.nextdate, harvestdefinitions.audience"
				+ ", harvestdefinitions.channel_id"
				+ " FROM harvestdefinitions, partialharvests, schedules"
				+ " WHERE harvestdefinitions.name=:name"
				+ " AND harvestdefinitions.harvest_id=partialharvests.harvest_id"
				+ " AND schedules.schedule_id=partialharvests.schedule_id",
				new ParameterMap("name", harvestName),
				new ResultSetExtractor<SparsePartialHarvest>() {
					@Override
					public SparsePartialHarvest extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						if (!rs.next()) {
							return null;
						}
						return mapSparsePartialHarvest(rs);
					}					
				});
	}

	/**
	 * Get all sparse versions of full harvests for GUI purposes.
	 * 
	 * @return An iterable (possibly empty) of SparseFullHarvests
	 */
	public Iterable<SparseFullHarvest> getAllSparseFullHarvestDefinitions() {
		return query(
				"SELECT harvestdefinitions.harvest_id, harvestdefinitions.name"
				+ ", harvestdefinitions.comments, harvestdefinitions.numevents"
				+ ", harvestdefinitions.isactive, harvestdefinitions.edition"
				+ ", fullharvests.maxobjects, fullharvests.maxbytes"
				+ ", fullharvests.maxjobrunningtime, fullharvests.previoushd"
				+ ", harvestdefinitions.channel_id"
				+ " FROM harvestdefinitions, fullharvests"
				+ " WHERE harvestdefinitions.harvest_id=fullharvests.harvest_id",
				new RowMapper<SparseFullHarvest>() {
					@Override
					public SparseFullHarvest mapRow(ResultSet rs, int pos)
							throws SQLException {
						return mapSparseFullHarvest(rs);
					}					
				});
	}

	/**
	 * Get the name of a harvest given its ID.
	 * 
	 * @param harvestDefinitionID The ID of a harvest
	 * 
	 * @return The name of the given harvest.
	 * 
	 * @throws ArgumentNotValid on null argument
	 * @throws UnknownID if no harvest has the given ID.
	 * @throws IOFailure on any other error talking to the database
	 */
	@Override
	public String getHarvestName(final Long harvestDefinitionID) {
		ArgumentNotValid.checkNotNull(harvestDefinitionID, "harvestDefinitionID");
		String name = queryStringValue(
				"SELECT name FROM harvestdefinitions WHERE harvest_id=:id",
				new ParameterMap("id", harvestDefinitionID));
			
		if (name == null) {
			throw new UnknownID("No name found for harvest definition " + harvestDefinitionID);
		}
		return name;
	}

	/**
	 * Get whether a given harvest is a snapshot or selective harvest.
	 * 
	 * @param harvestDefinitionID ID of a harvest
	 * 
	 * @return True if the given harvest is a snapshot harvest, false otherwise.
	 * 
	 * @throws ArgumentNotValid on null argument
	 * @throws UnknownID if no harvest has the given ID.
	 */
	@Override
	public boolean isSnapshot(final Long harvestDefinitionID) {
		ArgumentNotValid.checkNotNull(harvestDefinitionID, "harvestDefinitionID");
		ParameterMap idParam = new ParameterMap("id", harvestDefinitionID);
		Long id = queryLongValue(
				"SELECT harvest_id FROM fullharvests WHERE harvest_id=:id",
				idParam);
		if (id == null) {
			return true;
		}
		id = queryLongValue(
				"SELECT harvest_id FROM partialharvests WHERE harvest_id=:id", 
				idParam);
		if (id != null) {
			return false;
		}
		throw new UnknownID("Failed to find harvest definition with id " + harvestDefinitionID);
	}

	/**
	 * Get a sparse version of a full harvest for GUI purposes.
	 * 
	 * @param harvestName Name of harvest definition.
	 * @return Sparse version of full harvest or null for none.
	 * @throws ArgumentNotValid on null or empty name.
	 * @throws UnknownID if no harvest has the given ID.
	 */
	@Override
	public SparseFullHarvest getSparseFullHarvest(final String harvestName) {
		ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
		return query(
				"SELECT harvestdefinitions.harvest_id, harvestdefinitions.comments"
				+ ", harvestdefinitions.name, harvestdefinitions.numevents"
				+ ", harvestdefinitions.isactive, harvestdefinitions.edition"
				+ ", fullharvests.maxobjects, fullharvests.maxbytes"
				+ ", fullharvests.maxjobrunningtime, fullharvests.previoushd"
				+ ", harvestdefinitions.channel_id"
				+ " FROM harvestdefinitions, fullharvests"
				+ " WHERE harvestdefinitions.name=:name"
				+ " AND harvestdefinitions.harvest_id=fullharvests.harvest_id",
				new ParameterMap("name", harvestName),
				new ResultSetExtractor<SparseFullHarvest>() {
					@Override
					public SparseFullHarvest extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						if (!rs.next()) {
							return null;
						}
						return mapSparseFullHarvest(rs);
					}					
				});
	}

	/**
	 * Get a sorted list of all domain names of a HarvestDefinition.
	 * 
	 * @param harvestName
	 *            of HarvestDefinition
	 * @return List of all domains of the HarvestDefinition.
	 */
	@Override
	public List<String> getListOfDomainsOfHarvestDefinition(final String harvestName) {
		ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
		// Note: the DISTINCT below is put in deliberately to fix
		// bug 1878: Seeds for domain is shown twice on page
		// History/Harveststatus-seeds.jsp
		return queryStringList(
				"SELECT DISTINCT domains.name"
				+ " FROM domains, configurations, harvest_configs, harvestdefinitions"
				+ " WHERE configurations.domain_id=domains.domain_id"
				+ " AND harvest_configs.config_id=configurations.config_id"
				+ " AND harvest_configs.harvest_id=harvestdefinitions.harvest_id"
				+ " AND harvestdefinitions.name=:name"
				+ " ORDER BY domains.name",
				new ParameterMap("name", harvestName));
	}

	/**
	 * Get a sorted list of all seeds of a Domain in a HarvestDefinition.
	 * 
	 * @param harvestName
	 *            of HarvestDefinition
	 * @param domainName
	 *            of Domain
	 * @return List of all seeds of the Domain in the HarvestDefinition.
	 */
	@Override
	public List<String> getListOfSeedsOfDomainOfHarvestDefinition(
			final String harvestName, 
			final String domainName) {
		ArgumentNotValid.checkNotNullOrEmpty(harvestName, "harvestName");
		ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
		Set<String> seeds = query(
				"SELECT seedlists.seeds"
				+ " FROM configurations, harvest_configs, harvestdefinitions"
				+ ", seedlists, config_seedlists, domains"
				+ " WHERE config_seedlists.seedlist_id=seedlists.seedlist_id"
				+ " AND configurations.config_id=config_seedlists.config_id"
				+ " AND configurations.config_id=harvest_configs.config_id"
				+ " AND harvest_configs.harvest_id=harvestdefinitions.harvest_id"
				+ " AND configurations.domain_id=domains.domain_id"
				+ " AND domains.name=:domainName"
				+ " AND harvestdefinitions.name=:harvestName",
				new ParameterMap(
						"domainName", domainName,
						"harvestName", harvestName),
				new ResultSetExtractor<Set<String>>() {
					@Override
					public Set<String> extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						Set<String> seeds = new HashSet<String>();
						while (rs.next()) {
							StringTokenizer st = new StringTokenizer(
									rs.getString("seeds"), "\n");
							while (st.hasMoreTokens()) {
								seeds.add(st.nextToken());
							}
						}
						return seeds;
					}
				});
		List<String> list = new ArrayList<String>();
		list.addAll(seeds);
		Collections.sort(list, Collator.getInstance());
		return list;
	}

	@Override
	public Set<Long> getJobIdsForSnapshotDeduplicationIndex(
			final Long harvestId) {
		ArgumentNotValid.checkNotNull(harvestId, "Long harvestId");
		Set<Long> jobIds = new HashSet<Long>();
		if (!isSnapshot(harvestId)) {
			throw new NotImplementedException(
					"This functionality only works for snapshot harvests");
		}
		List<Long> harvestDefinitions = getPreviousFullHarvests(harvestId);
		
		List<Long> jobs = new ArrayList<Long>();
		if (!harvestDefinitions.isEmpty()) {
			// Select all jobs from a given list of harvest definitions
			jobs.addAll(queryLongList(
					"SELECT jobs.job_id FROM jobs"
					+ " WHERE jobs.harvest_id IN ("
					+ StringUtils.conjoin(",", harvestDefinitions)
					+ ")"));
		}
		jobIds.addAll(jobs);

		return jobIds;
	}

	/**
	 * Get list of harvests previous to this one.
	 * @param thisHarvest The id of this harvestdefinition
	 * @return a list of IDs belonging to harvests previous to this one.
	 */
	private List<Long> getPreviousFullHarvests(final Long thisHarvest) {
		List<Long> results = new ArrayList<Long>();
		// Follow the chain of originating IDs back
		for (Long originatingHarvest = thisHarvest; originatingHarvest != null;
				// Compute next originatingHarvest
				originatingHarvest = queryLongValue(
						"SELECT previoushd FROM fullharvests"
						+ " WHERE fullharvests.harvest_id=:id",
						new ParameterMap("id", originatingHarvest))) {
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
		Long olderHarvest = queryLongValue(
				"SELECT fullharvests.harvest_id"
				+ " FROM fullharvests, harvestdefinitions, harvestdefinitions AS currenthd"
				+ " WHERE currenthd.harvest_id=:harvestId"
				+ " AND fullharvests.harvest_id= harvestdefinitions.harvest_id"
				+ " AND harvestdefinitions.submitted < currenthd.submitted"
				+ " ORDER BY harvestdefinitions.submitted"
				+ " "  + HarvestStatusQuery.SORT_ORDER.DESC.name(),
				new ParameterMap("harvestId", firstHarvest));
		// Follow the chain of originating IDs back
		for (Long originatingHarvest = olderHarvest; originatingHarvest != null;
				originatingHarvest = queryLongValue(
						"SELECT previoushd FROM fullharvests"
						+ " WHERE fullharvests.harvest_id=:id",
						new ParameterMap("id", originatingHarvest))) {
			results.add(originatingHarvest);
		}
		return results;
	}

	@Override
	public void setIndexIsReady(final Long harvestId, final boolean newValue) {
		if (!isSnapshot(harvestId)) {
			throw new NotImplementedException(
					"Not implemented for non snapshot harvests");
		} else {
			executeTransaction(
					"doSetIndexIsReady", 
					Long.class, harvestId,
					Boolean.class, newValue);
		}
	}
	
	@SuppressWarnings("unused")
	private void doSetIndexIsReady(final Long harvestId, final Boolean newValue) {
			executeUpdate(
					"UPDATE fullharvests"
					+ " SET isindexready=:indexReady WHERE harvest_id=:id",
					new ParameterMap(
							"indexReady", newValue,
							"id", harvestId));
	}

	/*
	 * Removes the entry in harvest_configs, that binds a certain
	 * domainconfiguration to this PartialHarvest.
	 * TODO maybe update the edition as well.
	 */
	@Override
	public void removeDomainConfiguration(
			final Long harvestId,
			final SparseDomainConfiguration key) {
		ArgumentNotValid.checkNotNull(key, "DomainConfigurationKey key");
		if (harvestId == null) {
			// Don't need to do anything, if PartialHarvest is not
			// yet stored in database
			log.warn("No removal of domainConfiguration, "
					+ "as harvestId is null");
			return;
		}
		executeTransaction(
				"doRemoveDomainConfiguration", 
				Long.class, harvestId,
				SparseDomainConfiguration.class, key);
	}
	
	@SuppressWarnings("unused")
	private void doRemoveDomainConfiguration(
			final Long harvestId,
			final SparseDomainConfiguration key) {
		executeUpdate(
				"DELETE FROM harvest_configs WHERE harvest_id=:id"
				+ " AND config_id=(SELECT config_id FROM configurations, domains"
				+ " WHERE domains.name=:domainName AND configurations.name=:configName"
				+ " AND domains.domain_id=configurations.domain_id)",
				new ParameterMap(
						"id", harvestId,
						"domainName", key.getDomainName(),
						"confName", key.getConfigurationName()));
	}

	@Override
	public void updateNextdate(final long harvestId, final Date nextdate) {
		ArgumentNotValid.checkNotNull(harvestId, "Long harvest ID");
		ArgumentNotValid.checkNotNull(nextdate, "Date nextdate");
		if (harvestId < 0) {
			// Don't need to do anything, if PartialHarvest is not
			// yet stored in database
			return;
		}
		executeTransaction(
				"doUpdateNextdate",
				Long.class, harvestId,
				Date.class, nextdate);
	}
	
	@SuppressWarnings("unused")
	private void doUpdateNextdate(final Long harvestId, final Date nextdate) {
		executeUpdate(
				"UPDATE partialharvests SET nextdate=:nextDate WHERE harvest_id=:id",
				new ParameterMap(
						"nextDate", nextdate,
						"id", harvestId));
	}

	@Override
	public void addDomainConfiguration(
			final PartialHarvest ph,
			final SparseDomainConfiguration dcKey) {
		ArgumentNotValid.checkNotNull(ph, "PartialHarvest ph");
		ArgumentNotValid.checkNotNull(dcKey, "DomainConfigurationKey dcKey");
		executeTransaction(
				"doAddDomainConfiguration",
				PartialHarvest.class, ph,
				SparseDomainConfiguration.class, dcKey);
	}	
	
	@SuppressWarnings("unused")
	private void doAddDomainConfiguration(
			final PartialHarvest ph,
			final SparseDomainConfiguration dcKey) {
		executeUpdate(
				"INSERT INTO harvest_configs (harvest_id, config_id)"
				+ " SELECT :id, config_id FROM configurations, domains"
				+ " WHERE domains.name=:domainName AND configurations.name=:confName"
				+ "  AND domains.domain_id=configurations.domain_id",
				new ParameterMap(
						"id", ph.getOid(),
						"domainName", dcKey.getDomainName(),
						"confName", dcKey.getConfigurationName()));
	}

	@Override
	public void mapToHarvestChannel(
			final long harvestDefinitionId,
			final HarvestChannel channel) {
		ArgumentNotValid.checkNotNull(channel, "HarvestChannel channel");
		executeTransaction(
				"doMapToHarvestChannel",
				Long.class, harvestDefinitionId,
				Long.class, channel.getId());
	}
	
	@SuppressWarnings("unused")
	private void doMapToHarvestChannel(
			final long harvestDefinitionId,
			final long channelId) {
		executeUpdate(
				"UPDATE harvestdefinitions"
				+ " SET channel_id=:channelId WHERE harvest_id=:harvestId",
				new ParameterMap(
						"channelId", channelId,
						"harvestId", harvestDefinitionId));
	}
	
	private SparsePartialHarvest mapSparsePartialHarvest(ResultSet rs) throws SQLException {
		return new SparsePartialHarvest(
				rs.getLong("harvest_id"), 
				rs.getString("harvestName"), 
				rs.getString("comments"),
				rs.getInt("numevents"), 
				new Date(rs.getTimestamp("submitted").getTime()),
				rs.getBoolean("isactive"), 
				rs.getLong("edition"), 
				rs.getString("scheduleName"),
				getDateKeepNull(rs, "nextdate"), 
				rs.getString("audience"), 
				getLongKeepNull(rs, "channel_id"));
	}
	
	private SparseFullHarvest mapSparseFullHarvest(ResultSet rs) throws SQLException {
		return new SparseFullHarvest(
				rs.getLong("harvest_id"),
				rs.getString("name"), 
				rs.getString("comments"), 
				rs.getInt("numevents"),
				rs.getBoolean("isactive"), 
				rs.getLong("edition"), 
				rs.getLong("maxobjects"),
				rs.getLong("maxbytes"), 
				rs.getLong("maxjobrunningtime"),
				getLongKeepNull(rs, "previoushd"),
				getLongKeepNull(rs, "channel_id"));
	}

}
