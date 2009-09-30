/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * This class implements a frequency of a number of months.
 *
 */

public class MonthlyFrequency extends Frequency {
    /** The minute of the hour the event should happen at.*/
    private int minute;
    /** The hour of the day the event should happen at.*/
    private int hour;
    /** The day of the month the event should happen at.*/
    private int dayOfMonth;

    /** Create a new monthly frequency that happens every
     * numUnits month, anytime.
     *
     * @param numUnits Number of days from event to event.
     * @throws ArgumentNotValid if numUnits if 0 or negative
     */
    public MonthlyFrequency(int numUnits) {
        super(numUnits, true);
    }

    /** Create a new monthly frequency that happens every numUnits month, on
     * the given day of month, hour and minute.
     *
     * @param numUnits Number of days from event to event.
     * @param dayOfMonth The day of the month the event should happen.  The
     * month starts on day 1.
     * @param hour The hour on which the event should happen.
     * @param minute The minute of hour on which the event should happen.
     * @throws ArgumentNotValid if numUnits if 0 or negative
     * or dayOfMonth <1 or >31
     * or hour is <0 or >23 or minutes is <0 or >59
     */
    public MonthlyFrequency(int numUnits, int dayOfMonth, int hour, int minute) {
        super(numUnits, false);

        Calendar cal = GregorianCalendar.getInstance();
        if (dayOfMonth < cal.getMinimum(Calendar.DAY_OF_MONTH)
            || dayOfMonth > cal.getMaximum(Calendar.DAY_OF_MONTH)) {
            throw new ArgumentNotValid("Day of month must be in legal range '"
                                       + cal.getMinimum(Calendar.DAY_OF_MONTH)
                                       + "' to '"
                                       + cal.getMaximum(Calendar.DAY_OF_MONTH)
                                       + "'");
        }
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
        this.dayOfMonth = dayOfMonth;
        this.hour = hour;
        this.minute = minute;
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
        //Note: If the dayOfMonth becomes impossible by this addition, it is
        //set back to the maximum possible date for this month
        last.add(Calendar.MONTH, getNumUnits());
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
        // set day in month, to the given value if possible, or maximum
        start.set(Calendar.DAY_OF_MONTH,
                Math.min(start.getActualMaximum(Calendar.DAY_OF_MONTH),
                        dayOfMonth));
        if (start.getTime().before(startTime)) {
            start.add(Calendar.MONTH, 1);
        }
        // reset day in month, the last day might be later in this month
        start.set(Calendar.DAY_OF_MONTH,
                Math.min(start.getActualMaximum(Calendar.DAY_OF_MONTH),
                        dayOfMonth));
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

    /** If not anytime, the day in the month at which events should start.
     * @return the day
     */
    public int getDayOfMonth() {
        return dayOfMonth;
    }

    /**
     * Autogenerated equals.
     * @param o The object to compare with
     * @return Whether objects are equal
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonthlyFrequency)) return false;
        if (!super.equals(o)) return false;

        final MonthlyFrequency monthlyFrequency = (MonthlyFrequency) o;

        if (isAnytime()) return true;

        if (dayOfMonth != monthlyFrequency.dayOfMonth) return false;
        if (hour != monthlyFrequency.hour) return false;
        if (minute != monthlyFrequency.minute) return false;

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
        result = 29 * result + dayOfMonth;
        return result;
    }

    /**
     * Return the exact minute event should happen on, or null if this is
     * an anyTime event or doesn't define what minute it should happen on.
     *
     * @return the exact minute event should happen on
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
     * @return the exact hour event should happen on
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
     * @return the exact day of week event should happen on
     */
    public Integer getOnDayOfWeek() {
        return null;
    }

    /**
     * Return the exact day of month event should happen on, or null if this is
     * an anyTime event or doesn't define what day of month it should happen on.
     *
     * @return the exact day of month event should happen on
     */
    public Integer getOnDayOfMonth() {
        if (!isAnytime()) {
            return dayOfMonth;
        }
        return null;
    }

    /**
     * Return an integer that can be used to identify the kind of frequency.
     * No two subclasses should use the same integer
     *
     * @return an integer that can be used to identify the kind of frequency
     */
    public int ordinal() {
        return TimeUnit.MONTHLY.ordinal();
    }

    /** Human readable representation of this object.
     *
     * @return Human readable representation
     */
    public String toString() {
        if (isAnytime()) {
            return "every " + getNumUnits() + " months";
        }
        return "every " + getNumUnits() + " months, on day " + dayOfMonth
                   + " at " + hour + ":" + minute;
    }
}
