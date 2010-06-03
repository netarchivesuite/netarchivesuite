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

package dk.netarkivet.harvester.datamodel;

import java.util.Locale;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.I18n;

/**
 * Enumeration of the possible states (alt.: status) a Job can be in.
 *
 */
public enum JobStatus {
    
    /**
     * Job status new is used for a job that has been created but not yet 
     * sent to a JMS queue.
     */    
    NEW,
    /**
     * Job status submitted is used for a job that has been sent to a JMS 
     * queue, but not yet picked up by a harvester.
     */    
    SUBMITTED,
    /**
     * Job status started is used for a job that a harvester has started.
     */    
    STARTED,
    /**
     * Job status done is used for a job that a harvester has successfully 
     * finished.
     */    
    DONE,
    /**
     * Job status failed is used for a job that has failed to execute 
     * correctly.
     */    
    FAILED,
    /**
     * Job status resubmitted is used for a job that had failed and a new job
     * with this jobs data has been submitted.
     */    
    RESUBMITTED;

    /** Internationalisation object. */
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    
    /** Constant representing ALL states. */
    public static final int ALL_STATUS_CODE = -1;
    
    /** Localization key for the NEW JobStatus. */
    public static String JOBSTATUS_NEW_KEY = "status.job.new";
    /** Localization key for the SUBMITTED JobStatus. */
    public static String JOBSTATUS_SUBMITTED_KEY = "status.job.submitted";
    /** Localization key for the STARTED JobStatus. */
    public static String JOBSTATUS_STARTED_KEY = "status.job.started";
    /** Localization key for the DONE JobStatus. */
    public static String JOBSTATUS_DONE_KEY = "status.job.done";
    /** Localization key for the FAILED JobStatus. */
    public static String JOBSTATUS_FAILED_KEY = "status.job.failed";
    /** Localization key for the RESUBMITTED JobStatus. */
    public static String JOBSTATUS_RESUBMITTED_KEY = "status.job.resubmitted";
    /** Localization key for a unknown JobStatus. */
    public static String JOBSTATUS_UNKNOWN_KEY = "status.job.unknown";
    
    

    /** Helper method that gives a proper object from e.g. a DB-stored value.
     *
     * @param status a certain integer
     * @return the JobStatus related to a certain integer
     * @throws ArgumentNotValid
     */
    public static JobStatus fromOrdinal(int status) {
        switch (status) {
            case 0: return NEW;
            case 1: return SUBMITTED;
            case 2: return STARTED;
            case 3: return DONE;
            case 4: return FAILED;
            case 5: return RESUBMITTED;
            default: throw new ArgumentNotValid(
                    "Invalid job status '" + status + "'");
        }
    }
    
    /** Helper method that gives a proper object from e.g. a DB-stored value.
    *
    * @param status a status string
    * @return the JobStatus related to a string
    * @throws ArgumentNotValid
    */
   public static JobStatus parse(String status) {
        for (JobStatus s : values()) {
            if (s.name().equals(status)) {
                return s;
            }
        }
        throw new ArgumentNotValid("Invalid job status '" + status + "'");
    }

    /**
     * Return a localized human-readable string describing this status.
     *
     * Strings are read from the harvester translation bundle found in
     * Translation.properties in this module.
     *
     * @param l The locale
     * @return A human readable string for that locale.
     * @throws ArgumentNotValid on null locale.
     */
    public String getLocalizedString(Locale l) {
        ArgumentNotValid.checkNotNull(l, "Locale l");
        switch (this) {
            case NEW:
                return I18N.getString(l, JOBSTATUS_NEW_KEY);
            case SUBMITTED:
                return I18N.getString(l, JOBSTATUS_SUBMITTED_KEY);
            case STARTED:
                return I18N.getString(l, JOBSTATUS_STARTED_KEY);
            case DONE:
                return I18N.getString(l, JOBSTATUS_DONE_KEY);
            case FAILED:
                return I18N.getString(l, JOBSTATUS_FAILED_KEY);
            case RESUBMITTED:
                return I18N.getString(l, JOBSTATUS_RESUBMITTED_KEY);
            default:
                return I18N.getString(l, JOBSTATUS_UNKNOWN_KEY,
                        this.toString());
        }
    }

    /** True if it is legal to change from this status to a new status.
     *
     * @param newStatus a new JobStatus
     * @return true if it is legal to go from the current status 
     * to this new status
     */
    public boolean legalChange(JobStatus newStatus) {
        ArgumentNotValid.checkNotNull(newStatus, "JobStatus newStatus");
        return newStatus.ordinal() >= ordinal();
    }
}
