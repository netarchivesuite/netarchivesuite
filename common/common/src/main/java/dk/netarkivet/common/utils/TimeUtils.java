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
package dk.netarkivet.common.utils;

import java.util.Calendar;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Various utilities for waiting some time.
 */
public class TimeUtils {

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_SECONDS = 60;
    public static final long HOUR_IN_MINUTES = 60;
    public static final long DAY_IN_HOURS = 24;
    
    
    /** Sleep for an exponentially backing off amount of time, in milliseconds.
     * Thus the first attempt will sleep for 1 ms, the second for 2, the third
     * for 4, etc.
     *
     * @param attempt The attempt number, which is the log2 of the number of
     * milliseconds spent asleep.
     */
    public static void exponentialBackoffSleep(int attempt) {
        exponentialBackoffSleep(attempt, Calendar.MILLISECOND);
    }

    /**
     * Sleep for an exponentially backing off amount of time.
     * The mode describes the unit of time as defined by @see java.util.Calendar
     * @param attempt The attempt number, which is the log2 of the number of
     * timeunits spent asleep.
     * @param timeunit the specified timeunit in miliseconds
     * @throws ArgumentNotValid if timeunit is unsupported.
     */
    public static void exponentialBackoffSleep(int attempt, int timeunit) {
        ArgumentNotValid.checkNotNegative(attempt, "int attempt");
        ArgumentNotValid.checkTrue(timeunit >= 0
                                   && timeunit < Calendar.FIELD_COUNT,
                                   "Time unit must be one of the fields defined"
                                   + " by Calendar, not " + timeunit);

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
     * Method for translating a time in milliseconds to a human readable String.
     * E.g. the argument "604800000" should result in "7 days".
     * 
     * TODO handle larger than days.
     *  
     * @param millis The amount of milliseconds.
     * @return The human readable string. 
     */
    public static String readableTimeInterval(long millis) {
        // check whether it is in seconds (if not return in milliseconds).
        if((millis % SECOND_IN_MILLIS) != 0) {
            return millis + " milliseconds";
        }
        long seconds = millis/SECOND_IN_MILLIS;
        
        // check whether it is in minutes (if not return in seconds).
        if((seconds % MINUTE_IN_SECONDS) != 0) {
            return seconds + " seconds";
        }
        long minutes = seconds/MINUTE_IN_SECONDS;

        // check whether it is in hours (if not return in minutes).
        if((minutes % HOUR_IN_MINUTES) != 0) {
            return minutes + " minutes";
        }
        long hours = minutes/HOUR_IN_MINUTES;

        // check whether it is in days (if not return in hours).
        if((hours % DAY_IN_HOURS) != 0) {
            return hours + " hours";
        }
        long days = hours/DAY_IN_HOURS;

        return days + " days";
    }
}
