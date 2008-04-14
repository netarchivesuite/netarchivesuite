/* File:        $Id: JobStatus.java 343 2008-04-11 13:40:55Z elzi $
 * Revision:    $Revision: 343 $
 * Author:      $Author: elzi $
 * Date:        $Date: 2008-04-11 15:40:55 +0200 (Fri, 11 Apr 2008) $
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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Enumeration of the possible time units used for frequencies in schedules.
 */
public enum TimeUnit {
    /** 
     * 'No time unit' is only included for historic reasons, since 0 did not 
     *  denote a timeunit.
     **/    
	NOTIMEUNIT,
    /** 
     * Hourly time unit will result in a frequency where only "on minute" will
     * be set, i.e. "on hour", "on day of week" and "on day of month" are 
     * unused.
     **/
    HOURLY,
    /** 
     * Daily time unit will result in a frequency where only "on minute" and 
     * "on hour" will be set, i.e. "on day of week" and "on day of month" 
     * are unused.
     **/
    DAILY,
    /** 
     * Weekly time unit will result in a frequency where only "on minute",
     * "on hour" and "on day of week" will be set, i.e. "on day of month" 
     * are unused.
     **/
    WEEKLY,
    /** 
     * Monthly time unit will result in a frequency where only "on minute",
     * "on hour" and "on day of month" will be set, i.e. "on day of week" 
     * are unused.
     **/
    MONTHLY;

    /** Helper method that gives a proper object from e.g. a DB-stored value.
     *
     * @param tu a certain integer for a timeunit
     * @return the TimeUnit related to a certain integer
     * @throws ArgumentNotValid
     */
    public static TimeUnit fromOrdinal(int tu) {
        switch (tu) {
            case 1: return HOURLY;
            case 2: return DAILY;
            case 3: return WEEKLY;
            case 4: return MONTHLY;
            default: throw new ArgumentNotValid("Invalid time unit " + tu);
        }
    } 
}
