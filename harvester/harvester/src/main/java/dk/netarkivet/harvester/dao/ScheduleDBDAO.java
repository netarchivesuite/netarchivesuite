/* File:        $Id: ScheduleDBDAO.java 2251 2012-02-08 13:03:03Z mss $
 * Revision:    $Revision: 2251 $
 * Author:      $Author: mss $
 * Date:        $Date: 2012-02-08 14:03:03 +0100 (Wed, 08 Feb 2012) $
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FilterIterator;
import dk.netarkivet.harvester.datamodel.Constants;
import dk.netarkivet.harvester.datamodel.Frequency;
import dk.netarkivet.harvester.datamodel.RepeatingSchedule;
import dk.netarkivet.harvester.datamodel.Schedule;
import dk.netarkivet.harvester.datamodel.TimedSchedule;


/**
 * A database-based implementation of the ScheduleDAO.
 *
 * The statements to create the tables are now in
 * scripts/sql/createfullhddb.sql and scripts/sql/createfullhddb.mysql.
 */
public class ScheduleDBDAO extends ScheduleDAO {

	/** The logger. */
	private final Log log = LogFactory.getLog(getClass());

	@Override
	protected Collection<HarvesterDatabaseTables> getRequiredTables() {
		return Collections.singletonList(HarvesterDatabaseTables.SCHEDULES);
	}

	/**
	 * Create a new schedule.
	 *
	 * @param schedule The schedule to create
	 * @throws ArgumentNotValid if schedule is null
	 * @throws PermissionDenied if a schedule already exists
	 */
	public void create(Schedule schedule) {
		ArgumentNotValid.checkNotNull(schedule, "schedule");

		if (exists(schedule.getName())) {
			String msg = "Cannot create already existing schedule "
					+ schedule;
			log.debug(msg);
			throw new PermissionDenied(msg);
		}

		executeTransaction("doCreate", Schedule.class, schedule);
	}

	@SuppressWarnings("unused")
	private synchronized void doCreate(Schedule schedule) throws SQLException {
		Map<String, Object> generatedKeys = new HashMap<String, Object>();
		ParameterMap params = getScheduleParameters(schedule);
		params.put("edition", 1);
		executeUpdateGetGeneratedKeys(
				"INSERT INTO schedules(name, comments, startdate, enddate, maxrepeats,"
						+ " timeunit, numtimeunits, anytime, onminute, onhour,"
						+ " ondayofweek, ondayofmonth, edition)"
						+ " VALUES (:name, :comments, :startdate, :enddate, :maxrepeats,"
						+ " :timeunit, :numtimeunits, :anytime, :onminute, :onhour,"
						+ " :ondayofweek, :ondayofmonth, :edition)",
						params,
						generatedKeys);
		schedule.setID((Long) generatedKeys.get("schedule_id"));
		schedule.setEdition(1);
	}

	/** Sets the first twelve parameters of a Schedule in the order.
	 * name, comments, startdate, enddate, maxrepeats,
	 * timeunit, numtimeunits, anytime, onminute, onhour,
	 * ondayofweek, ondayofmonth
	 * @param schedule a given schedule.
	 * @throws SQLException If the operation fails.
	 */
	private ParameterMap getScheduleParameters(Schedule schedule)
			throws SQLException {

		Frequency freq = schedule.getFrequency();

		ParameterMap pm = new ParameterMap(
				"name", getMaxLengthStringValue(
						schedule, "name", schedule.getName(), Constants.MAX_NAME_SIZE),
						"comments", getMaxLengthStringValue(
								schedule, "comments", 
								schedule.getComments(), Constants.MAX_COMMENT_SIZE),
						"startdate", schedule.getStartDate(),
						"enddate", null,
						"maxrepeats", null,
						"timeunit", schedule.getFrequency().ordinal(),
						"numtimeunits", freq.getNumUnits(),
						"anytime", freq.isAnytime(),
						"onminute", freq.getOnMinute(),
						"onhour", freq.getOnHour(),
						"ondayofweek", freq.getOnDayOfWeek(),
						"ondayofmonth", freq.getOnDayOfMonth());

		if (schedule instanceof TimedSchedule) {
			TimedSchedule ts = (TimedSchedule) schedule;
			pm.put("enddate", ts.getEndDate());
		} else {
			RepeatingSchedule rs = (RepeatingSchedule) schedule;
			pm.put("maxrepeats", rs.getRepeats());
		}

		return pm;
	}

	/**
	 * Returns whether a named schedule exists.
	 * @param scheduleName The name of a schedule
	 * @return True if the schedule exists.
	 */
	public synchronized boolean exists(String scheduleName) {
		return 1 == queryIntValue(
				"SELECT COUNT(*) FROM schedules WHERE name=:name",
				new ParameterMap("name", scheduleName));
	}

	/**
	 * Read an existing schedule.
	 *
	 * @param scheduleName the name of the schedule
	 * @return The schedule read
	 * @throws ArgumentNotValid if schedulename is null or empty
	 * @throws UnknownID        if the schedule doesn't exist
	 */
	public synchronized Schedule read(final String scheduleName) {
		ArgumentNotValid.checkNotNullOrEmpty(
				scheduleName, "String scheduleName");
		return query("SELECT schedule_id, comments, startdate,"
				+ " enddate, maxrepeats, timeunit,"
				+ " numtimeunits, anytime, onminute,"
				+ " onhour, ondayofweek, ondayofmonth, edition"
				+ " FROM schedules WHERE name=:name",
				new ParameterMap("name", scheduleName),
				new ResultSetExtractor<Schedule>() {
					@Override
					public Schedule extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						if (!rs.next()) {
							throw new UnknownID("No schedule named '"
									+ scheduleName + "' found");
						}
						long id = rs.getLong("schedule_id");
						boolean isTimedSchedule;
						String comments = rs.getString("comments");
						Date startdate = getDateKeepNull(rs, "startdate");
						Date enddate = getDateKeepNull(rs, "enddate");
						Integer maxrepeats = getIntegerKeepNull(rs, "maxrepeats");
						isTimedSchedule = maxrepeats == null;
						int timeunit = rs.getInt("timeunit");
						int numtimeunits = rs.getInt("numtimeunits");
						boolean anytime = rs.getBoolean("anytime");
						Integer minute = getIntegerKeepNull(rs, "onminute");
						Integer hour = getIntegerKeepNull(rs, "onhour");
						Integer dayofweek = getIntegerKeepNull(rs, "ondayofweek");
						Integer dayofmonth = getIntegerKeepNull(rs, "ondayofmonth");
						log.debug("Creating frequency for "
								+ "(timeunit,anytime,numtimeunits,hour, minute, dayofweek,"
								+ "dayofmonth) = (" + timeunit + ", "
								+ anytime + ","
								+ numtimeunits + ","
								+ minute + ","
								+ hour + ","
								+ dayofweek + ","
								+ dayofmonth + ","
								+ ")");
						Frequency freq = Frequency.getNewInstance(timeunit, anytime,
								numtimeunits, minute, hour, dayofweek, dayofmonth);
						long edition = rs.getLong("edition");
						final Schedule schedule;
						if (isTimedSchedule) {
							schedule = Schedule.getInstance(
									startdate, enddate, freq, scheduleName, comments);
						} else {
							schedule = Schedule.getInstance(
									startdate, maxrepeats, freq, scheduleName, comments);
						}
						schedule.setID(id);
						schedule.setEdition(edition);
						return schedule;
					}

				});
	}

	/**
	 * Update a schedule in the DAO.
	 *
	 * @param schedule The schedule to update
	 * @throws ArgumentNotValid If the schedule is null
	 * @throws UnknownID        If the schedule doesn't exist in the DAO
	 * @throws PermissionDenied  If the edition of the schedule to update is
	 *                          older than the DAO's
	 */
	public synchronized void update(Schedule schedule) {
		ArgumentNotValid.checkNotNull(schedule, "schedule");
		
		if (!exists(schedule.getName())) {
			throw new PermissionDenied("No schedule with name "
					+ schedule.getName() + " exists");
		}
	}
	
	@SuppressWarnings("unused")
	private synchronized void doUpdate(Schedule schedule) throws SQLException {

		ParameterMap pm = getScheduleParameters(schedule);
		long edition = schedule.getEdition();
		pm.put("oldEdition", edition);
		pm.put("newEdition", edition + 1);
		
		int rows = executeUpdate("UPDATE schedules SET name=:name,"
				+ " comments=:comments, startdate=:startdate, enddate=:enddate,"
				+ " maxrepeats=:maxrepeats, timeunit=:timeunit, numtimeunits=:numtimeunits,"
				+ " anytime=:anytime, onminute=:onminute, onhour=:onhour,"
				+ " ondayofweek=:ondayofweek, ondayofmonth=:ondayofmonth, edition=:newEdition"
				+ " WHERE name=:name AND edition=:oldEdition",
				pm);
		if (rows == 0) {
			String message = "Edition " + schedule.getEdition()
					+ " has expired, cannot update " + schedule;
			log.debug(message);
			throw new PermissionDenied(message);
		}
		schedule.setEdition(edition + 1);
	}

	/**
	 * Get iterator to all available schedules.
	 *
	 * @return iterator to all available schedules
	 */
	public synchronized Iterator<Schedule> getAllSchedules() {
		List<String> names = queryStringList("SELECT name FROM schedules ORDER BY name");
		return new FilterIterator<String, Schedule>(names.iterator()) {
			/**
			 * Returns the object corresponding to the given object,
			 *  or null if that object is to be skipped.
			 *
			 * @param s An object in the source iterator domain
			 * @return An object in this iterators domain, or null
			 */
			public Schedule filter(String s) {
				return read(s);
			}
		};
	}

	@Override
	public synchronized int getCountSchedules() {
		return queryIntValue("SELECT COUNT(*) FROM schedules");
	}

}
