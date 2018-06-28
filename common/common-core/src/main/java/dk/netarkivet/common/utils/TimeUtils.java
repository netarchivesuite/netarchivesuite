/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.utils;

import java.util.Calendar;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Various utilities for waiting some time.
 */
public class TimeUtils {

    /** Constant for the number of milliseconds per second: 1000. */
    public static final long SECOND_IN_MILLIS = 1000;
    /** Constant for the number of seconds per minute: 60. */
    public static final long MINUTE_IN_SECONDS = 60;
    /** Constant for the number of minutes per hour: 60. */
    public static final long HOUR_IN_MINUTES = 60;
    /** Constant for the number of hours per day: 24. */
    public static final long DAY_IN_HOURS = 24;
    /** Constant for the number of days per week: 7. */
    public static final long WEEK_IN_DAYS = 7;

    /**
     * Sleep for an exponentially backing off amount of time, in milliseconds. Thus the first attempt will sleep for 1
     * ms, the second for 2, the third for 4, etc.
     *
     * @param attempt The attempt number, which is the log2 of the number of milliseconds spent asleep.
     */
    public static void exponentialBackoffSleep(int attempt) {
        exponentialBackoffSleep(attempt, Calendar.MILLISECOND);
    }

    /**
     * Sleep for an exponentially backing off amount of time. The mode describes the unit of time as defined by @see
     * java.util.Calendar
     *
     * @param attempt The attempt number, which is the log2 of the number of timeunits spent asleep.
     * @param timeunit the specified timeunit in miliseconds
     * @throws ArgumentNotValid if timeunit is unsupported.
     */
    public static void exponentialBackoffSleep(int attempt, int timeunit) {
        ArgumentNotValid.checkNotNegative(attempt, "int attempt");
        ArgumentNotValid.checkTrue(timeunit >= 0 && timeunit < Calendar.FIELD_COUNT,
                "Time unit must be one of the fields defined" + " by Calendar, not " + timeunit);

        Calendar now = Calendar.getInstance();
        long startTime = now.getTimeInMillis();
        now.add(timeunit, 1);
        long endTime = now.getTimeInMillis();
        long multiplyBy = endTime - startTime;

        try {
            Thread.sleep((long) (Math.pow(2, attempt)) * multiplyBy);
        } catch (InterruptedException e) {
            // Early wake-up is not a problem
        }
    }

    /**
     * Method for translating a time in milliseconds to a human readable String. E.g. the argument "604800000" should
     * result in "7 days".
     *
     * @param millis The amount of milliseconds.
     * @return The human readable string.
     */
    public static String readableTimeInterval(long millis) {
        // check whether it is in seconds (if not return in milliseconds).
        if ((millis % SECOND_IN_MILLIS) != 0) {
            if (millis == 1) {
                return millis + " millisecond";
            }
            return millis + " milliseconds";
        }
        long seconds = millis / SECOND_IN_MILLIS;

        // check whether it is in minutes (if not return in seconds).
        if ((seconds % MINUTE_IN_SECONDS) != 0) {
            if (seconds == 1) {
                return seconds + " second";
            }
            return seconds + " seconds";
        }
        long minutes = seconds / MINUTE_IN_SECONDS;

        // check whether it is in hours (if not return in minutes).
        if ((minutes % HOUR_IN_MINUTES) != 0) {
            if (minutes == 1) {
                return minutes + " minute";
            }
            return minutes + " minutes";
        }
        long hours = minutes / HOUR_IN_MINUTES;

        // check whether it is in days (if not return in hours).
        if ((hours % DAY_IN_HOURS) != 0) {
            if (hours == 1) {
                return hours + " hour";
            }
            return hours + " hours";
        }
        long days = hours / DAY_IN_HOURS;

        if ((days % WEEK_IN_DAYS) != 0) {
            if (days == 1) {
                return days + " day";
            }
            return days + " days";
        }
        long weeks = days / WEEK_IN_DAYS;

        if (weeks == 1) {
            return weeks + " week";
        }
        return weeks + " weeks";
    }

}
