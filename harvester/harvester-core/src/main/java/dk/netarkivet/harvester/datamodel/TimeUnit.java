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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Enumeration of the possible time units used for frequencies in schedules.
 */
public enum TimeUnit {

    /**
     * 'No time unit' is only included for historic reasons, since 0 did not denote a timeunit.
     */
    NOTIMEUNIT,
    /**
     * Hourly time unit will result in a frequency where only "on minute" will be set, i.e. "on hour", "on day of week"
     * and "on day of month" are unused.
     */
    HOURLY,
    /**
     * Daily time unit will result in a frequency where only "on minute" and "on hour" will be set, i.e.
     * "on day of week" and "on day of month" are unused.
     */
    DAILY,
    /**
     * Weekly time unit will result in a frequency where only "on minute", "on hour" and "on day of week" will be set,
     * i.e. "on day of month" is unused.
     */
    WEEKLY,
    /**
     * Monthly time unit will result in a frequency where only "on minute", "on hour" and "on day of month" will be set,
     * i.e. "on day of week" is unused.
     */
    MONTHLY,
    /**
     * For minute time units, it doesn't make sense to use any additional constraints: "on minute", "on hour",
     * "on day of month" or "on day of week"
     */
    MINUTE;

    /**
     * Helper method that gives a proper object from e.g. a DB-stored value.
     *
     * @param tu a certain integer for a timeunit
     * @return the TimeUnit related to a certain integer
     * @throws ArgumentNotValid If argument tu is invalid (i.e. does not correspond to a TimeUnit)
     */
    public static TimeUnit fromOrdinal(int tu) {
        switch (tu) {
        case 1:
            return HOURLY;
        case 2:
            return DAILY;
        case 3:
            return WEEKLY;
        case 4:
            return MONTHLY;
        case 5:
            return MINUTE;
        default:
            throw new ArgumentNotValid("Invalid time unit " + tu);
        }
    }

}
