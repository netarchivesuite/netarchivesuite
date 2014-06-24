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
     * is unused.
     **/
    WEEKLY,
    /** 
     * Monthly time unit will result in a frequency where only "on minute",
     * "on hour" and "on day of month" will be set, i.e. "on day of week" 
     * is unused.
     **/
    MONTHLY,
    /**
     * For minute time units, it doesn't make sense to use any additional
     * constraints: "on minute", "on hour", "on day of month" or "on day of week"
     */
    MINUTE;

    /**
     * Helper method that gives a proper object from e.g. a DB-stored value.
     *
     * @param tu a certain integer for a timeunit
     * @return the TimeUnit related to a certain integer
     * @throws ArgumentNotValid If argument tu is invalid
     * (i.e. does not correspond to a TimeUnit)
     */
    public static TimeUnit fromOrdinal(int tu) {
        switch (tu) {
            case 1: return HOURLY;
            case 2: return DAILY;
            case 3: return WEEKLY;
            case 4: return MONTHLY;
            case 5: return MINUTE;
            default: throw new ArgumentNotValid("Invalid time unit " + tu);
        }
    } 
}
