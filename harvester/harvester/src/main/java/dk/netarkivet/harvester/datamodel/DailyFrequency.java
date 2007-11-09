/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * This class implements a frequency of a number of days.
 *
 */

public class DailyFrequency extends Frequency {
    /** The minute of the hour the event should happen at.*/
    private int minute;
    /** The hour of the day the event should happen at.*/
    private int hour;

    /** Create a new daily frequency that happens every numUnits days, anytime.
     *
     * @param numUnits Number of days from event to event.
     * @throws ArgumentNotValid if numUnits if 0 or negative
     */
    public DailyFrequency(int numUnits) {
        super(numUnits, true);
    }

    /** Create a new daily frequency that happens every numUnits days, on
     * the given hour and minute.
     *
     * @param numUnits Number of days from event to event.
     * @param hour The hour on which the event should happen.
     * @param minute The minute of hour on which the event should happen.
     * @throws ArgumentNotValid if numUnits if 0 or negative
     * or hours is <0 or >23 or minutes is <0 or >59
     */
    public DailyFrequency(int numUnits, int hour, int minute) {
        super(numUnits, false);
        Calendar cal = GregorianCalendar.getInstance();
        if (hour < cal.getMinimum(Calendar.HOUR_OF_DAY)
                || hour > cal.getMaximum(Calendar.HOUR_OF_DAY)) {
            throw new ArgumentNotValid("Hour of day must be in legal range '"
                                       + cal.getMinimum(Calendar.HOUR_OF_DAY)
                                       + "' to '"
                                       + cal.getMaximum(Calendar.HOUR_OF_DAY)
                                       + "'");
        }
        if (minute < cal.getMinimum(Calendar.MINUTE)
                || minute > cal.getMaximum(Calendar.MINUTE)) {
            throw new ArgumentNotValid("Minute must be in legal range '"
                                       + cal.getMinimum(Calendar.MINUTE)
                                       + "' to '"
                                       + cal.getMaximum(Calendar.MINUTE)
                                       + "'");
        }

        this.minute = minute;
        this.hour = hour;
    }

    /**
     * Given when the last event happened, tell us when the next event should
     * happen (even if the new event is in the past).
     *
     * The time of the next event is guaranteed to be later that lastEvent.
     * For certain frequencies (e.g. once a day, any time of day), the time
     * of the next event is derived from lastEvent, for others (e.g. once a day
     * at 13:00) the time of the next event is the first matching time after
     * lastEvent.
     *
     * @param lastEvent A time from which the next event should be calculated.
     * @return At what point the event should happen next.
     */
    public Date getNextEvent(Date lastEvent) {
        ArgumentNotValid.checkNotNull(lastEvent, "lastEvent");

        Calendar last = new GregorianCalendar();
        last.setTime(getFirstEvent(lastEvent));
        last.add(Calendar.DAY_OF_YEAR, getNumUnits());
        return getFirstEvent(last.getTime());
    }

    /**
     * Given a starting time, tell us when the first event should happen.
     *
     * @param startTime The earliest time the event can happen.
     * @return At what point the event should happen the first time.
     */
    public Date getFirstEvent(Date startTime) {
        ArgumentNotValid.checkNotNull(startTime, "startTime");

        if (isAnytime()) {
            return startTime;
        }
        Calendar start = new GregorianCalendar();
        start.setTime(startTime);
        start.set(Calendar.MINUTE, minute);
        start.set(Calendar.HOUR_OF_DAY, hour);
        if (start.getTime().before(startTime)) {
            start.add(Calendar.DAY_OF_YEAR, 1);
        }
        return start.getTime();
    }

    /** If not anytime, the minute at which events should start.
     * @return the minute
     */
    public int getMinute() {
        return minute;
    }

    /** If not anytime, the hour at which events should start.
     * @return the hour
     */
    public int getHour() {
        return hour;
    }

    /**
     * Autogenerated equals.
     * @param o The object to compare with
     * @return Whether objects are equal
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyFrequency)) return false;
        if (!super.equals(o)) return false;

        final DailyFrequency dailyFrequency = (DailyFrequency) o;

        if (isAnytime()) return true;

        if (hour != dailyFrequency.hour) return false;
        if (minute != dailyFrequency.minute) return false;

        return true;
    }

    /**
     * Autogenerated hashcode method.
     * @return the hashcode
     */
    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + minute;
        result = 29 * result + hour;
        return result;
    }

    /**
     * Return the exact minute event should happen on, or null if this is
     * an anyTime event or doesn't define what minute it should happen on.
     *
     * @return the exact minute event should happen on, or null if this is
     * an anyTime event or doesn't define what minute it should happen on
     */
    public Integer getOnMinute() {
        if (!isAnytime()) {
            return minute;
        }
        return null;
    }

    /**
     * Return the exact hour event should happen on, or null if this is
     * an anyTime event or doesn't define what hour it should happen on.
     *
     * @return the exact hour event should happen on, or null if this is
     * an anyTime event or doesn't define what hour it should happen on
     */
    public Integer getOnHour() {
        if (!isAnytime()) {
            return hour;
        }
        return null;
    }

    /**
     * Return the exact day of week event should happen on, or null if this is
     * an anyTime event or doesn't define what day of week it should happen on.
     *
     * @return the exact day of week event should happen on, or null if this is
     * an anyTime event or doesn't define what day of week it should happen on
     */
    public Integer getOnDayOfWeek() {
        return null;
    }

    /**
     * Return the exact day of month event should happen on, or null if this is
     * an anyTime event or doesn't define what day of month it should happen on.
     *
     * @return the exact day of month event should happen on, or null if this is
     * an anyTime event or doesn't define what day of month it should happen on
     */
    public Integer getOnDayOfMonth() {
        return null;
    }

    /**
     * Return an integer that can be used to identify the kind of freqency.
     * No two subclasses should use the same integer
     *
     * @return an integer that can be used to identify the kind of freqency
     */
    public int ordinal() {
        return 2;
    }

    /** Human readable represenation of this object.
     *
     * @return Human readble representation
     */
    public String toString() {
        if (isAnytime()) {
            return "every " + getNumUnits() + " days";
        }
        return "every " + getNumUnits() + " days at " + hour + ":" + minute;
    }
}
