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
import java.sql.Types;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FilterIterator;

/**
 * A database-based implementation of the ScheduleDAO.
 * <p>
 * The statements to create the tables are now in scripts/sql/createfullhddb.sql and scripts/sql/createfullhddb.mysql.
 */
public class ScheduleDBDAO extends ScheduleDAO {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(ScheduleDBDAO.class);

    /**
     * Constructor for this class, that only checks that the schedules table has the expected version.
     */
    protected ScheduleDBDAO() {
        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.SCHEDULES);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Create a new schedule.
     *
     * @param schedule The schedule to create
     * @throws ArgumentNotValid if schedule is null
     * @throws PermissionDenied if a schedule already exists
     */
    public synchronized void create(Schedule schedule) {
        ArgumentNotValid.checkNotNull(schedule, "schedule");

        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            if (exists(c, schedule.getName())) {
                String msg = "Cannot create already existing schedule " + schedule;
                log.debug(msg);
                throw new PermissionDenied(msg);
            }

            s = c.prepareStatement("INSERT INTO schedules " + "( name, comments, startdate, enddate, maxrepeats, "
                    + "timeunit, numtimeunits, anytime, onminute, onhour," + " ondayofweek, ondayofmonth, edition )"
                    + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )", Statement.RETURN_GENERATED_KEYS);
            setScheduleParameters(s, schedule);
            final long edition = 1;
            s.setLong(13, edition);
            s.executeUpdate();
            schedule.setID(DBUtils.getGeneratedID(s));
            schedule.setEdition(edition);
        } catch (SQLException e) {
            throw new IOFailure("SQL error while creating schedule " + schedule + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Sets the first twelve parameters of a Schedule in the order. name, comments, startdate, enddate, maxrepeats,
     * timeunit, numtimeunits, anytime, onminute, onhour, ondayofweek, ondayofmonth
     *
     * @param s a prepared SQL statement
     * @param schedule a given schedule.
     * @throws SQLException If the operation fails.
     */
    private void setScheduleParameters(PreparedStatement s, Schedule schedule) throws SQLException {
        DBUtils.setName(s, 1, schedule, Constants.MAX_NAME_SIZE);
        DBUtils.setComments(s, 2, schedule, Constants.MAX_COMMENT_SIZE);
        final Date startDate = schedule.getStartDate();
        final int fieldNum = 3;
        DBUtils.setDateMaybeNull(s, fieldNum, startDate);
        if (schedule instanceof TimedSchedule) {
            TimedSchedule ts = (TimedSchedule) schedule;
            DBUtils.setDateMaybeNull(s, 4, ts.getEndDate());
            s.setNull(5, Types.BIGINT);
        } else {
            s.setNull(4, Types.DATE);
            RepeatingSchedule rs = (RepeatingSchedule) schedule;
            s.setLong(5, rs.getRepeats());
        }
        Frequency freq = schedule.getFrequency();
        s.setInt(6, freq.ordinal());
        s.setInt(7, freq.getNumUnits());
        s.setBoolean(8, freq.isAnytime());
        DBUtils.setIntegerMaybeNull(s, 9, freq.getOnMinute());
        DBUtils.setIntegerMaybeNull(s, 10, freq.getOnHour());
        DBUtils.setIntegerMaybeNull(s, 11, freq.getOnDayOfWeek());
        DBUtils.setIntegerMaybeNull(s, 12, freq.getOnDayOfMonth());
    }

    /**
     * Returns whether a named schedule exists.
     *
     * @param scheduleName The name of a schedule
     * @return True if the schedule exists.
     * @throws ArgumentNotValid if the schedulename is null or empty
     */
    public synchronized boolean exists(String scheduleName) {
        ArgumentNotValid.checkNotNullOrEmpty(scheduleName, "String scheduleName");

        Connection c = HarvestDBConnection.get();
        try {
            return exists(c, scheduleName);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Returns whether a named schedule exists.
     *
     * @param c An open connection to the harvestDatabase.
     * @param scheduleName The name of a schedule
     * @return True if the schedule exists.
     */
    private synchronized boolean exists(Connection c, String scheduleName) {
        final int count = DBUtils.selectIntValue(c, "SELECT COUNT(*) FROM schedules WHERE name = ?", scheduleName);
        return (1 == count);
    }

    /**
     * Read an existing schedule.
     *
     * @param scheduleName the name of the schedule
     * @return The schedule read
     * @throws ArgumentNotValid if schedulename is null or empty
     * @throws UnknownID if the schedule doesn't exist
     */
    public synchronized Schedule read(String scheduleName) {
        ArgumentNotValid.checkNotNullOrEmpty(scheduleName, "String scheduleName");
        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("SELECT schedule_id, comments, startdate, " + "enddate, maxrepeats, timeunit, "
                    + "numtimeunits, anytime, onminute, " + "onhour, ondayofweek, ondayofmonth, edition "
                    + "FROM schedules WHERE name = ?");
            s.setString(1, scheduleName);
            ResultSet rs = s.executeQuery();
            if (!rs.next()) {
                throw new UnknownID("No schedule named '" + scheduleName + "' found");
            }
            long id = rs.getLong(1);
            boolean isTimedSchedule;
            String comments = rs.getString(2);
            Date startdate = DBUtils.getDateMaybeNull(rs, 3);
            Date enddate = DBUtils.getDateMaybeNull(rs, 4);
            int maxrepeats = rs.getInt(5);
            isTimedSchedule = rs.wasNull();
            int timeunit = rs.getInt(6);
            int numtimeunits = rs.getInt(7);
            boolean anytime = rs.getBoolean(8);
            Integer minute = DBUtils.getIntegerMaybeNull(rs, 9);
            Integer hour = DBUtils.getIntegerMaybeNull(rs, 10);
            Integer dayofweek = DBUtils.getIntegerMaybeNull(rs, 11);
            Integer dayofmonth = DBUtils.getIntegerMaybeNull(rs, 12);
            if (log.isDebugEnabled()) {
                log.debug("Creating frequency for (timeunit,anytime,numtimeunits,hour, minute, dayofweek, dayofmonth)"
                        + " = ({},{},{},{},{},{},{},)", timeunit, anytime, numtimeunits, minute, hour, dayofweek,
                        dayofmonth);
            }
            Frequency freq = Frequency.getNewInstance(timeunit, anytime, numtimeunits, minute, hour, dayofweek,
                    dayofmonth);
            long edition = rs.getLong(13);
            final Schedule schedule;
            if (isTimedSchedule) {
                schedule = Schedule.getInstance(startdate, enddate, freq, scheduleName, comments);
            } else {
                schedule = Schedule.getInstance(startdate, maxrepeats, freq, scheduleName, comments);
            }
            schedule.setID(id);
            schedule.setEdition(edition);
            return schedule;
        } catch (SQLException e) {
            throw new IOFailure("SQL error reading schedule " + scheduleName + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Update a schedule in the DAO.
     *
     * @param schedule The schedule to update
     * @throws ArgumentNotValid If the schedule is null
     * @throws UnknownID If the schedule doesn't exist in the DAO
     * @throws PermissionDenied If the edition of the schedule to update is older than the DAO's
     */
    public synchronized void update(Schedule schedule) {
        ArgumentNotValid.checkNotNull(schedule, "schedule");

        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            if (!exists(c, schedule.getName())) {
                throw new PermissionDenied("No schedule with name " + schedule.getName() + " exists");
            }

            s = c.prepareStatement("UPDATE schedules " + "SET name = ?," + "    comments = ?," + "    startdate = ?,"
                    + "    enddate = ?," + "    maxrepeats = ?," + "    timeunit = ?," + "    numtimeunits = ?,"
                    + "    anytime = ?," + "    onminute = ?," + "    onhour = ?, " + "    ondayofweek = ?,"
                    + "    ondayofmonth = ?," + "    edition = ?" + " WHERE name = ? AND edition = ?");
            setScheduleParameters(s, schedule);
            long newEdition = schedule.getEdition() + 1;
            s.setLong(13, newEdition);
            s.setString(14, schedule.getName());
            s.setLong(15, schedule.getEdition());
            int rows = s.executeUpdate();
            if (rows == 0) {
                String message = "Edition " + schedule.getEdition() + " has expired, cannot update " + schedule;
                log.debug(message);
                throw new PermissionDenied(message);
            }
            schedule.setEdition(newEdition);
        } catch (SQLException e) {
            throw new IOFailure("SQL error while creating schedule " + schedule + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Get iterator to all available schedules.
     *
     * @return iterator to all available schedules
     */
    public synchronized Iterator<Schedule> getAllSchedules() {
        Connection c = HarvestDBConnection.get();
        try {
            List<String> names = DBUtils.selectStringList(c, "SELECT name FROM schedules ORDER BY name");
            return new FilterIterator<String, Schedule>(names.iterator()) {
                /**
                 * Returns the object corresponding to the given object, or null if that object is to be skipped.
                 *
                 * @param s An object in the source iterator domain
                 * @return An object in this iterators domain, or null
                 */
                public Schedule filter(String s) {
                    return read(s);
                }
            };
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public synchronized int getCountSchedules() {
        Connection c = HarvestDBConnection.get();
        try {
            return DBUtils.selectIntValue(c, "SELECT COUNT(*) FROM schedules");
        } finally {
            HarvestDBConnection.release(c);
        }
    }

}
