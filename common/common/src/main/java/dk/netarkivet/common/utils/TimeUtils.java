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
package dk.netarkivet.common.utils;

import java.util.Calendar;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Various utilities for waiting some time.
 */
public class TimeUtils {
    
    /** Sleep for an exponentially backing off amount of time, in milliseconds.
     * Thus the first attempt will sleep for 1 ms, the second for 2, the third
     * for 4, etc.
     *
     * @param attempt The attempt number, which is the log2 of the number of
     * milliseconds spent asleep.
     */
    public static void exponentialBackoffSleep(int attempt) {
        try {
            Thread.sleep((long) (Math.pow(2, attempt)));
        } catch (InterruptedException e) {
            // Early wake-up is not a problem
        }
    }
    
    /**
     * Sleep for an exponentially backing off amount of time. 
     * The mode describes the unit of time.
     * The supported timeunits is Calendar.MILLISECOND, Calendar.SECOND,
     * Calendar.MINUTE, and Calendar.HOUR.
     * @param attempt The attempt number, which is the log2 of the number of
     * timeunits spent asleep.
     * @param timeunit the specified timeunit
     * @throws ArgumentNotValid if timeunit is unsupported.
     */
    public static void exponentialBackOffSleep(int attempt, int timeunit) {
        long multiplyBy;
        if (timeunit == Calendar.MILLISECOND) {
            multiplyBy = 1L;
        } else if (timeunit == Calendar.SECOND) {
            multiplyBy = 1000L;
        } else if (timeunit == Calendar.MINUTE) {
            multiplyBy = 60000L;
        } else if (timeunit == Calendar.HOUR) {
            multiplyBy = 60000L;
        } else {
            throw new ArgumentNotValid("The selected timeunit '" +  timeunit + "'"
                    + " is not supported.");
        }
        
        try {
            Thread.sleep((long) (Math.pow(2, attempt)) * multiplyBy);
        } catch (InterruptedException e) {
            // Early wake-up is not a problem
        }   
    }
}
